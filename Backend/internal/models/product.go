package models

import "time"

// Product represents a vending machine product with pricing
type Product struct {
	ID             string    `json:"id" db:"id"`
	Name           string    `json:"name" db:"name"`
	Category       string    `json:"category" db:"category"`
	Price          float64   `json:"price" db:"price"`
	AgeRestriction int       `json:"age_restriction" db:"age_restriction"`
	ImageURL       string    `json:"image_url,omitempty" db:"image_url"`
	VideoFilename  string    `json:"video_filename,omitempty" db:"video_filename"`
	IsDigital      bool      `json:"is_digital" db:"is_digital"`
	Active         bool      `json:"active" db:"active"`
	CreatedAt      time.Time `json:"created_at" db:"created_at"`
	UpdatedAt      time.Time `json:"updated_at" db:"updated_at"`
}

// ProductWithInventory extends Product with available coil inventory
type ProductWithInventory struct {
	Product
	AvailableCoils []CoilInventory `json:"available_coils"`
	TotalInventory int             `json:"total_inventory"`
}

// CoilInventory represents inventory for a specific coil assigned to a product
type CoilInventory struct {
	CoilID    string `json:"coil_id" db:"coil_id"`
	Inventory int    `json:"inventory" db:"inventory"`
	Status    string `json:"status" db:"status"`
	Priority  int    `json:"priority" db:"priority"`
}

// ProductCoilAssignment represents the mapping between products and coils
type ProductCoilAssignment struct {
	ID        string    `json:"id" db:"id"`
	ProductID string    `json:"product_id" db:"product_id"`
	CoilID    string    `json:"coil_id" db:"coil_id"`
	Priority  int       `json:"priority" db:"priority"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
}

// CartSession represents a shopping cart session
type CartSession struct {
	ID                   string     `json:"id" db:"id"`
	DeviceID             string     `json:"device_id" db:"device_id"`
	TotalAmount          float64    `json:"total_amount" db:"total_amount"`
	ItemCount            int        `json:"item_count" db:"item_count"`
	Status               string     `json:"status" db:"status"` // active, paid, dispensing, completed, cancelled
	PaymentTransactionID *string    `json:"payment_transaction_id,omitempty" db:"payment_transaction_id"`
	CreatedAt            time.Time  `json:"created_at" db:"created_at"`
	CompletedAt          *time.Time `json:"completed_at,omitempty" db:"completed_at"`
}

// CartItem represents an item in a shopping cart
type CartItem struct {
	ID            string  `json:"id" db:"id"`
	CartSessionID string  `json:"cart_session_id" db:"cart_session_id"`
	ProductID     string  `json:"product_id" db:"product_id"`
	CoilID        *string `json:"coil_id,omitempty" db:"coil_id"`
	Quantity      int     `json:"quantity" db:"quantity"`
	UnitPrice     float64 `json:"unit_price" db:"unit_price"`
}

// CartSessionWithItems includes the cart session and all its items
type CartSessionWithItems struct {
	Session CartSession         `json:"session"`
	Items   []CartItemWithProduct `json:"items"`
}

// CartItemWithProduct extends CartItem with product details
type CartItemWithProduct struct {
	CartItem
	Product Product `json:"product"`
}

// Cart status constants
const (
	CartStatusActive     = "active"
	CartStatusPaid       = "paid"
	CartStatusDispensing = "dispensing"
	CartStatusCompleted  = "completed"
	CartStatusCancelled  = "cancelled"
)

// Payment method constants
const (
	PaymentMethodCard = "card"
	PaymentMethodNFC  = "nfc"
	PaymentMethodCash = "cash"
	PaymentMethodFree = "free"
)

// Payment status constants
const (
	PaymentStatusPending  = "pending"
	PaymentStatusApproved = "approved"
	PaymentStatusDeclined = "declined"
	PaymentStatusRefunded = "refunded"
)
