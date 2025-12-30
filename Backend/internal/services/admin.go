package services

import (
	"database/sql"
	"fmt"

	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/db/repositories"
	"github.com/zootbox/backend/internal/models"
)

// AdminService handles administrative operations
type AdminService struct {
	db       *sql.DB
	coilRepo *repositories.CoilRepository
}

// NewAdminService creates a new admin service
func NewAdminService(db *sql.DB) *AdminService {
	return &AdminService{
		db:       db,
		coilRepo: repositories.NewCoilRepository(db),
	}
}

// RefillAllResponse represents the response from refilling all coils
type RefillAllResponse struct {
	Message      string `json:"message"`
	CoilsUpdated int64  `json:"coils_updated"`
}

// RefillAll sets all 10 coils to inventory=10
func (s *AdminService) RefillAll() (*RefillAllResponse, error) {
	log.Info().Msg("Refilling all coils to inventory=10")

	tx, err := s.db.Begin()
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	rowsAffected, err := s.coilRepo.RefillAll(tx)
	if err != nil {
		return nil, fmt.Errorf("failed to refill all coils: %w", err)
	}

	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("failed to commit refill transaction: %w", err)
	}

	log.Info().Int64("coils_updated", rowsAffected).Msg("All coils refilled successfully")

	return &RefillAllResponse{
		Message:      fmt.Sprintf("All %d coils refilled to inventory 10", rowsAffected),
		CoilsUpdated: rowsAffected,
	}, nil
}

// SetCoilInventoryRequest represents a manual inventory update request
type SetCoilInventoryRequest struct {
	Inventory int `json:"inventory"`
}

// SetCoilInventory manually sets a specific coil's inventory (admin override)
func (s *AdminService) SetCoilInventory(coilID string, inventory int) (*models.Coil, error) {
	log.Info().
		Str("coil_id", coilID).
		Int("inventory", inventory).
		Msg("Manually setting coil inventory")

	// Validate inventory range
	if inventory < 0 || inventory > 10 {
		return nil, fmt.Errorf("inventory must be between 0 and 10, got %d", inventory)
	}

	tx, err := s.db.Begin()
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	// Update inventory without version check (admin override)
	if err := s.coilRepo.UpdateInventoryManual(tx, coilID, inventory); err != nil {
		return nil, fmt.Errorf("failed to update inventory: %w", err)
	}

	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("failed to commit inventory update: %w", err)
	}

	// Fetch updated coil
	coil, err := s.coilRepo.GetByID(coilID)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch updated coil: %w", err)
	}

	log.Info().
		Str("coil_id", coilID).
		Int("inventory", coil.Inventory).
		Msg("Coil inventory updated successfully")

	return coil, nil
}
