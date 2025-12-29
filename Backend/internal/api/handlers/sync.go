package handlers

import (
	"database/sql"
	"encoding/json"
	"log"
	"net/http"
	"time"
)

// SyncHandler handles inventory synchronization from Android app to backend
type SyncHandler struct {
	db *sql.DB
}

// NewSyncHandler creates a new sync handler
func NewSyncHandler(db *sql.DB) *SyncHandler {
	return &SyncHandler{db: db}
}

// InventorySyncRequest represents the inventory snapshot from Android
type InventorySyncRequest struct {
	Source       string           `json:"source"`        // "android"
	Timestamp    int64            `json:"timestamp"`     // Unix timestamp
	Coils        []CoilSyncData   `json:"coils"`         // Current coil states
	Transactions []TransactionSync `json:"transactions"` // Unsynced transactions
}

// CoilSyncData represents a single coil's state
type CoilSyncData struct {
	ID        string `json:"id"`        // Coil ID (A1-J1)
	Inventory int    `json:"inventory"` // Current inventory (0-10)
	Status    string `json:"status"`    // 'available' or 'jammed'
	UpdatedAt int64  `json:"updated_at"` // Unix timestamp
}

// TransactionSync represents a transaction to be synced
type TransactionSync struct {
	ID        string `json:"id"`         // Transaction ID from Android
	CoilID    string `json:"coil_id"`    // Coil that was vended
	Status    string `json:"status"`     // 'success', 'jam', or 'failed'
	Timestamp int64  `json:"timestamp"`  // Unix timestamp
}

// SyncInventory accepts inventory updates from the Android app
// POST /api/sync/inventory
func (h *SyncHandler) SyncInventory(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req InventorySyncRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Printf("Error decoding sync request: %v", err)
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	log.Printf("Received inventory sync from %s with %d coils and %d transactions",
		req.Source, len(req.Coils), len(req.Transactions))

	// Start transaction
	tx, err := h.db.Begin()
	if err != nil {
		log.Printf("Error starting transaction: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer tx.Rollback()

	// Sync coil inventory
	coilsUpdated := 0
	for _, coil := range req.Coils {
		result, err := tx.Exec(`
			UPDATE coils
			SET inventory = ?, status = ?, updated_at = datetime(?, 'unixepoch')
			WHERE id = ?
		`, coil.Inventory, coil.Status, coil.UpdatedAt, coil.ID)

		if err != nil {
			log.Printf("Error updating coil %s: %v", coil.ID, err)
			continue
		}

		rowsAffected, _ := result.RowsAffected()
		if rowsAffected > 0 {
			coilsUpdated++
		}
	}

	// Sync transactions (insert if not exists)
	transactionsAdded := 0
	for _, txn := range req.Transactions {
		_, err := tx.Exec(`
			INSERT OR IGNORE INTO transactions (id, coil_id, timestamp, status, transaction_id, inventory_before)
			VALUES (?, ?, datetime(?, 'unixepoch'), ?, ?,
				(SELECT inventory FROM coils WHERE id = ?))
		`, txn.ID, txn.CoilID, txn.Timestamp, txn.Status, txn.ID, txn.CoilID)

		if err != nil {
			log.Printf("Error inserting transaction %s: %v", txn.ID, err)
			continue
		}

		transactionsAdded++
	}

	// Commit transaction
	if err := tx.Commit(); err != nil {
		log.Printf("Error committing sync transaction: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Return sync summary
	response := map[string]interface{}{
		"success":             true,
		"coils_updated":       coilsUpdated,
		"transactions_added":  transactionsAdded,
		"timestamp":           time.Now().Unix(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)

	log.Printf("Sync complete: %d coils updated, %d transactions added", coilsUpdated, transactionsAdded)
}

// GetSyncStatus returns the last sync timestamp and stats
// GET /api/sync/status
func (h *SyncHandler) GetSyncStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Get last transaction timestamp
	var lastSync sql.NullTime
	err := h.db.QueryRow(`
		SELECT MAX(timestamp) FROM transactions
	`).Scan(&lastSync)

	if err != nil && err != sql.ErrNoRows {
		log.Printf("Error getting sync status: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Get total inventory
	var totalInventory int
	err = h.db.QueryRow(`
		SELECT COALESCE(SUM(inventory), 0) FROM coils
	`).Scan(&totalInventory)

	if err != nil {
		log.Printf("Error getting total inventory: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Get transaction count
	var transactionCount int
	err = h.db.QueryRow(`
		SELECT COUNT(*) FROM transactions
	`).Scan(&transactionCount)

	if err != nil {
		log.Printf("Error getting transaction count: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"total_inventory":    totalInventory,
		"transaction_count":  transactionCount,
		"last_sync":          nil,
	}

	if lastSync.Valid {
		response["last_sync"] = lastSync.Time.Unix()
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
