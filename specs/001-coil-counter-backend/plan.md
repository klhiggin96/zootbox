# Implementation Plan: ZootBox Coil-Counter Backend System

**Branch**: `001-coil-counter-backend` | **Date**: 2025-12-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-coil-counter-backend/spec.md`

**Note**: This plan implements a Go-based inventory management microservice that integrates with the existing ZootBox Android vending machine app via localhost REST API.

## Summary

Build a resource-constrained backend service in Go that manages real-time inventory for 100 vending machine coil positions, coordinates motor activation via USB serial hardware, provides localhost REST API for the existing Android app (MyApplication/), supports admin web interface for operators, and ensures atomic transaction guarantees with deterministic crash recovery—all while maintaining ≤30MB RAM, <5% CPU idle, and <50ms API response times on ARM64 Android tablet

## Technical Context

**Language/Version**: Go 1.21+ (compiled for ARM64/Linux for Android tablet deployment)
**Primary Dependencies**:
- **Web Framework**: Chi router v5 (lightweight, <1MB memory overhead, stdlib-compatible middleware)
- **Database**: SQLite3 with mattn/go-sqlite3 driver (CGO-enabled for ARM64 cross-compilation)
- **USB Serial**: github.com/tarm/serial or github.com/jacobsa/go-serial (motor controller communication)
- **HTTP Client**: net/http stdlib (for external monitoring system push notifications)
- **Logging**: zerolog (structured logging with <1% CPU overhead)
- **Testing**: Go stdlib testing + testify assertions

**Storage**: SQLite3 (single-file database, <10MB on disk, WAL mode for concurrent read/write, IMMEDIATE transactions for optimistic locking)

**Testing**: Go test framework with table-driven tests, httptest for API contract testing, testcontainers-go for integration tests (SQLite in-memory mode)

**Target Platform**: ARM64 Android tablet (Android 8.0+, Linux kernel 4.14+) running alongside existing Android app (MyApplication/)

**Project Type**: Single backend service (no separate frontend - serves REST API to existing Android app + minimal admin web UI)

**Performance Goals**:
- API throughput: 100 req/s sustained (single tablet, localhost only)
- API latency: <50ms p95, <100ms p99 (measured from Android app's Retrofit client)
- Database write latency: <10ms per inventory update (SQLite IMMEDIATE transaction)
- Motor activation response: <50ms from vend API call to USB serial command sent

**Constraints**:
- **Memory**: ≤30MB RSS (constitution requirement - Android app uses 150MB for video)
- **CPU**: <5% idle, <20% under load (spare cycles for Android video decoding)
- **Binary Size**: <15MB compiled (static linking, stripped symbols)
- **Startup Time**: <5 seconds from launch to API ready (constitution requirement)
- **Disk I/O**: Minimize writes to extend tablet storage life (batch transaction logs)

**Scale/Scope**:
- Users: 1 Android app instance + 2-3 admin web sessions concurrently
- Data: 100 coil records, ~1000 transactions/day, 90-day retention = ~30K transaction records
- Codebase: Estimated 3K-5K LOC (Go is concise, stdlib does heavy lifting)
- API Endpoints: ~15 REST endpoints (inventory, vend, admin, health, metrics)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Non-Negotiable Requirements Compliance

| Requirement | Status | Evidence |
|-------------|--------|----------|
| **1. API Compatibility** | ✅ PASS | Phase 1 will generate OpenAPI contracts matching Android app's data models (Product, Coil, Transaction). Research phase includes analyzing MyApplication/app/src/main/java/ to extract expected JSON schemas. |
| **2. Hardware Coordination** | ✅ PASS | Backend owns motor controller (USB serial via /dev/ttyUSB*). Android HardwareService retains ID Scanner (VID:0x0403) and Nayax Payment Reader (VID:0x26f1). No USB device conflicts. |
| **3. Atomic Transactions** | ✅ PASS | SQLite IMMEDIATE transactions with optimistic locking (version column). Vend operation: BEGIN → validate inventory → decrement → send motor command → COMMIT. Rollback on any failure (FR-011a/b/c). |
| **4. No Android App Dependencies** | ✅ PASS | Backend is standalone Go binary. Can start/stop independently. Android app crash does not affect backend process (runs as separate Linux process). |
| **5. Backward Compatibility** | ✅ PASS | Android app degradation strategy documented in spec (FR-031). Backend offline = Android uses cached inventory, blocks new purchases. No breaking API changes without coordinated deployment. |
| **6. Database Portability** | ✅ PASS | SQLite single-file database accessible to both processes. Backend: read/write via mattn/go-sqlite3. Android: emergency read-only access via android.database.sqlite (Java/Kotlin stdlib). |

### Architectural Principles Compliance

| Principle | Status | Implementation Approach |
|-----------|--------|------------------------|
| **1. Android-Friendly Resources** | ✅ PASS | Event-driven Chi router (goroutines only spawn per request). No background polling loops. Zerolog structured logging (<1% CPU). Memory profiling enforced in CI (≤30MB limit). |
| **2. API-First Design** | ✅ PASS | OpenAPI 3.0 spec generated in Phase 1 (contracts/openapi.yaml). API versioning: /api/v1/*, /api/v2/* for breaking changes. Backward compat maintained for 2 major versions via router middleware. |
| **3. Shared Hardware Orchestration** | ✅ PASS | Backend owns motor controller (exclusive USB serial lock). Android owns ID Scanner + Payment Reader. Hardware state exposed via GET /api/v1/hardware/status endpoint. Motor ready flag prevents concurrent ops (FR-020). |
| **4. Fail-Safe Defaults** | ✅ PASS | Offline backend: Android caches last successful GET /api/v1/coils response (ViewModel LiveData). Database lock: retry with exponential backoff (max 3 attempts, 100ms base). Motor jam: mark coil unavailable + persist jam event (FR-013/014). |
| **5. Localhost-Only Security** | ✅ PASS | Chi router binds to 127.0.0.1:8080 only (FR-037d). No TLS overhead on localhost. Admin interface: no authentication (physical access is security boundary, FR-037c). External monitoring: basic auth for webhook (FR-037b). |
| **6. Observable Integration** | ✅ PASS | Zerolog correlation IDs in all responses (FR-037). GET /health endpoint: DB ping + motor serial status + uptime. GET /metrics: Prometheus format (api_requests_total, api_latency_seconds, inventory_updates_total). |

### Constitution Gates: ✅ ALL PASS

No violations detected. Proceed to Phase 0 research.

---

### Post-Phase 1 Re-Evaluation (2025-12-27)

*Constitution Check re-executed after completing Phase 1 design artifacts.*

**Artifacts Reviewed**:
- `data-model.md`: 6-table database schema with optimistic locking, WAL mode, indexes
- `contracts/openapi.yaml`: 15 REST API endpoints with schemas matching Android data models
- `quickstart.md`: Development setup, cross-compilation, deployment instructions

**Compliance Status**:

| Requirement/Principle | Status | Phase 1 Evidence |
|----------------------|--------|------------------|
| **API Compatibility** | ✅ PASS | openapi.yaml defines all endpoints (GET /api/v1/coils, POST /api/v1/vend, etc.) with schemas matching Android app's expected JSON format. Request/response examples provided. |
| **Hardware Coordination** | ✅ PASS | openapi.yaml includes GET /api/v1/hardware/status endpoint exposing motor controller state. Hardware ownership documented in research.md (backend=motor, Android=scanner+payment). |
| **Atomic Transactions** | ✅ PASS | data-model.md transactions table tracks vend outcomes. openapi.yaml POST /api/v1/vend documents atomic guarantee. research.md Decision 6 implements optimistic locking pattern. |
| **No Android Dependencies** | ✅ PASS | quickstart.md shows standalone binary deployment. No shared processes. Backend can start/stop independently via `go run cmd/server/main.go`. |
| **Backward Compatibility** | ✅ PASS | openapi.yaml uses /api/v1/* versioning. Server URL bound to http://127.0.0.1:8080. Android app degradation strategy (cached inventory) documented in spec.md FR-031. |
| **Database Portability** | ✅ PASS | data-model.md shows SQLite schema at /data/local/tmp/zootbox/inventory.db. quickstart.md documents database access from both backend (mattn/go-sqlite3) and Android (android.database.sqlite). |
| **Android-Friendly Resources** | ✅ PASS | quickstart.md includes memory profiling instructions (≤30MB RSS limit). research.md Decision 3 (Chi router) and Decision 5 (Zerolog) ensure minimal overhead. Performance troubleshooting section included. |
| **API-First Design** | ✅ PASS | openapi.yaml generated with complete schemas, examples, and 15 documented endpoints. quickstart.md includes API contract testing with Spectral CLI. |
| **Hardware Orchestration** | ✅ PASS | openapi.yaml GET /api/v1/hardware/status returns motor controller status. data-model.md hardware_devices table tracks device state. Motor ownership clear (backend exclusive). |
| **Fail-Safe Defaults** | ✅ PASS | data-model.md jam_events table persists motor failures. openapi.yaml shows 409 Conflict for version conflicts. quickstart.md documents database busy_timeout=5000 for retry logic. |
| **Localhost-Only Security** | ✅ PASS | openapi.yaml servers section specifies http://127.0.0.1:8080. quickstart.md HTTP_HOST environment variable defaults to 127.0.0.1. No authentication endpoints in API. |
| **Observable Integration** | ✅ PASS | openapi.yaml includes X-Correlation-ID header in all endpoints. GET /health and GET /metrics endpoints documented. quickstart.md shows Zerolog structured logging configuration. |

**Conclusion**: ✅ ALL REQUIREMENTS AND PRINCIPLES REMAIN COMPLIANT

Phase 1 design decisions successfully implement all constitution constraints. No violations detected. Ready to proceed to Phase 2 (tasks.md generation via `/speckit.tasks` command).

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
Backend/ (Go microservice - this feature)
├── cmd/
│   └── server/
│       └── main.go                    # Entry point, CLI flags, signal handling
├── internal/
│   ├── api/                           # HTTP handlers and routing
│   │   ├── handlers/
│   │   │   ├── inventory.go           # GET/PUT /api/v1/coils
│   │   │   ├── vend.go                # POST /api/v1/vend
│   │   │   ├── admin.go               # POST /api/v1/admin/refill, etc.
│   │   │   ├── hardware.go            # GET /api/v1/hardware/status
│   │   │   └── health.go              # GET /health, /metrics
│   │   ├── middleware/
│   │   │   ├── cors.go                # Localhost-only CORS
│   │   │   ├── logging.go             # Zerolog request logging + correlation IDs
│   │   │   └── recovery.go            # Panic recovery
│   │   └── router.go                  # Chi router configuration
│   ├── models/                        # Data structures matching Android app
│   │   ├── coil.go                    # Coil struct (id, inventory, status, version)
│   │   ├── transaction.go             # Transaction struct (id, coil_id, timestamp, outcome)
│   │   ├── jam_event.go               # JamEvent struct
│   │   ├── alert.go                   # LowStockAlert struct (with delivery status)
│   │   └── hardware_device.go         # HardwareDevice struct
│   ├── services/                      # Business logic layer
│   │   ├── inventory.go               # Inventory management (CRUD operations)
│   │   ├── vend.go                    # Vend operation orchestration (atomic)
│   │   ├── motor.go                   # Motor controller communication (USB serial)
│   │   ├── alert.go                   # Low-stock alert delivery (HTTP push)
│   │   └── recovery.go                # Power-loss recovery (detect incomplete txns)
│   ├── db/                            # Database layer
│   │   ├── sqlite.go                  # SQLite connection pool + WAL mode setup
│   │   ├── migrations/                # SQL migration files
│   │   │   ├── 001_init_schema.sql
│   │   │   └── 002_add_version_column.sql
│   │   └── repositories/              # Data access layer
│   │       ├── coil_repo.go           # Coil CRUD with optimistic locking
│   │       ├── transaction_repo.go    # Transaction logging
│   │       └── jam_event_repo.go      # Jam event persistence
│   ├── hardware/                      # Hardware abstraction layer
│   │   └── motor_controller.go        # USB serial interface to motor controller
│   └── config/
│       └── config.go                  # Configuration struct (env vars, defaults)
├── tests/
│   ├── contract/                      # API contract tests (OpenAPI validation)
│   │   └── api_contract_test.go
│   ├── integration/                   # Integration tests (real SQLite, mock hardware)
│   │   ├── vend_integration_test.go
│   │   └── recovery_integration_test.go
│   └── unit/                          # Unit tests (mocked dependencies)
│       ├── inventory_test.go
│       └── vend_test.go
├── scripts/                           # Build and deployment scripts
│   ├── build_arm64.sh                 # Cross-compile for Android ARM64
│   ├── install_tablet.sh              # ADB push to Android tablet
│   └── run_local.sh                   # Local development server
├── go.mod
├── go.sum
├── Makefile                           # Build targets, test runners
└── README.md                          # Development setup instructions

MyApplication/ (Existing Android app - NOT modified by this feature)
└── app/src/main/java/com/example/myapplication/
    ├── ProductGridActivity.kt         # Will call GET /api/v1/coils
    ├── ProductDetailActivity.kt       # Will call POST /api/v1/vend
    └── hardware/HardwareService.kt    # Owns ID Scanner + Payment Reader (no changes)

Shared Data:
/data/local/tmp/zootbox/
├── inventory.db                       # SQLite database (shared access)
└── motor_controller -> /dev/ttyUSB0   # Symlink to motor controller device
```

**Structure Decision**: Single backend service (Go) that serves REST API to existing Android app. No frontend code in this repository—admin interface is minimal HTML served by Go handlers. Android app directory (MyApplication/) is pre-existing and NOT modified by this feature (constitution requirement #1: no app rewrites).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
