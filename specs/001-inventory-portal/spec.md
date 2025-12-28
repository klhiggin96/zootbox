# Feature Specification: ZootBox Inventory Web Portal

**Feature Branch**: `001-inventory-portal`
**Created**: 2025-12-28
**Status**: Draft
**Input**: User description: "Create a web portal for Zootbox machine inventory that operators can use to monitor inventory, manage refills, handle jams, and configure product links"

## Clarifications

### Session 2025-12-28

- Q: Access control & authentication - The portal includes privileged operations (bulk refill, manual inventory override, product link management) but doesn't specify authentication or access control. → A: No authentication (localhost-only trust) - portal accessible to anyone with network access to the endpoint
- Q: Single vs. multi-machine management - The specification describes managing "100 coils" but it's unclear whether this portal manages a single machine or multiple machines. → A: Multi-machine - portal can switch between/manage multiple machines
- Q: Audit trail for admin operations - The portal allows privileged operations (bulk refill, manual inventory adjustments, product link changes) but doesn't specify whether these operations should be logged/tracked. → A: No audit trail - operations execute without additional tracking to preserve machine performance
- Q: Portal deployment location - Where does the portal run (on tablet with backend, or separately)? → A: Portal runs on operator's Windows computer to remotely monitor field machines via ADB port forwarding or VPN (backend runs on each tablet at localhost:8080)
- Q: Machine configuration storage - Where are machine configurations (names, endpoint URLs) stored? → A: Browser local storage (single workstation) - machine configs saved in browser, tied to one PC
- Q: Navigation structure between pages - The specification mentions multiple pages (Inventory Grid, Jam Management, Product Links) but doesn't specify navigation pattern. → A: Top navigation bar with page tabs/links - distinct pages with navigation menu

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Monitor Real-Time Inventory (Priority: P1)

As a vending machine operator, I need to quickly view the current inventory status of all 100 coil positions so I can identify which products need restocking without physically inspecting the machine.

**Why this priority**: This is the core value proposition of the portal - providing remote visibility into inventory levels. Without this, operators must physically visit machines or rely on sales data to infer stock levels.

**Independent Test**: Can be fully tested by loading the portal and verifying all 100 coils (A1-J10) display with current inventory counts from the backend API. Delivers immediate value by eliminating manual inventory checks.

**Acceptance Scenarios**:

1. **Given** the backend is running and seeded with 100 coils, **When** operator opens the portal homepage, **Then** all 100 coil positions are displayed in a 10x10 grid (rows A-J, columns 1-10) with current inventory counts
2. **Given** a coil has low stock (inventory <= 2), **When** viewing the inventory grid, **Then** that coil is visually highlighted as low-stock
3. **Given** a coil is jammed, **When** viewing the inventory grid, **Then** that coil displays a "jammed" status indicator
4. **Given** the portal is open, **When** inventory changes in the backend (via Android app transaction), **Then** the portal reflects the updated inventory within 5 seconds
5. **Given** multiple coils are empty (inventory = 0), **When** viewing the inventory grid, **Then** empty coils are clearly distinguished from stocked coils

---

### User Story 2 - Bulk Refill After Route Visit (Priority: P2)

As a route driver completing a machine refill, I need to reset all coil inventories to full (10 units) with a single action so I can quickly log restocking operations without manually updating 100 individual coils.

**Why this priority**: This is the primary administrative action operators perform regularly. Route drivers visit machines on a schedule and need to efficiently record full restocks. Manual updates for 100 coils would be error-prone and time-consuming.

**Independent Test**: Can be fully tested by clicking a "Refill All" button and verifying all coil inventories are set to 10 via backend API. Delivers value by reducing 100 manual updates to a single click.

**Acceptance Scenarios**:

1. **Given** coils have varying inventory levels (0-10), **When** operator clicks "Refill All Coils", **Then** all 100 coils are set to inventory = 10 in the backend
2. **Given** a refill operation is in progress, **When** waiting for completion, **Then** a loading indicator shows progress and the operator cannot trigger duplicate refills
3. **Given** the refill completes successfully, **When** viewing the inventory grid, **Then** all coils display inventory = 10 and a success message confirms the operation
4. **Given** the refill fails (network error, backend unavailable), **When** the error occurs, **Then** a clear error message explains the failure and inventory remains unchanged
5. **Given** some coils are jammed, **When** performing a bulk refill, **Then** jammed coils are also reset to inventory = 10 (refill does not resolve jams, just updates count)

---

### User Story 3 - Manual Inventory Adjustment (Priority: P2)

As a vending machine operator, I need to manually set the inventory for a specific coil so I can correct discrepancies (e.g., damaged product removed, manual sale, inventory audit correction) without performing a full machine refill.

**Why this priority**: This handles exceptions and corrections that fall outside normal vend operations. While less frequent than bulk refills, it's essential for maintaining accurate inventory when physical reality diverges from system records.

**Independent Test**: Can be fully tested by selecting a single coil, entering a new inventory value (0-10), and verifying the update persists in the backend. Delivers value for handling one-off corrections without affecting other coils.

**Acceptance Scenarios**:

1. **Given** viewing a specific coil (e.g., A5 with inventory = 7), **When** operator clicks "Edit Inventory", **Then** a form appears allowing entry of a new inventory value (0-10)
2. **Given** the edit form is open, **When** operator enters a valid value (e.g., 3) and confirms, **Then** that coil's inventory updates to 3 in the backend and the grid refreshes
3. **Given** the edit form is open, **When** operator enters an invalid value (e.g., -1, 15, "abc"), **Then** a validation error prevents submission with a message "Inventory must be between 0 and 10"
4. **Given** editing a coil's inventory, **When** the update fails due to network error, **Then** the original inventory value is retained and an error message is displayed
5. **Given** a coil is jammed, **When** manually updating its inventory, **Then** the update succeeds and the jammed status remains unchanged (inventory and status are independent)

---

### User Story 4 - Resolve Jam Events (Priority: P3)

As a vending machine operator, I need to view open jam events and mark them as resolved after physically clearing the jam so the system accurately reflects which coils are operational.

**Why this priority**: While important for operational accuracy, jam resolution is reactive and happens after the jam is reported. This is lower priority than proactive inventory management (P1-P2) but essential for maintaining coil availability.

**Independent Test**: Can be fully tested by viewing the jam events list, selecting an open jam, clicking "Resolve", and verifying the jam status updates to "resolved" with a timestamp. Delivers value by tracking jam resolution workflow.

**Acceptance Scenarios**:

1. **Given** the backend has recorded jam events, **When** operator navigates to the Jam Management page, **Then** a list of all jam events is displayed showing coil ID, timestamp, and status (open/resolved)
2. **Given** viewing the jam events list, **When** filtering by "Open Jams Only", **Then** only unresolved jam events are displayed
3. **Given** an open jam event for coil B3, **When** operator clicks "Resolve" for that event, **Then** the jam status updates to "resolved" with the current timestamp recorded
4. **Given** a jam is resolved, **When** viewing the inventory grid, **Then** the coil's status changes from "jammed" to "available" (assuming inventory > 0)
5. **Given** resolving a jam fails (network error), **When** the error occurs, **Then** the jam remains in "open" status and an error message explains the failure

---

### User Story 5 - Configure Multi-Coil Products (Priority: P4)

As a vending machine manager, I need to link multiple coil positions to a single product SKU so the system can intelligently select from available coils when a product is purchased (e.g., "Coca-Cola" stocked in coils A1, A2, A3).

**Why this priority**: This is an advanced configuration feature for larger operations that stock the same product across multiple coils. It's valuable for optimizing coil utilization but not essential for basic inventory management. Most small operators may not use this feature.

**Independent Test**: Can be fully tested by creating a product link (SKU "COKE-001" → coils [A1, A2, A3]), verifying it appears in the product links list, and confirming deletion removes the link. Delivers value for advanced inventory optimization.

**Acceptance Scenarios**:

1. **Given** viewing the Product Links page, **When** operator clicks "Create Product Link", **Then** a form appears to enter Product SKU and select multiple coil IDs
2. **Given** the create form is open, **When** operator enters SKU "COKE-001" and selects coils [A1, A2, A3] and submits, **Then** a new product link is created with selection strategy "first_available"
3. **Given** existing product links, **When** viewing the Product Links list, **Then** each link displays the SKU, linked coil IDs, selection strategy, and creation date
4. **Given** a product link exists, **When** operator clicks "Delete" for that link, **Then** the link is removed and the coils are no longer associated with that SKU
5. **Given** creating a product link, **When** selecting coils already linked to another product, **Then** a validation error prevents creation with message "Coil [X] is already linked to product [Y]"
6. **Given** creating a product link with no coils selected, **When** submitting the form, **Then** a validation error prevents creation with message "At least one coil must be selected"

---

### Edge Cases

- What happens when the backend API is unavailable (service down, network failure)?
  - Portal displays a clear error message: "Unable to connect to inventory service"
  - Last successfully loaded data remains visible (if any) with a staleness indicator
  - User can retry the connection manually via a "Retry" button

- How does the system handle concurrent edits (two operators updating the same coil simultaneously)?
  - Backend uses optimistic locking (version field in Coil model)
  - If a concurrent edit occurs, the second request fails with error "Coil inventory was updated by another user. Please refresh and try again"

- What happens when attempting to refill all coils while some coils are being edited?
  - Bulk refill operations take precedence (no partial refills)
  - Any in-progress edit forms are invalidated and must be refreshed after bulk refill completes

- How does the portal handle extremely slow network connections?
  - Loading indicators display for any operation taking >1 second
  - Operations timeout after 30 seconds with clear timeout error message
  - User can cancel long-running operations via a "Cancel" button

- What happens when viewing a coil that was just deleted from the backend?
  - Portal displays "Coil not found" error when attempting to view/edit
  - Inventory grid refreshes automatically to remove deleted coil

- What happens when operator switches machines during an in-progress operation (refill, edit, etc.)?
  - Any in-progress operation for the previous machine is cancelled
  - Portal loads the newly selected machine's data from scratch
  - A warning message confirms the machine switch

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Portal MUST display all 100 coil positions (A1 through J10) in a 10x10 grid layout (rows A-J, columns 1-10)
- **FR-002**: Portal MUST show current inventory count (0-10) for each coil retrieved from backend GET /api/v1/coils
- **FR-003**: Portal MUST visually distinguish coil states: normal stock (3-10), low stock (1-2), empty (0), and jammed
- **FR-004**: Portal MUST highlight low-stock coils (inventory <= 2) retrieved from backend GET /api/v1/coils/low-stock
- **FR-005**: Portal MUST refresh inventory data automatically every 5 seconds or provide a manual "Refresh" button
- **FR-006**: Portal MUST provide a "Refill All Coils" button that calls backend POST /api/v1/admin/refill
- **FR-007**: Portal MUST display a confirmation dialog before executing bulk refill operation
- **FR-008**: Portal MUST show loading indicators during all API operations (refill, edit, resolve jam, create link)
- **FR-009**: Portal MUST allow operators to manually edit individual coil inventory via backend PUT /api/v1/admin/coils/{coilId}
- **FR-010**: Portal MUST validate manual inventory inputs (0-10 integer range) before submitting to backend
- **FR-011**: Portal MUST provide a Jam Management page displaying all jam events from backend GET /api/v1/jam-events
- **FR-012**: Portal MUST allow filtering jam events by status (open/resolved)
- **FR-013**: Portal MUST allow operators to resolve open jams via backend POST /api/v1/jam-events/{eventId}/resolve
- **FR-014**: Portal MUST provide a Product Links management page displaying all product links from backend GET /api/v1/admin/product-links
- **FR-015**: Portal MUST allow creating product links (SKU + multiple coil IDs) via backend POST /api/v1/admin/product-links
- **FR-016**: Portal MUST allow deleting product links via backend DELETE /api/v1/admin/product-links/{linkGroupId}
- **FR-017**: Portal MUST validate product link creation (non-empty SKU, at least one coil selected)
- **FR-018**: Portal MUST display clear error messages for all failed API operations (network errors, validation failures, backend errors)
- **FR-019**: Portal MUST work on Windows desktop browsers (Chrome, Edge, Firefox) with responsive design for 1024px+ width screens
- **FR-020**: Portal MUST connect to backend APIs at configurable endpoints (each machine accessed via ADB port forwarding like http://localhost:8080, or VPN/network IP like http://192.168.1.100:8080)
- **FR-021**: Portal MUST NOT implement authentication or authorization (relies on network-level access control via localhost binding or VPN/ADB port forwarding)
- **FR-022**: Portal MUST allow operators to configure multiple machine endpoints (machine name/ID + backend API URL)
- **FR-023**: Portal MUST provide a machine selector allowing operators to switch between configured machines
- **FR-024**: Portal MUST clearly display which machine is currently active/selected
- **FR-025**: Portal MUST persist machine configurations locally (browser storage) for operator convenience
- **FR-026**: Portal MUST show machine connection status (online/offline) for each configured machine
- **FR-027**: Portal MUST NOT implement audit logging for admin operations (to preserve machine performance - backend handles transaction logging)
- **FR-028**: Portal MUST provide a persistent top navigation bar with links/tabs for: Inventory, Jam Management, Product Links, and Machine Settings
- **FR-029**: Portal MUST visually indicate the currently active page in the navigation bar
- **FR-030**: Portal MUST display the currently selected machine name/ID in the navigation bar or header

### Key Entities

- **Machine**: Represents a configured vending machine endpoint. Attributes: machine name/identifier (operator-assigned, e.g., "Downtown Location", "Machine-042"), backend API endpoint URL, connection status (online/offline), last successful connection timestamp.

- **Coil**: Represents one of 100 vending machine coil positions (A1-J10) within a specific machine. Attributes: unique ID (e.g., "A5"), current inventory count (0-10), status (available/jammed), optional product link group ID, last update timestamp.

- **Jam Event**: Represents a motor jam or dispensing failure for a specific coil on a specific machine. Attributes: unique event ID, associated coil ID, timestamp when jam occurred, status (open/resolved), optional resolution timestamp.

- **Product Link**: Represents a mapping between a product SKU and multiple coil positions stocking the same product. Attributes: unique link group ID, product SKU identifier, list of linked coil IDs (e.g., [A1, A2, A3]), selection strategy (currently only "first_available"), creation timestamp.

- **Transaction** (read-only reference): Historical record of vend events created by Android app. Portal does not create transactions but may display transaction history in future iterations. Attributes: transaction ID, coil ID, timestamp, status (success/jam/failed), Nayax payment transaction ID, inventory before/after.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operators can view complete inventory status for all 100 coils in under 10 seconds (including initial page load and API response)
- **SC-002**: Bulk refill operation completes in under 30 seconds from button click to confirmation message
- **SC-003**: Manual inventory adjustments for individual coils complete in under 15 seconds from edit click to saved confirmation
- **SC-004**: 95% of inventory monitoring tasks require no scrolling or zooming (entire 10x10 grid visible on standard desktop/tablet screens)
- **SC-005**: Low-stock alerts are immediately identifiable (operators can spot low-stock coils within 3 seconds of viewing the grid)
- **SC-006**: Jam resolution workflow (view open jams → select jam → mark resolved) completes in under 1 minute
- **SC-007**: Product link creation (enter SKU → select coils → save) completes in under 2 minutes for up to 10 linked coils
- **SC-008**: Portal remains functional during backend API latency up to 5 seconds (shows loading indicators, does not freeze)
- **SC-009**: 90% of operators successfully complete their first inventory check without training or documentation
- **SC-010**: Zero data loss during concurrent operations (optimistic locking prevents conflicting updates)
