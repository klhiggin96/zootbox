package services

import (
	"database/sql"
	"encoding/json"
	"fmt"

	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/db/repositories"
	"github.com/zootbox/backend/internal/models"
)

// ProductLinkService handles product linking operations
type ProductLinkService struct {
	db              *sql.DB
	productLinkRepo *repositories.ProductLinkRepository
	coilRepo        *repositories.CoilRepository
}

// NewProductLinkService creates a new product link service
func NewProductLinkService(db *sql.DB) *ProductLinkService {
	return &ProductLinkService{
		db:              db,
		productLinkRepo: repositories.NewProductLinkRepository(db),
		coilRepo:        repositories.NewCoilRepository(db),
	}
}

// CreateProductLinkRequest represents a request to create a product link
type CreateProductLinkRequest struct {
	ProductSKU        string   `json:"product_sku"`
	LinkedCoilIDs     []string `json:"linked_coil_ids"`
	SelectionStrategy string   `json:"selection_strategy"`
}

// CreateProductLink creates a new product link group
func (s *ProductLinkService) CreateProductLink(req *CreateProductLinkRequest) (*models.ProductLink, error) {
	log.Info().
		Str("product_sku", req.ProductSKU).
		Int("linked_coils", len(req.LinkedCoilIDs)).
		Str("strategy", req.SelectionStrategy).
		Msg("Creating product link")

	// Validate request
	if req.ProductSKU == "" {
		return nil, fmt.Errorf("product_sku is required")
	}
	if len(req.LinkedCoilIDs) < 2 {
		return nil, fmt.Errorf("linked_coil_ids must contain at least 2 coils")
	}
	if req.SelectionStrategy == "" {
		req.SelectionStrategy = models.SelectionStrategyFirstAvailable
	}
	if req.SelectionStrategy != models.SelectionStrategyFirstAvailable {
		return nil, fmt.Errorf("invalid selection_strategy: %s", req.SelectionStrategy)
	}

	// Validate all coil IDs exist
	for _, coilID := range req.LinkedCoilIDs {
		if _, err := s.coilRepo.GetByID(coilID); err != nil {
			return nil, fmt.Errorf("coil %s not found", coilID)
		}
	}

	tx, err := s.db.Begin()
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	productLink, err := s.productLinkRepo.Create(tx, req.ProductSKU, req.LinkedCoilIDs, req.SelectionStrategy)
	if err != nil {
		return nil, fmt.Errorf("failed to create product link: %w", err)
	}

	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("failed to commit product link creation: %w", err)
	}

	log.Info().
		Str("link_group_id", productLink.LinkGroupID).
		Str("product_sku", productLink.ProductSKU).
		Msg("Product link created successfully")

	return productLink, nil
}

// ResolveProductToCoilResponse represents the response from resolving a product to a coil
type ResolveProductToCoilResponse struct {
	ProductSKU  string `json:"product_sku"`
	ResolvedTo  string `json:"resolved_to"`   // Coil ID
	InventoryAt int    `json:"inventory_at"`  // Inventory of resolved coil
	Strategy    string `json:"strategy"`
}

// ResolveProductToCoil resolves a product SKU to a specific coil based on selection strategy
func (s *ProductLinkService) ResolveProductToCoil(sku string) (*ResolveProductToCoilResponse, error) {
	log.Info().Str("product_sku", sku).Msg("Resolving product to coil")

	// Fetch product link
	productLink, err := s.productLinkRepo.GetBySKU(sku)
	if err != nil {
		return nil, fmt.Errorf("failed to find product link: %w", err)
	}

	// Parse linked coil IDs from JSON
	var linkedCoilIDs []string
	if err := json.Unmarshal([]byte(productLink.LinkedCoilIDs), &linkedCoilIDs); err != nil {
		return nil, fmt.Errorf("failed to parse linked coil IDs: %w", err)
	}

	if len(linkedCoilIDs) == 0 {
		return nil, fmt.Errorf("product link has no linked coils")
	}

	// Apply selection strategy: first_available
	var selectedCoilID string
	var selectedInventory int

	for _, coilID := range linkedCoilIDs {
		coil, err := s.coilRepo.GetByID(coilID)
		if err != nil {
			log.Warn().Str("coil_id", coilID).Msg("Linked coil not found, skipping")
			continue
		}

		if coil.Inventory > 0 {
			selectedCoilID = coil.ID
			selectedInventory = coil.Inventory
			break
		}
	}

	if selectedCoilID == "" {
		return nil, fmt.Errorf("no available coils for product SKU %s (all linked coils empty)", sku)
	}

	log.Info().
		Str("product_sku", sku).
		Str("resolved_coil", selectedCoilID).
		Int("inventory", selectedInventory).
		Msg("Product resolved to coil")

	return &ResolveProductToCoilResponse{
		ProductSKU:  sku,
		ResolvedTo:  selectedCoilID,
		InventoryAt: selectedInventory,
		Strategy:    productLink.SelectionStrategy,
	}, nil
}

// GetAllProductLinks retrieves all product links
func (s *ProductLinkService) GetAllProductLinks() ([]*models.ProductLink, error) {
	return s.productLinkRepo.GetAll()
}

// DeleteProductLink deletes a product link
func (s *ProductLinkService) DeleteProductLink(linkGroupID string) error {
	log.Info().Str("link_group_id", linkGroupID).Msg("Deleting product link")

	tx, err := s.db.Begin()
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	if err := s.productLinkRepo.Delete(tx, linkGroupID); err != nil {
		return fmt.Errorf("failed to delete product link: %w", err)
	}

	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit product link deletion: %w", err)
	}

	log.Info().Str("link_group_id", linkGroupID).Msg("Product link deleted successfully")

	return nil
}
