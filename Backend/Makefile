.PHONY: run build test build-arm64 install-tablet clean migrate

# Build for current platform
build:
	go build -o backend cmd/server/main.go

# Run the server locally
run:
	go run cmd/server/main.go

# Run all tests
test:
	go test -v ./...

# Run tests with coverage
test-coverage:
	go test -coverprofile=coverage.out ./...
	go tool cover -html=coverage.out -o coverage.html

# Run tests with race detector
test-race:
	go test -race -v ./...

# Run comprehensive test suite
test-all:
	chmod +x tests/run_tests.sh
	./tests/run_tests.sh

# Run integration tests (requires running server)
test-integration:
	chmod +x tests/integration_test.sh
	./tests/integration_test.sh

# Build for ARM64 Android tablet
build-arm64:
	CGO_ENABLED=1 GOOS=linux GOARCH=arm64 go build -o build/backend-arm64 -ldflags="-s -w" cmd/server/main.go

# Install to Android tablet via ADB
install-tablet: build-arm64
	adb push build/backend-arm64 /data/local/tmp/zootbox/backend
	adb shell chmod +x /data/local/tmp/zootbox/backend
	@echo "Backend installed to /data/local/tmp/zootbox/backend"

# Run database migrations
migrate:
	go run cmd/server/main.go --migrate

# Clean build artifacts
clean:
	rm -f backend build/backend-arm64 coverage.out coverage.html
	rm -rf build/

# Format code
fmt:
	go fmt ./...

# Lint code
lint:
	golangci-lint run

# Tidy dependencies
tidy:
	go mod tidy
