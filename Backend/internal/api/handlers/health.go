package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
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

// MetricsResponse represents basic Prometheus-style metrics
type MetricsResponse struct {
	// TODO: Add actual metrics from service layer
	Placeholder string `json:"placeholder"`
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

// Metrics returns basic metrics (placeholder for now)
func (h *HealthHandler) Metrics(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement Prometheus metrics
	// For now, return placeholder
	response := MetricsResponse{
		Placeholder: "Metrics endpoint - TODO: implement Prometheus format",
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}
