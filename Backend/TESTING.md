# Testing Guide: ZootBox Backend

Comprehensive testing documentation for the ZootBox Backend Inventory Service.

## Table of Contents
- [Quick Start](#quick-start)
- [Unit Tests](#unit-tests)
- [Integration Tests](#integration-tests)
- [Test Coverage](#test-coverage)
- [Test Structure](#test-structure)
- [Continuous Integration](#continuous-integration)

## Quick Start

### Run All Tests
```bash
# Run all unit tests
make test

# Run tests with coverage
make test-coverage

# Run comprehensive test suite
./tests/run_tests.sh

# Run integration tests (requires running server)
./tests/integration_test.sh
```

### Run Specific Test Suite
```bash
# Repository tests
go test -v ./internal/db/repositories/...

# Service tests
go test -v ./internal/services/...

# Handler tests
go test -v ./internal/api/handlers/...

# Run specific test
go test -v -run TestCoilRepository_UpdateInventory ./internal/db/repositories/
```

## Unit Tests

### Repository Layer Tests

**File**: `internal/db/repositories/coil_repo_test.go`

Tests the data access layer with in-memory SQLite database.

**Coverage**:
- ✅ GetByID - retrieves coil by ID, handles not found
- ✅ GetAll - returns all coils sorted by ID
- ✅ GetLowStock - filters coils with inventory ≤ 2
- ✅ UpdateInventory - optimistic locking with version conflict detection
- ✅ RefillAll - sets all coils to inventory=10
- ✅ UpdateInventoryManual - admin override without version check

**Example**:
```bash
go test -v ./internal/db/repositories/ -run TestCoilRepository
```

**Key Tests**:
- **Optimistic Locking**: Verifies version conflict detection prevents concurrent modification
- **Edge Cases**: Empty inventory (0), low stock threshold (≤2)
- **Atomicity**: Transactions rollback on error

### Service Layer Tests

**File**: `internal/services/transaction_test.go`

Tests core business logic for transaction recording.

**Coverage**:
- ✅ RecordVendEvent with status='success' - decrements inventory
- ✅ RecordVendEvent with status='jam' - creates jam event, inventory unchanged
- ✅ RecordVendEvent with status='failed' - logs transaction only
- ✅ Coil not found error handling
- ✅ Empty coil (inventory=0) rejection
- ✅ Atomic rollback on failure

**Example**:
```bash
go test -v ./internal/services/ -run TestTransactionService
```

**Critical Test Cases**:
```go
// Test 1: Successful vend decrements inventory
req := &RecordVendEventRequest{
    CoilID: "A1",
    Status: "success",
    TransactionID: "nayax-12345",
}
// Expected: inventory 10 → 9

// Test 2: Jam event preserves inventory
req := &RecordVendEventRequest{
    CoilID: "A1",
    Status: "jam",
    TransactionID: "nayax-99999",
}
// Expected: inventory unchanged, jam_event created

// Test 3: Empty coil rejection
req := &RecordVendEventRequest{
    CoilID: "A3", // inventory=0
    Status: "success",
}
// Expected: error "inventory is 0"
```

### Handler Layer Tests

**Files**:
- `internal/api/handlers/transaction_test.go`
- `internal/api/handlers/inventory_test.go`

Tests HTTP endpoints using `httptest` package.

**Coverage**:
- ✅ POST /api/v1/transactions - valid request, invalid JSON, missing fields
- ✅ GET /api/v1/coils - returns all coils
- ✅ GET /api/v1/coils/{coilId} - returns specific coil, 404 handling
- ✅ GET /api/v1/coils/low-stock - filters low inventory coils

**Example**:
```bash
go test -v ./internal/api/handlers/
```

**Sample Handler Test**:
```go
func TestTransactionHandler_RecordTransaction_Success(t *testing.T) {
    db := setupTestDB(t)
    defer db.Close()

    handler := NewTransactionHandler(db)

    reqBody := services.RecordVendEventRequest{
        CoilID: "A5",
        Status: "success",
        Timestamp: "2025-12-27T10:30:00Z",
        TransactionID: "nayax-12345",
    }

    body, _ := json.Marshal(reqBody)
    req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", bytes.NewReader(body))
    w := httptest.NewRecorder()

    handler.RecordTransaction(w, req)

    assert.Equal(t, http.StatusOK, w.Code)
}
```

## Integration Tests

**File**: `tests/integration_test.sh`

End-to-end API testing with a running server.

### Prerequisites
1. Start the server:
   ```bash
   go run cmd/server/main.go --migrate  # Run migrations first
   go run cmd/server/main.go            # Start server
   ```

2. Run integration tests:
   ```bash
   chmod +x tests/integration_test.sh
   ./tests/integration_test.sh
   ```

### Test Flow
1. **Health Check** - Verify server is running
2. **Get All Coils** - Retrieve 100 initial coils
3. **Get Specific Coil** - Query coil A5
4. **Record Successful Vend** - A5: 10 → 9
5. **Verify Inventory Decrement** - Confirm A5=9
6. **Record Jam Event** - B3 inventory unchanged
7. **Record Failed Vend** - C7 inventory unchanged
8. **Multiple Vends** - Trigger low stock (D1: 10 → 2)
9. **Low Stock Detection** - Verify D1 in low-stock list
10. **Error Cases** - Invalid coil ID, missing fields

### Expected Output
```
========================================
ZootBox Backend Integration Tests
========================================

Testing: Health Check
  GET /health
  ✓ PASS (HTTP 200)
  Response: {"status":"healthy","uptime_seconds":5.2,"database_ok":true}

Testing: Record Successful Vend (A5: 10 → 9)
  POST /api/v1/transactions
  ✓ PASS (HTTP 200)
  Response: {"success":true,"coil_id":"A5","inventory_after":9}

...

========================================
All Integration Tests Passed!
========================================
```

## Test Coverage

### Generate Coverage Report
```bash
# Generate coverage profile
go test -coverprofile=coverage.out ./...

# View coverage in terminal
go tool cover -func=coverage.out

# Generate HTML report
go tool cover -html=coverage.out -o coverage.html
open coverage.html  # macOS
xdg-open coverage.html  # Linux
start coverage.html  # Windows
```

### Coverage Targets
- **Overall**: ≥80% coverage
- **Services**: ≥90% coverage (critical business logic)
- **Repositories**: ≥85% coverage
- **Handlers**: ≥75% coverage

### Current Coverage (Expected)
```
github.com/zootbox/backend/internal/db/repositories    85.2%
github.com/zootbox/backend/internal/services           92.1%
github.com/zootbox/backend/internal/api/handlers       78.6%
github.com/zootbox/backend/internal/models             95.0%
-------------------------------------------------------
Total                                                   85.3%
```

## Test Structure

### In-Memory Database Setup
All tests use SQLite in-memory databases for fast, isolated testing:

```go
func setupTestDB(t *testing.T) *sql.DB {
    db, err := sql.Open("sqlite3", ":memory:")
    require.NoError(t, err)

    // Create schema
    schema := `CREATE TABLE coils (...)`
    _, err = db.Exec(schema)
    require.NoError(t, err)

    return db
}
```

### Test Naming Convention
```go
// Format: Test<Type>_<Method>_<Scenario>
func TestCoilRepository_UpdateInventory_VersionConflict(t *testing.T) { ... }
func TestTransactionService_RecordVendEvent_Success(t *testing.T) { ... }
func TestInventoryHandler_GetCoil_NotFound(t *testing.T) { ... }
```

### Table-Driven Tests
```go
func TestCoilRepository_GetByID(t *testing.T) {
    tests := []struct {
        name    string
        coilID  string
        wantErr bool
    }{
        {"existing coil", "A1", false},
        {"non-existent coil", "Z99", true},
    }

    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            // Test implementation
        })
    }
}
```

## Continuous Integration

### GitHub Actions Workflow
Create `.github/workflows/test.yml`:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up Go
      uses: actions/setup-go@v4
      with:
        go-version: '1.21'

    - name: Install dependencies
      run: |
        cd Backend
        go mod download

    - name: Run tests
      run: |
        cd Backend
        go test -v -race -coverprofile=coverage.out ./...

    - name: Upload coverage
      uses: codecov/codecov-action@v3
      with:
        files: ./Backend/coverage.out
```

### Pre-commit Hooks
Create `.git/hooks/pre-commit`:

```bash
#!/bin/bash
cd Backend
go test ./... || exit 1
go vet ./... || exit 1
```

## Troubleshooting

### Common Issues

**1. Tests fail with "database is locked"**
```
Solution: Use in-memory database (:memory:) for tests
Each test gets isolated database instance
```

**2. Handler tests fail with "404 Not Found"**
```
Solution: Ensure Chi router context is set up correctly
Use chi.NewRouteContext() and set URL parameters
```

**3. Coverage report shows 0% for some files**
```
Solution: Run tests from project root, not subdirectories
Use: go test ./... (not go test .)
```

**4. Integration tests timeout**
```
Solution: Verify server is running on http://localhost:8080
Check migrations were run: go run cmd/server/main.go --migrate
```

## Best Practices

### ✅ DO
- Use in-memory databases for unit tests
- Test both success and failure paths
- Mock external dependencies
- Use descriptive test names
- Clean up resources (defer db.Close())
- Test edge cases (empty inventory, invalid IDs)

### ❌ DON'T
- Use production database for tests
- Skip error handling tests
- Write flaky tests (time-dependent, order-dependent)
- Ignore test coverage
- Test implementation details (test behavior)

## Running Tests in Development

### Watch Mode (using entr)
```bash
# Install entr: brew install entr (macOS)
ls **/*.go | entr -c go test ./...
```

### Pre-Push Checklist
```bash
# 1. Run all tests
make test

# 2. Check coverage
make test-coverage

# 3. Static analysis
go vet ./...

# 4. Format code
make fmt

# 5. Integration tests
./tests/integration_test.sh
```

## Additional Resources

- [Go Testing Documentation](https://golang.org/pkg/testing/)
- [Testify Assertion Library](https://github.com/stretchr/testify)
- [SQLite Testing Best Practices](https://www.sqlite.org/testing.html)
- [httptest Package](https://golang.org/pkg/net/http/httptest/)
