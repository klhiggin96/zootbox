package models

import "time"

// Transaction represents a vend event reported by Android app
type Transaction struct {
	ID                  string    `json:"id" db:"id"`                         // UUID
	CoilID              string    `json:"coil_id" db:"coil_id"`
	Timestamp           time.Time `json:"timestamp" db:"timestamp"`
	Status              string    `json:"status" db:"status"`                 // success | jam | failed
	TransactionID       string    `json:"transaction_id" db:"transaction_id"` // Android-generated transaction ID
	InventoryBefore     int       `json:"inventory_before" db:"inventory_before"`
	InventoryAfter      *int      `json:"inventory_after,omitempty" db:"inventory_after"` // nullable

	// Payment fields (added in migration 002)
	Amount              *float64  `json:"amount,omitempty" db:"amount"`                     // Transaction amount in USD
	PaymentMethod       *string   `json:"payment_method,omitempty" db:"payment_method"`     // card, nfc, cash, free
	PaymentStatus       *string   `json:"payment_status,omitempty" db:"payment_status"`     // pending, approved, declined, refunded
	Currency            *string   `json:"currency,omitempty" db:"currency"`                 // USD, EUR, etc.
	NayaxTransactionID  *string   `json:"nayax_transaction_id,omitempty" db:"nayax_transaction_id"` // From VPOS Touch
	ProductID           *string   `json:"product_id,omitempty" db:"product_id"`             // Product SKU
}

// TransactionStatus constants
const (
	TransactionStatusSuccess = "success"
	TransactionStatusJam     = "jam"
	TransactionStatusFailed  = "failed"
)

// IsSuccess returns true if transaction completed successfully
func (t *Transaction) IsSuccess() bool {
	return t.Status == TransactionStatusSuccess
}

// IsJam returns true if transaction resulted in jam
func (t *Transaction) IsJam() bool {
	return t.Status == TransactionStatusJam
}
