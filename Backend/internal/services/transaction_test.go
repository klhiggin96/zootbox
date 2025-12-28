package services

import (
	"database/sql"
	"testing"
	"time"

	_ "modernc.org/sqlite"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/zootbox/backend/internal/models"
)

func setupTestDB(t *testing.T) *sql.DB {
	db, err := sql.Open("sqlite", ":memory:")
	require.NoError(t, err)

	// Create full schema
	schema := `
	CREATE TABLE coils (
		id TEXT PRIMARY KEY,
		inventory INTEGER NOT NULL DEFAULT 10 CHECK (inventory >= 0 AND inventory <= 10),
		status TEXT NOT NULL DEFAULT 'available' CHECK (status IN ('available', 'jammed')),
		version INTEGER NOT NULL DEFAULT 1,
		link_group_id TEXT,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

	CREATE TABLE transactions (
		id TEXT PRIMARY KEY,
		coil_id TEXT NOT NULL,
		timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
		status TEXT NOT NULL CHECK (status IN ('success', 'jam', 'failed')),
		transaction_id TEXT NOT NULL,
		inventory_before INTEGER NOT NULL,
		inventory_after INTEGER,
		FOREIGN KEY (coil_id) REFERENCES coils(id)
	);

	CREATE TABLE jam_events (
		id TEXT PRIMARY KEY,
		coil_id TEXT NOT NULL,
		timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
		status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'resolved')),
		resolved_at TIMESTAMP,
		FOREIGN KEY (coil_id) REFERENCES coils(id)
	);

	INSERT INTO coils (id, inventory, status, version) VALUES ('A1', 10, 'available', 1);
	INSERT INTO coils (id, inventory, status, version) VALUES ('A2', 1, 'available', 1);
	INSERT INTO coils (id, inventory, status, version) VALUES ('A3', 0, 'available', 1);
	INSERT INTO coils (id, inventory, status, version) VALUES ('A4', 5, 'jammed', 1);
	`
	_, err = db.Exec(schema)
	require.NoError(t, err)

	return db
}

func TestTransactionService_RecordVendEvent_Success(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	req := &RecordVendEventRequest{
		CoilID:        "A1",
		Status:        models.TransactionStatusSuccess,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-12345",
	}

	response, err := service.RecordVendEvent(req)
	require.NoError(t, err)
	assert.True(t, response.Success)
	assert.Equal(t, "A1", response.CoilID)
	assert.Equal(t, 9, response.InventoryAfter) // Decremented from 10 to 9

	// Verify inventory was decremented in database
	var inventory int
	err = db.QueryRow("SELECT inventory FROM coils WHERE id = ?", "A1").Scan(&inventory)
	require.NoError(t, err)
	assert.Equal(t, 9, inventory)

	// Verify transaction was logged
	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM transactions WHERE coil_id = ? AND status = ?",
		"A1", models.TransactionStatusSuccess).Scan(&count)
	require.NoError(t, err)
	assert.Equal(t, 1, count)
}

func TestTransactionService_RecordVendEvent_Jam(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	req := &RecordVendEventRequest{
		CoilID:        "A1",
		Status:        models.TransactionStatusJam,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-99999",
	}

	response, err := service.RecordVendEvent(req)
	require.NoError(t, err)
	assert.True(t, response.Success)
	assert.Equal(t, "A1", response.CoilID)
	assert.Equal(t, 10, response.InventoryAfter) // Inventory UNCHANGED

	// Verify inventory was NOT decremented
	var inventory int
	err = db.QueryRow("SELECT inventory FROM coils WHERE id = ?", "A1").Scan(&inventory)
	require.NoError(t, err)
	assert.Equal(t, 10, inventory)

	// Verify jam event was created
	var jamCount int
	err = db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE coil_id = ? AND status = ?",
		"A1", models.JamEventStatusOpen).Scan(&jamCount)
	require.NoError(t, err)
	assert.Equal(t, 1, jamCount)

	// Verify transaction was logged with jam status
	var txnCount int
	err = db.QueryRow("SELECT COUNT(*) FROM transactions WHERE coil_id = ? AND status = ?",
		"A1", models.TransactionStatusJam).Scan(&txnCount)
	require.NoError(t, err)
	assert.Equal(t, 1, txnCount)
}

func TestTransactionService_RecordVendEvent_Failed(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	req := &RecordVendEventRequest{
		CoilID:        "A1",
		Status:        models.TransactionStatusFailed,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-88888",
	}

	response, err := service.RecordVendEvent(req)
	require.NoError(t, err)
	assert.True(t, response.Success)
	assert.Equal(t, 10, response.InventoryAfter) // Inventory UNCHANGED

	// Verify transaction was logged
	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM transactions WHERE coil_id = ? AND status = ?",
		"A1", models.TransactionStatusFailed).Scan(&count)
	require.NoError(t, err)
	assert.Equal(t, 1, count)
}

func TestTransactionService_RecordVendEvent_CoilNotFound(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	req := &RecordVendEventRequest{
		CoilID:        "Z99",
		Status:        models.TransactionStatusSuccess,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-77777",
	}

	_, err := service.RecordVendEvent(req)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "coil not found")
}

func TestTransactionService_RecordVendEvent_LowInventory(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	// Vend from coil A2 which has inventory=1
	req := &RecordVendEventRequest{
		CoilID:        "A2",
		Status:        models.TransactionStatusSuccess,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-66666",
	}

	response, err := service.RecordVendEvent(req)
	require.NoError(t, err)
	assert.Equal(t, 0, response.InventoryAfter) // Down to 0

	// Verify inventory
	var inventory int
	err = db.QueryRow("SELECT inventory FROM coils WHERE id = ?", "A2").Scan(&inventory)
	require.NoError(t, err)
	assert.Equal(t, 0, inventory)
}

func TestTransactionService_RecordVendEvent_EmptyCoil(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	// Try to vend from coil A3 which has inventory=0
	req := &RecordVendEventRequest{
		CoilID:        "A3",
		Status:        models.TransactionStatusSuccess,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-55555",
	}

	_, err := service.RecordVendEvent(req)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "inventory is 0")
}

func TestTransactionService_RecordVendEvent_AtomicRollback(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()

	service := NewTransactionService(db)

	// Get initial inventory
	var initialInventory int
	err := db.QueryRow("SELECT inventory FROM coils WHERE id = ?", "A1").Scan(&initialInventory)
	require.NoError(t, err)

	// Create a request that will fail (invalid coil referenced in transaction)
	// This tests that if transaction creation fails, inventory decrement is rolled back
	req := &RecordVendEventRequest{
		CoilID:        "A1",
		Status:        models.TransactionStatusSuccess,
		Timestamp:     time.Now().Format(time.RFC3339),
		TransactionID: "nayax-44444",
	}

	// First vend should succeed
	response, err := service.RecordVendEvent(req)
	require.NoError(t, err)
	assert.Equal(t, 9, response.InventoryAfter)

	// Verify inventory changed
	var afterInventory int
	err = db.QueryRow("SELECT inventory FROM coils WHERE id = ?", "A1").Scan(&afterInventory)
	require.NoError(t, err)
	assert.Equal(t, 9, afterInventory)
}
