# ZootBox Backend Inventory Service

Lightweight Go microservice for managing real-time inventory across 100 vending machine coil slots. Integrates with the ZootBox Android app via localhost REST API.

## Features

- **Real-time Inventory Tracking**: Manage 100 coil positions (A1-J10)
- **Transaction Recording**: Log vend outcomes reported by Android app
- **Jam Event Management**: Track and resolve motor jam events
- **Product Linking**: Multi-coil support for high-demand products
- **Admin Interface**: Refill operations and monitoring dashboard
- **Power Loss Recovery**: Zero data loss with SQLite WAL mode

## Architecture

- **Language**: Go 1.21+
- **Database**: SQLite3 with WAL mode
- **Web Framework**: Chi router v5
- **Logging**: Zerolog structured logging
- **Target Platform**: ARM64 Android tablet (Android 8.0+)

## Resource Constraints

- **Memory**: ≤30MB RSS
- **CPU**: <5% idle, <20% under load
- **API Latency**: <50ms (95th percentile)
- **Binary Size**: <15MB
- **Startup Time**: <5 seconds

## Quick Start

### Prerequisites

- Go 1.21 or later
- SQLite3
- (For tablet deployment) Android Debug Bridge (ADB)

### Development

```bash
# Install dependencies
go mod download

# Run database migrations
make migrate

# Start the server
make run

# Run tests
make test

# Check test coverage
make test-coverage
```

### Building for Production

```bash
# Build for ARM64 Android tablet
make build-arm64

# Install to tablet via ADB
make install-tablet
```

## Configuration

Environment variables:

- `DB_PATH` - SQLite database path (default: `/tmp/zootbox/inventory.db`)
- `HTTP_HOST` - Server bind address (default: `127.0.0.1`)
- `HTTP_PORT` - Server port (default: `8080`)
- `MONITORING_URL` - External monitoring webhook URL
- `MONITORING_AUTH` - Basic auth for monitoring webhook
- `LOG_LEVEL` - Logging level: debug, info, warn, error (default: `info`)

## API Endpoints

See `contracts/openapi.yaml` for complete API documentation.

### Core Endpoints

- `GET /health` - Health check
- `GET /metrics` - Prometheus metrics
- `GET /api/v1/coils` - List all coils
- `GET /api/v1/coils/{coilId}` - Get coil details
- `POST /api/v1/transactions` - Record vend event from Android
- `GET /api/v1/jam-events` - List jam events

### Admin Endpoints

- `POST /api/v1/admin/refill` - Refill all coils to 10
- `PUT /api/v1/admin/coils/{coilId}` - Manually set coil inventory
- `POST /api/v1/admin/product-links` - Create product link group
- `POST /api/v1/jam-events/{eventId}/resolve` - Resolve jam event

## Integration with Android App

The backend provides a passive record-keeping service. The Android app:

1. Calls `GET /api/v1/coils` to display inventory
2. Processes payment via Nayax reader
3. Calls DMVI WallCoilMachineService to vend product
4. Reports outcome via `POST /api/v1/transactions`

Backend updates inventory based on reported status:
- `status=success` → Decrement inventory by 1
- `status=jam` → Create jam event, inventory unchanged
- `status=failed` → Log transaction, inventory unchanged

## Testing

### Quick Start
```bash
# Run all unit tests
make test

# Generate coverage report
make test-coverage

# Run integration tests (requires server running)
make test-integration

# Run comprehensive test suite
make test-all
```

### Test Coverage
- **39 unit tests** across repository, service, and handler layers
- **14 integration test scenarios** for end-to-end validation
- **≥85% code coverage** target met

### Documentation
- **Quick Reference**: [TESTING_QUICK_REF.md](TESTING_QUICK_REF.md) - Common test commands
- **Full Guide**: [TESTING.md](TESTING.md) - Comprehensive testing documentation
- **Coverage Report**: [TEST_SUMMARY.md](TEST_SUMMARY.md) - Detailed test breakdown

## Development Workflow

1. Make changes to source code
2. Run tests: `make test`
3. Check coverage: `make test-coverage`
4. Run locally: `make run`
5. Test API endpoints with curl or integration tests
6. Build for ARM64: `make build-arm64`
7. Deploy to tablet: `make install-tablet`

## Project Structure

```
Backend/
├── cmd/
│   └── server/
│       └── main.go           # Entry point
├── internal/
│   ├── api/                  # HTTP handlers and routing
│   ├── config/               # Configuration loading
│   ├── db/                   # Database connection and migrations
│   ├── models/               # Data structures
│   ├── services/             # Business logic
│   └── repositories/         # Data access layer
├── go.mod                    # Go module definition
├── Makefile                  # Build commands
└── README.md                 # This file
```

## License

Proprietary - ZootBox Vending Solutions
