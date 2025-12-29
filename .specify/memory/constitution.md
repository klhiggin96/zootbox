<!--
Sync Impact Report:
Version change: N/A → 1.0.0 (Initial constitution)
Modified principles: N/A (new constitution)
Added sections: All sections (initial creation)
Removed sections: N/A
Templates requiring updates:
  ✅ .specify/templates/plan-template.md (validated - Constitution Check section compatible)
  ✅ .specify/templates/spec-template.md (validated - no conflicts)
  ✅ .specify/templates/tasks-template.md (validated - compatible with principles)
Follow-up TODOs: None
-->

# Project Constitution: ZootBox Backend Inventory Service

**Version**: 1.0.0
**Ratification Date**: 2025-12-27
**Last Amended**: 2025-12-27

---

## Core Purpose

A lightweight Go microservice that manages real-time inventory across 100 vending machine coil slots. Integrates with the existing ZootBox Android app (in ../MyApplication/) via localhost REST API. Processes hardware vend operations via USB serial communication with Nayax payment reader and motor controller.

## Primary Users

1. **ZootBox Android App (../MyApplication/)** - Primary API consumer for inventory queries and vend operations
2. **Vending Machine Operators** - Use web admin portal for inventory management
3. **Route Drivers** - Use mobile portal to log refills
4. **Hardware Devices** - Nayax payment reader (already integrated in Android app's HardwareService) and motor controller

## Key Constraints

### Integration Constraints

- MUST integrate with existing Android app without requiring app rewrites
- MUST use same USB devices already managed by Android HardwareService:
  - E-Seek M260 ID Scanner (VID: 0x0403, PID: 0x6001, FTDI)
  - Nayax Payment Reader (VID: 0x26f1, PID: 0x5650, CDC-ACM)
- API MUST match data models used in Android app (see ../MyApplication/app/src/main/java/com/zootbox/models/)
- MUST run on same tablet as Android app (shared ARM64 Linux environment)

### Resource Constraints

- **Max RAM**: 30MB (Android app uses ~150MB for videos)
- **Binary size**: <15MB
- **CPU**: <5% idle (Android app needs CPU for video decoding)
- **Storage**: <50MB total

### Deployment Constraints

- Single binary deployment (no external dependencies)
- Localhost-only API (127.0.0.1:8080)
- MUST coexist with Android app's HardwareService
- No conflicts with Android app's USB device access

## Non-Negotiable Requirements

### 1. API Compatibility

All endpoints MUST match data contracts expected by Android app. Breaking changes require Android app updates (coordinate releases).

**Rationale**: The backend serves the Android app as its primary client. API contract breakage would cause app crashes or incorrect behavior, resulting in poor customer experience and lost sales.

### 2. Hardware Coordination

Backend and Android HardwareService MUST coordinate USB device access. Cannot have both trying to control same hardware simultaneously.

**Rationale**: USB devices can only have one active connection. Simultaneous access attempts will cause device lockup, requiring manual tablet restart and service interruption.

### 3. Atomic Transactions

Payment (via Android) → Backend API call → Inventory update → Motor trigger MUST complete or fully rollback.

**Rationale**: Partial transactions lead to inventory discrepancies, lost revenue (payment taken but no product dispensed), or product loss (product dispensed but inventory not decremented).

### 4. No Android App Dependencies

Backend MUST function independently. If Android app crashes, backend continues serving API and logging transactions.

**Rationale**: Backend provides critical inventory management and transaction logging. Administrative functions, refill operations, and remote monitoring depend on backend availability independent of Android app state.

### 5. Backward Compatibility

Android app MUST degrade gracefully if backend is offline (use cached inventory, prevent new purchases).

**Rationale**: Network or process failures should not cause complete vending machine failure. Customers should still see product information even if purchasing is temporarily disabled.

### 6. Database Portability

SQLite database MUST be accessible by both backend (read/write) and potentially Android app (read-only emergency mode).

**Rationale**: In catastrophic backend failure scenarios, Android app needs emergency read-only access to inventory data to display to customers and enable manual recovery procedures.

## Success Criteria

### Integration Success

- Android app's ProductDetailActivity successfully queries /api/v1/coils endpoint
- Android app's vend operation triggers backend /api/v1/vend and receives response <100ms
- Stock counts in Android UI update within 500ms of backend inventory change
- Zero API contract mismatches (400 Bad Request errors due to schema differences)

### Performance

- **API response**: <50ms (Android UI can't wait longer without janky UX)
- **Database queries**: <10ms
- **Memory**: <30MB (measured via `ps` on Android tablet)
- **Binary**: <15MB compiled

### Reliability

- Survives Android app crashes (backend keeps running)
- Survives backend crashes (Android app shows "Inventory temporarily unavailable")
- Zero inventory discrepancies between backend SQLite and Android's displayed counts

## Architectural Principles

### Principle 1: Android-Friendly Resources

**Directive**: Backend runs as passive daemon with event-driven architecture. No polling loops eating CPU. Minimal memory allocations to avoid GC pressure. No background goroutines unless absolutely necessary.

**Rationale**: The Android app is CPU-intensive (video decoding) and memory-constrained (video buffers). Backend must be a good neighbor on shared hardware, responding only when called and minimizing resource consumption.

**Enforcement**: Profile with `ps` and `top` during integration testing. Reject any implementation with >5% idle CPU or >30MB RSS memory.

### Principle 2: API-First Design

**Directive**: REST API is the contract with Android app. Database schema changes require API version bumps. OpenAPI/Swagger spec documents all endpoints (Android devs reference this). Backward compatibility maintained for 2 major versions.

**Rationale**: The Android app is deployed to physical vending machines with unpredictable update schedules. API versioning enables independent backend updates without coordinating simultaneous Android app deployments.

**Enforcement**: All PRs modifying API endpoints must include OpenAPI spec updates. CI must validate backward compatibility with previous version's test suite.

### Principle 3: Android as Hardware Source of Truth

**Directive**: Android app owns ALL hardware devices (ID Scanner, Payment Reader, Motor Controller via DMVI WallCoilMachineService). Backend is passive record-keeper that receives vend event notifications from Android via POST /api/v1/transactions. Backend NEVER sends commands to hardware. Android reports outcomes (success/jam/failed), backend records them.

**Rationale**: Android app already has working motor control via DMVI WallCoilMachineService (JSON-RPC to [::1]:57482). Duplicating motor control in backend creates race conditions, architecture complexity, and USB device conflicts. Backend's value is inventory tracking and web dashboard monitoring, not hardware orchestration.

**Enforcement**: Backend MUST NOT import USB serial libraries (tarm/serial, go-serial, jacobsa/go-serial). Backend MUST NOT open /dev/tty* devices. All hardware outcomes arrive via REST API from Android. Code review must reject any PR that adds direct hardware access to backend.

### Principle 4: Fail-Safe Defaults

**Directive**: If backend offline, Android uses last-known inventory (read-only mode). If database locked, queue write operations and retry with exponential backoff. If motor fails, log jam, prevent future vends to that coil, and notify via API response.

**Rationale**: Field deployment means limited physical access for repairs. System must degrade gracefully and continue operating in reduced capacity rather than failing completely.

**Enforcement**: Chaos testing must verify graceful degradation. Simulate backend crash, database lock, and motor jam scenarios. Android app must remain responsive in all cases.

### Principle 5: Localhost-Only Security

**Directive**: No authentication needed (only Android app on localhost can access). When admin portal accessed remotely, Basic HTTP auth or VPN required. No HTTPS overhead on localhost (adds latency for no security benefit).

**Rationale**: The backend and Android app run on the same physical tablet. Localhost-only API avoids authentication overhead and latency. Remote admin access is infrequent and can use basic auth over VPN.

**Enforcement**: Server MUST bind to 127.0.0.1 only, not 0.0.0.0. Configuration validation at startup must reject non-localhost bindings.

### Principle 6: Observable Integration

**Directive**: All API calls logged with correlation IDs (trace Android → Backend flow). Health check endpoint exposes database status, hardware status, and uptime. Metrics endpoint for monitoring: API call counts, response times, and error rates.

**Rationale**: Field debugging requires comprehensive logging since physical access is limited. Correlation IDs enable tracing requests across Android app and backend. Health/metrics endpoints enable remote monitoring and proactive issue detection.

**Enforcement**: All API handlers must extract/generate correlation ID from request headers. Health endpoint must return 503 if any critical component (database, motor controller) is unhealthy.

## Known Risks

### Integration Risks

**Risk 1: USB Device Conflicts**
Android HardwareService and backend both try to access payment reader.

**Mitigation**: Backend only controls motor, Android owns payment/scanner hardware. Document ownership in constitution and enforce via integration tests.

**Risk 2: API Schema Drift**
Android app expects different JSON structure than backend returns.

**Mitigation**: Shared data models (copy Product.kt structure to Go structs). API contract tests in CI validate JSON schema compatibility.

**Risk 3: Deployment Synchronization**
Backend updated but Android app not updated.

**Mitigation**: API versioning (/api/v1/ → /api/v2/). Maintain backward compatibility for 2 major versions. Deployment playbook specifies version compatibility matrix.

### Technical Risks

**Risk 1: SQLite Write Contention**
Concurrent vend requests lock database.

**Mitigation**: WAL mode + immediate transactions + retry logic. Load testing must validate concurrent vend handling.

**Risk 2: Memory Pressure**
Backend + Android app exceed 512MB tablet RAM.

**Mitigation**: Profile with `ps` and `top` during integration testing. Optimize or reduce backend features if needed. Hard limit backend to 30MB.

**Risk 3: Process Killing**
Android OS kills backend to reclaim memory.

**Mitigation**: Backend runs as foreground service via Android wrapper app. Implement proper lifecycle management to survive low-memory conditions.

## Governance

### Amendment Procedure

1. **Proposal**: Any team member may propose a constitutional amendment via pull request to `.specify/memory/constitution.md`.
2. **Review**: Amendment requires review by at least one senior engineer familiar with Android integration.
3. **Version Bump**: Follow semantic versioning:
   - **MAJOR**: Backward incompatible changes (e.g., removing a non-negotiable requirement)
   - **MINOR**: New principles or materially expanded guidance
   - **PATCH**: Clarifications, wording improvements, typo fixes
4. **Template Sync**: Amendment author MUST update dependent templates (plan, spec, tasks) and document changes in Sync Impact Report.
5. **Commit Message**: Use format `docs: amend constitution to vX.Y.Z (summary of changes)`

### Versioning Policy

- Constitution uses semantic versioning (MAJOR.MINOR.PATCH)
- Version MUST increment with every amendment
- LAST_AMENDED_DATE MUST be updated to amendment merge date
- RATIFICATION_DATE remains constant (original adoption date)
- Sync Impact Report MUST be prepended as HTML comment at top of file

### Compliance Review

- Every feature planning cycle MUST include Constitution Check (see plan template)
- If feature violates principles, MUST justify in Complexity Tracking table
- Unjustified violations block feature approval
- Repeated violations trigger principle review (may indicate outdated principle)

### Template Dependency Management

The following templates depend on this constitution and MUST be reviewed/updated when principles change:

- `.specify/templates/plan-template.md` - Constitution Check section
- `.specify/templates/spec-template.md` - Requirements alignment
- `.specify/templates/tasks-template.md` - Task categorization based on principles

Amendment authors MUST validate these templates and document results in Sync Impact Report.
