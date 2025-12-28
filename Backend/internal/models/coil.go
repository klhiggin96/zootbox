package models

import "time"

// Coil represents a vending machine coil position (A1-J10)
type Coil struct {
	ID           string    `json:"id" db:"id"`                                // A1-J10
	Inventory    int       `json:"inventory" db:"inventory"`                  // 0-10
	Status       string    `json:"status" db:"status"`                        // available | jammed
	Version      int       `json:"version" db:"version"`                      // optimistic locking
	LinkGroupID  *string   `json:"link_group_id,omitempty" db:"link_group_id"` // nullable FK to product_links
	UpdatedAt    time.Time `json:"updated_at" db:"updated_at"`
}

// CoilStatus constants
const (
	CoilStatusAvailable = "available"
	CoilStatusJammed    = "jammed"
)

// IsAvailable returns true if coil is available for vending
func (c *Coil) IsAvailable() bool {
	return c.Status == CoilStatusAvailable && c.Inventory > 0
}

// CanDecrement returns true if inventory can be decremented
func (c *Coil) CanDecrement() bool {
	return c.Inventory > 0
}
