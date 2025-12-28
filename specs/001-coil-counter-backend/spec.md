# Feature Specification: ZootBox Coil-Counter Backend System

**Feature Branch**: `001-coil-counter-backend`
**Created**: 2025-12-27
**Status**: Draft
**Input**: User description: "Specify the complete ZootBox 'Coil-Counter' system: an Android-hosted, resource-constrained vending control platform consisting of a Go background service, SQLite persistence, a localhost REST API, and USB-serial hardware communication."

## Clarifications

### Session 2025-12-27

- Q: How should low-stock alerts be delivered to operators? → A: Real-time push notifications to external monitoring system
- Q: What authentication and authorization mechanism should the admin interface use? → A: No authentication required (admin interface accessible to anyone on localhost)
- Q: How should the system handle concurrent purchase requests for the same coil? → A: Optimistic locking with conflict detection (both proceed, second fails on commit and retries)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer Purchase Flow (Priority: P1)

A customer approaches the vending machine, selects a product from the Android display interface, completes payment via the Nayax reader, and receives their product. The backend system must coordinate inventory deduction, motor activation, and transaction logging.

**Why this priority**: This is the core revenue-generating flow. Without reliable purchase processing, the vending machine cannot fulfill its primary business function.

**Independent Test**: Can be fully tested by simulating a payment signal, verifying inventory decrements from 10 to 9 for the selected coil, confirming motor activation signal sent, and validating transaction logged with timestamp and coil ID.

**Acceptance Scenarios**:

1. **Given** coil A5 has inventory count of 5, **When** customer purchases product from coil A5, **Then** inventory decrements to 4, motor activates for coil A5, and transaction is logged with timestamp
2. **Given** coil B3 has inventory count of 1, **When** customer purchases product from coil B3, **Then** inventory decrements to 0 and low-stock alert triggers (count ≤ 2)
3. **Given** coil C7 has inventory count of 0, **When** customer attempts to purchase from coil C7, **Then** purchase is rejected and inventory remains at 0
4. **Given** motor jam occurs during vend from coil D2, **When** jam is detected, **Then** transaction is marked as failed, jam event is logged with coil ID and timestamp, and inventory is not decremented

---

### User Story 2 - Route Driver Refill Operations (Priority: P2)

A route driver arrives to restock the vending machine. They access the admin interface, view current inventory levels across all 100 coils, perform a global refill operation to reset all coils to maximum capacity, or manually adjust specific coil counts for partial refills.

**Why this priority**: Efficient inventory management directly impacts machine uptime and revenue. Route drivers need quick, accurate refill workflows to service multiple machines per day.

**Independent Test**: Can be tested by accessing admin interface, viewing 10x10 grid showing all coil counts, executing "Refill All" action and verifying all coils reset to 10, and manually setting coil F8 to count 7 and verifying persistence.

**Acceptance Scenarios**:

1. **Given** coils have varied inventory levels (some empty, some partial, some full), **When** driver executes "Refill All" action, **Then** all 100 coils are set to count 10 and changes persist to database
2. **Given** driver opens admin interface, **When** viewing inventory grid, **Then** real-time counts for all 100 coils are displayed in 10x10 visual layout
3. **Given** coil G4 shows count of 3, **When** driver manually overrides count to 10, **Then** coil G4 count updates to 10 and change persists across system restart
4. **Given** coil H1 shows count of 8, **When** driver manually sets count to 0 to mark as out-of-service, **Then** coil H1 becomes unavailable for purchase

---

### User Story 3 - Operator Hardware Diagnostics (Priority: P3)

A vending machine operator receives a jam alert notification. They access the admin interface to view hardware status, identify which coil reported the jam (coil E6), execute a "Test Spin" command on coil E6 to verify motor function, and review jam event logs to determine if mechanical service is required.

**Why this priority**: Proactive hardware monitoring reduces downtime and service calls. Operators need diagnostic tools to distinguish between transient jams and mechanical failures requiring technician dispatch.

**Independent Test**: Can be tested by accessing admin interface hardware status page, verifying ID Scanner and Payment Reader connection status displayed, executing "Test Spin" on coil A1 and confirming motor activation without inventory change, and reviewing jam log showing historical events with coil IDs and timestamps.

**Acceptance Scenarios**:

1. **Given** coil B7 experienced a jam 2 hours ago, **When** operator views jam log, **Then** log shows entry with coil B7, timestamp, and event type "motor_jam"
2. **Given** operator is on hardware diagnostics page, **When** E-Seek M260 ID Scanner is connected via USB, **Then** status shows "Connected" with device VID:0x0403/PID:0x6001
3. **Given** operator is on hardware diagnostics page, **When** Nayax Payment Reader is disconnected, **Then** status shows "Disconnected" and last-seen timestamp
4. **Given** operator suspects coil C3 motor malfunction, **When** operator executes "Test Spin" on coil C3, **Then** motor activation signal is sent, operator observes physical motor movement, and inventory count remains unchanged

---

### User Story 4 - Multi-Coil Product Linking (Priority: P4)

An operator configures the system to link coils A1, A2, and A3 to a single product SKU (e.g., "Coca-Cola"). When a customer purchases "Coca-Cola", the system automatically selects the first non-empty linked coil (A1 if available, else A2, else A3) and dispenses from that coil.

**Why this priority**: Product linking enables high-demand products to occupy multiple coil positions, reducing out-of-stock scenarios and increasing revenue. This is an optimization feature that enhances but is not critical to basic vending operations.

**Independent Test**: Can be tested by configuring coils D1, D2, D3 as linked to SKU "Product-X", setting D1 count to 0, D2 count to 5, D3 count to 10, purchasing "Product-X", and verifying vend occurs from D2 (first non-empty coil) with D2 inventory decrementing to 4.

**Acceptance Scenarios**:

1. **Given** coils E1, E2, E3 are linked to SKU "Chips" with counts 0, 0, 7, **When** customer purchases "Chips", **Then** vend occurs from coil E3 and inventory decrements from 7 to 6
2. **Given** coils F1, F2 are linked to SKU "Water" with counts 10, 10, **When** customer purchases "Water", **Then** vend occurs from coil F1 (primary) and inventory decrements from 10 to 9
3. **Given** coils G1, G2, G3 are linked to SKU "Candy" with all counts at 0, **When** customer attempts to purchase "Candy", **Then** purchase is rejected with "Out of Stock" message
4. **Given** coil H1 is primary for SKU "Soda" and is empty, **When** customer purchases "Soda", **Then** system automatically selects next linked coil with inventory and dispenses from that coil

---

### User Story 5 - System Recovery from Power Loss (Priority: P5)

During active operation, a power outage occurs mid-transaction. When power is restored and the system restarts, all inventory counts match pre-outage state, in-progress transactions are detected and marked as failed, and the system resumes normal operation without manual intervention.

**Why this priority**: Vending machines operate in diverse environments with unreliable power. Automatic recovery without data loss or corruption is essential for unattended operation.

**Independent Test**: Can be tested by simulating power loss during a vend operation, restarting the system, verifying inventory counts match pre-failure state, confirming incomplete transaction logged as failed, and validating system accepts new purchase requests immediately after restart.

**Acceptance Scenarios**:

1. **Given** system is processing a vend from coil A5 (count transitioning from 10 to 9), **When** power loss occurs before transaction commits, **Then** after restart inventory remains at 10 and transaction is logged as "power_failure"
2. **Given** system was idle with stable inventory state, **When** power loss and restart occur, **Then** all 100 coil counts match pre-outage state exactly
3. **Given** system restarts after power loss, **When** first customer purchase request arrives, **Then** purchase processes successfully with correct inventory update and motor activation
4. **Given** multiple incomplete transactions exist in recovery log, **When** operator reviews recovery log, **Then** all failed transactions are listed with failure reason, timestamp, and affected coil IDs

---

### Edge Cases

- What happens when a customer purchase request arrives while a "Test Spin" operation is in progress on the same coil?
- **Concurrent purchase requests for same coil**: System uses optimistic locking with conflict detection. First request succeeds and commits inventory decrement. Second request detects version conflict on commit attempt, fails with conflict error status, and client must retry with alternative coil selection.
- What occurs when database becomes locked due to long-running query during high-traffic period?
- How does the system respond when motor activation signal is sent but no acknowledgment is received from hardware controller?
- What happens when inventory count is manually set to an invalid value (negative number or greater than 10)?
- How does the system behave when USB hardware device reconnects mid-operation after brief disconnect?
- What occurs when admin executes "Refill All" while active customer purchases are in progress?
- How does multi-coil linking behave when primary coil has inventory but motor is marked as jammed?

## Requirements *(mandatory)*

### Functional Requirements

**Inventory Management**

- **FR-001**: System MUST maintain inventory state for exactly 100 coils, organized as a 10x10 logical grid (rows A-J, columns 1-10)
- **FR-002**: Each coil MUST store an integer inventory count constrained to range 0-10
- **FR-003**: System MUST persist all inventory state changes to durable storage within 100ms of change
- **FR-004**: System MUST provide a "Refill All" operation that atomically sets all 100 coil counts to 10
- **FR-005**: System MUST allow manual override of any individual coil count via admin interface
- **FR-006**: System MUST reject purchase requests when target coil inventory count is 0
- **FR-007**: System MUST decrement inventory count by exactly 1 for each successful vend operation

**Purchase & Vend Coordination**

- **FR-008**: System MUST accept vend event notifications from Android app after payment and motor activation complete
- **FR-009**: System MUST validate coil exists before recording vend transaction
- **FR-010**: System MUST provide POST /api/v1/transactions endpoint accepting coil_id, status (success/jam/failed), timestamp, and transaction_id from Nayax payment system
- **FR-010a**: System MUST decrement inventory by 1 ONLY when status='success' is reported by Android app
- **FR-010b**: System MUST create jam_event record when status='jam' is reported by Android app
- **FR-010c**: System MUST NOT decrement inventory when status='jam' or status='failed' is reported
- **FR-011**: System MUST ensure atomic transaction behavior: inventory update and transaction logging must both succeed or both rollback
- **FR-011a**: System MUST use optimistic locking for inventory updates to detect concurrent modification conflicts
- **FR-011b**: System MUST return conflict error response when concurrent inventory update detected for same coil
- **FR-011c**: System MUST allow first request to commit successfully while second request fails with conflict status
- **FR-012**: System MUST log every vend event with timestamp, coil ID, outcome (success/jam/failed), and transaction_id from payment system
- **FR-013**: System MUST record motor jam events when reported by Android app via POST /api/v1/transactions with status='jam'
- **FR-014**: System MUST mark coil status='jammed' when jam event is recorded, preventing further inventory decrements until admin manually resolves jam

**Hardware Communication**

- **FR-015**: System MUST provide API endpoint to query jam event history for troubleshooting
- **FR-016**: System MUST provide API endpoint for admin to manually resolve jam events (mark coil as available again)
- **FR-017**: System MUST expose jam event counts and recent jam history via admin interface
- **FR-018**: System MUST log all jam events with coil ID, timestamp, and resolution status

**Operational Automation**

- **FR-021**: System MUST trigger low-stock alert when any coil inventory count reaches value less than or equal to 2
- **FR-021a**: System MUST send real-time push notifications to external monitoring system for all low-stock alerts
- **FR-021b**: System MUST include coil ID, current inventory count, and timestamp in alert notifications
- **FR-021c**: System MUST retry failed alert deliveries with exponential backoff (maximum 3 attempts)
- **FR-022**: System MUST support configuration of multi-coil product linking (multiple coils mapped to single product identifier)
- **FR-023**: System MUST automatically select first non-empty linked coil when processing purchase for linked product
- **FR-024**: System MUST maintain link configuration persistence across system restarts
- **FR-025**: System MUST provide admin interface for creating, modifying, and deleting product links

**Data Persistence & Recovery**

- **FR-026**: System MUST persist all inventory state, transaction logs, jam events, and configuration to durable storage
- **FR-027**: System MUST detect incomplete transactions on startup (power loss recovery)
- **FR-028**: System MUST mark incomplete transactions as failed with reason "power_failure" in transaction log
- **FR-029**: System MUST restore last-known-good inventory state after unclean shutdown
- **FR-030**: System MUST complete startup and become ready to serve requests within 5 seconds

**API & Integration**

- **FR-031**: System MUST expose localhost API on address 127.0.0.1 port 8080 for Android app integration
- **FR-032**: System MUST provide API endpoint to query current inventory state for all 100 coils
- **FR-033**: System MUST provide API endpoint to record vend outcome with coil_id, status, timestamp, and transaction_id
- **FR-033a**: System MUST provide API endpoint to query low-stock coils (inventory ≤ 2) for web dashboard monitoring
- **FR-034**: System MUST provide API endpoint to query jam event history and resolution status
- **FR-035**: System MUST provide API endpoint for admin operations (refill, manual count override, jam resolution)
- **FR-036**: System MUST return API responses in structured data format with appropriate status codes
- **FR-037**: System MUST include correlation ID in all API responses for request tracing
- **FR-037a**: System MUST support configuration of external monitoring system endpoint URL for push notification delivery
- **FR-037b**: System MUST authenticate with external monitoring system using configurable credentials
- **FR-037c**: System MUST NOT require authentication for admin interface access (localhost access only)
- **FR-037d**: System MUST bind admin interface endpoints to localhost (127.0.0.1) only to prevent remote access

**Resource Constraints**

- **FR-038**: System MUST limit memory consumption to maximum 30MB during operation
- **FR-039**: System MUST limit idle CPU usage to maximum 5% on target platform
- **FR-040**: System MUST complete all API requests within 50ms (95th percentile)
- **FR-041**: System MUST minimize storage operations to prevent interference with concurrent video playback

### Key Entities

- **Coil**: Represents a single physical coil position in the 10x10 grid. Attributes include unique ID (e.g., A5), current inventory count (0-10), operational status (available/jammed), link group ID (for multi-coil products), and version number (for optimistic locking conflict detection).

- **Transaction**: Represents a single purchase event. Attributes include unique transaction ID, timestamp, coil ID, outcome status (success/failed/incomplete), failure reason if applicable, and correlation ID for tracing.

- **Jam Event**: Represents a hardware failure event. Attributes include event ID, timestamp, coil ID, event type (motor_jam/motor_timeout), and resolution status (unresolved/cleared).

- **Hardware Device**: Represents a USB-connected peripheral. Attributes include device type (ID_Scanner/Payment_Reader), vendor ID, product ID, connection status (connected/disconnected), and last-seen timestamp.

- **Product Link**: Represents a mapping between multiple coils and a single product identifier. Attributes include link group ID, product identifier, ordered list of linked coil IDs, and selection strategy (first-available).

- **Low-Stock Alert**: Represents an inventory alert condition. Attributes include alert ID, timestamp, coil ID, inventory count at alert time, acknowledgment status, delivery status (pending/sent/failed), and retry count.

## Success Criteria *(mandatory)*

### Measurable Outcomes

**Performance & Responsiveness**

- **SC-001**: Customer purchases complete from payment signal to motor activation within 100ms (95th percentile)
- **SC-002**: Admin interface displays real-time inventory grid with all 100 coil counts refreshing within 500ms of any change
- **SC-003**: System startup completes and becomes ready to serve requests within 5 seconds after power restoration
- **SC-004**: Inventory queries return results within 50ms for all 100 coils (99th percentile)

**Reliability & Data Integrity**

- **SC-005**: Zero inventory discrepancies detected when comparing physical counts to system counts across 1000 vend operations
- **SC-006**: 100% of transactions are either fully completed (inventory decremented and motor activated) or fully rolled back (no inventory change)
- **SC-007**: System recovers from power loss with zero data loss (all pre-outage inventory counts restored exactly)
- **SC-008**: System processes 500 consecutive vend operations without memory growth exceeding 5%

**Resource Efficiency**

- **SC-009**: System maintains memory consumption below 30MB during peak operation (measured via process monitoring)
- **SC-010**: System maintains idle CPU usage below 5% when no active requests are being processed
- **SC-011**: Video playback subsystem maintains 60fps frame rate with zero dropped frames during concurrent vend operations

**Operational Effectiveness**

- **SC-012**: Route drivers complete full machine refill (all 100 coils) in under 30 seconds using "Refill All" function
- **SC-013**: Operators identify jammed coils within 10 seconds of jam occurrence via admin interface alerts
- **SC-014**: Multi-coil linked products achieve 99% availability when at least one linked coil has inventory greater than 0
- **SC-015**: Low-stock alerts trigger within 1 second of inventory reaching threshold (count less than or equal to 2)

**Integration & API Quality**

- **SC-016**: Android app successfully queries inventory state and receives response for all 100 coils in single API call
- **SC-017**: 100% of API requests include correlation IDs enabling full request tracing from Android app to backend
- **SC-018**: Hardware connection status updates reflect actual USB device state within 2 seconds of connect/disconnect event

## Assumptions

**Hardware Environment**

- System operates on ARM64 Android tablet with 512MB total RAM (Android app consumes approximately 150MB)
- USB hardware devices (ID Scanner, Payment Reader, Motor Controller) provide standard serial communication interfaces
- Motor controller provides acknowledgment signal or timeout within 5 seconds of activation command
- Physical coil motors complete dispensing action within 10 seconds of activation

**Integration Boundaries**

- Existing Android app (MyApplication/) handles customer-facing UI, product display, and payment reader integration
- Android app communicates with backend exclusively via localhost API (no direct database access)
- Payment validation and authorization occurs within Android app before backend vend API is called
- Backend does not handle customer authentication, pricing, or payment processing

**Security Model**

- Physical access to the tablet is the security boundary (no software authentication for admin interface)
- Admin interface accessible only via localhost prevents remote unauthorized access
- Vending machine physical enclosure provides access control for admin operations
- Route drivers and operators have equivalent access privileges to all admin functions

**Operational Context**

- Vending machines operate in unattended environments with network connectivity required for alert delivery
- Network outages may occur; alert delivery will retry with exponential backoff
- Power interruptions may occur without warning (no UPS or battery backup assumed)
- Route driver refill operations occur during low-traffic periods (early morning)
- Hardware jam rate is approximately 1-2% of vend operations based on typical vending machine statistics

**Data & Configuration**

- Maximum coil capacity is standardized at 10 units per coil (physical constraint)
- Product linking configuration changes are infrequent (weekly at most)
- Transaction log retention period is 90 days (configurable, industry standard for vending analytics)
- Jam events require manual operator intervention to clear (no automatic retry mechanism)
