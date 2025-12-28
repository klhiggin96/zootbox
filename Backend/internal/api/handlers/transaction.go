package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"

	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/services"
)

// TransactionHandler handles transaction recording endpoints
type TransactionHandler struct {
	db                 *sql.DB
	transactionService *services.TransactionService
}

// NewTransactionHandler creates a new transaction handler
func NewTransactionHandler(db *sql.DB) *TransactionHandler {
	return &TransactionHandler{
		db:                 db,
		transactionService: services.NewTransactionService(db),
	}
}

// RecordTransaction records a vend event reported by Android app
func (h *TransactionHandler) RecordTransaction(w http.ResponseWriter, r *http.Request) {
	var req services.RecordVendEventRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Error().Err(err).Msg("Failed to decode request body")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Invalid request body",
		})
		return
	}

	// Validate request
	if req.CoilID == "" || req.Status == "" || req.TransactionID == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Missing required fields: coil_id, status, transaction_id",
		})
		return
	}

	// Process vend event
	response, err := h.transactionService.RecordVendEvent(&req)
	if err != nil {
		log.Error().Err(err).
			Str("coil_id", req.CoilID).
			Str("status", req.Status).
			Msg("Failed to record vend event")

		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error": err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}
