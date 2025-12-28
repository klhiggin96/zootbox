# Memory Optimization Guide

ZootBox Backend is designed for resource-constrained Android tablets. This guide explains memory profiling, optimization techniques, and best practices.

## Memory Budget

**Target**: ≤30MB RSS under typical load

Breakdown:
- Go runtime: ~8-10MB
- Database connections: ~2-5MB
- HTTP server: ~3-5MB
- Application logic: ~5-10MB
- Headroom: ~5-10MB

## Profiling Tools

### 1. Built-in Memory Profiling

```bash
# Start server with memory profiling
go run cmd/server/main.go --memprofile=mem.prof

# Stop server (Ctrl+C) to write profile

# Analyze profile
go tool pprof mem.prof

# Interactive commands in pprof:
(pprof) top10        # Top 10 memory consumers
(pprof) list main    # Source code with allocations
(pprof) web          # Visual graph (requires graphviz)
```

### 2. CPU Profiling

```bash
# Profile CPU usage for performance optimization
go run cmd/server/main.go --cpuprofile=cpu.prof

# Run load tests, then stop server

# Analyze
go tool pprof cpu.prof
```

### 3. Live Memory Monitoring

```bash
# Enable periodic memory logging
go run cmd/server/main.go --memmonitor

# Logs memory stats every 30 seconds
# Alerts if usage exceeds 25MB threshold
```

### 4. Runtime Metrics via /metrics Endpoint

```bash
# Prometheus-format metrics including memory
curl http://localhost:8080/metrics | grep memory

# Key metrics:
# - zootbox_memory_alloc_bytes
# - zootbox_memory_sys_bytes
# - zootbox_memory_heap_objects
# - zootbox_goroutines
```

## Optimization Techniques

### Database Connection Pooling

Already implemented via `sql.DB`:

```go
// internal/db/db.go
db.SetMaxOpenConns(5)      // Limit concurrent connections
db.SetMaxIdleConns(2)      // Keep 2 idle connections
db.SetConnMaxLifetime(1 * time.Hour)
```

**Memory saved**: ~1-2MB per avoided connection

### Query Result Streaming

For large result sets, use `rows.Next()` iteration instead of loading all results:

```go
// Good: Streaming (low memory)
rows, _ := db.Query("SELECT * FROM transactions")
defer rows.Close()
for rows.Next() {
    var tx Transaction
    rows.Scan(&tx.ID, &tx.CoilID, ...)
    // Process one row at a time
}

// Avoid: Loading all rows into memory at once
var allTx []Transaction
rows, _ := db.Query("SELECT * FROM transactions")
for rows.Next() {
    // Builds up memory as result set grows
}
```

**Memory saved**: Scales with result set size

### String Builder for Concatenation

```go
// Good: strings.Builder (pre-allocated buffer)
var b strings.Builder
b.Grow(256) // Pre-allocate if size known
for _, metric := range metrics {
    b.WriteString(metric)
}
result := b.String()

// Avoid: String concatenation (creates temporary strings)
result := ""
for _, metric := range metrics {
    result += metric  // New allocation each iteration
}
```

**Memory saved**: ~50-80% for large concatenations

### Pointer vs Value Receivers

```go
// Use pointer receivers for structs > 64 bytes
type LargeStruct struct {
    Data [1000]byte
}

// Good: Pointer receiver (no copy)
func (s *LargeStruct) Process() {}

// Avoid: Value receiver (copies 1KB each call)
func (s LargeStruct) Process() {}
```

**Memory saved**: Struct size × call frequency

### JSON Encoding Optimization

```go
// Good: Stream directly to http.ResponseWriter
func handler(w http.ResponseWriter, r *http.Request) {
    data := fetchData()
    json.NewEncoder(w).Encode(data)  // Streams output
}

// Avoid: Intermediate buffer
func handler(w http.ResponseWriter, r *http.Request) {
    data := fetchData()
    jsonBytes, _ := json.Marshal(data)  // Full JSON in memory
    w.Write(jsonBytes)
}
```

**Memory saved**: Size of JSON payload

## Monitoring Best Practices

### 1. Regular Memory Profiling

```bash
# Weekly profiling during development
./scripts/profile_memory.sh

# Compare profiles over time
go tool pprof -base=mem_baseline.prof mem_current.prof
```

### 2. Leak Detection

```bash
# Monitor goroutine count
curl http://localhost:8080/metrics | grep goroutines

# Should remain stable over time
# Increasing count = potential goroutine leak
```

### 3. Garbage Collection Tuning

```bash
# Default: GC when heap grows 100%
GOGC=100 ./zootbox-backend

# Lower value = more frequent GC, lower memory
GOGC=50 ./zootbox-backend

# Higher value = less frequent GC, higher throughput
GOGC=200 ./zootbox-backend

# Disable GC (testing only)
GOGC=off ./zootbox-backend
```

### 4. GC Trace Logging

```bash
# Enable GC trace for detailed analysis
GODEBUG=gctrace=1 ./zootbox-backend

# Output format:
# gc # @#s #%: #+#+# ms clock, #+#/#+# ms cpu, #->#-># MB, # MB goal, # P
#        │    │    │         │          heap    heap  target
#        │    │    │         │          before  after
#        │    │    │         CPU time
#        │    │    Wall clock time
#        │    Heap growth %
#        GC number
```

## Deployment Optimizations

### Build Flags

```bash
# Strip debug symbols and optimize binary size
go build -ldflags="-w -s" -trimpath ./cmd/server

# -w: Omit DWARF debug info
# -s: Omit symbol table
# -trimpath: Remove absolute paths

# Result: ~30-40% smaller binary
```

### Runtime Environment

```bash
# Tablet deployment settings
export GOMEMLIMIT=30MiB   # Hard memory limit (Go 1.19+)
export GOGC=50            # Aggressive GC for low memory
export GOMAXPROCS=2       # Limit to 2 CPU cores

./zootbox-backend --memmonitor
```

## Troubleshooting

### Memory Usage Exceeds 30MB

1. **Check metrics endpoint**:
   ```bash
   curl http://localhost:8080/metrics | grep memory
   ```

2. **Capture memory profile**:
   ```bash
   pkill -USR1 zootbox-backend  # Trigger mem profile
   go tool pprof mem.prof
   (pprof) top10
   ```

3. **Check for leaks**:
   ```bash
   # Goroutine count increasing?
   watch -n 5 'curl -s http://localhost:8080/metrics | grep goroutines'
   ```

4. **Review logs**:
   ```bash
   grep "Memory" backend.log
   ```

### OOM (Out of Memory) Crashes

1. Enable memory monitoring:
   ```bash
   ./zootbox-backend --memmonitor --log-level=debug
   ```

2. Reduce GOGC to force more frequent GC:
   ```bash
   GOGC=30 ./zootbox-backend
   ```

3. Check database query result sizes:
   ```sql
   -- Ensure queries don't return huge result sets
   SELECT COUNT(*) FROM transactions;  -- Should be reasonable
   ```

## Performance vs Memory Tradeoffs

| Optimization | Memory Impact | CPU Impact | Recommendation |
|--------------|---------------|------------|----------------|
| GOGC=50 | -20% | +10% | ✓ Use on tablet |
| GOGC=200 | +30% | -5% | ✗ Avoid on tablet |
| MaxOpenConns=5 | -2MB | +5% | ✓ Use on tablet |
| MaxOpenConns=25 | +8MB | -2% | ✗ Avoid on tablet |
| --memmonitor | +1MB | +2% | ✓ Use during dev |
| Streaming JSON | -50% | +5% | ✓ Always use |

## Automated Monitoring

### Systemd Service with Memory Limits

```ini
# /etc/systemd/system/zootbox-backend.service
[Service]
MemoryMax=30M
MemoryHigh=25M
Environment="GOGC=50"
Environment="GOMEMLIMIT=30MiB"
```

### Prometheus Alerts

```yaml
# prometheus-alerts.yml
groups:
  - name: zootbox
    rules:
      - alert: HighMemoryUsage
        expr: zootbox_memory_alloc_bytes > 28000000
        for: 5m
        annotations:
          summary: "Memory usage above 28MB"

      - alert: GoroutineLeak
        expr: delta(zootbox_goroutines[1h]) > 10
        annotations:
          summary: "Goroutine count increasing"
```

## References

- Go Memory Management: https://go.dev/doc/gc-guide
- pprof Tutorial: https://go.dev/blog/pprof
- SQLite Memory Management: https://sqlite.org/malloc.html
- Prometheus Best Practices: https://prometheus.io/docs/practices/

## Quick Commands Cheat Sheet

```bash
# Profile memory usage
go run cmd/server/main.go --memprofile=mem.prof

# Analyze profile
go tool pprof mem.prof

# Monitor live metrics
watch -n 2 'curl -s http://localhost:8080/metrics | grep -E "memory|goroutines"'

# GC tracing
GODEBUG=gctrace=1 ./zootbox-backend 2>&1 | grep gc

# Force GC (via signal)
pkill -SIGUSR1 zootbox-backend

# Memory-constrained deployment
GOGC=50 GOMEMLIMIT=30MiB ./zootbox-backend --memmonitor
```
