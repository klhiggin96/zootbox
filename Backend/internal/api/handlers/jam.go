package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/services"
)

// JamHandler handles jam event endpoints
type JamHandler struct {
	db         *sql.DB
	jamService *services.JamService
}

// NewJamHandler creates a new jam event handler
func NewJamHandler(db *sql.DB) *JamHandler {
	return &JamHandler{
		db:         db,
		jamService: services.NewJamService(db),
	}
}

// GetJamEvents returns jam events (optionally filtered by status)
func (h *JamHandler) GetJamEvents(w http.ResponseWriter, r *http.Request) {
	// Extract optional status query parameter (e.g., ?status=open or ?status=resolved)
	status := r.URL.Query().Get("status")

	log.Info().
		Str("status_filter", status).
		Msg("Fetching jam events")

	jamEvents, err := h.jamService.GetJamEvents(status)
	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch jam events")

		statusCode := http.StatusInternalServerError
		if strings.HasPrefix(err.Error(), "invalid") {
			statusCode = http.StatusBadRequest
		}

		w.WriteHeader(statusCode)
		json.NewEncoder(w).Encode(map[string]string{
			"error": err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(jamEvents)
}

// ResolveJamEvent marks a jam event as resolved
func (h *JamHandler) ResolveJamEvent(w http.ResponseWriter, r *http.Request) {
	eventID := chi.URLParam(r, "eventId")
	if eventID == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Missing event ID parameter",
		})
		return
	}

	log.Info().Str("jam_event_id", eventID).Msg("Resolving jam event")

	resolvedEvent, err := h.jamService.ResolveJamEvent(eventID)
	if err != nil {
		log.Error().Err(err).
			Str("jam_event_id", eventID).
			Msg("Failed to resolve jam event")

		statusCode := http.StatusInternalServerError
		if strings.HasSuffix(err.Error(), "not found") {
			statusCode = http.StatusNotFound
		}

		w.WriteHeader(statusCode)
		json.NewEncoder(w).Encode(map[string]string{
			"error": err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(resolvedEvent)
}
