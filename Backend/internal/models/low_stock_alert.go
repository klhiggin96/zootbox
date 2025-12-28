package models

import "time"

// LowStockAlert represents a pending alert for external monitoring
type LowStockAlert struct {
	ID                string     `json:"id" db:"id"` // UUID
	CoilID            string     `json:"coil_id" db:"coil_id"`
	Timestamp         time.Time  `json:"timestamp" db:"timestamp"`
	InventoryAtAlert  int        `json:"inventory_at_alert" db:"inventory_at_alert"`
	DeliveryStatus    string     `json:"delivery_status" db:"delivery_status"` // pending | sent | failed
	RetryCount        int        `json:"retry_count" db:"retry_count"`
	DeliveredAt       *time.Time `json:"delivered_at,omitempty" db:"delivered_at"` // nullable
}

// DeliveryStatus constants
const (
	DeliveryStatusPending = "pending"
	DeliveryStatusSent    = "sent"
	DeliveryStatusFailed  = "failed"
)
