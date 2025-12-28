package profiling

import (
	"fmt"
	"os"
	"runtime"
	"runtime/pprof"
	"time"

	"github.com/rs/zerolog/log"
)

// MemoryProfile captures a memory profile snapshot
func MemoryProfile(filename string) error {
	f, err := os.Create(filename)
	if err != nil {
		return fmt.Errorf("failed to create memory profile file: %w", err)
	}
	defer f.Close()

	runtime.GC() // Force garbage collection for accurate profile

	if err := pprof.WriteHeapProfile(f); err != nil {
		return fmt.Errorf("failed to write memory profile: %w", err)
	}

	log.Info().Str("file", filename).Msg("Memory profile written")
	return nil
}

// CPUProfile starts CPU profiling to a file
func StartCPUProfile(filename string) (func(), error) {
	f, err := os.Create(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to create CPU profile file: %w", err)
	}

	if err := pprof.StartCPUProfile(f); err != nil {
		f.Close()
		return nil, fmt.Errorf("failed to start CPU profiling: %w", err)
	}

	log.Info().Str("file", filename).Msg("CPU profiling started")

	// Return cleanup function
	stopFunc := func() {
		pprof.StopCPUProfile()
		f.Close()
		log.Info().Str("file", filename).Msg("CPU profiling stopped")
	}

	return stopFunc, nil
}

// MemoryStats represents memory usage statistics
type MemoryStats struct {
	Alloc        uint64 // Bytes allocated and in use
	TotalAlloc   uint64 // Total bytes allocated (even if freed)
	Sys          uint64 // Bytes obtained from system
	NumGC        uint32 // Number of completed GC cycles
	HeapAlloc    uint64 // Bytes allocated on heap
	HeapInuse    uint64 // Bytes in use on heap
	HeapObjects  uint64 // Total number of allocated heap objects
	StackInuse   uint64 // Bytes in use by stack
	AllocMB      float64
	SysMB        float64
	HeapAllocMB  float64
}

// GetMemoryStats retrieves current memory statistics
func GetMemoryStats() *MemoryStats {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	return &MemoryStats{
		Alloc:        m.Alloc,
		TotalAlloc:   m.TotalAlloc,
		Sys:          m.Sys,
		NumGC:        m.NumGC,
		HeapAlloc:    m.HeapAlloc,
		HeapInuse:    m.HeapInuse,
		HeapObjects:  m.HeapObjects,
		StackInuse:   m.StackInuse,
		AllocMB:      float64(m.Alloc) / 1024 / 1024,
		SysMB:        float64(m.Sys) / 1024 / 1024,
		HeapAllocMB:  float64(m.HeapAlloc) / 1024 / 1024,
	}
}

// LogMemoryStats logs current memory statistics
func LogMemoryStats() {
	stats := GetMemoryStats()
	log.Info().
		Float64("alloc_mb", stats.AllocMB).
		Float64("sys_mb", stats.SysMB).
		Float64("heap_alloc_mb", stats.HeapAllocMB).
		Uint64("heap_objects", stats.HeapObjects).
		Uint32("num_gc", stats.NumGC).
		Msg("Memory statistics")
}

// MonitorMemory starts periodic memory monitoring
func MonitorMemory(interval time.Duration, threshold float64) {
	ticker := time.NewTicker(interval)
	go func() {
		for range ticker.C {
			stats := GetMemoryStats()

			// Log if memory exceeds threshold
			if stats.AllocMB > threshold {
				log.Warn().
					Float64("alloc_mb", stats.AllocMB).
					Float64("threshold_mb", threshold).
					Msg("Memory usage exceeds threshold")
			} else {
				log.Debug().
					Float64("alloc_mb", stats.AllocMB).
					Float64("heap_objects", float64(stats.HeapObjects)).
					Msg("Memory monitor")
			}
		}
	}()
}

// ForceGC triggers garbage collection and logs before/after stats
func ForceGC() {
	before := GetMemoryStats()

	runtime.GC()

	after := GetMemoryStats()

	log.Info().
		Float64("before_mb", before.AllocMB).
		Float64("after_mb", after.AllocMB).
		Float64("freed_mb", before.AllocMB - after.AllocMB).
		Msg("Forced garbage collection")
}

// MemoryOptimizationTips returns optimization recommendations
func MemoryOptimizationTips() []string {
	return []string{
		"1. Use connection pooling for database (already implemented with sql.DB)",
		"2. Limit in-memory caching - prefer database queries",
		"3. Use sync.Pool for frequently allocated objects",
		"4. Close HTTP response bodies properly",
		"5. Avoid unnecessary string concatenation (use strings.Builder)",
		"6. Use pointers for large structs in function parameters",
		"7. Profile regularly with pprof: go tool pprof mem.prof",
		"8. Monitor with: GODEBUG=gctrace=1 ./zootbox-backend",
		"9. Set GOGC environment variable to tune GC (default: 100)",
		"10. Review goroutine leaks with: go tool pprof goroutine.prof",
	}
}
