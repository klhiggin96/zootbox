package handlers

import (
	"database/sql"
	"net/http"
)

// ProductLinkHandler handles product linking endpoints
type ProductLinkHandler struct {
	db *sql.DB
}

// NewProductLinkHandler creates a new product link handler
func NewProductLinkHandler(db *sql.DB) *ProductLinkHandler {
	return &ProductLinkHandler{db: db}
}

// ResolveProductToCoil returns recommended coil_id for a product SKU
func (h *ProductLinkHandler) ResolveProductToCoil(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement in Phase 6 (T038)
	w.WriteHeader(http.StatusNotImplemented)
	w.Write([]byte(`{"error":"Not implemented yet - Phase 6 T038"}`))
}
