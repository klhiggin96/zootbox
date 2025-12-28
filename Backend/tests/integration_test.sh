#!/bin/bash

# Integration Test Script for ZootBox Backend API
# Tests the full API flow: setup -> vend -> verify

set -e  # Exit on error

API_BASE="http://localhost:8080"
CYAN='\033[0;36m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}ZootBox Backend Integration Tests${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Function to test API endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    local description=$5

    echo -e "${CYAN}Testing: ${description}${NC}"
    echo "  ${method} ${endpoint}"

    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X ${method} "${API_BASE}${endpoint}" \
            -H "Content-Type: application/json" \
            -H "X-Correlation-ID: test-$(date +%s)")
    else
        response=$(curl -s -w "\n%{http_code}" -X ${method} "${API_BASE}${endpoint}" \
            -H "Content-Type: application/json" \
            -H "X-Correlation-ID: test-$(date +%s)" \
            -d "${data}")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" -eq "$expected_status" ]; then
        echo -e "  ${GREEN}✓ PASS${NC} (HTTP $http_code)"
        echo "  Response: $body"
    else
        echo -e "  ${RED}✗ FAIL${NC} (Expected HTTP $expected_status, got $http_code)"
        echo "  Response: $body"
        exit 1
    fi
    echo ""
}

# Test 1: Health Check
test_endpoint "GET" "/health" "" 200 "Health Check"

# Test 2: Get All Coils
test_endpoint "GET" "/api/v1/coils" "" 200 "Get All Coils (should return 100 coils)"

# Test 3: Get Specific Coil
test_endpoint "GET" "/api/v1/coils/A5" "" 200 "Get Coil A5"

# Test 4: Get Low Stock Coils (initially should be empty)
test_endpoint "GET" "/api/v1/coils/low-stock" "" 200 "Get Low Stock Coils (initial)"

# Test 5: Record Successful Vend
vend_data='{
  "coil_id": "A5",
  "status": "success",
  "timestamp": "2025-12-27T10:30:00Z",
  "transaction_id": "nayax-test-001"
}'
test_endpoint "POST" "/api/v1/transactions" "$vend_data" 200 "Record Successful Vend (A5: 10 → 9)"

# Test 6: Verify Inventory Decremented
echo -e "${CYAN}Verifying: Inventory decremented from 10 to 9${NC}"
response=$(curl -s "${API_BASE}/api/v1/coils/A5")
inventory=$(echo "$response" | grep -o '"inventory":[0-9]*' | cut -d':' -f2)
if [ "$inventory" -eq 9 ]; then
    echo -e "  ${GREEN}✓ PASS${NC} (Inventory = 9)"
else
    echo -e "  ${RED}✗ FAIL${NC} (Expected inventory=9, got $inventory)"
    exit 1
fi
echo ""

# Test 7: Record Jam Event
jam_data='{
  "coil_id": "B3",
  "status": "jam",
  "timestamp": "2025-12-27T10:31:00Z",
  "transaction_id": "nayax-test-002"
}'
test_endpoint "POST" "/api/v1/transactions" "$jam_data" 200 "Record Jam Event (B3: inventory unchanged)"

# Test 8: Verify Inventory Unchanged for Jam
echo -e "${CYAN}Verifying: Inventory unchanged for jam${NC}"
response=$(curl -s "${API_BASE}/api/v1/coils/B3")
inventory=$(echo "$response" | grep -o '"inventory":[0-9]*' | cut -d':' -f2)
if [ "$inventory" -eq 10 ]; then
    echo -e "  ${GREEN}✓ PASS${NC} (Inventory still = 10)"
else
    echo -e "  ${RED}✗ FAIL${NC} (Expected inventory=10, got $inventory)"
    exit 1
fi
echo ""

# Test 9: Record Failed Vend
failed_data='{
  "coil_id": "C7",
  "status": "failed",
  "timestamp": "2025-12-27T10:32:00Z",
  "transaction_id": "nayax-test-003"
}'
test_endpoint "POST" "/api/v1/transactions" "$failed_data" 200 "Record Failed Vend (C7: inventory unchanged)"

# Test 10: Multiple Vends to Trigger Low Stock
echo -e "${CYAN}Testing: Multiple vends to reach low stock threshold${NC}"
for i in {1..8}; do
    vend_data="{
      \"coil_id\": \"D1\",
      \"status\": \"success\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
      \"transaction_id\": \"nayax-test-low-$i\"
    }"
    curl -s -X POST "${API_BASE}/api/v1/transactions" \
        -H "Content-Type: application/json" \
        -d "$vend_data" > /dev/null
    echo "  Vend $i completed"
done
echo ""

# Test 11: Verify Low Stock Detection
echo -e "${CYAN}Verifying: D1 appears in low-stock list (inventory=2)${NC}"
response=$(curl -s "${API_BASE}/api/v1/coils/low-stock")
if echo "$response" | grep -q "D1"; then
    echo -e "  ${GREEN}✓ PASS${NC} (D1 in low-stock list)"
else
    echo -e "  ${RED}✗ FAIL${NC} (D1 not in low-stock list)"
    echo "  Response: $response"
    exit 1
fi
echo ""

# Test 12: Test Invalid Coil ID
invalid_data='{
  "coil_id": "Z99",
  "status": "success",
  "timestamp": "2025-12-27T10:33:00Z",
  "transaction_id": "nayax-test-invalid"
}'
test_endpoint "POST" "/api/v1/transactions" "$invalid_data" 500 "Record Vend with Invalid Coil (should fail)"

# Test 13: Test Missing Required Fields
missing_data='{
  "coil_id": "E5"
}'
test_endpoint "POST" "/api/v1/transactions" "$missing_data" 400 "Record Vend with Missing Fields (should fail)"

# Test 14: Metrics Endpoint
test_endpoint "GET" "/metrics" "" 200 "Metrics Endpoint"

echo -e "${CYAN}========================================${NC}"
echo -e "${GREEN}All Integration Tests Passed!${NC}"
echo -e "${CYAN}========================================${NC}"
