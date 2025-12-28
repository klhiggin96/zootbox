package handlers

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/zootbox/backend/internal/models"
)

func TestInventoryHandler_GetAllCoils(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	// Seed multiple coils
	_, err := db.Exec(`
		INSERT INTO coils (id, inventory, status, version) VALUES
		('A1', 10, 'available', 1),
		('A2', 5, 'available', 1),
		('A3', 2, 'available', 1),
		('A4', 0, 'available', 1)
	`)
	require.NoError(t, err)

	handler := NewInventoryHandler(db)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
	w := httptest.NewRecorder()

	handler.GetAllCoils(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var coils []*models.Coil
	err = json.NewDecoder(w.Body).Decode(&coils)
	require.NoError(t, err)

	assert.Len(t, coils, 4)
	assert.Equal(t, "A1", coils[0].ID)
	assert.Equal(t, 10, coils[0].Inventory)
}

func TestInventoryHandler_GetCoil(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	_, err := db.Exec("INSERT INTO coils (id, inventory, status, version) VALUES ('B3', 7, 'available', 1)")
	require.NoError(t, err)

	handler := NewInventoryHandler(db)

	t.Run("returns coil when exists", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/api/v1/coils/B3", nil)
		w := httptest.NewRecorder()

		// Create chi router context with URL param
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("coilId", "B3")
		req = req.WithContext(chi.NewRouteContext().WithValue(req.Context(), chi.RouteCtxKey, rctx))

		handler.GetCoil(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var coil models.Coil
		err := json.NewDecoder(w.Body).Decode(&coil)
		require.NoError(t, err)

		assert.Equal(t, "B3", coil.ID)
		assert.Equal(t, 7, coil.Inventory)
	})

	t.Run("returns 404 when coil not found", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/api/v1/coils/Z99", nil)
		w := httptest.NewRecorder()

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("coilId", "Z99")
		req = req.WithContext(chi.NewRouteContext().WithValue(req.Context(), chi.RouteCtxKey, rctx))

		handler.GetCoil(w, req)

		assert.Equal(t, http.StatusNotFound, w.Code)
	})
}

func TestInventoryHandler_GetLowStockCoils(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	// Seed coils with various inventory levels
	_, err := db.Exec(`
		INSERT INTO coils (id, inventory, status, version) VALUES
		('C1', 10, 'available', 1),
		('C2', 5, 'available', 1),
		('C3', 2, 'available', 1),
		('C4', 1, 'available', 1),
		('C5', 0, 'available', 1)
	`)
	require.NoError(t, err)

	handler := NewInventoryHandler(db)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils/low-stock", nil)
	w := httptest.NewRecorder()

	handler.GetLowStockCoils(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var coils []*models.Coil
	err = json.NewDecoder(w.Body).Decode(&coils)
	require.NoError(t, err)

	// Should return only coils with inventory <= 2
	assert.Len(t, coils, 3) // C3, C4, C5

	// Verify sorted by inventory ascending
	assert.Equal(t, 0, coils[0].Inventory)
	assert.Equal(t, 1, coils[1].Inventory)
	assert.Equal(t, 2, coils[2].Inventory)
}
