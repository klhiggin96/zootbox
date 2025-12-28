package handlers

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	_ "modernc.org/sqlite"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/zootbox/backend/internal/services"
)

func setupTestDB(t *testing.T) *sql.DB {
	db, err := sql.Open("sqlite", ":memory:")
	require.NoError(t, err)

	schema := `
	CREATE TABLE coils (
		id TEXT PRIMARY KEY,
		inventory INTEGER NOT NULL DEFAULT 10,
		status TEXT NOT NULL DEFAULT 'available',
		version INTEGER NOT NULL DEFAULT 1,
		link_group_id TEXT,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

	CREATE TABLE transactions (
		id TEXT PRIMARY KEY,
		coil_id TEXT NOT NULL,
		timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
		status TEXT NOT NULL,
		transaction_id TEXT NOT NULL,
		inventory_before INTEGER NOT NULL,
		inventory_after INTEGER
	);

	CREATE TABLE jam_events (
		id TEXT PRIMARY KEY,
		coil_id TEXT NOT NULL,
		timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
		status TEXT NOT NULL DEFAULT 'open',
		resolved_at TIMESTAMP
	);

	INSERT INTO coils (id, inventory, status, version) VALUES ('A5', 10, 'available', 1);
	`
	_, err = db.Exec(schema)
	require.NoError(t, err)

	return db
}

func TestTransactionHandler_RecordTransaction_Success(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	handler := NewTransactionHandler(db)

	reqBody := services.RecordVendEventRequest{
		CoilID:        "A5",
		Status:        "success",
		Timestamp:     "2025-12-27T10:30:00Z",
		TransactionID: "nayax-12345",
	}

	body, err := json.Marshal(reqBody)
	require.NoError(t, err)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.RecordTransaction(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response services.RecordVendEventResponse
	err = json.NewDecoder(w.Body).Decode(&response)
	require.NoError(t, err)

	assert.True(t, response.Success)
	assert.Equal(t, "A5", response.CoilID)
	assert.Equal(t, 9, response.InventoryAfter)
}

func TestTransactionHandler_RecordTransaction_InvalidJSON(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	handler := NewTransactionHandler(db)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", bytes.NewReader([]byte("invalid json")))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.RecordTransaction(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestTransactionHandler_RecordTransaction_MissingFields(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	handler := NewTransactionHandler(db)

	reqBody := services.RecordVendEventRequest{
		CoilID: "A5",
		// Missing status and transaction_id
	}

	body, err := json.Marshal(reqBody)
	require.NoError(t, err)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.RecordTransaction(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)

	var errorResp map[string]string
	err = json.NewDecoder(w.Body).Decode(&errorResp)
	require.NoError(t, err)
	assert.Contains(t, errorResp["error"], "Missing required fields")
}

func TestTransactionHandler_RecordTransaction_CoilNotFound(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	handler := NewTransactionHandler(db)

	reqBody := services.RecordVendEventRequest{
		CoilID:        "Z99",
		Status:        "success",
		Timestamp:     "2025-12-27T10:30:00Z",
		TransactionID: "nayax-99999",
	}

	body, err := json.Marshal(reqBody)
	require.NoError(t, err)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.RecordTransaction(w, req)

	assert.Equal(t, http.StatusInternalServerError, w.Code)
}

func TestTransactionHandler_RecordTransaction_JamStatus(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	handler := NewTransactionHandler(db)

	reqBody := services.RecordVendEventRequest{
		CoilID:        "A5",
		Status:        "jam",
		Timestamp:     "2025-12-27T10:31:00Z",
		TransactionID: "nayax-88888",
	}

	body, err := json.Marshal(reqBody)
	require.NoError(t, err)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.RecordTransaction(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response services.RecordVendEventResponse
	err = json.NewDecoder(w.Body).Decode(&response)
	require.NoError(t, err)

	assert.True(t, response.Success)
	assert.Equal(t, 10, response.InventoryAfter) // Inventory unchanged for jam
}
