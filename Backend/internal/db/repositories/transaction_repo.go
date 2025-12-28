package repositories

import (
	"database/sql"
	"fmt"

	"github.com/zootbox/backend/internal/models"
)

// TransactionRepository handles database operations for transactions
type TransactionRepository struct {
	db *sql.DB
}

// NewTransactionRepository creates a new transaction repository
func NewTransactionRepository(db *sql.DB) *TransactionRepository {
	return &TransactionRepository{db: db}
}

// Create inserts a new transaction record
func (r *TransactionRepository) Create(tx *sql.Tx, transaction *models.Transaction) error {
	query := `INSERT INTO transactions (id, coil_id, timestamp, status, transaction_id, inventory_before, inventory_after)
	          VALUES (?, ?, ?, ?, ?, ?, ?)`

	_, err := tx.Exec(
		query,
		transaction.ID,
		transaction.CoilID,
		transaction.Timestamp,
		transaction.Status,
		transaction.TransactionID,
		transaction.InventoryBefore,
		transaction.InventoryAfter,
	)

	if err != nil {
		return fmt.Errorf("failed to create transaction: %w", err)
	}

	return nil
}

// GetByCoilID retrieves all transactions for a specific coil
func (r *TransactionRepository) GetByCoilID(coilID string, limit int) ([]*models.Transaction, error) {
	query := `SELECT id, coil_id, timestamp, status, transaction_id, inventory_before, inventory_after
	          FROM transactions
	          WHERE coil_id = ?
	          ORDER BY timestamp DESC
	          LIMIT ?`

	rows, err := r.db.Query(query, coilID, limit)
	if err != nil {
		return nil, fmt.Errorf("failed to query transactions: %w", err)
	}
	defer rows.Close()

	var transactions []*models.Transaction
	for rows.Next() {
		var txn models.Transaction
		err := rows.Scan(
			&txn.ID,
			&txn.CoilID,
			&txn.Timestamp,
			&txn.Status,
			&txn.TransactionID,
			&txn.InventoryBefore,
			&txn.InventoryAfter,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan transaction: %w", err)
		}
		transactions = append(transactions, &txn)
	}

	return transactions, nil
}
