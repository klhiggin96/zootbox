package repositories

import (
	"database/sql"
	"encoding/json"
	"fmt"

	"github.com/google/uuid"
	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/models"
)

// ProductLinkRepository handles product link data operations
type ProductLinkRepository struct {
	db *sql.DB
}

// NewProductLinkRepository creates a new product link repository
func NewProductLinkRepository(db *sql.DB) *ProductLinkRepository {
	return &ProductLinkRepository{db: db}
}

// Create creates a new product link group
func (r *ProductLinkRepository) Create(tx *sql.Tx, sku string, coilIDs []string, strategy string) (*models.ProductLink, error) {
	linkGroupID := uuid.New().String()

	// Serialize coil IDs to JSON
	coilIDsJSON, err := json.Marshal(coilIDs)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize coil IDs: %w", err)
	}

	query := `
		INSERT INTO product_links (link_group_id, product_sku, linked_coil_ids, selection_strategy, created_at)
		VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
	`

	_, err = tx.Exec(query, linkGroupID, sku, string(coilIDsJSON), strategy)
	if err != nil {
		return nil, fmt.Errorf("failed to insert product link: %w", err)
	}

	log.Info().
		Str("link_group_id", linkGroupID).
		Str("product_sku", sku).
		Int("coil_count", len(coilIDs)).
		Msg("Product link created")

	return r.GetByID(linkGroupID)
}

// GetByID retrieves a product link by link group ID
func (r *ProductLinkRepository) GetByID(linkGroupID string) (*models.ProductLink, error) {
	query := `
		SELECT link_group_id, product_sku, linked_coil_ids, selection_strategy, created_at
		FROM product_links
		WHERE link_group_id = ?
	`

	var pl models.ProductLink
	err := r.db.QueryRow(query, linkGroupID).Scan(
		&pl.LinkGroupID,
		&pl.ProductSKU,
		&pl.LinkedCoilIDs,
		&pl.SelectionStrategy,
		&pl.CreatedAt,
	)
	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("product link %s not found", linkGroupID)
	}
	if err != nil {
		return nil, fmt.Errorf("failed to query product link: %w", err)
	}

	return &pl, nil
}

// GetBySKU retrieves a product link by product SKU
func (r *ProductLinkRepository) GetBySKU(sku string) (*models.ProductLink, error) {
	query := `
		SELECT link_group_id, product_sku, linked_coil_ids, selection_strategy, created_at
		FROM product_links
		WHERE product_sku = ?
	`

	var pl models.ProductLink
	err := r.db.QueryRow(query, sku).Scan(
		&pl.LinkGroupID,
		&pl.ProductSKU,
		&pl.LinkedCoilIDs,
		&pl.SelectionStrategy,
		&pl.CreatedAt,
	)
	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("product link for SKU %s not found", sku)
	}
	if err != nil {
		return nil, fmt.Errorf("failed to query product link: %w", err)
	}

	return &pl, nil
}

// GetAll retrieves all product links
func (r *ProductLinkRepository) GetAll() ([]*models.ProductLink, error) {
	query := `
		SELECT link_group_id, product_sku, linked_coil_ids, selection_strategy, created_at
		FROM product_links
		ORDER BY created_at DESC
	`

	rows, err := r.db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("failed to query product links: %w", err)
	}
	defer rows.Close()

	var productLinks []*models.ProductLink
	for rows.Next() {
		var pl models.ProductLink
		if err := rows.Scan(
			&pl.LinkGroupID,
			&pl.ProductSKU,
			&pl.LinkedCoilIDs,
			&pl.SelectionStrategy,
			&pl.CreatedAt,
		); err != nil {
			return nil, fmt.Errorf("failed to scan product link: %w", err)
		}
		productLinks = append(productLinks, &pl)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating product links: %w", err)
	}

	return productLinks, nil
}

// Delete deletes a product link by link group ID
func (r *ProductLinkRepository) Delete(tx *sql.Tx, linkGroupID string) error {
	query := `DELETE FROM product_links WHERE link_group_id = ?`

	result, err := tx.Exec(query, linkGroupID)
	if err != nil {
		return fmt.Errorf("failed to delete product link: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("product link %s not found", linkGroupID)
	}

	log.Info().Str("link_group_id", linkGroupID).Msg("Product link deleted")

	return nil
}
