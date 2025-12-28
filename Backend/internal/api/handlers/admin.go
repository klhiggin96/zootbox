package handlers

import (
	"database/sql"
	"net/http"
)

// AdminHandler handles admin-only endpoints
type AdminHandler struct {
	db *sql.DB
}

// NewAdminHandler creates a new admin handler
func NewAdminHandler(db *sql.DB) *AdminHandler {
	return &AdminHandler{db: db}
}

// RefillAll sets all 100 coils to inventory=10
func (h *AdminHandler) RefillAll(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement in Phase 4 (T027)
	w.WriteHeader(http.StatusNotImplemented)
	w.Write([]byte(`{"error":"Not implemented yet - Phase 4 T027"}`))
}

// UpdateCoilInventory manually sets a coil's inventory
func (h *AdminHandler) UpdateCoilInventory(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement in Phase 4 (T028)
	w.WriteHeader(http.StatusNotImplemented)
	w.Write([]byte(`{"error":"Not implemented yet - Phase 4 T028"}`))
}

// CreateProductLink creates a new product link group
func (h *AdminHandler) CreateProductLink(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement in Phase 6 (T039)
	w.WriteHeader(http.StatusNotImplemented)
	w.Write([]byte(`{"error":"Not implemented yet - Phase 6 T039"}`))
}

// GetProductLinks returns all product link groups
func (h *AdminHandler) GetProductLinks(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement in Phase 6 (T040)
	w.WriteHeader(http.StatusNotImplemented)
	w.Write([]byte(`{"error":"Not implemented yet - Phase 6 T040"}`))
}

// DeleteProductLink deletes a product link group
func (h *AdminHandler) DeleteProductLink(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement in Phase 6 (T040)
	w.WriteHeader(http.StatusNotImplemented)
	w.Write([]byte(`{"error":"Not implemented yet - Phase 6 T040"}`))
}
