# Testing Checklist - Phases 4-8

## Pre-Test Setup

```bash
cd Backend
go mod tidy
go mod verify
```

## Unit Tests

### Phase 4: Admin Operations
- [ ] Test RefillAll sets all coils to inventory=10
- [ ] Test RefillAll returns correct count
- [ ] Test SetCoilInventory validates range (0-10)
- [ ] Test SetCoilInventory rejects invalid coil IDs
- [ ] Test admin endpoints return proper status codes

### Phase 5: Jam Management
- [ ] Test GetJamEvents returns all when no filter
- [ ] Test GetJamEvents filters by status='open'
- [ ] Test GetJamEvents filters by status='resolved'
- [ ] Test GetJamEvents rejects invalid status
- [ ] Test ResolveJamEvent marks jam as resolved
- [ ] Test ResolveJamEvent returns 404 for non-existent jam

### Phase 6: Product Linking
- [ ] Test CreateProductLink validates minimum 2 coils
- [ ] Test CreateProductLink validates coil existence
- [ ] Test CreateProductLink rejects invalid strategy
- [ ] Test ResolveProductToCoil returns first available coil
- [ ] Test ResolveProductToCoil skips empty coils
- [ ] Test ResolveProductToCoil returns error when all empty
- [ ] Test DeleteProductLink removes link
- [ ] Test GetProductLinks returns all links

### Phase 7: Power Loss Recovery
- [ ] Test ValidateDataIntegrity detects invalid coil count
- [ ] Test ValidateDataIntegrity detects invalid inventory
- [ ] Test ValidateDataIntegrity detects orphaned transactions
- [ ] Test ValidateDataIntegrity detects orphaned jam events
- [ ] Test ValidateDataIntegrity verifies WAL mode
- [ ] Test RecoverFromPowerLoss succeeds on valid DB
- [ ] Test savepoint creation/rollback/release

### Phase 8: Monitoring & Deployment
- [ ] Test metrics endpoint returns Prometheus format
- [ ] Test metrics include memory stats
- [ ] Test metrics include inventory counts
- [ ] Test metrics include transaction counts
- [ ] Test metrics include jam event counts
- [ ] Test profiling flags work correctly
- [ ] Test memory monitoring threshold alerts

## Integration Tests

### API Endpoints

```bash
# Start server
go run cmd/server/main.go --migrate
go run cmd/server/main.go

# In another terminal
./tests/integration_test.sh
```

Expected: All 14 scenarios pass

### New Endpoint Tests

```bash
# Admin: Refill All
curl -X POST http://localhost:8080/api/v1/admin/refill
# Expected: 200 OK, {"message": "All 100 coils refilled...", "coils_updated": 100}

# Admin: Set Coil Inventory
curl -X PUT http://localhost:8080/api/v1/admin/coils/A1 \
  -H "Content-Type: application/json" \
  -d '{"inventory": 5}'
# Expected: 200 OK, coil object with inventory=5

# Jam Events: Get All Open
curl http://localhost:8080/api/v1/jam-events?status=open
# Expected: 200 OK, array of open jam events

# Jam Events: Resolve
curl -X POST http://localhost:8080/api/v1/jam-events/{eventId}/resolve
# Expected: 200 OK, resolved jam event object

# Product Links: Create
curl -X POST http://localhost:8080/api/v1/admin/product-links \
  -H "Content-Type: application/json" \
  -d '{
    "product_sku": "SKU-001",
    "linked_coil_ids": ["A1", "A2", "A3"],
    "selection_strategy": "first_available"
  }'
# Expected: 201 Created, product link object

# Product Links: Resolve
curl http://localhost:8080/api/v1/product-links/SKU-001/resolve
# Expected: 200 OK, {"product_sku": "SKU-001", "resolved_to": "A1", ...}

# Product Links: Get All
curl http://localhost:8080/api/v1/admin/product-links
# Expected: 200 OK, array of product links

# Product Links: Delete
curl -X DELETE http://localhost:8080/api/v1/admin/product-links/{linkGroupId}
# Expected: 204 No Content

# Metrics
curl http://localhost:8080/metrics
# Expected: 200 OK, Prometheus-format text
```

## Power Loss Recovery Test

```bash
./tests/power_loss_test.sh
```

Expected: All scenarios pass, WAL recovery verified

## Build & Deployment Test

```bash
# Build for ARM64
./scripts/build_arm64.sh

# Verify binary
ls -lh build/zootbox-backend-arm64
file build/zootbox-backend-arm64

# Test deployment (requires Android tablet connected)
./scripts/install_tablet.sh

# Verify running on tablet
adb shell ps -A | grep zootbox
adb shell curl http://localhost:8080/health
```

## Performance Tests

### Memory Usage Test

```bash
# Start with memory monitoring
go run cmd/server/main.go --memmonitor

# Generate load (in another terminal)
for i in {1..100}; do
  curl -s http://localhost:8080/api/v1/coils > /dev/null
done

# Check metrics
curl http://localhost:8080/metrics | grep memory

# Expected: Memory < 30MB
```

### Profiling Test

```bash
# CPU profiling
go run cmd/server/main.go --cpuprofile=cpu.prof &
# ... generate load ...
pkill zootbox-backend
go tool pprof cpu.prof

# Memory profiling
go run cmd/server/main.go --memprofile=mem.prof &
# ... generate load ...
pkill zootbox-backend
go tool pprof mem.prof
```

## Regression Tests

Run all existing tests to ensure no regressions:

```bash
make test-all
make test-coverage

# Expected:
# - All 39 existing unit tests pass
# - Coverage ≥ 85%
# - No race conditions
```

## Edge Cases & Error Handling

### Invalid Inputs
- [ ] Invalid coil IDs return 404
- [ ] Invalid inventory values return 400
- [ ] Invalid jam status values return 400
- [ ] Invalid product link SKUs return 404
- [ ] Missing required fields return 400
- [ ] Malformed JSON returns 400

### Concurrency
- [ ] Concurrent refill operations don't conflict
- [ ] Concurrent jam resolutions don't conflict
- [ ] Concurrent product link operations don't conflict
- [ ] Optimistic locking prevents race conditions

### Database Errors
- [ ] Database connection failures handled gracefully
- [ ] Transaction rollbacks on errors
- [ ] Recovery from database locks

## Security Tests

- [ ] SQL injection attempts fail safely
- [ ] Large payloads rejected
- [ ] Invalid content types rejected
- [ ] Error messages don't leak sensitive info

## Documentation Review

- [ ] README.md updated with new endpoints
- [ ] API documentation reflects new features
- [ ] Deployment instructions accurate
- [ ] Memory optimization guide complete

## Final Checklist

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code review issues addressed
- [ ] No unsafe string operations
- [ ] All transactions have defer rollback
- [ ] No SQL injection vulnerabilities
- [ ] Error handling consistent
- [ ] Logging comprehensive
- [ ] Metrics endpoint functional
- [ ] Build scripts work
- [ ] Deployment scripts work
- [ ] Memory usage < 30MB
- [ ] Binary size < 15MB
- [ ] Documentation complete

---

## Test Execution Results

### Date: _____________
### Tester: _____________

| Category | Tests | Passed | Failed | Notes |
|----------|-------|--------|--------|-------|
| Unit Tests | | | | |
| Integration Tests | | | | |
| Power Loss Recovery | | | | |
| Build & Deploy | | | | |
| Performance | | | | |
| Regression | | | | |
| Edge Cases | | | | |
| Security | | | | |

### Issues Found:

1.
2.
3.

### Sign-off:

- [ ] All tests passed
- [ ] Ready for production deployment

Signature: _________________ Date: _____________
