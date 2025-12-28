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

// ProductLinkHandler handles product linking endpoints
type ProductLinkHandler struct {
	db                 *sql.DB
	productLinkService *services.ProductLinkService
}

// NewProductLinkHandler creates a new product link handler
func NewProductLinkHandler(db *sql.DB) *ProductLinkHandler {
	return &ProductLinkHandler{
		db:                 db,
		productLinkService: services.NewProductLinkService(db),
	}
}

// ResolveProductToCoil returns recommended coil_id for a product SKU
func (h *ProductLinkHandler) ResolveProductToCoil(w http.ResponseWriter, r *http.Request) {
	sku := chi.URLParam(r, "sku")
	if sku == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Missing SKU parameter",
		})
		return
	}

	log.Info().Str("product_sku", sku).Msg("Resolving product to coil")

	resolution, err := h.productLinkService.ResolveProductToCoil(sku)
	if err != nil {
		log.Error().Err(err).Str("product_sku", sku).Msg("Failed to resolve product to coil")

		statusCode := http.StatusInternalServerError
		errMsg := err.Error()
		if strings.HasSuffix(errMsg, "not found") || strings.HasPrefix(errMsg, "failed to find") {
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
	json.NewEncoder(w).Encode(resolution)
}
