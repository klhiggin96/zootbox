package models

import "time"

// ProductLink represents multi-coil product configuration
type ProductLink struct {
	LinkGroupID       string    `json:"link_group_id" db:"link_group_id"` // UUID
	ProductSKU        string    `json:"product_sku" db:"product_sku"`
	LinkedCoilIDs     string    `json:"linked_coil_ids" db:"linked_coil_ids"` // JSON array as string
	SelectionStrategy string    `json:"selection_strategy" db:"selection_strategy"`
	CreatedAt         time.Time `json:"created_at" db:"created_at"`
}

// SelectionStrategy constants
const (
	SelectionStrategyFirstAvailable = "first_available"
)
