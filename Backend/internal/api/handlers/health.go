package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"runtime"
	"time"

	"github.com/rs/zerolog/log"
)

var startTime = time.Now()

// HealthHandler handles health check and metrics endpoints
type HealthHandler struct {
	db *sql.DB
}

// NewHealthHandler creates a new health handler
func NewHealthHandler(db *sql.DB) *HealthHandler {
	return &HealthHandler{db: db}
}

// HealthResponse represents the health check response
type HealthResponse struct {
	Status        string  `json:"status"`
	UptimeSeconds float64 `json:"uptime_seconds"`
	DatabaseOK    bool    `json:"database_ok"`
}

// Health returns the health status of the service
func (h *HealthHandler) Health(w http.ResponseWriter, r *http.Request) {
	// Check database connectivity
	dbOK := true
	if err := h.db.Ping(); err != nil {
		log.Error().Err(err).Msg("Database ping failed")
		dbOK = false
	}

	uptime := time.Since(startTime).Seconds()

	response := HealthResponse{
		Status:        "healthy",
		UptimeSeconds: uptime,
		DatabaseOK:    dbOK,
	}

	// Return 503 if database is down
	statusCode := http.StatusOK
	if !dbOK {
		response.Status = "unhealthy"
		statusCode = http.StatusServiceUnavailable
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	json.NewEncoder(w).Encode(response)
}

// Metrics returns Prometheus-format metrics
func (h *HealthHandler) Metrics(w http.ResponseWriter, r *http.Request) {
	var metrics string

	// System uptime
	uptime := time.Since(startTime).Seconds()
	metrics += fmt.Sprintf("# HELP zootbox_uptime_seconds Service uptime in seconds\n")
	metrics += fmt.Sprintf("# TYPE zootbox_uptime_seconds gauge\n")
	metrics += fmt.Sprintf("zootbox_uptime_seconds %.2f\n\n", uptime)

	// Memory metrics
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	metrics += fmt.Sprintf("# HELP zootbox_memory_alloc_bytes Memory allocated in bytes\n")
	metrics += fmt.Sprintf("# TYPE zootbox_memory_alloc_bytes gauge\n")
	metrics += fmt.Sprintf("zootbox_memory_alloc_bytes %d\n\n", m.Alloc)

	metrics += fmt.Sprintf("# HELP zootbox_memory_sys_bytes Total memory obtained from OS in bytes\n")
	metrics += fmt.Sprintf("# TYPE zootbox_memory_sys_bytes gauge\n")
	metrics += fmt.Sprintf("zootbox_memory_sys_bytes %d\n\n", m.Sys)

	metrics += fmt.Sprintf("# HELP zootbox_memory_heap_objects Number of heap objects\n")
	metrics += fmt.Sprintf("# TYPE zootbox_memory_heap_objects gauge\n")
	metrics += fmt.Sprintf("zootbox_memory_heap_objects %d\n\n", m.HeapObjects)

	// Goroutine count
	metrics += fmt.Sprintf("# HELP zootbox_goroutines Number of goroutines\n")
	metrics += fmt.Sprintf("# TYPE zootbox_goroutines gauge\n")
	metrics += fmt.Sprintf("zootbox_goroutines %d\n\n", runtime.NumGoroutine())

	// Database connectivity
	dbStatus := 1
	if err := h.db.Ping(); err != nil {
		log.Error().Err(err).Msg("Database ping failed in metrics")
		dbStatus = 0
	}

	metrics += fmt.Sprintf("# HELP zootbox_database_up Database connectivity status (1=up, 0=down)\n")
	metrics += fmt.Sprintf("# TYPE zootbox_database_up gauge\n")
	metrics += fmt.Sprintf("zootbox_database_up %d\n\n", dbStatus)

	// Inventory metrics
	var totalCoils, coilsWithInventory, totalInventory int
	h.db.QueryRow("SELECT COUNT(*) FROM coils").Scan(&totalCoils)
	h.db.QueryRow("SELECT COUNT(*) FROM coils WHERE inventory > 0").Scan(&coilsWithInventory)
	h.db.QueryRow("SELECT COALESCE(SUM(inventory), 0) FROM coils").Scan(&totalInventory)

	metrics += fmt.Sprintf("# HELP zootbox_coils_total Total number of coil positions\n")
	metrics += fmt.Sprintf("# TYPE zootbox_coils_total gauge\n")
	metrics += fmt.Sprintf("zootbox_coils_total %d\n\n", totalCoils)

	metrics += fmt.Sprintf("# HELP zootbox_coils_stocked Number of coils with inventory > 0\n")
	metrics += fmt.Sprintf("# TYPE zootbox_coils_stocked gauge\n")
	metrics += fmt.Sprintf("zootbox_coils_stocked %d\n\n", coilsWithInventory)

	metrics += fmt.Sprintf("# HELP zootbox_inventory_total Total inventory across all coils\n")
	metrics += fmt.Sprintf("# TYPE zootbox_inventory_total gauge\n")
	metrics += fmt.Sprintf("zootbox_inventory_total %d\n\n", totalInventory)

	// Transaction metrics
	var totalTransactions, successCount, jamCount, failedCount int
	h.db.QueryRow("SELECT COUNT(*) FROM transactions").Scan(&totalTransactions)
	h.db.QueryRow("SELECT COUNT(*) FROM transactions WHERE status = 'success'").Scan(&successCount)
	h.db.QueryRow("SELECT COUNT(*) FROM transactions WHERE status = 'jam'").Scan(&jamCount)
	h.db.QueryRow("SELECT COUNT(*) FROM transactions WHERE status = 'failed'").Scan(&failedCount)

	metrics += fmt.Sprintf("# HELP zootbox_transactions_total Total number of transactions\n")
	metrics += fmt.Sprintf("# TYPE zootbox_transactions_total counter\n")
	metrics += fmt.Sprintf("zootbox_transactions_total{status=\"success\"} %d\n", successCount)
	metrics += fmt.Sprintf("zootbox_transactions_total{status=\"jam\"} %d\n", jamCount)
	metrics += fmt.Sprintf("zootbox_transactions_total{status=\"failed\"} %d\n\n", failedCount)

	// Jam event metrics
	var openJams, resolvedJams int
	h.db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE status = 'open'").Scan(&openJams)
	h.db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE status = 'resolved'").Scan(&resolvedJams)

	metrics += fmt.Sprintf("# HELP zootbox_jam_events_total Total number of jam events\n")
	metrics += fmt.Sprintf("# TYPE zootbox_jam_events_total counter\n")
	metrics += fmt.Sprintf("zootbox_jam_events_total{status=\"open\"} %d\n", openJams)
	metrics += fmt.Sprintf("zootbox_jam_events_total{status=\"resolved\"} %d\n\n", resolvedJams)

	// Product links
	var productLinkCount int
	h.db.QueryRow("SELECT COUNT(*) FROM product_links").Scan(&productLinkCount)

	metrics += fmt.Sprintf("# HELP zootbox_product_links_total Total number of product link groups\n")
	metrics += fmt.Sprintf("# TYPE zootbox_product_links_total gauge\n")
	metrics += fmt.Sprintf("zootbox_product_links_total %d\n\n", productLinkCount)

	// Write Prometheus text format response
	w.Header().Set("Content-Type", "text/plain; version=0.0.4")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(metrics))
}
