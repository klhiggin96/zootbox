package services

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/db/repositories"
	"github.com/zootbox/backend/internal/models"
)

// TransactionService handles transaction recording business logic
type TransactionService struct {
	db              *sql.DB
	coilRepo        *repositories.CoilRepository
	transactionRepo *repositories.TransactionRepository
	jamEventRepo    *repositories.JamEventRepository
}

// NewTransactionService creates a new transaction service
func NewTransactionService(db *sql.DB) *TransactionService {
	return &TransactionService{
		db:              db,
		coilRepo:        repositories.NewCoilRepository(db),
		transactionRepo: repositories.NewTransactionRepository(db),
		jamEventRepo:    repositories.NewJamEventRepository(db),
	}
}

// RecordVendEventRequest represents the request from Android app
type RecordVendEventRequest struct {
	CoilID        string `json:"coil_id"`
	Status        string `json:"status"`
	Timestamp     string `json:"timestamp"`
	TransactionID string `json:"transaction_id"`
}

// RecordVendEventResponse represents the response to Android app
type RecordVendEventResponse struct {
	Success        bool   `json:"success"`
	CoilID         string `json:"coil_id"`
	InventoryAfter int    `json:"inventory_after"`
	Message        string `json:"message,omitempty"`
}

// RecordVendEvent processes a vend event reported by Android app
// - If status=success: Decrement inventory by 1
// - If status=jam: Create jam event, inventory unchanged
// - If status=failed: Log transaction, inventory unchanged
func (s *TransactionService) RecordVendEvent(req *RecordVendEventRequest) (*RecordVendEventResponse, error) {
	log.Info().
		Str("coil_id", req.CoilID).
		Str("status", req.Status).
		Str("transaction_id", req.TransactionID).
		Msg("Recording vend event")

	// Begin transaction
	tx, err := s.db.Begin()
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback() // Rollback if not committed

	// Get current coil state
	coil, err := s.coilRepo.GetByID(req.CoilID)
	if err != nil {
		return nil, fmt.Errorf("coil not found: %w", err)
	}

	inventoryBefore := coil.Inventory
	inventoryAfter := inventoryBefore

	// Parse timestamp
	timestamp, err := time.Parse(time.RFC3339, req.Timestamp)
	if err != nil {
		timestamp = time.Now() // Fallback to current time
	}

	// Process based on status
	if req.Status == models.TransactionStatusSuccess {
		// Decrement inventory for successful vend
		if err := s.coilRepo.UpdateInventory(tx, req.CoilID, coil.Version); err != nil {
			return nil, fmt.Errorf("failed to decrement inventory: %w", err)
		}
		inventoryAfter = inventoryBefore - 1

		log.Info().
			Str("coil_id", req.CoilID).
			Int("inventory_before", inventoryBefore).
			Int("inventory_after", inventoryAfter).
			Msg("Inventory decremented successfully")
	} else if req.Status == models.TransactionStatusJam {
		// Create jam event for jam status
		jamEvent := &models.JamEvent{
			ID:        uuid.New().String(),
			CoilID:    req.CoilID,
			Timestamp: timestamp,
			Status:    models.JamEventStatusOpen,
		}

		if err := s.jamEventRepo.Create(tx, jamEvent); err != nil {
			return nil, fmt.Errorf("failed to create jam event: %w", err)
		}

		log.Warn().
			Str("coil_id", req.CoilID).
			Str("jam_event_id", jamEvent.ID).
			Msg("Jam event created")
	}

	// Log transaction record
	inventoryAfterPtr := &inventoryAfter
	transaction := &models.Transaction{
		ID:              uuid.New().String(),
		CoilID:          req.CoilID,
		Timestamp:       timestamp,
		Status:          req.Status,
		TransactionID:   req.TransactionID,
		InventoryBefore: inventoryBefore,
		InventoryAfter:  inventoryAfterPtr,
	}

	if err := s.transactionRepo.Create(tx, transaction); err != nil {
		return nil, fmt.Errorf("failed to create transaction record: %w", err)
	}

	// Commit transaction
	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("failed to commit transaction: %w", err)
	}

	log.Info().
		Str("coil_id", req.CoilID).
		Str("status", req.Status).
		Str("transaction_id", transaction.ID).
		Msg("Vend event recorded successfully")

	return &RecordVendEventResponse{
		Success:        true,
		CoilID:         req.CoilID,
		InventoryAfter: inventoryAfter,
		Message:        fmt.Sprintf("Vend event recorded with status: %s", req.Status),
	}, nil
}

// Savepoint management utilities for nested transaction support
// SQLite supports savepoints for partial rollback within a transaction

// CreateSavepoint creates a named savepoint within a transaction
func (s *TransactionService) CreateSavepoint(tx *sql.Tx, name string) error {
	_, err := tx.Exec(fmt.Sprintf("SAVEPOINT %s", name))
	if err != nil {
		return fmt.Errorf("failed to create savepoint %s: %w", name, err)
	}
	log.Debug().Str("savepoint", name).Msg("Savepoint created")
	return nil
}

// ReleaseSavepoint releases a savepoint (commits changes up to that point)
func (s *TransactionService) ReleaseSavepoint(tx *sql.Tx, name string) error {
	_, err := tx.Exec(fmt.Sprintf("RELEASE SAVEPOINT %s", name))
	if err != nil {
		return fmt.Errorf("failed to release savepoint %s: %w", name, err)
	}
	log.Debug().Str("savepoint", name).Msg("Savepoint released")
	return nil
}

// RollbackToSavepoint rolls back to a named savepoint
func (s *TransactionService) RollbackToSavepoint(tx *sql.Tx, name string) error {
	_, err := tx.Exec(fmt.Sprintf("ROLLBACK TO SAVEPOINT %s", name))
	if err != nil {
		return fmt.Errorf("failed to rollback to savepoint %s: %w", name, err)
	}
	log.Debug().Str("savepoint", name).Msg("Rolled back to savepoint")
	return nil
}
