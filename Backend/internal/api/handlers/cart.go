package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/zootbox/backend/internal/models"
)

// CartHandler handles shopping cart endpoints
type CartHandler struct {
	db *sql.DB
}

// NewCartHandler creates a new cart handler
func NewCartHandler(db *sql.DB) *CartHandler {
	return &CartHandler{db: db}
}

// CreateCart creates a new shopping cart session
// POST /api/v1/cart/create
func (h *CartHandler) CreateCart(w http.ResponseWriter, r *http.Request) {
	var req struct {
		DeviceID string `json:"device_id"`
	}

	decoder := json.NewDecoder(r.Body)
	if err := decoder.Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload", err)
		return
	}
	defer r.Body.Close()

	if req.DeviceID == "" {
		respondWithError(w, http.StatusBadRequest, "Device ID is required", nil)
		return
	}

	database := h.db

	// Create new cart session
	cartSession := models.CartSession{
		ID:          fmt.Sprintf("CART%s", uuid.New().String()[:8]),
		DeviceID:    req.DeviceID,
		TotalAmount: 0.0,
		ItemCount:   0,
		Status:      models.CartStatusActive,
		CreatedAt:   time.Now(),
	}

	query := `
		INSERT INTO cart_sessions (id, device_id, total_amount, item_count, status, created_at)
		VALUES (?, ?, ?, ?, ?, ?)
	`

	_, err := database.Exec(query,
		cartSession.ID, cartSession.DeviceID, cartSession.TotalAmount,
		cartSession.ItemCount, cartSession.Status, cartSession.CreatedAt,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to create cart session", err)
		return
	}

	respondWithJSON(w, http.StatusCreated, cartSession)
}

// AddItemToCart adds a product to an existing cart
// POST /api/v1/cart/{id}/add-item
func (h *CartHandler) AddItemToCart(w http.ResponseWriter, r *http.Request) {
	cartID := chi.URLParam(r, "id")

	var req struct {
		ProductID string `json:"product_id"`
		Quantity  int    `json:"quantity"`
		CoilID    string `json:"coil_id,omitempty"` // Optional: pre-assign coil
	}

	decoder := json.NewDecoder(r.Body)
	if err := decoder.Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload", err)
		return
	}
	defer r.Body.Close()

	if req.Quantity <= 0 {
		respondWithError(w, http.StatusBadRequest, "Quantity must be greater than 0", nil)
		return
	}

	database := h.db

	// Start transaction
	tx, err := database.Begin()
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to start transaction", err)
		return
	}
	defer tx.Rollback()

	// Verify cart exists and is active
	var cartStatus string
	err = tx.QueryRow("SELECT status FROM cart_sessions WHERE id = ?", cartID).Scan(&cartStatus)
	if err == sql.ErrNoRows {
		respondWithError(w, http.StatusNotFound, "Cart session not found", err)
		return
	}
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch cart session", err)
		return
	}
	if cartStatus != models.CartStatusActive {
		respondWithError(w, http.StatusBadRequest, "Cart is not active", nil)
		return
	}

	// Get product details and verify it exists
	var product models.Product
	productQuery := `
		SELECT id, name, category, price, age_restriction, active
		FROM products
		WHERE id = ?
	`
	err = tx.QueryRow(productQuery, req.ProductID).Scan(
		&product.ID, &product.Name, &product.Category,
		&product.Price, &product.AgeRestriction, &product.Active,
	)
	if err == sql.ErrNoRows {
		respondWithError(w, http.StatusNotFound, "Product not found", err)
		return
	}
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch product", err)
		return
	}
	if !product.Active {
		respondWithError(w, http.StatusBadRequest, "Product is not available", nil)
		return
	}

	// Check inventory availability if coil is specified or product is physical
	if !product.IsDigital {
		var totalInventory int
		inventoryQuery := `
			SELECT COALESCE(SUM(c.inventory), 0)
			FROM product_coil_assignments pca
			JOIN coils c ON pca.coil_id = c.id
			WHERE pca.product_id = ? AND c.status = 'normal'
		`
		err = tx.QueryRow(inventoryQuery, req.ProductID).Scan(&totalInventory)
		if err != nil {
			respondWithError(w, http.StatusInternalServerError, "Failed to check inventory", err)
			return
		}
		if totalInventory < req.Quantity {
			respondWithError(w, http.StatusBadRequest, "Insufficient inventory", nil)
			return
		}
	}

	// Create cart item
	cartItem := models.CartItem{
		ID:            fmt.Sprintf("ITEM%s", uuid.New().String()[:8]),
		CartSessionID: cartID,
		ProductID:     req.ProductID,
		Quantity:      req.Quantity,
		UnitPrice:     product.Price,
	}

	if req.CoilID != "" {
		cartItem.CoilID = &req.CoilID
	}

	itemQuery := `
		INSERT INTO cart_items (id, cart_session_id, product_id, coil_id, quantity, unit_price)
		VALUES (?, ?, ?, ?, ?, ?)
	`
	_, err = tx.Exec(itemQuery,
		cartItem.ID, cartItem.CartSessionID, cartItem.ProductID,
		cartItem.CoilID, cartItem.Quantity, cartItem.UnitPrice,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to add item to cart", err)
		return
	}

	// Update cart session totals
	updateQuery := `
		UPDATE cart_sessions
		SET total_amount = total_amount + ?,
		    item_count = item_count + ?
		WHERE id = ?
	`
	itemTotal := product.Price * float64(req.Quantity)
	_, err = tx.Exec(updateQuery, itemTotal, req.Quantity, cartID)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to update cart totals", err)
		return
	}

	// Commit transaction
	if err = tx.Commit(); err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to commit transaction", err)
		return
	}

	respondWithJSON(w, http.StatusCreated, cartItem)
}

// RemoveItemFromCart removes an item from the cart
// DELETE /api/v1/cart/{id}/remove-item/{item_id}
func (h *CartHandler) RemoveItemFromCart(w http.ResponseWriter, r *http.Request) {
	cartID := chi.URLParam(r, "id")
	itemID := chi.URLParam(r, "item_id")

	database := h.db

	// Start transaction
	tx, err := database.Begin()
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to start transaction", err)
		return
	}
	defer tx.Rollback()

	// Get item details before deletion
	var quantity int
	var unitPrice float64
	err = tx.QueryRow(
		"SELECT quantity, unit_price FROM cart_items WHERE id = ? AND cart_session_id = ?",
		itemID, cartID,
	).Scan(&quantity, &unitPrice)
	if err == sql.ErrNoRows {
		respondWithError(w, http.StatusNotFound, "Cart item not found", err)
		return
	}
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch cart item", err)
		return
	}

	// Delete the item
	result, err := tx.Exec("DELETE FROM cart_items WHERE id = ? AND cart_session_id = ?", itemID, cartID)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to remove item", err)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil || rowsAffected == 0 {
		respondWithError(w, http.StatusNotFound, "Cart item not found", err)
		return
	}

	// Update cart session totals
	itemTotal := unitPrice * float64(quantity)
	updateQuery := `
		UPDATE cart_sessions
		SET total_amount = total_amount - ?,
		    item_count = item_count - ?
		WHERE id = ?
	`
	_, err = tx.Exec(updateQuery, itemTotal, quantity, cartID)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to update cart totals", err)
		return
	}

	// Commit transaction
	if err = tx.Commit(); err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to commit transaction", err)
		return
	}

	respondWithJSON(w, http.StatusOK, map[string]string{"message": "Item removed successfully"})
}

// GetCart retrieves cart session with all items and product details
// GET /api/v1/cart/{id}
func (h *CartHandler) GetCart(w http.ResponseWriter, r *http.Request) {
	cartID := chi.URLParam(r, "id")

	database := h.db

	// Get cart session
	var session models.CartSession
	sessionQuery := `
		SELECT id, device_id, total_amount, item_count, status,
		       payment_transaction_id, created_at, completed_at
		FROM cart_sessions
		WHERE id = ?
	`
	err := database.QueryRow(sessionQuery, cartID).Scan(
		&session.ID, &session.DeviceID, &session.TotalAmount, &session.ItemCount,
		&session.Status, &session.PaymentTransactionID, &session.CreatedAt, &session.CompletedAt,
	)
	if err == sql.ErrNoRows {
		respondWithError(w, http.StatusNotFound, "Cart session not found", err)
		return
	}
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch cart session", err)
		return
	}

	// Get cart items with product details
	itemsQuery := `
		SELECT ci.id, ci.cart_session_id, ci.product_id, ci.coil_id,
		       ci.quantity, ci.unit_price,
		       p.id, p.name, p.category, p.price, p.age_restriction,
		       p.image_url, p.video_filename, p.is_digital, p.active,
		       p.created_at, p.updated_at
		FROM cart_items ci
		JOIN products p ON ci.product_id = p.id
		WHERE ci.cart_session_id = ?
		ORDER BY ci.id
	`
	rows, err := database.Query(itemsQuery, cartID)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch cart items", err)
		return
	}
	defer rows.Close()

	items := []models.CartItemWithProduct{}
	for rows.Next() {
		var item models.CartItemWithProduct
		err := rows.Scan(
			&item.ID, &item.CartSessionID, &item.ProductID, &item.CoilID,
			&item.Quantity, &item.UnitPrice,
			&item.Product.ID, &item.Product.Name, &item.Product.Category,
			&item.Product.Price, &item.Product.AgeRestriction,
			&item.Product.ImageURL, &item.Product.VideoFilename,
			&item.Product.IsDigital, &item.Product.Active,
			&item.Product.CreatedAt, &item.Product.UpdatedAt,
		)
		if err != nil {
			respondWithError(w, http.StatusInternalServerError, "Failed to scan cart item", err)
			return
		}
		items = append(items, item)
	}

	response := models.CartSessionWithItems{
		Session: session,
		Items:   items,
	}

	respondWithJSON(w, http.StatusOK, response)
}

// CheckoutCart prepares cart for payment processing
// POST /api/v1/cart/{id}/checkout
func (h *CartHandler) CheckoutCart(w http.ResponseWriter, r *http.Request) {
	cartID := chi.URLParam(r, "id")

	database := h.db

	// Start transaction
	tx, err := database.Begin()
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to start transaction", err)
		return
	}
	defer tx.Rollback()

	// Verify cart exists and is active
	var session models.CartSession
	err = tx.QueryRow(
		"SELECT id, device_id, total_amount, item_count, status FROM cart_sessions WHERE id = ?",
		cartID,
	).Scan(&session.ID, &session.DeviceID, &session.TotalAmount, &session.ItemCount, &session.Status)
	if err == sql.ErrNoRows {
		respondWithError(w, http.StatusNotFound, "Cart session not found", err)
		return
	}
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch cart session", err)
		return
	}

	if session.Status != models.CartStatusActive {
		respondWithError(w, http.StatusBadRequest, "Cart is not active", nil)
		return
	}

	if session.ItemCount == 0 {
		respondWithError(w, http.StatusBadRequest, "Cart is empty", nil)
		return
	}

	// Verify inventory availability for all items
	itemsQuery := `
		SELECT ci.product_id, ci.quantity, p.is_digital
		FROM cart_items ci
		JOIN products p ON ci.product_id = p.id
		WHERE ci.cart_session_id = ?
	`
	rows, err := tx.Query(itemsQuery, cartID)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch cart items", err)
		return
	}
	defer rows.Close()

	for rows.Next() {
		var productID string
		var quantity int
		var isDigital bool
		err := rows.Scan(&productID, &quantity, &isDigital)
		if err != nil {
			respondWithError(w, http.StatusInternalServerError, "Failed to scan cart item", err)
			return
		}

		// Skip inventory check for digital products
		if isDigital {
			continue
		}

		// Check available inventory
		var totalInventory int
		inventoryQuery := `
			SELECT COALESCE(SUM(c.inventory), 0)
			FROM product_coil_assignments pca
			JOIN coils c ON pca.coil_id = c.id
			WHERE pca.product_id = ? AND c.status = 'normal'
		`
		err = tx.QueryRow(inventoryQuery, productID).Scan(&totalInventory)
		if err != nil {
			respondWithError(w, http.StatusInternalServerError, "Failed to check inventory", err)
			return
		}

		if totalInventory < quantity {
			respondWithError(w, http.StatusBadRequest, fmt.Sprintf("Insufficient inventory for product %s", productID), nil)
			return
		}
	}

	// Update cart status to paid (ready for payment)
	// Note: Payment processing happens on Android, this just validates the cart
	_, err = tx.Exec(
		"UPDATE cart_sessions SET status = ? WHERE id = ?",
		models.CartStatusPaid, cartID,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to update cart status", err)
		return
	}

	// Commit transaction
	if err = tx.Commit(); err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to commit transaction", err)
		return
	}

	response := map[string]interface{}{
		"cart_id":      cartID,
		"total_amount": session.TotalAmount,
		"item_count":   session.ItemCount,
		"status":       models.CartStatusPaid,
		"message":      "Cart ready for payment",
	}

	respondWithJSON(w, http.StatusOK, response)
}

// CancelCart cancels an active cart session
// POST /api/v1/cart/{id}/cancel
func (h *CartHandler) CancelCart(w http.ResponseWriter, r *http.Request) {
	cartID := chi.URLParam(r, "id")

	database := h.db

	result, err := database.Exec(
		"UPDATE cart_sessions SET status = ?, completed_at = ? WHERE id = ? AND status IN (?, ?)",
		models.CartStatusCancelled, time.Now(), cartID,
		models.CartStatusActive, models.CartStatusPaid,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to cancel cart", err)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil || rowsAffected == 0 {
		respondWithError(w, http.StatusNotFound, "Cart not found or already completed", nil)
		return
	}

	respondWithJSON(w, http.StatusOK, map[string]string{"message": "Cart cancelled successfully"})
}
