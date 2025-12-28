package services

import (
	"database/sql"

	"github.com/zootbox/backend/internal/db/repositories"
	"github.com/zootbox/backend/internal/models"
)

// InventoryService handles inventory query operations
type InventoryService struct {
	db       *sql.DB
	coilRepo *repositories.CoilRepository
}

// NewInventoryService creates a new inventory service
func NewInventoryService(db *sql.DB) *InventoryService {
	return &InventoryService{
		db:       db,
		coilRepo: repositories.NewCoilRepository(db),
	}
}

// GetCoil retrieves a single coil by ID
func (s *InventoryService) GetCoil(coilID string) (*models.Coil, error) {
	return s.coilRepo.GetByID(coilID)
}

// GetAllCoils retrieves all 100 coils
func (s *InventoryService) GetAllCoils() ([]*models.Coil, error) {
	return s.coilRepo.GetAll()
}

// GetLowStockCoils retrieves coils with inventory <= 2
func (s *InventoryService) GetLowStockCoils() ([]*models.Coil, error) {
	return s.coilRepo.GetLowStock()
}
