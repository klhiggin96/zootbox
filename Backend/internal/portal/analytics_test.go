package portal

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	_ "modernc.org/sqlite"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// setupTestDB creates an in-memory SQLite database with test schema
func setupTestDB(t *testing.T) *sql.DB {
	db, err := sql.Open("sqlite", ":memory:")
	require.NoError(t, err)

	// Create products table
	_, err = db.Exec(`CREATE TABLE products (
		id TEXT PRIMARY KEY,
		name TEXT NOT NULL,
		category TEXT,
		price REAL
	)`)
	require.NoError(t, err)

	// Create transactions table
	_, err = db.Exec(`CREATE TABLE transactions (
		id TEXT PRIMARY KEY,
		timestamp TIMESTAMP NOT NULL,
		product_id TEXT,
		coil_id TEXT,
		amount REAL,
		payment_method TEXT,
		payment_status TEXT,
		nayax_transaction_id TEXT,
		currency TEXT
	)`)
	require.NoError(t, err)

	return db
}

// seedTestData inserts test data into database
func seedTestData(t *testing.T, db *sql.DB) {
	// Insert test products
	_, err := db.Exec(`INSERT INTO products VALUES
		('prod1', 'ZYN CITRUS', 'Nicotine Pouches', 3.50),
		('prod2', 'ZYN COOL MINT', 'Nicotine Pouches', 3.50)`)
	require.NoError(t, err)

	// Insert test transactions (today)
	today := time.Now().Format("2006-01-02 15:04:05")
	_, err = db.Exec(`INSERT INTO transactions VALUES
		('txn1', ?, 'prod1', 'A1', 3.50, 'card', 'approved', 'NAY123', 'USD'),
		('txn2', ?, 'prod2', 'B1', 3.50, 'nfc', 'approved', 'NAY124', 'USD'),
		('txn3', ?, 'prod1', 'A1', 3.50, 'card', 'refunded', 'NAY125', 'USD')`,
		today, today, today)
	require.NoError(t, err)

	// Insert yesterday's transactions for change calculation
	yesterday := time.Now().Add(-24 * time.Hour).Format("2006-01-02 15:04:05")
	_, err = db.Exec(`INSERT INTO transactions VALUES
		('txn4', ?, 'prod1', 'A1', 3.50, 'card', 'approved', 'NAY126', 'USD')`,
		yesterday)
	require.NoError(t, err)
}

// TestAnalyticsHandler_Metrics tests the metrics endpoint
func TestAnalyticsHandler_Metrics(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestData(t, db)

	handler := NewAnalyticsHandler(db)

	req := httptest.NewRequest("GET", "/api/v1/portal/analytics/metrics", nil)
	w := httptest.NewRecorder()

	handler.Metrics(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "application/json", w.Header().Get("Content-Type"))

	var response MetricsResponse
	err := json.NewDecoder(w.Body).Decode(&response)
	require.NoError(t, err)

	// Verify today's metrics (2 approved transactions @ $3.50 each)
	assert.Equal(t, 7.0, response.TodayRevenue)
	assert.Equal(t, 2, response.TodaySales)
	assert.Equal(t, 3.5, response.AvgTransaction)
	assert.Equal(t, 1, response.Refunds) // 1 refunded transaction

	// Verify change calculations (today 2 sales vs yesterday 1 sale)
	assert.Equal(t, 100.0, response.RevenueChange) // 100% increase
	assert.Equal(t, 100.0, response.SalesChange)   // 100% increase
	assert.Equal(t, 50.0, response.RefundRate)     // 1 refund out of 2 transactions
}

// TestAnalyticsHandler_RevenueByDay tests revenue chart data
func TestAnalyticsHandler_RevenueByDay(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestData(t, db)

	handler := NewAnalyticsHandler(db)

	t.Run("default 7 days", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/revenue-by-day", nil)
		w := httptest.NewRecorder()

		handler.RevenueByDay(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response RevenueByDayResponse
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.NotEmpty(t, response.Labels)
		assert.NotEmpty(t, response.Values)
		assert.Len(t, response.Labels, len(response.Values))
	})

	t.Run("custom days parameter", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/revenue-by-day?days=3", nil)
		w := httptest.NewRecorder()

		handler.RevenueByDay(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response RevenueByDayResponse
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.NotEmpty(t, response.Labels)
		assert.NotEmpty(t, response.Values)
	})
}

// TestAnalyticsHandler_PaymentMethods tests payment method breakdown
func TestAnalyticsHandler_PaymentMethods(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestData(t, db)

	handler := NewAnalyticsHandler(db)

	req := httptest.NewRequest("GET", "/api/v1/portal/analytics/payment-methods", nil)
	w := httptest.NewRecorder()

	handler.PaymentMethods(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response PaymentMethodsResponse
	err := json.NewDecoder(w.Body).Decode(&response)
	require.NoError(t, err)

	assert.NotEmpty(t, response.Labels)
	assert.NotEmpty(t, response.Values)
	assert.Len(t, response.Labels, len(response.Values))

	// Verify we have card and nfc methods
	assert.Contains(t, response.Labels, "Card")
	assert.Contains(t, response.Labels, "Nfc")
}

// TestAnalyticsHandler_TopProducts tests top products query
func TestAnalyticsHandler_TopProducts(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestData(t, db)

	handler := NewAnalyticsHandler(db)

	t.Run("default limit", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/top-products", nil)
		w := httptest.NewRecorder()

		handler.TopProducts(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []TopProduct
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.NotEmpty(t, response)
		// Verify product has required fields
		if len(response) > 0 {
			assert.NotEmpty(t, response[0].Name)
			assert.NotEmpty(t, response[0].Category)
			assert.Greater(t, response[0].UnitsSold, 0)
			assert.Greater(t, response[0].Revenue, 0.0)
		}
	})

	t.Run("custom limit", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/top-products?limit=5", nil)
		w := httptest.NewRecorder()

		handler.TopProducts(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []TopProduct
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.LessOrEqual(t, len(response), 5)
	})
}

// TestAnalyticsHandler_RecentTransactions tests recent transactions feed
func TestAnalyticsHandler_RecentTransactions(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestData(t, db)

	handler := NewAnalyticsHandler(db)

	t.Run("default limit", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/recent-transactions", nil)
		w := httptest.NewRecorder()

		handler.RecentTransactions(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []Transaction
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.NotEmpty(t, response)
		// Verify transactions have required fields
		if len(response) > 0 {
			txn := response[0]
			assert.NotEmpty(t, txn.ID)
			assert.NotEmpty(t, txn.CoilID)
			assert.NotEmpty(t, txn.PaymentMethod)
			assert.NotEmpty(t, txn.PaymentStatus)
		}
	})

	t.Run("custom limit", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/recent-transactions?limit=2", nil)
		w := httptest.NewRecorder()

		handler.RecentTransactions(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []Transaction
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.LessOrEqual(t, len(response), 2)
	})
}

// TestAnalyticsHandler_AllTransactions tests transaction log with filtering
func TestAnalyticsHandler_AllTransactions(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestData(t, db)

	handler := NewAnalyticsHandler(db)

	t.Run("no filters", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/transactions", nil)
		w := httptest.NewRecorder()

		handler.AllTransactions(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []Transaction
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.NotEmpty(t, response)
		assert.LessOrEqual(t, len(response), 1000) // Default limit
	})

	t.Run("with date range filter", func(t *testing.T) {
		today := time.Now().Format("2006-01-02")
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/transactions?start="+today+"&end="+today, nil)
		w := httptest.NewRecorder()

		handler.AllTransactions(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []Transaction
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		// Should only get today's transactions
		assert.Equal(t, 3, len(response))
	})

	t.Run("with custom limit", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/transactions?limit=2", nil)
		w := httptest.NewRecorder()

		handler.AllTransactions(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []Transaction
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.LessOrEqual(t, len(response), 2)
	})
}

// TestAnalyticsHandler_EmptyDatabase tests endpoints with no data
func TestAnalyticsHandler_EmptyDatabase(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	// Don't seed any data

	handler := NewAnalyticsHandler(db)

	t.Run("metrics with empty db", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/metrics", nil)
		w := httptest.NewRecorder()

		handler.Metrics(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response MetricsResponse
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.Equal(t, 0.0, response.TodayRevenue)
		assert.Equal(t, 0, response.TodaySales)
	})

	t.Run("recent transactions with empty db", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/v1/portal/analytics/recent-transactions", nil)
		w := httptest.NewRecorder()

		handler.RecentTransactions(w, req)

		assert.Equal(t, http.StatusOK, w.Code)

		var response []Transaction
		err := json.NewDecoder(w.Body).Decode(&response)
		require.NoError(t, err)

		assert.Empty(t, response)
	})
}
