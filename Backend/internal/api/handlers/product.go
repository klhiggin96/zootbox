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

// ProductHandler handles product catalog endpoints
type ProductHandler struct {
	db *sql.DB
}

// NewProductHandler creates a new product handler
func NewProductHandler(db *sql.DB) *ProductHandler {
	return &ProductHandler{db: db}
}

// ListProducts returns all active products with inventory information
// GET /api/v1/products
func (h *ProductHandler) ListProducts(w http.ResponseWriter, r *http.Request) {
	database := h.db

	// Query products with their assigned coils and inventory
	query := `
		SELECT
			p.id, p.name, p.category, p.price, p.age_restriction,
			p.image_url, p.video_filename, p.is_digital, p.active,
			p.created_at, p.updated_at,
			COALESCE(c.id, '') as coil_id,
			COALESCE(c.inventory, 0) as inventory,
			COALESCE(c.status, '') as status,
			COALESCE(pca.priority, 0) as priority
		FROM products p
		LEFT JOIN product_coil_assignments pca ON p.id = pca.product_id
		LEFT JOIN coils c ON pca.coil_id = c.id
		WHERE p.active = TRUE
		ORDER BY p.category, p.name, pca.priority
	`

	rows, err := database.Query(query)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch products", err)
		return
	}
	defer rows.Close()

	// Group products with their coils
	productsMap := make(map[string]*models.ProductWithInventory)

	for rows.Next() {
		var p models.Product
		var coilInv models.CoilInventory

		err := rows.Scan(
			&p.ID, &p.Name, &p.Category, &p.Price, &p.AgeRestriction,
			&p.ImageURL, &p.VideoFilename, &p.IsDigital, &p.Active,
			&p.CreatedAt, &p.UpdatedAt,
			&coilInv.CoilID, &coilInv.Inventory, &coilInv.Status, &coilInv.Priority,
		)
		if err != nil {
			respondWithError(w, http.StatusInternalServerError, "Failed to scan product", err)
			return
		}

		// Get or create ProductWithInventory
		pwi, exists := productsMap[p.ID]
		if !exists {
			pwi = &models.ProductWithInventory{
				Product:        p,
				AvailableCoils: []models.CoilInventory{},
				TotalInventory: 0,
			}
			productsMap[p.ID] = pwi
		}

		// Add coil if it exists
		if coilInv.CoilID != "" {
			pwi.AvailableCoils = append(pwi.AvailableCoils, coilInv)
			pwi.TotalInventory += coilInv.Inventory
		}
	}

	// Convert map to slice
	products := make([]models.ProductWithInventory, 0, len(productsMap))
	for _, pwi := range productsMap {
		products = append(products, *pwi)
	}

	respondWithJSON(w, http.StatusOK, products)
}

// GetProduct returns a single product by ID with inventory
// GET /api/v1/products/{id}
func (h *ProductHandler) GetProduct(w http.ResponseWriter, r *http.Request) {
	productID := chi.URLParam(r, "id")

	database := h.db

	// Get product details
	var product models.Product
	query := `
		SELECT id, name, category, price, age_restriction,
		       image_url, video_filename, is_digital, active,
		       created_at, updated_at
		FROM products
		WHERE id = ?
	`

	err := database.QueryRow(query, productID).Scan(
		&product.ID, &product.Name, &product.Category, &product.Price,
		&product.AgeRestriction, &product.ImageURL, &product.VideoFilename,
		&product.IsDigital, &product.Active, &product.CreatedAt, &product.UpdatedAt,
	)
	if err == sql.ErrNoRows {
		respondWithError(w, http.StatusNotFound, "Product not found", err)
		return
	}
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch product", err)
		return
	}

	// Get assigned coils and inventory
	coilQuery := `
		SELECT c.id, c.inventory, c.status, pca.priority
		FROM product_coil_assignments pca
		JOIN coils c ON pca.coil_id = c.id
		WHERE pca.product_id = ?
		ORDER BY pca.priority
	`

	rows, err := database.Query(coilQuery, productID)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to fetch coils", err)
		return
	}
	defer rows.Close()

	pwi := models.ProductWithInventory{
		Product:        product,
		AvailableCoils: []models.CoilInventory{},
		TotalInventory: 0,
	}

	for rows.Next() {
		var coilInv models.CoilInventory
		err := rows.Scan(&coilInv.CoilID, &coilInv.Inventory, &coilInv.Status, &coilInv.Priority)
		if err != nil {
			respondWithError(w, http.StatusInternalServerError, "Failed to scan coil", err)
			return
		}
		pwi.AvailableCoils = append(pwi.AvailableCoils, coilInv)
		pwi.TotalInventory += coilInv.Inventory
	}

	respondWithJSON(w, http.StatusOK, pwi)
}

// CreateProduct creates a new product (admin only)
// POST /api/v1/products
func (h *ProductHandler) CreateProduct(w http.ResponseWriter, r *http.Request) {
	var product models.Product

	decoder := json.NewDecoder(r.Body)
	if err := decoder.Decode(&product); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload", err)
		return
	}
	defer r.Body.Close()

	// Generate ID if not provided
	if product.ID == "" {
		product.ID = fmt.Sprintf("PRD%s", uuid.New().String()[:8])
	}

	// Set timestamps
	now := time.Now()
	product.CreatedAt = now
	product.UpdatedAt = now

	database := h.db

	query := `
		INSERT INTO products (id, name, category, price, age_restriction,
		                      image_url, video_filename, is_digital, active,
		                      created_at, updated_at)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
	`

	_, err := database.Exec(query,
		product.ID, product.Name, product.Category, product.Price, product.AgeRestriction,
		product.ImageURL, product.VideoFilename, product.IsDigital, product.Active,
		product.CreatedAt, product.UpdatedAt,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to create product", err)
		return
	}

	respondWithJSON(w, http.StatusCreated, product)
}

// UpdateProduct updates an existing product (admin only)
// PUT /api/v1/products/{id}
func (h *ProductHandler) UpdateProduct(w http.ResponseWriter, r *http.Request) {
	productID := chi.URLParam(r, "id")

	var product models.Product
	decoder := json.NewDecoder(r.Body)
	if err := decoder.Decode(&product); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload", err)
		return
	}
	defer r.Body.Close()

	product.ID = productID
	product.UpdatedAt = time.Now()

	database := h.db

	query := `
		UPDATE products
		SET name = ?, category = ?, price = ?, age_restriction = ?,
		    image_url = ?, video_filename = ?, is_digital = ?, active = ?,
		    updated_at = ?
		WHERE id = ?
	`

	result, err := database.Exec(query,
		product.Name, product.Category, product.Price, product.AgeRestriction,
		product.ImageURL, product.VideoFilename, product.IsDigital, product.Active,
		product.UpdatedAt, product.ID,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to update product", err)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to check update result", err)
		return
	}

	if rowsAffected == 0 {
		respondWithError(w, http.StatusNotFound, "Product not found", nil)
		return
	}

	respondWithJSON(w, http.StatusOK, product)
}

// AssignProductToCoil assigns a product to a specific coil
// POST /api/v1/products/{id}/assign-coil
func (h *ProductHandler) AssignProductToCoil(w http.ResponseWriter, r *http.Request) {
	productID := chi.URLParam(r, "id")

	var req struct {
		CoilID   string `json:"coil_id"`
		Priority int    `json:"priority"`
	}

	decoder := json.NewDecoder(r.Body)
	if err := decoder.Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload", err)
		return
	}
	defer r.Body.Close()

	database := h.db

	// Check if product exists
	var exists bool
	err := database.QueryRow("SELECT EXISTS(SELECT 1 FROM products WHERE id = ?)", productID).Scan(&exists)
	if err != nil || !exists {
		respondWithError(w, http.StatusNotFound, "Product not found", err)
		return
	}

	// Check if coil exists
	err = database.QueryRow("SELECT EXISTS(SELECT 1 FROM coils WHERE id = ?)", req.CoilID).Scan(&exists)
	if err != nil || !exists {
		respondWithError(w, http.StatusNotFound, "Coil not found", err)
		return
	}

	assignment := models.ProductCoilAssignment{
		ID:        fmt.Sprintf("ASSIGN%s", uuid.New().String()[:8]),
		ProductID: productID,
		CoilID:    req.CoilID,
		Priority:  req.Priority,
		CreatedAt: time.Now(),
	}

	query := `
		INSERT INTO product_coil_assignments (id, product_id, coil_id, priority, created_at)
		VALUES (?, ?, ?, ?, ?)
		ON CONFLICT (product_id, coil_id) DO UPDATE SET priority = ?
	`

	_, err = database.Exec(query,
		assignment.ID, assignment.ProductID, assignment.CoilID, assignment.Priority,
		assignment.CreatedAt, assignment.Priority,
	)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, "Failed to assign product to coil", err)
		return
	}

	respondWithJSON(w, http.StatusOK, assignment)
}

// Helper functions
func respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, err := json.Marshal(payload)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Failed to marshal response"))
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}

func respondWithError(w http.ResponseWriter, code int, message string, err error) {
	errorResponse := map[string]string{"error": message}
	if err != nil {
		errorResponse["details"] = err.Error()
	}
	respondWithJSON(w, code, errorResponse)
}
