#!/bin/bash

# Comprehensive Test Runner for ZootBox Backend
# Runs all unit tests, generates coverage report, and optionally runs integration tests

set -e

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}ZootBox Backend Test Suite${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Change to Backend directory
cd "$(dirname "$0")/.."

# Step 1: Run unit tests
echo -e "${CYAN}Step 1: Running Unit Tests${NC}"
echo "Running go test on all packages..."
echo ""

if go test -v ./... 2>&1 | tee test_output.txt; then
    echo -e "${GREEN}✓ All unit tests passed${NC}"
else
    echo -e "${RED}✗ Some unit tests failed${NC}"
    exit 1
fi
echo ""

# Step 2: Generate coverage report
echo -e "${CYAN}Step 2: Generating Coverage Report${NC}"
echo "Creating coverage profile..."
go test -coverprofile=coverage.out ./...
go tool cover -html=coverage.out -o coverage.html

# Calculate coverage percentage
coverage=$(go tool cover -func=coverage.out | grep total | awk '{print $3}')
echo -e "Total Coverage: ${GREEN}${coverage}${NC}"
echo "Coverage report saved to: coverage.html"
echo ""

# Step 3: Run go vet (static analysis)
echo -e "${CYAN}Step 3: Running Static Analysis (go vet)${NC}"
if go vet ./...; then
    echo -e "${GREEN}✓ No issues found${NC}"
else
    echo -e "${RED}✗ Static analysis found issues${NC}"
    exit 1
fi
echo ""

# Step 4: Check formatting
echo -e "${CYAN}Step 4: Checking Code Formatting${NC}"
unformatted=$(gofmt -l .)
if [ -z "$unformatted" ]; then
    echo -e "${GREEN}✓ All files properly formatted${NC}"
else
    echo -e "${YELLOW}⚠ The following files need formatting:${NC}"
    echo "$unformatted"
    echo ""
    echo -e "${YELLOW}Run 'make fmt' to format code${NC}"
fi
echo ""

# Step 5: Test summary
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Test Summary${NC}"
echo -e "${CYAN}========================================${NC}"

total_tests=$(grep -c "^=== RUN" test_output.txt || echo "0")
passed_tests=$(grep -c "^--- PASS" test_output.txt || echo "0")
failed_tests=$(grep -c "^--- FAIL" test_output.txt || echo "0")

echo "Total Tests Run: $total_tests"
echo -e "Passed: ${GREEN}$passed_tests${NC}"
if [ "$failed_tests" -gt 0 ]; then
    echo -e "Failed: ${RED}$failed_tests${NC}"
else
    echo -e "Failed: $failed_tests"
fi
echo "Coverage: $coverage"
echo ""

# Step 6: Integration tests (optional)
if [ "$1" == "--integration" ]; then
    echo -e "${CYAN}Step 6: Running Integration Tests${NC}"
    echo -e "${YELLOW}NOTE: Server must be running on http://localhost:8080${NC}"
    echo ""

    read -p "Is the server running? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        chmod +x tests/integration_test.sh
        ./tests/integration_test.sh
    else
        echo -e "${YELLOW}Skipping integration tests${NC}"
        echo "To run integration tests:"
        echo "  1. Start server: go run cmd/server/main.go"
        echo "  2. Run: ./tests/integration_test.sh"
    fi
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All Tests Completed Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"

# Cleanup
rm -f test_output.txt
