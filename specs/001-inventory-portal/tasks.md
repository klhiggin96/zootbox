# Implementation Tasks: ZootBox Inventory Web Portal

**Feature**: 001-inventory-portal
**Branch**: `001-inventory-portal`
**Created**: 2025-12-28
**Status**: Ready for Implementation

---

## Overview

This document breaks down the implementation into phases aligned with user stories from spec.md. Each phase represents a complete, independently testable increment that delivers value.

**Total Tasks**: 47
**User Stories**: 5 (P1-P4 priorities)
**Estimated Timeline**: 5-7 days (3-4 days for MVP)

---

## Implementation Strategy

### MVP Scope (2-3 days)
- **Phase 1**: Setup (project structure)
- **Phase 2**: Foundational (shared infrastructure)
- **Phase 3**: User Story 1 (Inventory Grid - core feature)

This delivers the primary value proposition: remote inventory visibility.

### Incremental Delivery
- **Phase 4**: User Story 2 + 3 (Bulk Refill + Manual Edit) - Day 4
- **Phase 5**: User Story 4 (Jam Resolution) - Day 5
- **Phase 6**: User Story 5 (Product Links) - Day 6-7
- **Phase 7**: Polish - Day 7

---

## Dependencies

### User Story Completion Order

```
Phase 1 (Setup)
  ↓
Phase 2 (Foundational: Navigation, API Client, LocalStorage)
  ↓
Phase 3 (US1: Inventory Grid) ← MVP Complete
  ↓
Phase 4 (US2: Bulk Refill) [parallel with US3]
  ↓
Phase 4 (US3: Manual Edit) [parallel with US2]
  ↓
Phase 5 (US4: Jam Management) [independent]
  ↓
Phase 6 (US5: Product Links) [independent]
  ↓
Phase 7 (Polish)
```

**Key Insights**:
- US2 and US3 can be developed in parallel (both use Inventory Grid from US1)
- US4 and US5 are independent (can be done in any order after US1)
- US1 (Inventory Grid) is the critical path - all other stories depend on it

---

## Parallel Execution Opportunities

### Phase 1-2: Setup & Foundational (1 developer)
- Sequential tasks (project structure must exist before components)

### Phase 3: US1 - Inventory Grid (2-3 developers)
- **Dev A**: CoilGrid component + CSS Grid layout
- **Dev B**: API client (coils.js) + state management (inventory.js)
- **Dev C**: Auto-refresh logic (sync.js) + status indicators

### Phase 4: US2 + US3 (2 developers in parallel)
- **Dev A**: Bulk refill (US2) - admin.js API + RefillButton component
- **Dev B**: Manual edit (US3) - CoilEditModal component + validation

### Phase 5-6: US4 + US5 (2 developers in parallel)
- **Dev A**: Jam Management (US4) - jam-management.html + jams.js API
- **Dev B**: Product Links (US5) - product-links.html + products.js API

---

## Phase 1: Setup

**Goal**: Initialize project structure and dependencies

**Tasks**: 5

- [ ] T001 Create portal/ directory at repository root
- [ ] T002 Create directory structure per plan.md (css/, js/api/, js/components/, js/state/, js/utils/, assets/icons/)
- [ ] T003 Create README.md in portal/ with project overview and quick start instructions
- [ ] T004 Create .gitignore in portal/ (ignore .DS_Store, Thumbs.db, node_modules if added later)
- [ ] T005 [P] Create package.json for development dependencies (live-server, eslint) - optional for simple static serving

**Validation**:
- ✅ Directory structure matches plan.md
- ✅ README.md contains setup instructions
- ✅ Can serve portal via `python -m http.server 3000` or `npx live-server`

---

## Phase 2: Foundational

**Goal**: Build shared infrastructure required by all user stories

**Tasks**: 10

### Navigation & Layout

- [ ] T006 Create navigation template (nav.html fragment with logo, machine selector, page links) reusable across all pages
- [ ] T007 Create css/navigation.css with top nav bar styles (fixed header, tabs, machine dropdown)
- [ ] T008 Create css/main.css with global styles (CSS reset, typography, color palette from design, responsive grid)

### API Client Infrastructure

- [ ] T009 [P] Create js/api/client.js with fetch wrapper (handles CORS, error responses, timeout, AbortController for cancellation)
- [ ] T010 [P] Create js/api/coils.js with endpoints (GET /api/v1/coils, GET /coils/{id}, GET /coils/low-stock)
- [ ] T011 [P] Create js/api/admin.js with endpoints (POST /admin/refill, PUT /admin/coils/{id})
- [ ] T012 [P] Create js/api/jams.js with endpoints (GET /jam-events, POST /jam-events/{id}/resolve)
- [ ] T013 [P] Create js/api/products.js with endpoints (GET/POST/DELETE /admin/product-links, GET /product-links/{sku}/resolve)

### State Management

- [ ] T014 [P] Create js/state/machines.js with LocalStorage wrapper (CRUD for machines, validate schema, migration logic)
- [ ] T015 [P] Create js/state/inventory.js with session cache (Map-based cache, invalidation logic, diff detection for grid updates)

### Utilities

- [ ] T016 [P] Create js/utils/validation.js with input validators (coil ID pattern A1-J10, inventory 0-10, URL format, SKU non-empty)
- [ ] T017 [P] Create js/utils/formatting.js with date/time formatters (ISO 8601 → human-readable, time ago, staleness indicators)

**Validation**:
- ✅ API client can call backend health endpoint: `GET /health`
- ✅ LocalStorage can save/load machine configs
- ✅ Validation functions reject invalid inputs (inventory = 15 → error)
- ✅ Navigation template renders correctly in browser

---

## Phase 3: User Story 1 - Monitor Real-Time Inventory (Priority: P1)

**Goal**: Display 100 coils in 10x10 grid with real-time updates

**Independent Test**: Load portal, verify all 100 coils displayed with inventory counts from backend API

**Tasks**: 11

### Page Setup

- [ ] T018 [US1] Create index.html (Inventory Grid page) with navigation, grid container, status bar (last sync time, connection status)

### Grid Rendering

- [ ] T019 [US1] Create css/grid.css with 10x10 CSS Grid layout (rows A-J labels, columns 1-10 labels, cell sizing, responsive breakpoints)
- [ ] T020 [US1] Create css/grid.css cell state styles (green=available, orange=low-stock, gray=empty, red=jammed, loading state)
- [ ] T021 [US1] Create js/components/CoilGrid.js with grid renderer (generate 100 cells, bind data to cells, handle cell clicks)
- [ ] T022 [US1] Implement CoilGrid.render() to fetch coils from API and populate grid (call GET /api/v1/coils, map to cells A1-J10)

### Machine Selector

- [ ] T023 [US1] Create js/components/MachineSelector.js with dropdown (load machines from LocalStorage, render options, handle selection change)
- [ ] T024 [US1] Implement machine switching logic (cancel in-flight requests, clear cache, load new machine inventory, update URL hash)

### Status Indicators

- [ ] T025 [P] [US1] Create js/components/StatusIndicator.js with online/offline badge (green=online, red=offline, yellow=stale data)
- [ ] T026 [P] [US1] Create assets/icons/ with SVG icons for coil states (low-stock warning, jammed error, empty circle, available checkmark)

### Auto-Refresh

- [ ] T027 [US1] Create js/state/sync.js with auto-refresh manager (setInterval 5s, call GET /api/v1/coils, diff old vs new, update changed cells only)
- [ ] T028 [US1] Implement offline detection in sync.js (3 failed requests → stop polling, show stale data banner, retry every 30s)

**Acceptance Validation**:
- ✅ **Scenario 1**: Open portal → 100 coils displayed in 10x10 grid with inventory counts
- ✅ **Scenario 2**: Coil with inventory=2 → orange highlight (low stock)
- ✅ **Scenario 3**: Coil with status=jammed → red cell with jammed icon
- ✅ **Scenario 4**: Backend updates coil A5 inventory → grid updates within 5 seconds
- ✅ **Scenario 5**: Coil with inventory=0 → gray cell clearly distinguished

**Parallel Execution**:
- T019-T020 (CSS) + T021-T022 (Grid JS) can be done by separate developers
- T025-T026 (Status indicators) independent of grid rendering
- T027-T028 (Auto-refresh) depends on T022 (grid render) but can be added incrementally

---

## Phase 4A: User Story 2 - Bulk Refill After Route Visit (Priority: P2)

**Goal**: Reset all 100 coils to inventory=10 with one button click

**Independent Test**: Click "Refill All" button, verify all coils set to 10 via backend API

**Tasks**: 6

### UI Components

- [ ] T029 [US2] Add "Refill All Coils" button to index.html (top-right corner of grid, primary action styling)
- [ ] T030 [US2] Create js/components/RefillButton.js with click handler (show confirmation dialog, call POST /admin/refill, handle response)

### Confirmation Dialog

- [ ] T031 [US2] Create js/components/Modal.js reusable modal dialog (backdrop, close button, confirm/cancel actions, keyboard ESC support)
- [ ] T032 [US2] Create css/forms.css with modal styles (centered overlay, button styles, loading spinner)

### Refill Logic

- [ ] T033 [US2] Implement refill operation in RefillButton (disable button during operation, show loading spinner, call API, handle success/error)
- [ ] T034 [US2] Add success/error toast notifications (display API response, auto-dismiss after 5s, green=success red=error)

**Acceptance Validation**:
- ✅ **Scenario 1**: Click "Refill All" → confirmation dialog "Set all 100 coils to inventory=10?"
- ✅ **Scenario 2**: Confirm refill → loading indicator, button disabled, no duplicate requests
- ✅ **Scenario 3**: Refill succeeds → all cells show "10", success toast "100 coils refilled"
- ✅ **Scenario 4**: Refill fails (network error) → error toast, inventory unchanged
- ✅ **Scenario 5**: Jammed coils → inventory set to 10, status remains "jammed"

**Parallel Execution**:
- T031-T032 (Modal component) independent of refill logic
- T034 (Toast notifications) can be built separately and integrated

---

## Phase 4B: User Story 3 - Manual Inventory Adjustment (Priority: P2)

**Goal**: Edit individual coil inventory (0-10)

**Independent Test**: Click coil, enter new value, verify update persists in backend

**Tasks**: 6

### Edit Modal

- [ ] T035 [US3] Create js/components/CoilEditModal.js with edit form (coil ID label, inventory input 0-10, current value pre-filled, save/cancel buttons)
- [ ] T036 [US3] Add click handler to CoilGrid cells (open CoilEditModal with selected coil data)

### Validation

- [ ] T037 [US3] Implement client-side validation in CoilEditModal (inventory 0-10 integer, show inline error for invalid input, disable save button)
- [ ] T038 [US3] Implement frontend validation in js/utils/validation.js (reuse for coil ID, inventory range checks)

### Save Logic

- [ ] T039 [US3] Implement save operation in CoilEditModal (call PUT /admin/coils/{id}, pass current version for optimistic locking, handle 409 Conflict)
- [ ] T040 [US3] Handle optimistic locking conflicts (display "Coil updated by another user. Please refresh.", reload coil data, allow retry)

**Acceptance Validation**:
- ✅ **Scenario 1**: Click coil A5 (inventory=7) → modal opens with value "7"
- ✅ **Scenario 2**: Enter 3, click Save → coil updates to 3, grid refreshes, toast "Coil A5 updated"
- ✅ **Scenario 3**: Enter -1 → validation error "Inventory must be between 0 and 10"
- ✅ **Scenario 4**: Network error → error toast, original value retained
- ✅ **Scenario 5**: Jammed coil → inventory updates, status remains "jammed"

**Parallel Execution**:
- T037-T038 (Validation) independent of modal UI
- T039-T040 (Save logic) depends on T035-T036 but can overlap

---

## Phase 5: User Story 4 - Resolve Jam Events (Priority: P3)

**Goal**: View and resolve open jam events

**Independent Test**: View jam list, click Resolve, verify jam status updates to "resolved"

**Tasks**: 7

### Page Setup

- [ ] T041 [US4] Create jam-management.html with navigation, jam events table (columns: Coil ID, Timestamp, Status, Actions)
- [ ] T042 [US4] Add "Jam Management" link to navigation bar (all pages)

### Jam List

- [ ] T043 [US4] Create js/components/JamTable.js with table renderer (fetch GET /jam-events, render rows, format timestamps)
- [ ] T044 [US4] Implement status filter dropdown (options: All, Open, Resolved → call GET /jam-events?status={filter})

### Resolve Action

- [ ] T045 [US4] Add "Resolve" button to each open jam row (disabled for resolved jams)
- [ ] T046 [US4] Implement resolve handler (call POST /jam-events/{id}/resolve, refresh jam list, update inventory grid if visible)
- [ ] T047 [US4] Handle resolve errors (network error → show error toast, jam remains open)

**Acceptance Validation**:
- ✅ **Scenario 1**: Navigate to Jam Management → table shows all jam events (coil ID, timestamp, status)
- ✅ **Scenario 2**: Filter "Open Jams Only" → only open jams displayed
- ✅ **Scenario 3**: Click Resolve for open jam (coil B3) → status updates to "resolved", timestamp recorded
- ✅ **Scenario 4**: After resolution → inventory grid shows B3 as "available" (if inventory > 0)
- ✅ **Scenario 5**: Resolve fails → error toast, jam remains open

**Parallel Execution**:
- T043-T044 (Table + Filter) independent of resolve logic
- T045-T047 (Resolve action) can be built after table is functional

---

## Phase 6: User Story 5 - Configure Multi-Coil Products (Priority: P4)

**Goal**: Link multiple coils to a product SKU

**Independent Test**: Create product link (SKU → coils), verify in list, delete link

**Tasks**: 8

### Page Setup

- [ ] T048 [US5] Create product-links.html with navigation, product links table (columns: SKU, Linked Coils, Strategy, Created, Actions)
- [ ] T049 [US5] Add "Product Links" link to navigation bar (all pages)

### Product Links List

- [ ] T050 [US5] Create js/components/ProductLinkTable.js with table renderer (fetch GET /admin/product-links, parse linked_coil_ids JSON string, render rows)

### Create Link Form

- [ ] T051 [US5] Create js/components/ProductLinkForm.js with create form (SKU input, multi-select coil dropdown, strategy dropdown defaulting to "first_available")
- [ ] T052 [US5] Implement coil multi-select dropdown (load all coils A1-J10, allow multiple selection, show selected count "3 coils selected")

### Create Logic

- [ ] T053 [US5] Implement create handler (validate SKU non-empty, at least 1 coil selected, call POST /admin/product-links, handle 400 errors)
- [ ] T054 [US5] Handle validation errors (display "Coil A1 is already linked to product COKE-001", prevent submission)

### Delete Action

- [ ] T055 [US5] Add "Delete" button to each product link row
- [ ] T056 [US5] Implement delete handler (confirmation dialog "Delete product link for SKU?", call DELETE /admin/product-links/{linkGroupId}, refresh table)

**Acceptance Validation**:
- ✅ **Scenario 1**: Click "Create Product Link" → form appears (SKU input, coil multi-select)
- ✅ **Scenario 2**: Enter "COKE-001", select [A1, A2, A3], submit → link created, appears in table
- ✅ **Scenario 3**: View table → displays SKU, coils (A1, A2, A3), strategy, creation date
- ✅ **Scenario 4**: Click Delete → confirmation, link removed from table
- ✅ **Scenario 5**: Select coil already linked → validation error "Coil A1 is already linked to product COKE-001"
- ✅ **Scenario 6**: Submit with no coils selected → error "At least one coil must be selected"

**Parallel Execution**:
- T051-T052 (Create form) + T050 (Table) can be done by separate developers
- T055-T056 (Delete action) independent of create logic

---

## Phase 7: Machine Settings (Supporting Infrastructure)

**Goal**: Configure machines for multi-machine management

**Tasks**: 6

### Page Setup

- [ ] T057 Create machine-settings.html with navigation, machine list table (columns: Name, Endpoint URL, Status, Last Sync, Actions)
- [ ] T058 Add "Machine Settings" link to navigation bar (all pages)

### Machine List

- [ ] T059 Create js/components/MachineTable.js with table renderer (load machines from LocalStorage, render rows, show online/offline status)

### Add Machine Form

- [ ] T060 Create js/components/AddMachineForm.js with form (name input, endpoint URL input, validation)
- [ ] T061 Implement add handler (validate name non-empty, URL format http(s)://, save to LocalStorage, test connection GET /health, update status)

### Edit/Delete Actions

- [ ] T062 Implement edit/delete handlers (edit → open modal with current values, delete → confirmation dialog, remove from LocalStorage)

**Validation**:
- ✅ Can add machine with name "Test Machine", URL "http://localhost:8080"
- ✅ Machine status updates to "online" after successful health check
- ✅ Can edit machine name/URL
- ✅ Can delete machine (confirms first, removes from LocalStorage)

---

## Phase 8: Polish & Cross-Cutting Concerns

**Goal**: Finalize user experience and production readiness

**Tasks**: 10

### Error Handling

- [ ] T063 Implement global error handler for uncaught exceptions (display error toast, log to console)
- [ ] T064 Add error boundaries for API failures (graceful degradation, show cached data with staleness indicator)

### Loading States

- [ ] T065 Add loading spinners to all async operations (grid load, refill, edit, jam resolve, product link create/delete)
- [ ] T066 Implement skeleton screens for initial page load (gray placeholder grid while fetching data)

### Accessibility

- [ ] T067 Add ARIA labels to all interactive elements (buttons, inputs, dropdowns, modals)
- [ ] T068 Ensure keyboard navigation works (Tab order, Enter to submit forms, ESC to close modals)
- [ ] T069 Test with screen reader (NVDA/JAWS) - grid should announce coil ID and inventory

### Performance

- [ ] T070 Optimize grid rendering (use DocumentFragment for batch DOM updates, debounce auto-refresh updates by 50ms)
- [ ] T071 Add service worker for offline support (cache HTML/CSS/JS files, show offline page if no network)

### Documentation

- [ ] T072 Update README.md with deployment instructions (copy to operator's PC, ADB port forwarding setup)

**Validation**:
- ✅ All error scenarios show user-friendly messages (no raw JSON errors)
- ✅ Loading indicators appear for operations >1 second
- ✅ Keyboard navigation works (can navigate entire portal without mouse)
- ✅ Screen reader announces coil information correctly
- ✅ Grid renders 100 coils in <1 second
- ✅ Portal works offline with cached data (shows staleness indicator)

---

## Task Summary by Phase

| Phase | Description | Tasks | Est. Time |
|-------|-------------|-------|-----------|
| 1 | Setup | 5 | 0.5 day |
| 2 | Foundational | 10 | 1 day |
| 3 | US1 - Inventory Grid (P1) | 11 | 1.5 days |
| 4A | US2 - Bulk Refill (P2) | 6 | 0.5 day |
| 4B | US3 - Manual Edit (P2) | 6 | 0.5 day |
| 5 | US4 - Jam Management (P3) | 7 | 1 day |
| 6 | US5 - Product Links (P4) | 8 | 1 day |
| 7 | Machine Settings | 6 | 0.5 day |
| 8 | Polish | 10 | 1 day |
| **Total** | | **69** | **7-8 days** |

**MVP Scope (Phases 1-3)**: 26 tasks, 3 days

---

## Testing Strategy

**Note**: No automated tests requested in spec.md. Validation relies on manual acceptance testing per user story.

### Manual Testing Checklist (Per User Story)

**US1 - Inventory Grid**:
1. Open portal → verify 100 coils displayed
2. Check low-stock highlighting (coil with inventory=2 → orange)
3. Check jammed indicator (coil with status=jammed → red)
4. Wait 5 seconds → verify grid auto-refreshes
5. Backend updates coil A5 → verify grid updates within 5 seconds

**US2 - Bulk Refill**:
1. Click "Refill All" → verify confirmation dialog
2. Confirm → verify loading indicator, button disabled
3. Wait for completion → verify all cells show "10"
4. Disconnect backend → refill fails with error message

**US3 - Manual Edit**:
1. Click coil A5 → verify edit modal opens
2. Enter 3, save → verify coil updates to 3
3. Enter -1 → verify validation error
4. Disconnect backend → edit fails with error message

**US4 - Jam Resolution**:
1. Navigate to Jam Management → verify jam table loads
2. Filter "Open Jams Only" → verify filtering works
3. Click Resolve → verify jam status updates
4. Return to Inventory Grid → verify coil shows "available"

**US5 - Product Links**:
1. Create link (SKU="COKE-001", coils=[A1, A2, A3]) → verify appears in table
2. Try selecting already-linked coil → verify validation error
3. Delete link → verify removed from table

### Integration Testing (With Real Backend)

**Prerequisites**:
1. Backend running at http://localhost:8080
2. ADB port forwarding established: `adb forward tcp:8080 tcp:8080`
3. Backend seeded with 100 coils

**Test Scenarios**:
1. **Multi-Machine**: Add 2 machines, switch between them, verify grid updates
2. **Concurrent Edits**: Two operators edit same coil → verify 409 Conflict handled
3. **Offline Recovery**: Disconnect backend, wait 30s, reconnect → verify auto-recovery
4. **Performance**: Load grid with 100 coils → verify <1 second render time

---

## Deployment Checklist

- [ ] Copy portal/ directory to C:\ZootBoxPortal\ on operator's PC
- [ ] Create desktop shortcut to index.html or start-portal.bat script
- [ ] Configure ADB port forwarding for remote tablets
- [ ] Add first machine in Machine Settings (name, endpoint URL)
- [ ] Verify connection status shows "Online"
- [ ] Test bulk refill operation
- [ ] Export machine configs to backup JSON file

---

## Known Limitations & Future Enhancements

### Current Limitations
1. **No Authentication**: Relies on network-level security (localhost/ADB/VPN)
2. **No Audit Trail**: Admin operations not logged (performance optimization)
3. **Single Operator**: LocalStorage tied to one PC (no cloud sync)
4. **No Transaction History**: Portal doesn't display historical vend events

### Future Enhancements (Post-MVP)
1. **Transaction History Page**: Display vend events from GET /api/v1/transactions (when backend endpoint added)
2. **Reports & Analytics**: Generate refill schedules, inventory turnover reports
3. **Push Notifications**: Browser notifications for low stock or jams (via WebSockets)
4. **Mobile Responsive**: Optimize for tablet/phone screens (current target: desktop 1024px+)
5. **Config Export/Import**: Export machine configs to JSON file for multi-PC portability

---

## Support & References

- **Specification**: `specs/001-inventory-portal/spec.md`
- **Implementation Plan**: `specs/001-inventory-portal/plan.md`
- **API Documentation**: `specs/001-inventory-portal/contracts/api-endpoints.md`
- **Data Model**: `specs/001-inventory-portal/data-model.md`
- **Quickstart Guide**: `specs/001-inventory-portal/quickstart.md`
- **Backend Source**: `Backend/internal/api/router.go`

---

**Ready for implementation!** Start with Phase 1 (Setup) and progress through phases sequentially for MVP, or parallelize Phases 4A/4B and 5/6 with multiple developers.
