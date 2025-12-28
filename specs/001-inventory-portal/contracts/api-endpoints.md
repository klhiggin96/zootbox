# API Endpoints: ZootBox Backend

**Feature**: 001-inventory-portal
**Date**: 2025-12-28
**Backend Source**: `Backend/internal/api/router.go` (deployed at http://localhost:8080)

**Note**: This document references the **already deployed** ZootBox backend service. All endpoints are implemented, tested, and running on Android tablets. The portal consumes these existing APIs.

---

## Base URL

```
http://localhost:8080
```

**Access Methods**:
- **Local (development)**: Portal running on same machine as backend (localhost:8080)
- **Remote (ADB)**: `adb forward tcp:8080 tcp:8080` → Portal at localhost:8080 connects to tablet
- **Remote (VPN/Network)**: Portal connects to tablet's private IP (e.g., http://192.168.1.100:8080)

---

## Health & Monitoring

### GET /health

Check backend service health and database connectivity.

**Request**: None

**Response** (200 OK):
```json
{
  "status": "healthy",
  "uptime_seconds": 397,
  "database_ok": true
}
```

**Portal Usage**:
- Called every 30 seconds to determine machine online/offline status
- If 3 consecutive failures → mark machine offline
- Display uptime in Machine Settings page

---

### GET /metrics

Prometheus metrics for monitoring (not used by portal).

**Request**: None

**Response** (200 OK):
```
# Prometheus text format
http_requests_total{method="GET",endpoint="/api/v1/coils"} 1523
```

**Portal Usage**: Future feature (analytics dashboard)

---

## Inventory Endpoints

### GET /api/v1/coils

Retrieve all 100 coils for the selected machine.

**Request**: None

**Response** (200 OK):
```json
[
  {
    "id": "A1",
    "inventory": 10,
    "status": "available",
    "version": 1,
    "link_group_id": null,
    "updated_at": "2025-12-28T14:00:00Z"
  },
  {
    "id": "A2",
    "inventory": 3,
    "status": "available",
    "version": 5,
    "link_group_id": "c47ac10b-58cc-4372-a567-0e02b2c3d481",
    "updated_at": "2025-12-28T13:45:00Z"
  },
  ...
  {
    "id": "J10",
    "inventory": 0,
    "status": "jammed",
    "version": 8,
    "link_group_id": null,
    "updated_at": "2025-12-28T12:30:00Z"
  }
]
```

**Portal Usage**:
- Called every 5 seconds (auto-refresh)
- Renders 10x10 grid (rows A-J, columns 1-10)
- Color-coded cells: green (available), red (jammed), orange (low stock), gray (empty)

---

### GET /api/v1/coils/{coilId}

Retrieve a single coil by ID.

**Request**:
- Path parameter: `coilId` (e.g., "A5", "J10")

**Response** (200 OK):
```json
{
  "id": "A5",
  "inventory": 7,
  "status": "available",
  "version": 12,
  "link_group_id": null,
  "updated_at": "2025-12-28T14:30:00Z"
}
```

**Response** (404 Not Found):
```json
{
  "error": "Coil not found"
}
```

**Portal Usage**:
- Called when user clicks a coil cell for detailed view
- Displays current inventory, last update time, link status

---

### GET /api/v1/coils/low-stock

Retrieve coils with inventory <= 2 (low stock threshold).

**Request**: None

**Response** (200 OK):
```json
[
  {
    "id": "B3",
    "inventory": 2,
    "status": "available",
    "version": 7,
    "link_group_id": null,
    "updated_at": "2025-12-28T13:00:00Z"
  },
  {
    "id": "C7",
    "inventory": 1,
    "status": "available",
    "version": 10,
    "link_group_id": "d58bd21d-69dd-5594-c789-2g24d4e5f691",
    "updated_at": "2025-12-28T12:15:00Z"
  }
]
```

**Portal Usage**:
- Display low-stock indicator badge (e.g., "5 coils low") in nav bar
- Filter grid to show only low-stock coils
- Generate restocking task list

---

## Transaction Endpoint (Android App Only)

### POST /api/v1/transactions

Record a vend event (called by Android app, NOT portal).

**Request**:
```json
{
  "coil_id": "A5",
  "status": "success",
  "timestamp": "2025-12-28T14:32:00Z",
  "transaction_id": "nayax-12345"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "coil_id": "A5",
  "inventory_after": 6
}
```

**Portal Usage**: NOT USED (Android app creates transactions, portal only reads inventory)

---

## Jam Management Endpoints

### GET /api/v1/jam-events

Retrieve jam events with optional status filter.

**Request**:
- Query parameter: `status` (optional: "open", "resolved", "all")
- Default: Returns all jam events

**Examples**:
- `/api/v1/jam-events` → All jams
- `/api/v1/jam-events?status=open` → Only open jams
- `/api/v1/jam-events?status=resolved` → Only resolved jams

**Response** (200 OK):
```json
[
  {
    "id": "e47ac10b-58cc-4372-a567-0e02b2c3d480",
    "coil_id": "B3",
    "timestamp": "2025-12-28T13:15:00Z",
    "status": "open",
    "resolved_at": null
  },
  {
    "id": "f58bd21d-69dd-5594-c789-2g24d4e5f691",
    "coil_id": "G8",
    "timestamp": "2025-12-28T11:00:00Z",
    "status": "resolved",
    "resolved_at": "2025-12-28T11:30:00Z"
  }
]
```

**Portal Usage**:
- Display in Jam Management page (table with coil ID, timestamp, status)
- Filter dropdown: "Open Jams Only" → `?status=open`
- Show resolution timestamp for resolved jams

---

### POST /api/v1/jam-events/{eventId}/resolve

Resolve an open jam event.

**Request**:
- Path parameter: `eventId` (UUID)
- Body: None

**Response** (200 OK):
```json
{
  "success": true,
  "jam_event_id": "e47ac10b-58cc-4372-a567-0e02b2c3d480",
  "coil_id": "B3",
  "resolved_at": "2025-12-28T14:35:00Z"
}
```

**Response** (404 Not Found):
```json
{
  "error": "Jam event not found"
}
```

**Response** (400 Bad Request):
```json
{
  "error": "Jam event already resolved"
}
```

**Portal Usage**:
- "Resolve" button in Jam Management table
- After resolution: refresh jam list, update coil status to "available"

**Side Effects** (Backend):
- Sets `jam_events.status = 'resolved'`
- Sets `jam_events.resolved_at = NOW()`
- Updates `coils.status = 'available'` (if inventory > 0)

---

## Product Linking Endpoints

### GET /api/v1/product-links/{sku}/resolve

Resolve product SKU to available coil (used by Android app during purchase).

**Request**:
- Path parameter: `sku` (e.g., "COKE-001")

**Response** (200 OK):
```json
{
  "product_sku": "COKE-001",
  "selected_coil_id": "A2",
  "link_group_id": "c47ac10b-58cc-4372-a567-0e02b2c3d481",
  "strategy": "first_available"
}
```

**Response** (404 Not Found):
```json
{
  "error": "Product SKU not found or no coils available"
}
```

**Portal Usage**: NOT USED (Android app resolves SKU to coil during vend operation)

---

### GET /api/v1/admin/product-links

Retrieve all product link configurations.

**Request**: None

**Response** (200 OK):
```json
[
  {
    "link_group_id": "c47ac10b-58cc-4372-a567-0e02b2c3d481",
    "product_sku": "COKE-001",
    "linked_coil_ids": "[\"A1\",\"A2\",\"A3\"]",
    "selection_strategy": "first_available",
    "created_at": "2025-12-27T10:00:00Z"
  },
  {
    "link_group_id": "d58bd21d-69dd-5594-c789-2g24d4e5f691",
    "product_sku": "PEPSI-002",
    "linked_coil_ids": "[\"B1\",\"B2\"]",
    "selection_strategy": "first_available",
    "created_at": "2025-12-27T11:30:00Z"
  }
]
```

**Portal Usage**:
- Display in Product Links page (table with SKU, coil IDs, creation date)
- Parse `linked_coil_ids` JSON string: `JSON.parse(link.linked_coil_ids)` → `["A1", "A2", "A3"]`

---

### POST /api/v1/admin/product-links

Create a new product link group.

**Request**:
```json
{
  "product_sku": "SPRITE-003",
  "linked_coil_ids": ["C1", "C2", "C3"]
}
```

**Response** (201 Created):
```json
{
  "link_group_id": "a69ce32e-70ee-6605-d890-3h35e5f6g702",
  "product_sku": "SPRITE-003",
  "linked_coil_ids": "[\"C1\",\"C2\",\"C3\"]",
  "selection_strategy": "first_available",
  "created_at": "2025-12-28T14:40:00Z"
}
```

**Response** (400 Bad Request):
```json
{
  "error": "Coil C1 is already linked to product COKE-001"
}
```

**Validation Rules**:
- `product_sku`: Non-empty string (1-100 characters)
- `linked_coil_ids`: Array of 1-10 valid coil IDs (A1-J10)
- Coils cannot belong to multiple link groups (unique constraint)

**Portal Usage**:
- "Create Product Link" form in Product Links page
- Multi-select dropdown for coil IDs
- Validate coils not already linked before submitting

---

### DELETE /api/v1/admin/product-links/{linkGroupId}

Delete a product link group.

**Request**:
- Path parameter: `linkGroupId` (UUID)

**Response** (204 No Content):
- Empty body

**Response** (404 Not Found):
```json
{
  "error": "Product link not found"
}
```

**Portal Usage**:
- "Delete" button in Product Links table
- Confirmation dialog before deletion
- After deletion: refresh product links list

**Side Effects** (Backend):
- Deletes `product_links` row
- Sets `coils.link_group_id = NULL` for all linked coils

---

## Admin Endpoints

### POST /api/v1/admin/refill

Refill all 100 coils to maximum inventory (10 units).

**Request**: None (no body)

**Response** (200 OK):
```json
{
  "success": true,
  "refilled_count": 100,
  "timestamp": "2025-12-28T14:45:00Z"
}
```

**Portal Usage**:
- "Refill All Coils" button on Inventory page
- Confirmation dialog: "Set all 100 coils to inventory=10?"
- After refill: refresh inventory grid (all cells show "10")
- Show success toast: "100 coils refilled successfully"

**Side Effects** (Backend):
- Updates `coils SET inventory = 10, updated_at = NOW()` for all 100 coils
- Increments `version` for each coil (optimistic locking)

---

### PUT /api/v1/admin/coils/{coilId}

Manually set inventory for a specific coil.

**Request**:
- Path parameter: `coilId` (e.g., "A5")
- Body:
```json
{
  "inventory": 5
}
```

**Response** (200 OK):
```json
{
  "id": "A5",
  "inventory": 5,
  "status": "available",
  "version": 13,
  "link_group_id": null,
  "updated_at": "2025-12-28T14:50:00Z"
}
```

**Response** (400 Bad Request):
```json
{
  "error": "Inventory must be between 0 and 10"
}
```

**Response** (404 Not Found):
```json
{
  "error": "Coil A5 not found"
}
```

**Response** (409 Conflict):
```json
{
  "error": "Coil was updated by another user. Please refresh."
}
```

**Validation Rules**:
- `inventory`: Integer 0-10
- Optimistic locking: Request must include current `version`, backend increments on success

**Portal Usage**:
- "Edit Inventory" button on coil cell
- Modal dialog with number input (0-10)
- Frontend validates range before submitting
- Handle 409 Conflict → show error, reload coil data

**Side Effects** (Backend):
- Updates `coils SET inventory = ?, version = version + 1, updated_at = NOW()`
- If `inventory > 0` and `status = 'jammed'` → status remains jammed (manual status change via jam resolution)

---

## CORS Configuration

**Backend Middleware**: `Backend/internal/api/middleware/cors.go`

**Allowed Origins** (configured for portal):
- `http://localhost:3000` (development server)
- `http://localhost:8000` (alternative dev port)
- `file://` (opening HTML directly in browser)

**Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS

**Allowed Headers**: Content-Type, Authorization (future), X-Correlation-ID

**Portal Requirement**: If portal runs on different origin (e.g., http://localhost:5000), backend CORS middleware must be updated to whitelist that origin.

---

## Error Response Format

All error responses follow consistent format:

**4xx/5xx Errors**:
```json
{
  "error": "Human-readable error message"
}
```

**Examples**:
- 400 Bad Request: `{ "error": "Inventory must be between 0 and 10" }`
- 404 Not Found: `{ "error": "Coil A5 not found" }`
- 409 Conflict: `{ "error": "Coil was updated by another user" }`
- 500 Internal Server Error: `{ "error": "Database connection failed" }`

**Portal Handling**:
- Extract `error` field from JSON response
- Display in toast notification (bottom-right corner)
- Log to browser console for debugging

---

## Rate Limiting & Throttling

**Current Status**: NOT IMPLEMENTED (backend has no rate limiting)

**Portal Responsibility**:
- Throttle auto-refresh to 5 seconds minimum (avoid overwhelming tablet)
- Debounce user inputs (500ms delay before API call)
- Cancel in-flight requests on machine switch (avoid stale responses)

**Future Enhancement**: Backend could add rate limiting (e.g., 100 requests/min per IP) using middleware.

---

## Authentication & Authorization

**Current Status**: NONE (per constitution Principle 5: Localhost-Only Security)

**Security Model**:
- Backend binds to 127.0.0.1:8080 (localhost only)
- Portal accesses via ADB port forwarding or VPN
- Network-level security (firewall, VPN) protects endpoints
- No API keys, tokens, or passwords

**Future Enhancement**: If remote access needed, add Basic HTTP Auth for /admin/* endpoints.

---

## API Versioning

**Current Version**: v1 (`/api/v1/*`)

**Stability**: All endpoints are stable and deployed. Portal depends on these exact contracts.

**Breaking Changes**: If backend introduces breaking changes, increment API version (/api/v2/*) and maintain v1 for backward compatibility.

**Portal Compatibility**: Portal targets v1 APIs. If v2 introduced, portal must be updated to support both versions (graceful degradation).

---

## Testing Endpoints

**Local Development**:
```bash
# Check backend health
curl http://localhost:8080/health

# Get all coils
curl http://localhost:8080/api/v1/coils

# Get single coil
curl http://localhost:8080/api/v1/coils/A5

# Refill all coils
curl -X POST http://localhost:8080/api/v1/admin/refill

# Manual inventory update
curl -X PUT http://localhost:8080/api/v1/admin/coils/A5 \
  -H "Content-Type: application/json" \
  -d '{"inventory": 5}'
```

**Portal Development** (with backend running locally):
1. Start backend: `cd Backend && ./backend`
2. Verify health: `curl http://localhost:8080/health`
3. Open portal: `cd portal && python -m http.server 3000`
4. Portal calls: `fetch('http://localhost:8080/api/v1/coils')`

---

## References

- **Backend Source Code**: `Backend/internal/api/router.go` (route definitions)
- **Backend Handlers**: `Backend/internal/api/handlers/*.go` (implementation)
- **Backend Models**: `Backend/internal/models/*.go` (data structures)
- **Deployed Backend**: Running on Android tablets at `http://localhost:8080` (PID 5784)
- **Portal API Client**: `portal/js/api/*.js` (fetch wrappers)
