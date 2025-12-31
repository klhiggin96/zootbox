package portal

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/rs/zerolog/log"
)

// AnalyticsHandler handles analytics API endpoints for the dashboard
type AnalyticsHandler struct {
	db *sql.DB
}

// NewAnalyticsHandler creates a new analytics handler
func NewAnalyticsHandler(db *sql.DB) *AnalyticsHandler {
	return &AnalyticsHandler{db: db}
}

// MetricsResponse contains key dashboard metrics
type MetricsResponse struct {
	TodayRevenue   float64 `json:"today_revenue"`
	TodaySales     int     `json:"today_sales"`
	AvgTransaction float64 `json:"avg_transaction"`
	Refunds        int     `json:"refunds"`
	RevenueChange  float64 `json:"revenue_change"`
	SalesChange    float64 `json:"sales_change"`
	RefundRate     float64 `json:"refund_rate"`
}

// Metrics returns key metrics for today
func (h *AnalyticsHandler) Metrics(w http.ResponseWriter, r *http.Request) {
	today := time.Now().Truncate(24 * time.Hour)
	yesterday := today.Add(-24 * time.Hour)

	// Today's metrics
	var todayRevenue sql.NullFloat64
	var todaySales int
	err := h.db.QueryRow(`
		SELECT
			COALESCE(SUM(amount), 0) as revenue,
			COUNT(*) as sales
		FROM transactions
		WHERE timestamp >= ?
		AND payment_status = 'approved'
	`, today).Scan(&todayRevenue, &todaySales)

	if err != nil && err != sql.ErrNoRows {
		log.Error().Err(err).Msg("Failed to fetch today's metrics")
		http.Error(w, "Failed to fetch metrics", http.StatusInternalServerError)
		return
	}

	// Yesterday's metrics for comparison
	var yesterdayRevenue sql.NullFloat64
	var yesterdaySales int
	err = h.db.QueryRow(`
		SELECT
			COALESCE(SUM(amount), 0) as revenue,
			COUNT(*) as sales
		FROM transactions
		WHERE timestamp >= ? AND timestamp < ?
		AND payment_status = 'approved'
	`, yesterday, today).Scan(&yesterdayRevenue, &yesterdaySales)

	if err != nil && err != sql.ErrNoRows {
		log.Error().Err(err).Msg("Failed to fetch yesterday's metrics")
	}

	// Average transaction
	var avgTransaction sql.NullFloat64
	err = h.db.QueryRow(`
		SELECT AVG(amount)
		FROM transactions
		WHERE timestamp >= ?
		AND payment_status = 'approved'
		AND amount > 0
	`, today).Scan(&avgTransaction)

	if err != nil && err != sql.ErrNoRows {
		log.Error().Err(err).Msg("Failed to fetch average transaction")
	}

	// Refunds count
	var refunds int
	err = h.db.QueryRow(`
		SELECT COUNT(*)
		FROM transactions
		WHERE timestamp >= ?
		AND payment_status = 'refunded'
	`, today).Scan(&refunds)

	if err != nil && err != sql.ErrNoRows {
		log.Error().Err(err).Msg("Failed to fetch refunds")
	}

	// Calculate changes
	revenueChange := 0.0
	if yesterdayRevenue.Valid && yesterdayRevenue.Float64 > 0 {
		revenueChange = ((todayRevenue.Float64 - yesterdayRevenue.Float64) / yesterdayRevenue.Float64) * 100
	}

	salesChange := 0.0
	if yesterdaySales > 0 {
		salesChange = ((float64(todaySales) - float64(yesterdaySales)) / float64(yesterdaySales)) * 100
	}

	refundRate := 0.0
	if todaySales > 0 {
		refundRate = (float64(refunds) / float64(todaySales)) * 100
	}

	response := MetricsResponse{
		TodayRevenue:   todayRevenue.Float64,
		TodaySales:     todaySales,
		AvgTransaction: avgTransaction.Float64,
		Refunds:        refunds,
		RevenueChange:  revenueChange,
		SalesChange:    salesChange,
		RefundRate:     refundRate,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// RevenueByDayResponse contains revenue data for charts
type RevenueByDayResponse struct {
	Labels []string  `json:"labels"`
	Values []float64 `json:"values"`
}

// RevenueByDay returns daily revenue for the last N days
func (h *AnalyticsHandler) RevenueByDay(w http.ResponseWriter, r *http.Request) {
	daysParam := r.URL.Query().Get("days")
	days := 7 // default
	if daysParam != "" {
		if parsed, err := strconv.Atoi(daysParam); err == nil && parsed > 0 && parsed <= 90 {
			days = parsed
		}
	}

	startDate := time.Now().Truncate(24*time.Hour).Add(-time.Duration(days-1) * 24 * time.Hour)

	rows, err := h.db.Query(`
		SELECT
			DATE(timestamp) as day,
			COALESCE(SUM(amount), 0) as revenue
		FROM transactions
		WHERE timestamp >= ?
		AND payment_status = 'approved'
		GROUP BY DATE(timestamp)
		ORDER BY day ASC
	`, startDate)

	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch revenue by day")
		http.Error(w, "Failed to fetch revenue data", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var labels []string
	var values []float64

	for rows.Next() {
		var day string
		var revenue float64
		if err := rows.Scan(&day, &revenue); err != nil {
			log.Error().Err(err).Msg("Failed to scan revenue row")
			continue
		}

		// Format date as "Jan 2"
		if t, err := time.Parse("2006-01-02", day); err == nil {
			labels = append(labels, t.Format("Jan 2"))
		} else {
			labels = append(labels, day)
		}
		values = append(values, revenue)
	}

	response := RevenueByDayResponse{
		Labels: labels,
		Values: values,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// PaymentMethodsResponse contains payment method breakdown
type PaymentMethodsResponse struct {
	Labels []string `json:"labels"`
	Values []int    `json:"values"`
}

// PaymentMethods returns transaction count by payment method
func (h *AnalyticsHandler) PaymentMethods(w http.ResponseWriter, r *http.Request) {
	rows, err := h.db.Query(`
		SELECT
			COALESCE(payment_method, 'unknown') as method,
			COUNT(*) as count
		FROM transactions
		WHERE payment_status = 'approved'
		AND timestamp >= datetime('now', '-30 days')
		GROUP BY payment_method
		ORDER BY count DESC
	`)

	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch payment methods")
		http.Error(w, "Failed to fetch payment methods", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var labels []string
	var values []int

	for rows.Next() {
		var method string
		var count int
		if err := rows.Scan(&method, &count); err != nil {
			log.Error().Err(err).Msg("Failed to scan payment method row")
			continue
		}

		// Capitalize method name
		if method != "" {
			method = string(method[0]-32) + method[1:]
		}

		labels = append(labels, method)
		values = append(values, count)
	}

	response := PaymentMethodsResponse{
		Labels: labels,
		Values: values,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// TopProduct represents a top-selling product
type TopProduct struct {
	Name      string  `json:"name"`
	Category  string  `json:"category"`
	UnitsSold int     `json:"units_sold"`
	Revenue   float64 `json:"revenue"`
	Trend     int     `json:"trend"`
}

// TopProducts returns the top-selling products
func (h *AnalyticsHandler) TopProducts(w http.ResponseWriter, r *http.Request) {
	limitParam := r.URL.Query().Get("limit")
	limit := 10 // default
	if limitParam != "" {
		if parsed, err := strconv.Atoi(limitParam); err == nil && parsed > 0 && parsed <= 100 {
			limit = parsed
		}
	}

	rows, err := h.db.Query(`
		SELECT
			COALESCE(p.name, 'Unknown') as name,
			COALESCE(p.category, 'Other') as category,
			COUNT(t.id) as units_sold,
			COALESCE(SUM(t.amount), 0) as revenue
		FROM transactions t
		LEFT JOIN products p ON t.product_id = p.id
		WHERE t.payment_status = 'approved'
		AND t.timestamp >= datetime('now', '-7 days')
		GROUP BY t.product_id
		ORDER BY units_sold DESC
		LIMIT ?
	`, limit)

	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch top products")
		http.Error(w, "Failed to fetch top products", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var products []TopProduct

	for rows.Next() {
		var product TopProduct
		if err := rows.Scan(&product.Name, &product.Category, &product.UnitsSold, &product.Revenue); err != nil {
			log.Error().Err(err).Msg("Failed to scan top product row")
			continue
		}

		// Trend calculation would require historical comparison
		// For now, set to 0 (neutral)
		product.Trend = 0

		products = append(products, product)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(products)
}

// Transaction represents a transaction for the log
type Transaction struct {
	ID                  string    `json:"id"`
	Timestamp           time.Time `json:"timestamp"`
	CoilID              string    `json:"coil_id"`
	ProductName         string    `json:"product_name"`
	Amount              float64   `json:"amount"`
	PaymentMethod       string    `json:"payment_method"`
	PaymentStatus       string    `json:"payment_status"`
	NayaxTransactionID  string    `json:"nayax_transaction_id"`
	Currency            string    `json:"currency"`
}

// RecentTransactions returns the most recent transactions
func (h *AnalyticsHandler) RecentTransactions(w http.ResponseWriter, r *http.Request) {
	limitParam := r.URL.Query().Get("limit")
	limit := 10 // default
	if limitParam != "" {
		if parsed, err := strconv.Atoi(limitParam); err == nil && parsed > 0 && parsed <= 100 {
			limit = parsed
		}
	}

	rows, err := h.db.Query(`
		SELECT
			t.id,
			t.timestamp,
			t.coil_id,
			COALESCE(p.name, 'Unknown') as product_name,
			COALESCE(t.amount, 0) as amount,
			COALESCE(t.payment_method, 'unknown') as payment_method,
			COALESCE(t.payment_status, 'unknown') as payment_status,
			COALESCE(t.nayax_transaction_id, '') as nayax_transaction_id,
			COALESCE(t.currency, 'USD') as currency
		FROM transactions t
		LEFT JOIN products p ON t.product_id = p.id
		ORDER BY t.timestamp DESC
		LIMIT ?
	`, limit)

	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch recent transactions")
		http.Error(w, "Failed to fetch transactions", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var transactions []Transaction

	for rows.Next() {
		var txn Transaction
		if err := rows.Scan(
			&txn.ID,
			&txn.Timestamp,
			&txn.CoilID,
			&txn.ProductName,
			&txn.Amount,
			&txn.PaymentMethod,
			&txn.PaymentStatus,
			&txn.NayaxTransactionID,
			&txn.Currency,
		); err != nil {
			log.Error().Err(err).Msg("Failed to scan transaction row")
			continue
		}

		transactions = append(transactions, txn)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(transactions)
}

// AllTransactions returns all transactions with optional filters
func (h *AnalyticsHandler) AllTransactions(w http.ResponseWriter, r *http.Request) {
	query := `
		SELECT
			t.id,
			t.timestamp,
			t.coil_id,
			COALESCE(p.name, 'Unknown') as product_name,
			COALESCE(t.amount, 0) as amount,
			COALESCE(t.payment_method, 'unknown') as payment_method,
			COALESCE(t.payment_status, 'unknown') as payment_status,
			COALESCE(t.nayax_transaction_id, '') as nayax_transaction_id,
			COALESCE(t.currency, 'USD') as currency
		FROM transactions t
		LEFT JOIN products p ON t.product_id = p.id
		WHERE 1=1
	`

	args := []interface{}{}

	// Date filters
	startDate := r.URL.Query().Get("start")
	endDate := r.URL.Query().Get("end")

	if startDate != "" {
		query += " AND DATE(t.timestamp) >= ?"
		args = append(args, startDate)
	}

	if endDate != "" {
		query += " AND DATE(t.timestamp) <= ?"
		args = append(args, endDate)
	}

	query += " ORDER BY t.timestamp DESC"

	// Limit
	limitParam := r.URL.Query().Get("limit")
	limit := 1000 // default
	if limitParam != "" {
		if parsed, err := strconv.Atoi(limitParam); err == nil && parsed > 0 && parsed <= 10000 {
			limit = parsed
		}
	}
	query += " LIMIT ?"
	args = append(args, limit)

	rows, err := h.db.Query(query, args...)
	if err != nil {
		log.Error().Err(err).Msg("Failed to fetch all transactions")
		http.Error(w, "Failed to fetch transactions", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var transactions []Transaction

	for rows.Next() {
		var txn Transaction
		if err := rows.Scan(
			&txn.ID,
			&txn.Timestamp,
			&txn.CoilID,
			&txn.ProductName,
			&txn.Amount,
			&txn.PaymentMethod,
			&txn.PaymentStatus,
			&txn.NayaxTransactionID,
			&txn.Currency,
		); err != nil {
			log.Error().Err(err).Msg("Failed to scan transaction row")
			continue
		}

		transactions = append(transactions, txn)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(transactions)
}
