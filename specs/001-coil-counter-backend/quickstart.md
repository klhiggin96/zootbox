# Quickstart: ZootBox Coil-Counter Backend

**Feature**: `001-coil-counter-backend` | **Date**: 2025-12-27 | **Spec**: [spec.md](./spec.md)

This guide will get you from zero to a running backend service in under 10 minutes.

---

## Prerequisites

**System Requirements**:
- **Go 1.21+** (download from https://go.dev/dl/)
- **SQLite3 CLI** (optional, for inspecting database: `sudo apt-get install sqlite3`)
- **Git** (for cloning repository)
- **Make** (for build automation: `sudo apt-get install build-essential`)

**For Cross-Compilation to ARM64 Android Tablet**:
- **Android SDK Platform Tools** (for `adb`: https://developer.android.com/studio/releases/platform-tools)
- **USB Cable** (to connect development machine to Android tablet)

**Hardware Requirements** (for full integration testing):
- Android tablet running Android 8.0+ with USB debugging enabled
- Motor controller connected to `/dev/ttyUSB0` (or configure alternative device path)

---

## Quick Start (Local Development)

### 1. Clone Repository

```bash
cd ~/dev  # Or your preferred directory
git clone <repository-url>
cd Backend
```

### 2. Install Go Dependencies

```bash
# Initialize Go module (if not already done)
go mod init github.com/zootbox/backend

# Install dependencies
go get github.com/go-chi/chi/v5
go get github.com/mattn/go-sqlite3
go get github.com/tarm/serial
go get github.com/rs/zerolog
go get github.com/stretchr/testify

# Tidy up dependencies
go mod tidy
```

### 3. Set Up Database

```bash
# Create data directory
mkdir -p /tmp/zootbox

# Initialize database with schema (manual approach)
sqlite3 /tmp/zootbox/inventory.db < internal/db/migrations/001_init_schema.sql
sqlite3 /tmp/zootbox/inventory.db < internal/db/migrations/002_add_version_column.sql

# OR use automated migration runner (once implemented)
go run cmd/server/main.go --migrate
```

**Verify Database**:
```bash
sqlite3 /tmp/zootbox/inventory.db
sqlite> .mode column
sqlite> SELECT * FROM coils LIMIT 10;
sqlite> .quit
```

You should see 100 coil records (A1 to J10) with inventory=10 and status='available'.

### 4. Run Backend Service

```bash
# Development mode (verbose logging)
go run cmd/server/main.go --log-level debug

# OR use Makefile
make run
```

**Expected Output**:
```
{"level":"info","time":"2025-12-27T10:30:00Z","message":"backend_startup","version":"0.1.0"}
{"level":"info","time":"2025-12-27T10:30:00Z","message":"database_connected","path":"/tmp/zootbox/inventory.db"}
{"level":"info","time":"2025-12-27T10:30:00Z","message":"http_server_listening","addr":"127.0.0.1:8080"}
```

### 5. Test API Endpoints

Open a new terminal and run:

```bash
# Health check
curl http://localhost:8080/health

# Get all coil inventory
curl http://localhost:8080/api/v1/coils | jq

# Get single coil
curl http://localhost:8080/api/v1/coils/A5 | jq

# Execute vend operation (requires motor controller)
curl -X POST http://localhost:8080/api/v1/vend \
  -H "Content-Type: application/json" \
  -d '{"coil_id": "A5"}'

# Check metrics
curl http://localhost:8080/metrics
```

---

## Development Workflow

### Project Structure Navigation

```text
Backend/
├── cmd/server/main.go          # Start here - entry point
├── internal/
│   ├── api/                    # HTTP layer
│   │   ├── router.go           # Route definitions
│   │   └── handlers/           # Request handlers
│   ├── services/               # Business logic
│   ├── db/                     # Database layer
│   │   ├── repositories/       # Data access
│   │   └── migrations/         # SQL migrations
│   └── hardware/               # Motor controller interface
└── tests/                      # Test suites
```

### Running Tests

```bash
# Run all tests
make test

# Run unit tests only
go test ./tests/unit/... -v

# Run integration tests (requires SQLite)
go test ./tests/integration/... -v

# Run with coverage
go test ./... -coverprofile=coverage.out
go tool cover -html=coverage.out
```

### Code Quality Checks

```bash
# Format code
go fmt ./...

# Lint (install golangci-lint first: https://golangci-lint.run/usage/install/)
golangci-lint run

# Static analysis
go vet ./...
```

---

## Cross-Compilation for ARM64 Android Tablet

### Build ARM64 Binary

```bash
# Manual build
CGO_ENABLED=1 GOOS=linux GOARCH=arm64 \
  CC=aarch64-linux-gnu-gcc \
  go build -o build/zootbox-backend-arm64 \
  -ldflags="-s -w" \
  cmd/server/main.go

# OR use build script
./scripts/build_arm64.sh
```

**Prerequisites for Cross-Compilation**:
```bash
# Ubuntu/Debian
sudo apt-get install gcc-aarch64-linux-gnu

# macOS (requires Docker)
# Use the provided Dockerfile in scripts/Dockerfile.arm64
docker build -f scripts/Dockerfile.arm64 -t zootbox-builder .
docker run --rm -v $(pwd):/workspace zootbox-builder
```

### Deploy to Android Tablet

```bash
# Connect tablet via USB and enable USB debugging

# Verify connection
adb devices

# Push binary and database to tablet
./scripts/install_tablet.sh

# Manual deployment steps:
adb push build/zootbox-backend-arm64 /data/local/tmp/zootbox/backend
adb push /tmp/zootbox/inventory.db /data/local/tmp/zootbox/inventory.db
adb shell chmod +x /data/local/tmp/zootbox/backend

# Run backend on tablet
adb shell /data/local/tmp/zootbox/backend --log-level info
```

### Access Backend from Android App

The Android app (MyApplication/) will connect to the backend via:
```
http://127.0.0.1:8080/api/v1/*
```

No changes needed in Android app code - it's already configured to use localhost.

---

## Configuration

### Environment Variables

```bash
# HTTP server configuration
export HTTP_PORT=8080
export HTTP_HOST=127.0.0.1

# Database configuration
export DB_PATH=/data/local/tmp/zootbox/inventory.db
export DB_WAL_MODE=true
export DB_BUSY_TIMEOUT=5000

# Motor controller configuration
export MOTOR_SERIAL_PORT=/dev/ttyUSB0
export MOTOR_BAUD_RATE=115200
export MOTOR_TIMEOUT_MS=1000

# Monitoring system configuration
export MONITORING_URL=https://monitoring.example.com/alerts
export MONITORING_AUTH=base64encodedcredentials

# Logging configuration
export LOG_LEVEL=info  # debug, info, warn, error
export LOG_FORMAT=json  # json or text
```

### Configuration File (Alternative)

Create `config.yaml` in project root:
```yaml
http:
  host: 127.0.0.1
  port: 8080

database:
  path: /data/local/tmp/zootbox/inventory.db
  wal_mode: true
  busy_timeout: 5000

motor:
  serial_port: /dev/ttyUSB0
  baud_rate: 115200
  timeout_ms: 1000

monitoring:
  url: https://monitoring.example.com/alerts
  auth: base64encodedcredentials

logging:
  level: info
  format: json
```

Load config in main.go:
```go
cfg, err := config.LoadFromFile("config.yaml")
```

---

## Development Tips

### Mock Motor Controller for Testing

For local development without hardware:

```go
// internal/hardware/motor_controller.go
type MockMotorController struct{}

func (m *MockMotorController) Activate(coilID string) error {
    log.Info().Str("coil_id", coilID).Msg("mock_motor_activation")
    time.Sleep(50 * time.Millisecond)  // Simulate motor spin time
    return nil
}
```

Set environment variable to enable mock mode:
```bash
export MOTOR_MOCK=true
go run cmd/server/main.go
```

### Structured Logging Best Practices

```go
import "github.com/rs/zerolog/log"

// Add correlation ID from request context
correlationID := chi.URLParam(r, "X-Correlation-ID")

log.Info().
    Str("correlation_id", correlationID).
    Str("coil_id", "A5").
    Int("inventory", 9).
    Dur("latency", time.Since(start)).
    Msg("vend_success")
```

### Debugging Database Issues

```bash
# Check WAL mode is enabled
sqlite3 /tmp/zootbox/inventory.db "PRAGMA journal_mode;"
# Should output: wal

# Monitor database file size
watch -n 1 ls -lh /tmp/zootbox/inventory.db*

# Check for locked database
sqlite3 /tmp/zootbox/inventory.db "PRAGMA busy_timeout;"
# Should output: 5000

# Manually checkpoint WAL (merge WAL into main DB)
sqlite3 /tmp/zootbox/inventory.db "PRAGMA wal_checkpoint(FULL);"
```

### Performance Profiling

```bash
# Enable CPU profiling
go run cmd/server/main.go --cpuprofile=cpu.prof

# Generate load (requires hey: https://github.com/rakyll/hey)
hey -n 10000 -c 10 http://localhost:8080/api/v1/coils

# Analyze profile
go tool pprof cpu.prof
(pprof) top10
(pprof) web  # Generate visual graph

# Memory profiling
go run cmd/server/main.go --memprofile=mem.prof
go tool pprof mem.prof
```

---

## Troubleshooting

### Issue: `cannot find package "github.com/mattn/go-sqlite3"`

**Solution**: CGO must be enabled for SQLite driver:
```bash
export CGO_ENABLED=1
go get github.com/mattn/go-sqlite3
```

### Issue: `database is locked`

**Cause**: Another process has exclusive lock on database.

**Solution**:
```bash
# Check for stale locks
lsof /tmp/zootbox/inventory.db

# Kill processes holding lock
kill -9 <PID>

# OR increase busy timeout
export DB_BUSY_TIMEOUT=10000  # 10 seconds
```

### Issue: `permission denied: /dev/ttyUSB0`

**Cause**: User lacks permission to access USB serial device.

**Solution**:
```bash
# Add user to dialout group (Linux)
sudo usermod -a -G dialout $USER
newgrp dialout

# OR change device permissions (temporary)
sudo chmod 666 /dev/ttyUSB0
```

### Issue: ARM64 binary fails with `exec format error`

**Cause**: Binary compiled for wrong architecture.

**Solution**:
```bash
# Verify binary architecture
file build/zootbox-backend-arm64
# Should output: ELF 64-bit LSB executable, ARM aarch64

# Recompile with correct flags
CGO_ENABLED=1 GOOS=linux GOARCH=arm64 go build ...
```

### Issue: High memory usage (>30MB)

**Diagnosis**:
```bash
# Check memory usage on Android tablet
adb shell ps -o PID,RSS,CMD | grep backend
```

**Solutions**:
1. Reduce GOGC value (more aggressive garbage collection):
   ```bash
   export GOGC=50  # Default is 100
   ```

2. Profile memory allocations:
   ```bash
   go tool pprof -alloc_space mem.prof
   ```

3. Check for goroutine leaks:
   ```bash
   curl http://localhost:8080/debug/pprof/goroutine?debug=2
   ```

### Issue: Slow API responses (>100ms)

**Diagnosis**:
```bash
# Enable request timing logs
export LOG_LEVEL=debug

# Check database query times
sqlite3 /tmp/zootbox/inventory.db
sqlite> EXPLAIN QUERY PLAN SELECT * FROM coils WHERE id = 'A5';
```

**Solutions**:
1. Add missing indexes (see data-model.md for index definitions)
2. Enable SQLite query optimization:
   ```sql
   PRAGMA optimize;
   ```
3. Profile slow handlers:
   ```go
   defer func(start time.Time) {
       log.Debug().Dur("latency", time.Since(start)).Msg("handler_complete")
   }(time.Now())
   ```

---

## Next Steps

1. **Implement Feature**: Follow tasks.md (generated by `/speckit.tasks` command)
2. **API Contract Testing**: Validate OpenAPI spec against implementation
   ```bash
   # Use openapi-validator
   npm install -g @stoplight/spectral-cli
   spectral lint specs/001-coil-counter-backend/contracts/openapi.yaml
   ```
3. **Integration with Android App**: Test end-to-end vend flow from ProductDetailActivity.kt
4. **Production Deployment**: Configure systemd service on Android tablet for auto-start
5. **Monitoring Setup**: Integrate with external monitoring system for low-stock alerts

---

## Additional Resources

- **Go Best Practices**: https://go.dev/doc/effective_go
- **Chi Router Documentation**: https://github.com/go-chi/chi
- **SQLite Performance Tuning**: https://www.sqlite.org/pragma.html
- **Zerolog Examples**: https://github.com/rs/zerolog#examples
- **Cross-Compilation Guide**: https://golang.org/doc/install/source#environment

For detailed architecture decisions, see [research.md](./research.md).
For database schema details, see [data-model.md](./data-model.md).
For API contracts, see [contracts/openapi.yaml](./contracts/openapi.yaml).
