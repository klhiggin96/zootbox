# Testing Quick Reference

One-page cheat sheet for running tests in the ZootBox Backend.

## 🚀 Quick Start (3 Commands)

```bash
# 1. Install dependencies
cd Backend && go mod download

# 2. Run all unit tests
make test

# 3. Generate coverage report
make test-coverage && open coverage.html
```

## 📋 Test Commands

### Unit Tests
```bash
# Run all tests
make test
go test -v ./...

# Run specific package
go test -v ./internal/services/
go test -v ./internal/db/repositories/
go test -v ./internal/api/handlers/

# Run specific test
go test -v -run TestCoilRepository_UpdateInventory ./internal/db/repositories/
go test -v -run TestTransactionService_RecordVendEvent ./internal/services/
```

### Coverage
```bash
# Generate coverage report
make test-coverage
go test -coverprofile=coverage.out ./...
go tool cover -html=coverage.out -o coverage.html

# View coverage in terminal
go tool cover -func=coverage.out

# Coverage percentage only
go test -cover ./...
```

### Race Detection
```bash
# Check for race conditions
make test-race
go test -race -v ./...
```

### Integration Tests
```bash
# Terminal 1: Start server
go run cmd/server/main.go --migrate
go run cmd/server/main.go

# Terminal 2: Run integration tests
make test-integration
./tests/integration_test.sh
```

### Comprehensive Suite
```bash
# Run everything (unit + static analysis)
make test-all
./tests/run_tests.sh

# With integration tests
./tests/run_tests.sh --integration
```

## 🔧 Static Analysis

```bash
# Vet (static analysis)
go vet ./...

# Format code
make fmt
gofmt -w .

# Format check (CI)
gofmt -l .

# Tidy dependencies
make tidy
go mod tidy
```

## 🎯 Common Test Patterns

### Run Failed Tests Only
```bash
# After test failure, re-run failed tests
go test -v ./... | grep -A 10 "FAIL"
go test -v -run <FailedTestName> ./path/to/package/
```

### Watch Mode
```bash
# Re-run tests on file changes (requires entr)
ls **/*.go | entr -c go test ./...
```

### Verbose Output
```bash
# Show all test output
go test -v ./...

# Show test names only
go test -v ./... | grep "^=== RUN"

# Show only failures
go test ./... 2>&1 | grep -A 5 FAIL
```

### Parallel Execution
```bash
# Run tests in parallel (faster)
go test -parallel 4 ./...

# Disable parallel (for debugging)
go test -parallel 1 ./...
```

## 📊 Test Targets

| Component | Command | Expected Coverage |
|-----------|---------|-------------------|
| Repositories | `go test ./internal/db/repositories/` | ≥85% |
| Services | `go test ./internal/services/` | ≥90% |
| Handlers | `go test ./internal/api/handlers/` | ≥75% |
| Overall | `make test-coverage` | ≥85% |

## 🐛 Debugging Tests

### Run Single Test with Verbose Output
```bash
go test -v -run TestCoilRepository_UpdateInventory ./internal/db/repositories/
```

### Print Test Output (use t.Log)
```go
func TestExample(t *testing.T) {
    t.Log("Debug message here")
    t.Logf("Variable value: %v", myVar)
}
```

### Skip Slow Tests
```go
func TestSlow(t *testing.T) {
    if testing.Short() {
        t.Skip("Skipping slow test")
    }
    // Test code
}
```

```bash
go test -short ./...  # Skips tests marked as slow
```

## ✅ Pre-Commit Checklist

```bash
# Run before committing code
make test          # All tests pass
make test-coverage # Coverage ≥85%
go vet ./...       # No static analysis issues
make fmt           # Code formatted
go mod tidy        # Dependencies clean
```

## 🔍 Test File Locations

```
Backend/
├── internal/
│   ├── db/repositories/
│   │   └── coil_repo_test.go          # 7 tests
│   ├── services/
│   │   └── transaction_test.go        # 9 tests
│   └── api/handlers/
│       ├── transaction_test.go        # 6 tests
│       └── inventory_test.go          # 3 tests
└── tests/
    ├── integration_test.sh            # 14 scenarios
    └── run_tests.sh                   # Test orchestrator
```

## 📈 CI/CD Integration

### GitHub Actions
```yaml
# .github/workflows/test.yml
- name: Run tests
  run: |
    cd Backend
    go test -v -race -coverprofile=coverage.out ./...
```

### Git Hooks (Pre-commit)
```bash
#!/bin/bash
# .git/hooks/pre-commit
cd Backend
go test ./... || exit 1
go vet ./... || exit 1
```

## 🚨 Troubleshooting

| Issue | Solution |
|-------|----------|
| **"database is locked"** | Use `:memory:` for tests (already configured) |
| **"404 Not Found" in handler tests** | Ensure Chi router context is set |
| **Tests hang** | Check for deadlocks, use `-timeout 30s` flag |
| **Coverage shows 0%** | Run from project root: `go test ./...` |
| **Integration tests fail** | Verify server running on `localhost:8080` |

## 📚 Documentation

- **Full Guide**: `TESTING.md` (detailed documentation)
- **Test Summary**: `TEST_SUMMARY.md` (coverage report)
- **This File**: Quick reference for daily use

## 🎓 Test Examples

### Repository Test
```go
func TestCoilRepository_GetByID(t *testing.T) {
    db := setupTestDB(t)
    defer db.Close()

    repo := NewCoilRepository(db)
    coil, err := repo.GetByID("A1")

    require.NoError(t, err)
    assert.Equal(t, "A1", coil.ID)
}
```

### Service Test
```go
func TestTransactionService_RecordVendEvent(t *testing.T) {
    db := setupTestDB(t)
    defer db.Close()

    service := NewTransactionService(db)
    response, err := service.RecordVendEvent(&RecordVendEventRequest{
        CoilID: "A5",
        Status: "success",
        TransactionID: "nayax-12345",
    })

    require.NoError(t, err)
    assert.Equal(t, 9, response.InventoryAfter)
}
```

### Handler Test
```go
func TestTransactionHandler_RecordTransaction(t *testing.T) {
    db := setupTestDB(t)
    defer db.Close()

    handler := NewTransactionHandler(db)
    req := httptest.NewRequest(http.MethodPost, "/api/v1/transactions", body)
    w := httptest.NewRecorder()

    handler.RecordTransaction(w, req)

    assert.Equal(t, http.StatusOK, w.Code)
}
```

---

**Quick Tip**: Bookmark this page for fast access to testing commands!

**Most Used Commands**:
1. `make test` - Run all tests
2. `make test-coverage` - Coverage report
3. `make test-integration` - Full API testing
