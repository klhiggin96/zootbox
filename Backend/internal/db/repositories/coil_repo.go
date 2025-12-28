package repositories

import (
	"database/sql"
	"fmt"

	"github.com/zootbox/backend/internal/models"
)

// CoilRepository handles database operations for coils
type CoilRepository struct {
	db *sql.DB
}

// NewCoilRepository creates a new coil repository
func NewCoilRepository(db *sql.DB) *CoilRepository {
	return &CoilRepository{db: db}
}

// GetByID retrieves a coil by ID
func (r *CoilRepository) GetByID(coilID string) (*models.Coil, error) {
	query := `SELECT id, inventory, status, version, link_group_id, updated_at
	          FROM coils WHERE id = ?`

	var coil models.Coil
	err := r.db.QueryRow(query, coilID).Scan(
		&coil.ID,
		&coil.Inventory,
		&coil.Status,
		&coil.Version,
		&coil.LinkGroupID,
		&coil.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("coil %s not found", coilID)
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get coil: %w", err)
	}

	return &coil, nil
}

// GetAll retrieves all 100 coils
func (r *CoilRepository) GetAll() ([]*models.Coil, error) {
	query := `SELECT id, inventory, status, version, link_group_id, updated_at
	          FROM coils ORDER BY id`

	rows, err := r.db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("failed to query coils: %w", err)
	}
	defer rows.Close()

	var coils []*models.Coil
	for rows.Next() {
		var coil models.Coil
		err := rows.Scan(
			&coil.ID,
			&coil.Inventory,
			&coil.Status,
			&coil.Version,
			&coil.LinkGroupID,
			&coil.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan coil: %w", err)
		}
		coils = append(coils, &coil)
	}

	if err = rows.Err(); err != nil {
		return nil, fmt.Errorf("rows iteration error: %w", err)
	}

	return coils, nil
}

// GetLowStock retrieves coils with inventory <= 2
func (r *CoilRepository) GetLowStock() ([]*models.Coil, error) {
	query := `SELECT id, inventory, status, version, link_group_id, updated_at
	          FROM coils WHERE inventory <= 2 ORDER BY inventory ASC, id`

	rows, err := r.db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("failed to query low-stock coils: %w", err)
	}
	defer rows.Close()

	var coils []*models.Coil
	for rows.Next() {
		var coil models.Coil
		err := rows.Scan(
			&coil.ID,
			&coil.Inventory,
			&coil.Status,
			&coil.Version,
			&coil.LinkGroupID,
			&coil.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan coil: %w", err)
		}
		coils = append(coils, &coil)
	}

	return coils, nil
}

// UpdateInventory decrements inventory by 1 with optimistic locking
// Returns error if version mismatch (concurrent modification detected)
func (r *CoilRepository) UpdateInventory(tx *sql.Tx, coilID string, expectedVersion int) error {
	query := `UPDATE coils
	          SET inventory = inventory - 1, version = version + 1
	          WHERE id = ? AND version = ? AND inventory > 0`

	result, err := tx.Exec(query, coilID, expectedVersion)
	if err != nil {
		return fmt.Errorf("failed to update inventory: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("version conflict or inventory is 0 for coil %s", coilID)
	}

	return nil
}

// RefillAll sets all coils to inventory=10
func (r *CoilRepository) RefillAll(tx *sql.Tx) (int64, error) {
	query := `UPDATE coils SET inventory = 10, version = version + 1`

	result, err := tx.Exec(query)
	if err != nil {
		return 0, fmt.Errorf("failed to refill all coils: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return 0, fmt.Errorf("failed to get rows affected: %w", err)
	}

	return rowsAffected, nil
}

// UpdateInventoryManual manually sets a coil's inventory (admin override, no version check)
func (r *CoilRepository) UpdateInventoryManual(tx *sql.Tx, coilID string, inventory int) error {
	query := `UPDATE coils
	          SET inventory = ?, version = version + 1
	          WHERE id = ?`

	result, err := tx.Exec(query, inventory, coilID)
	if err != nil {
		return fmt.Errorf("failed to manually update inventory: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("coil %s not found", coilID)
	}

	return nil
}
