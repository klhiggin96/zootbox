package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/services"
)

// InventoryHandler handles inventory-related endpoints
type InventoryHandler struct {
	db               *sql.DB
	inventoryService *services.InventoryService
}

// NewInventoryHandler creates a new inventory handler
func NewInventoryHandler(db *sql.DB) *InventoryHandler {
	return &InventoryHandler{
		db:               db,
		inventoryService: services.NewInventoryService(db),
	}
}

// GetAllCoils returns all 100 coils with current inventory
func (h *InventoryHandler) GetAllCoils(w http.ResponseWriter, r *http.Request) {
	coils, err := h.inventoryService.GetAllCoils()
	if err != nil {
		log.Error().Err(err).Msg("Failed to get all coils")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Failed to retrieve coils",
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(coils)
}

// GetCoil returns a specific coil by ID
func (h *InventoryHandler) GetCoil(w http.ResponseWriter, r *http.Request) {
	coilID := chi.URLParam(r, "coilId")
	if coilID == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Missing coil ID parameter",
		})
		return
	}

	coil, err := h.inventoryService.GetCoil(coilID)
	if err != nil {
		log.Error().Err(err).Str("coil_id", coilID).Msg("Failed to get coil")
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Coil not found",
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(coil)
}

// GetLowStockCoils returns coils with inventory <= 2
func (h *InventoryHandler) GetLowStockCoils(w http.ResponseWriter, r *http.Request) {
	coils, err := h.inventoryService.GetLowStockCoils()
	if err != nil {
		log.Error().Err(err).Msg("Failed to get low-stock coils")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Failed to retrieve low-stock coils",
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(coils)
}
