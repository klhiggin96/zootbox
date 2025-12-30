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

// AdminHandler handles admin-only endpoints
type AdminHandler struct {
	db                 *sql.DB
	adminService       *services.AdminService
	productLinkService *services.ProductLinkService
}

// NewAdminHandler creates a new admin handler
func NewAdminHandler(db *sql.DB) *AdminHandler {
	return &AdminHandler{
		db:                 db,
		adminService:       services.NewAdminService(db),
		productLinkService: services.NewProductLinkService(db),
	}
}

// RefillAll sets all 10 coils to inventory=10
func (h *AdminHandler) RefillAll(w http.ResponseWriter, r *http.Request) {
	response, err := h.adminService.RefillAll()
	if err != nil {
		log.Error().Err(err).Msg("Failed to refill all coils")
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

// UpdateCoilInventory manually sets a coil's inventory
func (h *AdminHandler) UpdateCoilInventory(w http.ResponseWriter, r *http.Request) {
	coilID := chi.URLParam(r, "coilId")
	if coilID == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Missing coil ID parameter",
		})
		return
	}

	var req services.SetCoilInventoryRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Error().Err(err).Msg("Failed to decode request body")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Invalid request body",
		})
		return
	}

	coil, err := h.adminService.SetCoilInventory(coilID, req.Inventory)
	if err != nil {
		log.Error().Err(err).
			Str("coil_id", coilID).
			Int("inventory", req.Inventory).
			Msg("Failed to update coil inventory")

		statusCode := http.StatusInternalServerError
		errMsg := err.Error()
		if errMsg == "coil "+coilID+" not found" {
			statusCode = http.StatusNotFound
		} else if strings.HasPrefix(errMsg, "inventory") {
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
	json.NewEncoder(w).Encode(coil)
}

// CreateProductLink creates a new product link group
func (h *AdminHandler) CreateProductLink(w http.ResponseWriter, r *http.Request) {
	var req services.CreateProductLinkRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Error().Err(err).Msg("Failed to decode request body")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Invalid request body",
		})
		return
	}

	log.Info().
		Str("product_sku", req.ProductSKU).
		Int("coil_count", len(req.LinkedCoilIDs)).
		Msg("Creating product link")

	productLink, err := h.productLinkService.CreateProductLink(&req)
	if err != nil {
		log.Error().Err(err).Msg("Failed to create product link")

		statusCode := http.StatusInternalServerError
		errMsg := err.Error()
		if strings.HasPrefix(errMsg, "coil") || strings.HasPrefix(errMsg, "product") ||
		   strings.HasPrefix(errMsg, "linked") || strings.HasPrefix(errMsg, "invalid") {
			statusCode = http.StatusBadRequest
		}

		w.WriteHeader(statusCode)
		json.NewEncoder(w).Encode(map[string]string{
			"error": err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(productLink)
}

// GetProductLinks returns all product link groups
func (h *AdminHandler) GetProductLinks(w http.ResponseWriter, r *http.Request) {
	log.Info().Msg("Fetching all product links")

	productLinks, err := h.productLinkService.GetAllProductLinks()
	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch product links")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error": err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(productLinks)
}

// DeleteProductLink deletes a product link group
func (h *AdminHandler) DeleteProductLink(w http.ResponseWriter, r *http.Request) {
	linkGroupID := chi.URLParam(r, "linkGroupId")
	if linkGroupID == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Missing link group ID parameter",
		})
		return
	}

	log.Info().Str("link_group_id", linkGroupID).Msg("Deleting product link")

	err := h.productLinkService.DeleteProductLink(linkGroupID)
	if err != nil {
		log.Error().Err(err).Str("link_group_id", linkGroupID).Msg("Failed to delete product link")

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

	w.WriteHeader(http.StatusNoContent)
}
