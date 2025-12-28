# ZootBox Inventory Web Portal - Technology Research & Decisions

**Date:** December 28, 2025
**Project:** ZootBox Inventory Management Web Portal
**Portal Type:** Multi-page static HTML/CSS/JS running on Windows desktop
**Backend:** Go REST API on tablet (localhost:8080) via ADB port forwarding

---

## Table of Contents

1. [JavaScript Framework Decision](#1-javascript-framework-decision)
2. [CORS Configuration](#2-cors-configuration)
3. [Auto-Refresh Pattern](#3-auto-refresh-pattern)
4. [LocalStorage Data Model](#4-localstorage-data-model)
5. [Testing Strategy](#5-testing-strategy)
6. [10x10 Grid Rendering](#6-10x10-grid-rendering)
7. [Offline Behavior](#7-offline-behavior)
8. [Multi-Machine Switching](#8-multi-machine-switching)

---

## 1. JavaScript Framework Decision

### Decision

**Vanilla JavaScript (ES6+)** with minimal build tooling, no framework dependencies.

### Rationale

For the ZootBox portal, vanilla JavaScript is the optimal choice:

1. **Simplicity & Maintainability**: The portal is relatively simple (4 pages, 10-15 components) with straightforward requirements. Vanilla JS eliminates framework overhead and complexity.

2. **No Build Infrastructure**: Per requirements, "no build infrastructure initially." Vanilla JS can run directly in the browser without transpilation, Webpack, or bundlers. This enables simple file:// or localhost:3000 serving.

3. **Performance**: Vanilla JS has zero bundle overhead. React (40KB) or Vue (30KB) add unnecessary bytes for a simple inventory portal. The portal meets performance requirements without framework abstraction layers.

4. **Desktop Windows Context**: Running on Windows desktop with basic operator skill level, vanilla JS reduces setup complexity. No need for npm, Node.js, or build tooling—just HTML, CSS, and JS.

5. **REST API Integration**: The backend already exists (Go API). Vanilla JS's Fetch API handles REST calls efficiently without framework-specific wrappers.

6. **State Management Simplicity**: With 4 pages and a small state surface (machine configs, current inventory), vanilla Object-based state management (no Redux/Vuex) suffices.

### Alternatives Considered

| Framework | Rationale for Rejection |
|-----------|------------------------|
| **React** | Requires build tools (Webpack, Babel), npm dependencies, and transcompilation. Overly complex for simple portal. 40KB bundle adds overhead. Better for complex SPAs with hundreds of components. |
| **Vue.js** | More approachable than React but still requires build infrastructure. 30KB bundle. Progressive enhancement possible but adds cognitive load for minimal benefit. |
| **Svelte** | Excellent performance but requires build tooling (SvelteKit). Newer ecosystem, smaller community. Doesn't fit "no build infrastructure" requirement. |
| **Framework-agnostic**: Lit, HTMX | Web components require ES6+ browser support (met) but add complexity. HTMX encourages server-side rendering, not suitable for static file serving. |

### Implementation Notes

**Architecture:**
```
src/
├── index.html                    # Entry point (4 pages via hash routing)
├── styles/
│   ├── reset.css                 # Base styles
│   ├── grid.css                  # 10x10 grid styles (CSS Grid)
│   └── components.css            # Reusable component styles
├── js/
│   ├── app.js                    # Main app entry, page routing
│   ├── state.js                  # Global state management
│   ├── api.js                    # REST API client (Fetch wrapper)
│   ├── pages/
│   │   ├── dashboard.js          # Inventory overview
│   │   ├── machine-selector.js   # Machine picker
│   │   ├── grid.js               # 10x10 grid view
│   │   └── operations.js         # Admin operations
│   └── utils/
│       ├── storage.js            # localStorage wrapper
│       ├── polling.js            # setInterval polling logic
│       └── debounce.js           # Debounce/throttle utilities
└── data/
    └── machines.json             # Initial machine config (optional)
```

**Module Pattern (for encapsulation without framework):**
```javascript
// state.js - Global state manager
const AppState = (() => {
  let state = {
    machines: [],
    currentMachine: null,
    inventory: {},
    lastSync: null,
    isDirty: false
  };

  return {
    get: (key) => state[key],
    set: (key, value) => {
      state[key] = value;
      AppState.notify();
    },
    subscribe: (listener) => {
      listeners.push(listener);
      return () => listeners.splice(listeners.indexOf(listener), 1);
    },
    notify: () => listeners.forEach(l => l(state))
  };
})();

// Usage in pages
AppState.subscribe((newState) => {
  console.log('State updated:', newState);
  render();
});
```

**Fetch API Wrapper (api.js):**
```javascript
const API = {
  baseURL: 'http://localhost:8080/api/v1',

  async getCoils(machineURL) {
    try {
      const response = await fetch(`${machineURL}/api/v1/coils`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.json();
    } catch (error) {
      console.error('Failed to fetch coils:', error);
      throw error;
    }
  },

  async recordTransaction(machineURL, data) {
    const response = await fetch(`${machineURL}/api/v1/transactions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    return response.json();
  }
};
```

**Router (Single HTML, multiple pages via hash):**
```javascript
// app.js
const pages = {
  '#/': { module: Dashboard, el: '#dashboard-page' },
  '#/machines': { module: MachineSelector, el: '#machines-page' },
  '#/grid': { module: GridView, el: '#grid-page' },
  '#/admin': { module: Operations, el: '#admin-page' }
};

window.addEventListener('hashchange', () => {
  const page = pages[window.location.hash] || pages['#/'];
  document.querySelectorAll('[data-page]').forEach(el => el.hidden = true);
  document.querySelector(page.el).hidden = false;
  page.module.init();
});
```

**Benefits of This Approach:**
- ✅ Runs immediately in browser (no build step)
- ✅ Minimal dependencies (just HTML + Fetch API)
- ✅ Direct file:// URL support or simple HTTP server
- ✅ Easy to modify for operators (no transpilation confusion)
- ✅ Fast page load (no framework overhead)
- ✅ Familiar to backend developers (JavaScript, no JSX/templates)

---

## 2. CORS Configuration

### Decision

**CORS enabled on Go backend for file:// and localhost:3000/8000 origins only.**
**Client-side: Configure API base URL dynamically based on machine URL.**

### Rationale

The scenario is unique: browser-based portal (file:// or localhost) calls tablet backends at different localhost ports via ADB port forwarding.

**Issue Analysis:**
- Portal runs on Windows desktop (file:// or localhost:3000)
- Backend runs on tablet (localhost:8080 via ADB forward)
- ADB forward maps tablet:8080 → desktop:8080
- Browser treats file:// and localhost:3000 as different origins
- Each origin requires explicit CORS header from backend

**Current Backend Implementation:**
```go
// C:\dev\Backend\internal\api\middleware\cors.go
func CORS(next http.Handler) http.Handler {
  return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Access-Control-Allow-Origin", "http://localhost:*")
    w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
    w.Header().Set("Access-Control-Allow-Headers", "Content-Type, X-Correlation-ID")
    w.Header().Set("Access-Control-Max-Age", "86400") // 24 hours

    if r.Method == http.MethodOptions {
      w.WriteHeader(http.StatusNoContent)
      return
    }
    next.ServeHTTP(w, r)
  })
}
```

### Alternatives Considered

| Approach | Status | Issues |
|----------|--------|--------|
| **File:// + CORS** | ❌ Rejected | Browsers block file:// from XMLHttpRequest/Fetch for security. Even with CORS headers, file:// requests fail. |
| **Localhost:3000 + CORS** | ✅ **Chosen** | Works. Browser allows localhost:3000 → localhost:8080. ADB forward handles routing. CORS headers validate origin. |
| **No CORS (Proxy pattern)** | ⚠️ Alternative | Backend proxy/reverse proxy on same port as portal. Adds complexity, requires additional server. Not necessary with proper CORS. |
| **Disable CORS in DevTools** | ❌ Rejected | Only works in development, not operationally suitable. Operators shouldn't need browser hacks. |
| **websockets/server-sent events** | ⚠️ Hybrid | Good for real-time polling, not CORS solution. Can coexist with REST+CORS. |

### Implementation Notes

**Backend Enhancement for Production:**
```go
// Improved CORS middleware supporting multiple origins
func CORS(next http.Handler) http.Handler {
  return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
    origin := r.Header.Get("Origin")

    // Whitelist allowed origins (file served on specific ports)
    allowedOrigins := map[string]bool{
      "http://localhost:3000": true,
      "http://localhost:8000": true,
      "http://127.0.0.1:3000": true,
      "http://127.0.0.1:8000": true,
    }

    if allowedOrigins[origin] {
      w.Header().Set("Access-Control-Allow-Origin", origin)
      w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
      w.Header().Set("Access-Control-Allow-Headers", "Content-Type, X-Correlation-ID, X-Request-ID")
      w.Header().Set("Access-Control-Allow-Credentials", "true")
      w.Header().Set("Access-Control-Max-Age", "86400")
    }

    if r.Method == http.MethodOptions {
      w.WriteHeader(http.StatusNoContent)
      return
    }
    next.ServeHTTP(w, r)
  })
}
```

**Portal Setup (Frontend):**
```javascript
// api.js - Multi-machine API client
class MachineAPI {
  constructor(machineConfig) {
    // machineConfig = { name: "Machine A", url: "http://localhost:8080" }
    this.machineConfig = machineConfig;
    this.apiBase = `${machineConfig.url}/api/v1`;
  }

  async getCoils() {
    const response = await fetch(`${this.apiBase}/coils`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'X-Request-ID': generateRequestID() // Helps with debugging
      },
      credentials: 'omit' // No cookies across origins
    });

    if (response.status === 403) {
      throw new Error('CORS blocked by backend');
    }
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    return response.json();
  }
}

// Usage
const api = new MachineAPI({
  name: "Machine A",
  url: "http://localhost:8080"
});
const coils = await api.getCoils();
```

**ADB Port Forwarding Setup:**
```bash
# On Windows (development machine)
adb forward tcp:8080 tcp:8080

# Portal then calls: http://localhost:8080/api/v1/coils
# ADB routes: localhost:8080 → tablet:8080
```

**Portal Server Launch (Simple HTTP):**
```bash
# Option 1: Python SimpleHTTPServer (no dependencies)
python -m http.server 3000

# Option 2: Node.js http-server (if Node available)
npx http-server -p 3000

# Option 3: Windows built-in (if available)
php -S localhost:3000
```

**Testing CORS Locally:**
```javascript
// In browser console, verify CORS works
fetch('http://localhost:8080/health')
  .then(r => r.json())
  .then(data => console.log('CORS OK:', data))
  .catch(e => console.error('CORS Failed:', e));
```

---

## 3. Auto-Refresh Pattern

### Decision

**setInterval (5-second polling) for inventory updates, with debounced UI rendering.**
**requestAnimationFrame only for visual animations (grid highlights, cell updates).**
**No WebSockets initially** (REST polling sufficient; WebSockets added if real-time collaboration needed).

### Rationale

1. **setInterval for Polling**: Inventory polling (5 seconds) is not a visual animation. It's a fixed-interval, non-rendering task that doesn't depend on browser repaint cycles. `setInterval` matches the use case perfectly.

2. **Avoid requestAnimationFrame for Polling**: `requestAnimationFrame` is optimized for rendering—it pauses in background tabs and syncs with monitor refresh rates. For a simple REST poll, this overhead is unnecessary. Moreover, RAF is meant for continuous visual work; 5-second polling doesn't benefit.

3. **No Jank Risk**: The portal has minimal UI complexity (single 10x10 grid + UI controls). A fetch request + small JSON parsing doesn't cause layout thrashing. Debouncing UI updates prevents excessive reflows.

4. **No WebSockets Yet**: WebSockets add complexity (connection mgmt, heartbeats, error recovery). Polling is simpler and sufficient for:
   - 5-second refresh intervals (not real-time)
   - Single operator per machine (no live collaboration)
   - Graceful offline degradation (polling fails cleanly, WebSocket leaves connection state ambiguous)

5. **Simplicity**: One `setInterval` is easier to understand and debug than managing WebSocket lifecycle events.

### Alternatives Considered

| Approach | Use Case | Status |
|----------|----------|--------|
| **setInterval (5s polling)** | ✅ **Chosen** | Simple, reliable, sufficient for 5-second refresh rate. Easy error recovery. |
| **requestAnimationFrame** | Visual animations only | ⚠️ Not for polling. Use for grid cell animations/highlights. |
| **WebSocket** | ❌ Rejected for now | Adds complexity (connection state, heartbeats, reconnection logic). Better for real-time features (multiple operators, live feed). |
| **Server-Sent Events (SSE)** | ⚠️ Future alternative | Simpler than WebSocket, one-way from server. Good if backend supports push. Current REST API doesn't. |
| **fetch...then(() => setTimeout(*, 5000))** | ❌ Rejected | Recursive polling is fragile (errors break the loop). `setInterval` is more robust. |

### Implementation Notes

**Polling Controller (polling.js):**
```javascript
// polling.js - Centralized polling manager
class PollingManager {
  constructor(options = {}) {
    this.interval = options.interval || 5000; // 5 seconds
    this.onData = options.onData || (() => {});
    this.onError = options.onError || (() => {});
    this.isRunning = false;
    this.timerId = null;
    this.lastUpdate = null;
  }

  start(fetchFn) {
    if (this.isRunning) return;
    this.isRunning = true;

    // Initial fetch immediately
    this._poll(fetchFn);
  }

  async _poll(fetchFn) {
    try {
      const data = await fetchFn();
      this.lastUpdate = new Date();

      // Debounce UI updates (50ms)
      debounce(() => this.onData(data), 50);
    } catch (error) {
      console.error('Polling error:', error);
      this.onError(error);
    } finally {
      // Schedule next poll
      this.timerId = setTimeout(() => this._poll(fetchFn), this.interval);
    }
  }

  stop() {
    if (this.timerId) clearTimeout(this.timerId);
    this.isRunning = false;
  }

  setInterval(newInterval) {
    this.interval = newInterval;
  }
}

// Usage in app
const poller = new PollingManager({
  interval: 5000,
  onData: (coils) => updateGridUI(coils),
  onError: (err) => showErrorBanner(`Polling failed: ${err.message}`)
});

poller.start(async () => {
  return await api.getCoils();
});

// Stop on page unload
window.addEventListener('beforeunload', () => poller.stop());
```

**Debounce Utility (utils/debounce.js):**
```javascript
// Prevent UI thrashing (e.g., multiple renders in 50ms window)
function debounce(fn, delay) {
  let timeoutId;
  return function(...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  };
}

function throttle(fn, delay) {
  let lastRun = 0;
  return function(...args) {
    const now = Date.now();
    if (now - lastRun >= delay) {
      fn(...args);
      lastRun = now;
    }
  };
}

// Usage
const updateUI = debounce((data) => {
  // This won't execute if called multiple times within 50ms
  document.getElementById('grid').innerText = JSON.stringify(data);
}, 50);

// In polling callback
poller.onData = (data) => updateUI(data);
```

**Handling Polling Errors (Graceful Degradation):**
```javascript
class PollingWithFallback {
  constructor(api, options = {}) {
    this.api = api;
    this.options = options;
    this.failureCount = 0;
    this.maxFailures = 3; // Stop polling after 3 failures
    this.poller = new PollingManager({
      interval: options.interval || 5000,
      onData: (data) => this._onSuccess(data),
      onError: (err) => this._onError(err)
    });
  }

  async _onSuccess(data) {
    this.failureCount = 0;
    this.options.onData?.(data);
    // Show "last synced" timestamp
    document.getElementById('last-sync').innerText =
      `Last synced: ${new Date().toLocaleTimeString()}`;
  }

  _onError(error) {
    this.failureCount++;
    console.warn(`Polling failure #${this.failureCount}:`, error);

    if (this.failureCount >= this.maxFailures) {
      this.poller.stop();
      this.options.onFatalError?.(error);
      showBanner('Backend unreachable. Using cached data.', 'warning');
    } else {
      showBanner(`Sync failed. Retrying in 5s...`, 'warning');
    }
  }

  start() {
    this.poller.start(() => this.api.getCoils());
  }

  stop() {
    this.poller.stop();
  }
}
```

**Visual Animation with requestAnimationFrame (Optional Enhancement):**
```javascript
// Use RAF only for visual updates (grid highlights, cell animations)
function animateGridUpdate(cellElement, newData) {
  // Highlight updated cell for 300ms
  cellElement.classList.add('updated');

  // Use RAF for smooth fade-out
  let startTime = performance.now();
  const duration = 300;

  function animate(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);

    cellElement.style.backgroundColor =
      `rgba(76, 175, 80, ${1 - progress})`; // Fade green highlight

    if (progress < 1) {
      requestAnimationFrame(animate);
    } else {
      cellElement.classList.remove('updated');
    }
  }

  requestAnimationFrame(animate);
}

// On each polling update, animate changed cells
poller.onData = (newCoils) => {
  const oldCoils = AppState.get('inventory');
  newCoils.forEach(coil => {
    if (oldCoils[coil.id]?.quantity !== coil.quantity) {
      const cellEl = document.querySelector(`[data-coil-id="${coil.id}"]`);
      animateGridUpdate(cellEl, coil);
    }
  });
  AppState.set('inventory', newCoils);
};
```

**Performance Monitoring:**
```javascript
// Track polling performance
class PollingMetrics {
  constructor(poller) {
    this.poller = poller;
    this.metrics = {
      totalPollsAttempted: 0,
      successfulPolls: 0,
      failedPolls: 0,
      totalResponseTime: 0,
      avgResponseTime: 0,
      lastPollDuration: null
    };
  }

  trackPoll(fetchFn) {
    return async () => {
      this.metrics.totalPollsAttempted++;
      const startTime = performance.now();

      try {
        const data = await fetchFn();
        this.metrics.successfulPolls++;
        return data;
      } catch (error) {
        this.metrics.failedPolls++;
        throw error;
      } finally {
        const duration = performance.now() - startTime;
        this.metrics.lastPollDuration = duration;
        this.metrics.totalResponseTime += duration;
        this.metrics.avgResponseTime =
          this.metrics.totalResponseTime / this.metrics.successfulPolls;

        console.debug('Poll metrics:', {
          duration: `${duration.toFixed(2)}ms`,
          success_rate: `${(this.metrics.successfulPolls / this.metrics.totalPollsAttempted * 100).toFixed(1)}%`,
          avg_response: `${this.metrics.avgResponseTime.toFixed(2)}ms`
        });
      }
    };
  }
}
```

---

## 4. LocalStorage Data Model

### Decision

**Structured JSON for machine configs, inventory cache, and UI state.**
**Schema: `machines: [{id, name, url, status, lastSync, config}]`, `inventory: {coilId: {...}}`, `uiState: {currentMachine, viewMode}`**
**Max storage: ~100KB per data structure (well under 5MB limit).**

### Rationale

1. **Structured Schema**: Predictable structure enables easy querying, filtering, and updates without parsing/regex logic.

2. **Size Efficient**: 20 machines × ~500 bytes each = ~10KB. Inventory (100 coils × 100 bytes) = ~10KB. Well under browser's ~5MB limit per origin.

3. **Offline Cache**: When backend unreachable, stale inventory from localStorage enables graceful degradation (show last-known stock).

4. **Machine Persistence**: Operators configure machines once; localStorage persists config across browser sessions.

5. **Atomic Updates**: Each data type (machines, inventory) is separate, avoiding partial corruption.

### Alternatives Considered

| Approach | Issues | Status |
|----------|--------|--------|
| **localStorage (flat keys)** | ❌ Rejected | `localStorage['machine_1_name']`, `['machine_1_url']`... becomes unmanageable. Hard to iterate. |
| **localStorage (JSON strings)** | ✅ **Chosen** | `localStorage['machines']` = JSON.stringify([...]) Clean, queryable, standard. |
| **IndexedDB** | ⚠️ Overkill | Supports larger data (~50MB), but overkill for 100KB portal config. Adds async complexity. Use if offline database queries needed. |
| **sessionStorage** | ⚠️ Alternative | Data lost on tab close. Fine for temporary state (current form), but not suitable for machine configs (need persistence). |
| **No caching** | ❌ Rejected | Every page load requires network. Offline operation impossible. Poor UX. |

### Implementation Notes

**Storage Schema:**
```javascript
// storage.js - localStorage abstraction
const StorageSchema = {
  MACHINES: 'zootbox_machines',           // [{id, name, url, status, config}]
  INVENTORY: 'zootbox_inventory_cache',   // {coilId: {id, quantity, lastUpdated}}
  UI_STATE: 'zootbox_ui_state',           // {currentMachine, viewMode, lastPage}
  METADATA: 'zootbox_metadata'            // {version, lastSync, schemaVersion}
};

class StorageManager {
  static validateJSON(str) {
    try {
      return JSON.parse(str);
    } catch (e) {
      console.error('Storage corruption detected:', e);
      return null;
    }
  }

  static getMachines() {
    const stored = localStorage.getItem(StorageSchema.MACHINES);
    return this.validateJSON(stored) || [];
  }

  static setMachines(machines) {
    // Validate schema
    if (!Array.isArray(machines)) {
      throw new Error('Machines must be array');
    }
    machines.forEach(m => {
      if (!m.id || !m.name || !m.url) {
        throw new Error('Machine missing required fields: id, name, url');
      }
    });
    localStorage.setItem(StorageSchema.MACHINES, JSON.stringify(machines));
  }

  static getInventory() {
    const stored = localStorage.getItem(StorageSchema.INVENTORY);
    return this.validateJSON(stored) || {};
  }

  static setInventory(inventory) {
    if (typeof inventory !== 'object' || Array.isArray(inventory)) {
      throw new Error('Inventory must be object');
    }
    localStorage.setItem(StorageSchema.INVENTORY, JSON.stringify(inventory));
  }

  static getUIState() {
    const stored = localStorage.getItem(StorageSchema.UI_STATE);
    return this.validateJSON(stored) || {
      currentMachine: null,
      viewMode: 'grid',
      lastPage: '#/'
    };
  }

  static setUIState(state) {
    localStorage.setItem(StorageSchema.UI_STATE, JSON.stringify(state));
  }

  static clear() {
    Object.values(StorageSchema).forEach(key => {
      localStorage.removeItem(key);
    });
  }

  static getStorageSize() {
    let total = 0;
    for (let key in localStorage) {
      if (localStorage.hasOwnProperty(key)) {
        total += localStorage[key].length + key.length;
      }
    }
    return Math.round(total / 1024); // KB
  }
}

// Usage
const machines = StorageManager.getMachines();
console.log(`Storage usage: ${StorageManager.getStorageSize()}KB / 5000KB`);
```

**Machine Configuration Example:**
```javascript
const machines = [
  {
    id: 'machine_001',
    name: 'Main Vending Unit',
    url: 'http://localhost:8080',
    status: 'online',
    lastSync: '2025-12-28T14:32:00Z',
    config: {
      gridSize: 10,         // 10x10
      refreshInterval: 5000, // 5 seconds
      location: 'Break Room',
      operatorId: 'op_001'
    }
  },
  {
    id: 'machine_002',
    name: 'Backup Unit',
    url: 'http://localhost:8081',
    status: 'offline',
    lastSync: '2025-12-28T10:00:00Z',
    config: {
      gridSize: 10,
      refreshInterval: 5000,
      location: 'Lobby'
    }
  }
];

StorageManager.setMachines(machines);
```

**Inventory Cache Structure:**
```javascript
const inventory = {
  'A1': {
    id: 'A1',
    coilNumber: 'A-01',
    productSKU: 'ZYN-CITRUS',
    quantity: 8,
    maxCapacity: 10,
    lastUpdated: '2025-12-28T14:30:00Z',
    isLowStock: false
  },
  'A2': {
    id: 'A2',
    coilNumber: 'A-02',
    productSKU: 'LIGHTER-GOLD',
    quantity: 0,
    maxCapacity: 15,
    lastUpdated: '2025-12-28T14:30:00Z',
    isLowStock: true
  },
  // ... 98 more coils
};

StorageManager.setInventory(inventory);
```

**UI State Example:**
```javascript
const uiState = {
  currentMachine: 'machine_001',
  viewMode: 'grid',        // 'grid' | 'list' | 'operations'
  lastPage: '#/grid',
  gridFilter: 'all',       // 'all' | 'lowstock' | 'empty'
  selectedCoil: 'A5',
  sidebarExpanded: true,
  theme: 'light'           // 'light' | 'dark'
};

StorageManager.setUIState(uiState);
```

**Migration for Version Updates:**
```javascript
class StorageManager {
  static SCHEMA_VERSION = 2;

  static initialize() {
    const metadata = this.getMetadata();
    if (metadata.schemaVersion < this.SCHEMA_VERSION) {
      console.log(`Migrating storage from v${metadata.schemaVersion} to v${this.SCHEMA_VERSION}`);
      this._migrateSchema(metadata.schemaVersion);
      metadata.schemaVersion = this.SCHEMA_VERSION;
      this.setMetadata(metadata);
    }
  }

  static _migrateSchema(fromVersion) {
    if (fromVersion < 2) {
      // v1 → v2: Add 'status' field to machines
      const machines = this.getMachines();
      machines.forEach(m => {
        if (!m.status) m.status = 'online';
      });
      this.setMachines(machines);
    }
  }

  static getMetadata() {
    const stored = localStorage.getItem(StorageSchema.METADATA);
    return this.validateJSON(stored) || {
      version: '1.0.0',
      schemaVersion: 1,
      lastSync: null,
      createdAt: new Date().toISOString()
    };
  }

  static setMetadata(metadata) {
    localStorage.setItem(StorageSchema.METADATA, JSON.stringify(metadata));
  }
}

// On app init
StorageManager.initialize();
```

**Size Monitoring & Warnings:**
```javascript
// Warn if approaching 5MB limit
setInterval(() => {
  const used = StorageManager.getStorageSize();
  const percentUsed = (used / 5000) * 100;

  if (percentUsed > 80) {
    console.warn(`Storage ${percentUsed.toFixed(1)}% full (${used}KB / 5000KB)`);
    showBanner('Storage nearly full. Consider clearing old data.', 'warning');
  }
}, 60000); // Check every minute
```

---

## 5. Testing Strategy

### Decision

**Testing Pyramid:**
- **70% Unit Tests** (JavaScript logic, state management)
- **20% Integration Tests** (API calls with mocks)
- **10% E2E Tests** (critical user flows in real browser)

**Tools:** Jest (unit), Mock Service Worker (MSW, API mocking), Playwright (E2E)
**No test infrastructure initially**; add incrementally as codebase grows.

### Rationale

1. **Unit Tests First**: Test state.js, storage.js, api.js modules in isolation. Fast feedback (< 1s), no external dependencies.

2. **Mock Service Worker (MSW)**: Shared API mocks for both development and tests. Intercepts Fetch calls without modifying app code.

3. **Integration Tests**: Verify pages work with mocked API. Example: "Load machine selector → fetch machines → render list."

4. **E2E Tests Selectively**: Only test critical paths (machine switching, grid updates). E2E is slow; avoid over-testing.

5. **No Build Required**: Jest runs vanilla JS directly. No transpilation needed.

### Alternatives Considered

| Tool | Status | Notes |
|------|--------|-------|
| **Jest (unit)** | ✅ **Chosen** | Popular, fast, no config needed for vanilla JS. Supports mocking. |
| **Vitest** | ⚠️ Alternative | Faster than Jest, but newer. Jest more familiar. |
| **Playwright (E2E)** | ✅ **Chosen** | Cross-browser, runs real browser, good for critical flows. |
| **Cypress (E2E)** | ⚠️ Alternative | Developer-friendly, but slower startup. Playwright faster. |
| **Selenium** | ❌ Rejected | Verbose, slow, outdated patterns. Playwright/Cypress better. |
| **Manual Testing Only** | ❌ Rejected | Portal operator changes, risk of regression. Tests provide safety net. |

### Implementation Notes

**Unit Test Example (state.js):**
```javascript
// test/state.test.js
import { AppState } from '../js/state.js';

describe('AppState', () => {
  beforeEach(() => {
    AppState.reset(); // Clear state before each test
  });

  test('should initialize with default state', () => {
    expect(AppState.get('machines')).toEqual([]);
    expect(AppState.get('currentMachine')).toBeNull();
  });

  test('should set and get state', () => {
    const machines = [{ id: '1', name: 'Test' }];
    AppState.set('machines', machines);
    expect(AppState.get('machines')).toEqual(machines);
  });

  test('should notify subscribers on state change', () => {
    const listener = jest.fn();
    AppState.subscribe(listener);

    AppState.set('machines', []);
    expect(listener).toHaveBeenCalled();
  });

  test('should allow unsubscription', () => {
    const listener = jest.fn();
    const unsubscribe = AppState.subscribe(listener);

    unsubscribe();
    AppState.set('machines', []);
    expect(listener).not.toHaveBeenCalled();
  });
});
```

**API Test with Mock Service Worker:**
```javascript
// test/api.test.js
import { server } from './mocks/server.js';
import { MachineAPI } from '../js/api.js';

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('MachineAPI', () => {
  test('should fetch coils successfully', async () => {
    const api = new MachineAPI({ name: 'Test', url: 'http://localhost:8080' });
    const coils = await api.getCoils();

    expect(coils).toHaveLength(100);
    expect(coils[0]).toHaveProperty('id');
    expect(coils[0]).toHaveProperty('quantity');
  });

  test('should handle API errors gracefully', async () => {
    const api = new MachineAPI({ name: 'Test', url: 'http://localhost:8080' });

    // MSW will return 500 for this URL
    await expect(api.getCoils()).rejects.toThrow('HTTP 500');
  });

  test('should handle network timeout', async () => {
    const api = new MachineAPI({
      name: 'Test',
      url: 'http://localhost:9999'
    });

    jest.useFakeTimers();
    const promise = api.getCoils();
    jest.advanceTimersByTime(5000);
    await expect(promise).rejects.toThrow();
  });
});
```

**Mock Service Worker Setup:**
```javascript
// test/mocks/handlers.js
import { http, HttpResponse } from 'msw';

export const handlers = [
  // Coils endpoint
  http.get('http://localhost:8080/api/v1/coils', () => {
    const coils = Array.from({ length: 100 }, (_, i) => ({
      id: String.fromCharCode(65 + Math.floor(i / 10)) + (i % 10 + 1),
      quantity: Math.floor(Math.random() * 10),
      productSKU: `PRODUCT_${i}`,
      lastUpdated: new Date().toISOString()
    }));
    return HttpResponse.json(coils);
  }),

  // Transactions endpoint
  http.post('http://localhost:8080/api/v1/transactions', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      transactionId: 'TXN_001',
      status: 'success',
      timestamp: new Date().toISOString(),
      ...body
    });
  }),

  // Error handler
  http.get('http://localhost:9999/api/v1/*', () => {
    return HttpResponse.json(
      { error: 'Service unavailable' },
      { status: 500 }
    );
  })
];

// test/mocks/server.js
import { setupServer } from 'msw/node';
import { handlers } from './handlers.js';
export const server = setupServer(...handlers);
```

**Integration Test Example (Grid Page):**
```javascript
// test/pages/grid.test.js
import { GridPage } from '../js/pages/grid.js';
import { AppState } from '../js/state.js';
import { server } from '../mocks/server.js';

beforeAll(() => server.listen());
beforeEach(() => {
  document.body.innerHTML = '<div id="grid-page"></div>';
  AppState.set('currentMachine', {
    id: 'test_1',
    name: 'Test',
    url: 'http://localhost:8080'
  });
});
afterEach(() => server.resetHandlers());

describe('GridPage', () => {
  test('should render 10x10 grid', async () => {
    await GridPage.init();

    const cells = document.querySelectorAll('[data-coil-id]');
    expect(cells.length).toBe(100);
  });

  test('should display coil quantities', async () => {
    await GridPage.init();

    const firstCell = document.querySelector('[data-coil-id="A1"]');
    expect(firstCell.innerText).toMatch(/\d+/); // Quantity number
  });

  test('should highlight low-stock cells', async () => {
    await GridPage.init();

    const lowStockCells = document.querySelectorAll('[data-stock-level="low"]');
    expect(lowStockCells.length).toBeGreaterThan(0);
  });

  test('should update on polling', async () => {
    await GridPage.init();

    const initialQuantity = GridPage.getCoilQuantity('A1');

    // Simulate polling update
    AppState.set('inventory', {
      ...AppState.get('inventory'),
      'A1': { ...AppState.get('inventory')['A1'], quantity: 5 }
    });

    expect(GridPage.getCoilQuantity('A1')).toBe(5);
  });
});
```

**E2E Test Example (Critical Flow):**
```javascript
// test/e2e/machine-switch.spec.js
import { test, expect } from '@playwright/test';

test.describe('Machine Switching', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000');
  });

  test('should switch between machines', async ({ page }) => {
    // Step 1: Navigate to machines page
    await page.click('[data-nav="machines"]');
    await expect(page).toHaveURL(/#\/machines/);

    // Step 2: Select first machine
    await page.click('[data-machine-id="machine_001"]');

    // Step 3: Verify grid loads for selected machine
    await expect(page).toHaveURL(/#\/grid/);
    const gridCells = page.locator('[data-coil-id]');
    await expect(gridCells.first()).toBeVisible();

    // Step 4: Switch to second machine
    await page.click('[data-nav="machines"]');
    await page.click('[data-machine-id="machine_002"]');

    // Step 5: Verify grid updates for new machine
    await expect(page.locator('#machine-name')).toContainText('machine_002');
  });

  test('should handle machine offline gracefully', async ({ page }) => {
    // Simulate backend offline
    await page.route('**/api/v1/coils', (route) => {
      route.abort('failed');
    });

    await page.click('[data-nav="machines"]');
    await page.click('[data-machine-id="machine_001"]');

    // Should show cached data with warning
    await expect(page.locator('[data-status="offline"]')).toBeVisible();
    await expect(page.locator('[data-notice]')).toContainText('cached data');
  });
});
```

**Test Coverage Goals:**
```javascript
// jest.config.js
module.exports = {
  testEnvironment: 'jsdom',
  collectCoverage: true,
  collectCoverageFrom: [
    'js/**/*.js',
    '!js/**/*.test.js',
    '!js/**/index.js'
  ],
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70
    }
  }
};
```

**Run Tests:**
```bash
# Install dependencies (one-time)
npm install jest @testing-library/dom msw @playwright/test

# Run unit tests
npm test

# Run unit tests with coverage
npm test -- --coverage

# Run E2E tests
npx playwright test

# Run E2E tests in UI mode
npx playwright test --ui
```

---

## 6. 10x10 Grid Rendering

### Decision

**CSS Grid** (layout) + **HTML table structure** (semantics) + **vanilla DOM updates** (rendering).
**No canvas** (unnecessary for static grid display).

### Rationale

1. **CSS Grid for Layout**: Modern, performant, responsive. 100 cells render in <100ms.

2. **HTML Table for Accessibility**: Screen readers understand table semantics (rows, columns, headers). Canvas provides zero accessibility.

3. **Vanilla DOM for Updates**: Simple cell updates (quantity changes) via `innerText` or `classList` without re-rendering entire grid.

4. **Performance Meets Requirement**: <1s render time easily achieved (typically 80-150ms for 100 cells on mid-range desktop).

5. **Responsive Design**: CSS Grid adapts to different screen sizes (portrait/landscape tablets, desktop monitors).

### Alternatives Considered

| Approach | Performance | Accessibility | Issues |
|----------|-------------|----------------|--------|
| **CSS Grid (chosen)** | ✅ ~100ms | ✅ Full | Native, semantic, responsive |
| **HTML Table** | ✅ ~100ms | ✅ Full | Less modern layout; CSS Grid superior |
| **Canvas 2D** | ✅ <50ms | ❌ None | No screen reader support. Requires manual event handling. |
| **SVG Grid** | ⚠️ ~200ms | ⚠️ Limited | Bulky DOM (100 elements). Events harder to manage. |
| **Div Flexbox Grid** | ⚠️ ~150ms | ✅ Full | Works, but CSS Grid more efficient for 2D layouts. |
| **Virtual Scrolling** | ✅ <10ms | ⚠️ Partial | Overkill; all 100 cells fit on screen. Complicates updates. |

### Implementation Notes

**HTML Structure:**
```html
<!-- index.html -->
<table class="coil-grid" id="grid-table">
  <thead>
    <tr>
      <th class="row-header"></th>
      <th colspan="10">Coils Grid</th>
    </tr>
    <tr>
      <th class="col-header"></th>
      <th class="col-number">1</th>
      <th class="col-number">2</th>
      <!-- ... 3-10 -->
    </tr>
  </thead>
  <tbody id="grid-body">
    <!-- Dynamically populated -->
  </tbody>
</table>
```

**CSS Grid Styling:**
```css
/* styles/grid.css */
.coil-grid {
  display: grid;
  grid-template-columns: 40px repeat(10, 1fr);
  grid-template-rows: auto auto repeat(10, 1fr);
  gap: 2px;
  padding: 10px;
  background: #f5f5f5;
  border: 1px solid #ccc;
  max-width: 100%;
}

.coil-grid td, .coil-grid th {
  padding: 10px;
  text-align: center;
  border: 1px solid #ddd;
  background: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s, box-shadow 0.2s;
}

.coil-grid td:hover {
  background-color: #f0f0f0;
  box-shadow: inset 0 0 4px rgba(0, 0, 0, 0.1);
}

.coil-cell {
  position: relative;
}

.coil-cell.empty {
  background-color: #ffebee; /* Light red */
  color: #c62828;
  font-weight: bold;
}

.coil-cell.low-stock {
  background-color: #fff9c4; /* Light yellow */
  color: #f57f17;
}

.coil-cell.normal {
  background-color: #e8f5e9; /* Light green */
  color: #2e7d32;
}

.coil-cell.selected {
  box-shadow: inset 0 0 0 3px #2196f3;
  background-color: #e3f2fd;
}

.coil-cell.updating {
  animation: pulse 0.5s ease-out;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

/* Row and column headers */
.row-header {
  font-weight: bold;
  background-color: #f9f9f9;
  writing-mode: vertical-rl;
  text-orientation: mixed;
}

.col-header {
  font-weight: bold;
  background-color: #f9f9f9;
}

.col-number {
  background-color: #f9f9f9;
}

/* Responsive: Stack on small screens */
@media (max-width: 768px) {
  .coil-grid {
    grid-template-columns: 30px repeat(10, minmax(40px, 1fr));
    gap: 1px;
  }

  .coil-grid td, .coil-grid th {
    padding: 8px 4px;
    font-size: 12px;
  }
}
```

**Grid Rendering (JavaScript):**
```javascript
// js/pages/grid.js
class GridPage {
  static ROWS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];
  static COLS = Array.from({ length: 10 }, (_, i) => i + 1);

  static async init() {
    try {
      const startTime = performance.now();

      // Fetch coils from API
      const api = this._getAPI();
      const coils = await api.getCoils();

      // Render grid
      this._renderGrid(coils);

      const renderTime = performance.now() - startTime;
      console.log(`Grid rendered in ${renderTime.toFixed(2)}ms`);

      // Cache for later
      AppState.set('inventory', this._coilsToMap(coils));
    } catch (error) {
      this._showErrorState(error);
    }
  }

  static _renderGrid(coils) {
    const tbody = document.getElementById('grid-body');
    tbody.innerHTML = ''; // Clear existing

    const fragment = document.createDocumentFragment();

    this.ROWS.forEach((row, rowIdx) => {
      const tr = document.createElement('tr');

      // Row header (A, B, C, ...)
      const headerTd = document.createElement('td');
      headerTd.className = 'row-header';
      headerTd.textContent = row;
      tr.appendChild(headerTd);

      // Cells (1-10)
      this.COLS.forEach((col) => {
        const coilId = `${row}${col}`;
        const coil = coils.find(c => c.id === coilId);
        const td = document.createElement('td');

        td.className = 'coil-cell';
        td.id = `cell-${coilId}`;
        td.setAttribute('data-coil-id', coilId);

        if (coil) {
          td.setAttribute('data-quantity', coil.quantity);
          td.innerHTML = `
            <div class="quantity">${coil.quantity}</div>
            <div class="sku">${coil.productSKU}</div>
          `;

          // Apply stock level class
          if (coil.quantity === 0) {
            td.classList.add('empty');
          } else if (coil.quantity <= 2) {
            td.classList.add('low-stock');
          } else {
            td.classList.add('normal');
          }
        } else {
          td.textContent = '?';
        }

        // Click handler
        td.addEventListener('click', () => this._onCellClick(coilId));
        tr.appendChild(td);
      });

      fragment.appendChild(tr);
    });

    tbody.appendChild(fragment);
  }

  static _coilsToMap(coils) {
    return Object.fromEntries(
      coils.map(c => [c.id, c])
    );
  }

  static _onCellClick(coilId) {
    // Deselect previous
    document.querySelectorAll('.coil-cell.selected').forEach(el => {
      el.classList.remove('selected');
    });

    // Select new
    const cell = document.getElementById(`cell-${coilId}`);
    cell.classList.add('selected');

    // Show detail panel
    const coil = AppState.get('inventory')[coilId];
    this._showCoilDetail(coil);
  }

  static _showCoilDetail(coil) {
    const panel = document.getElementById('detail-panel');
    panel.innerHTML = `
      <h3>${coil.id}</h3>
      <p>SKU: ${coil.productSKU}</p>
      <p>Quantity: <strong>${coil.quantity}</strong></p>
      <p>Max Capacity: ${coil.maxCapacity}</p>
      <p>Last Updated: ${new Date(coil.lastUpdated).toLocaleTimeString()}</p>
      <button onclick="GridPage.refillCoil('${coil.id}')">Refill</button>
    `;
  }

  static updateCoilQuantity(coilId, newQuantity) {
    const cell = document.getElementById(`cell-${coilId}`);
    if (!cell) return;

    // Add animation class
    cell.classList.add('updating');

    // Update text
    const quantityDiv = cell.querySelector('.quantity');
    quantityDiv.textContent = newQuantity;
    cell.setAttribute('data-quantity', newQuantity);

    // Update stock level class
    cell.classList.remove('empty', 'low-stock', 'normal');
    if (newQuantity === 0) {
      cell.classList.add('empty');
    } else if (newQuantity <= 2) {
      cell.classList.add('low-stock');
    } else {
      cell.classList.add('normal');
    }

    // Remove animation after 500ms
    setTimeout(() => cell.classList.remove('updating'), 500);
  }

  static _getAPI() {
    const machine = AppState.get('currentMachine');
    return new MachineAPI(machine);
  }

  static _showErrorState(error) {
    const tbody = document.getElementById('grid-body');
    tbody.innerHTML = `
      <tr><td colspan="11" style="text-align: center; color: red; padding: 40px;">
        <strong>Failed to load grid:</strong> ${error.message}
        <p><small>Check backend connection and try again.</small></p>
      </td></tr>
    `;
  }
}
```

**Performance Optimization:**
```javascript
// Batch DOM updates for multiple coil changes
class GridBatchUpdater {
  constructor(delayMs = 50) {
    this.delayMs = delayMs;
    this.updates = new Map();
    this.timerId = null;
  }

  queue(coilId, quantity) {
    this.updates.set(coilId, quantity);

    if (this.timerId) clearTimeout(this.timerId);
    this.timerId = setTimeout(() => this.flush(), this.delayMs);
  }

  flush() {
    const startTime = performance.now();

    this.updates.forEach((quantity, coilId) => {
      GridPage.updateCoilQuantity(coilId, quantity);
    });

    const duration = performance.now() - startTime;
    console.log(`Batch update ${this.updates.size} cells in ${duration.toFixed(2)}ms`);

    this.updates.clear();
  }
}

// Usage in polling callback
const updater = new GridBatchUpdater(50); // Batch within 50ms windows

poller.onData = (newCoils) => {
  const oldCoils = AppState.get('inventory');
  newCoils.forEach(coil => {
    if (oldCoils[coil.id]?.quantity !== coil.quantity) {
      updater.queue(coil.id, coil.quantity);
    }
  });
};
```

**Accessibility Enhancements:**
```javascript
// Add ARIA labels for screen readers
class GridAccessibility {
  static enhanceGrid() {
    const table = document.getElementById('grid-table');
    table.setAttribute('role', 'grid');
    table.setAttribute('aria-label', '10x10 Coil Inventory Grid');

    document.querySelectorAll('.coil-cell').forEach((cell) => {
      const coilId = cell.getAttribute('data-coil-id');
      const quantity = cell.getAttribute('data-quantity');

      cell.setAttribute('role', 'gridcell');
      cell.setAttribute('tabindex', '0');
      cell.setAttribute('aria-label',
        `Coil ${coilId}: ${quantity} units in stock`);

      // Keyboard navigation
      cell.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          cell.click();
        }
      });
    });
  }
}

GridAccessibility.enhanceGrid();
```

---

## 7. Offline Behavior

### Decision

**Cache inventory + machine configs; serve stale data with age indicator. Stop polling after 3 failures; show "offline" status. Disable refill/admin operations until online.**

### Rationale

1. **Graceful Degradation**: Operator can still view last-known inventory (5-30 min old) when backend unreachable.

2. **Clear Data Staleness**: UI shows "Last synced: 5 min ago" so operator knows data may be outdated.

3. **Prevent Invalid Operations**: Disable refill/transactions offline (would fail anyway; better to warn upfront).

4. **Auto-Recovery**: Resume polling when connection restored (detect successful poll).

5. **Operator Awareness**: Red banner explains situation clearly.

### Alternatives Considered

| Approach | Issues | Status |
|----------|--------|--------|
| **Show empty grid when offline** | ❌ Rejected | Operator loses all context when backend flaky. Stale data > no data. |
| **Cache indefinitely** | ❌ Rejected | Days-old data is worse than no data. Operator refills wrong items. |
| **Cache 24 hours** | ⚠️ Alternative | Too long. Inventory changes throughout day. 30 min better. |
| **Stale-while-revalidate (30 min)** | ✅ **Chosen** | Industry standard. Serve old data immediately, fetch fresh in background. |
| **Switch to offline mode (local DB)** | ⚠️ Future | IndexedDB enables offline operations, but adds complexity. Skip v1. |

### Implementation Notes

**Offline Cache Manager:**
```javascript
// js/offline.js
class OfflineManager {
  static CACHE_DURATION_MS = 30 * 60 * 1000; // 30 minutes
  static MAX_FAILURES = 3;

  constructor() {
    this.isOffline = false;
    this.failureCount = 0;
    this.lastSuccessTime = null;
    this.cachedInventory = null;
  }

  async fetchWithOfflineFallback(fetchFn, cacheKey) {
    try {
      const data = await fetchFn();

      // Success: reset failure counter and cache
      this.failureCount = 0;
      this.lastSuccessTime = Date.now();
      this.cachedInventory = data;

      // Update status UI
      this._setOnlineStatus(true);

      return data;
    } catch (error) {
      this.failureCount++;
      console.warn(`Fetch failed (${this.failureCount}/${this.MAX_FAILURES}):`, error);

      if (this.failureCount >= this.MAX_FAILURES) {
        this._goOffline();
        return this._getStaleData();
      } else {
        // First/second failure: show warning but continue trying
        this._showWarning(`Connection issue. Attempt ${this.failureCount}/${this.MAX_FAILURES}`);
        throw error;
      }
    }
  }

  _getStaleData() {
    const cached = StorageManager.getInventory();

    if (!cached || Object.keys(cached).length === 0) {
      throw new Error('No cached data available');
    }

    const age = Date.now() - (this.lastSuccessTime || 0);
    const ageMinutes = Math.round(age / 60000);

    console.warn(`Using cached data from ${ageMinutes} minutes ago`);
    return cached;
  }

  _goOffline() {
    this.isOffline = true;
    this._setOnlineStatus(false);
    this._showErrorBanner(
      `Backend unreachable. Showing cached inventory from ${this._getAgeString()}.`,
      'error'
    );
  }

  _setOnlineStatus(isOnline) {
    const statusEl = document.getElementById('connection-status');
    if (!statusEl) return;

    if (isOnline) {
      statusEl.textContent = 'Online';
      statusEl.className = 'status-online';
      statusEl.title = `Last synced: ${new Date().toLocaleTimeString()}`;
    } else {
      statusEl.textContent = 'Offline (cached)';
      statusEl.className = 'status-offline';
      statusEl.title = `Last synced: ${this._getAgeString()} ago`;
    }
  }

  _getAgeString() {
    if (!this.lastSuccessTime) return 'unknown';
    const ageMs = Date.now() - this.lastSuccessTime;
    const minutes = Math.floor(ageMs / 60000);
    const seconds = Math.floor((ageMs % 60000) / 1000);

    if (minutes === 0) return `${seconds}s`;
    if (minutes === 1) return '1m';
    return `${minutes}m`;
  }

  _showWarning(message) {
    const banner = document.getElementById('warning-banner');
    if (banner) {
      banner.style.display = 'block';
      banner.innerHTML = `⚠️ ${message}`;
      banner.className = 'banner-warning';
    }
  }

  _showErrorBanner(message, level) {
    const banner = document.getElementById('error-banner');
    if (banner) {
      banner.style.display = 'block';
      banner.innerHTML = `🔴 ${message}`;
      banner.className = `banner-${level}`;
    }
  }

  isDataStale() {
    if (!this.lastSuccessTime) return true;
    const age = Date.now() - this.lastSuccessTime;
    return age > this.CACHE_DURATION_MS;
  }

  reset() {
    this.failureCount = 0;
    this.isOffline = false;
    this.lastSuccessTime = null;
  }
}

const offlineManager = new OfflineManager();
```

**Polling with Offline Support:**
```javascript
class ResilientPollingManager {
  constructor(api, offlineManager, options = {}) {
    this.api = api;
    this.offlineManager = offlineManager;
    this.interval = options.interval || 5000;
    this.isRunning = false;
    this.timerId = null;
  }

  start() {
    if (this.isRunning) return;
    this.isRunning = true;

    // Initial fetch immediately
    this._poll();
  }

  async _poll() {
    try {
      const coils = await this.offlineManager.fetchWithOfflineFallback(
        () => this.api.getCoils(),
        'inventory'
      );

      // Update UI
      this._updateUI(coils);

      // Cache for offline use
      StorageManager.setInventory(this.offlineManager._coilsToMap(coils));
    } catch (error) {
      // Handled by offlineManager
      console.error('Poll failed:', error);
    } finally {
      if (this.isRunning) {
        this.timerId = setTimeout(() => this._poll(), this.interval);
      }
    }
  }

  _updateUI(coils) {
    // Update grid with new data
    coils.forEach(coil => {
      GridPage.updateCoilQuantity(coil.id, coil.quantity);
    });
  }

  stop() {
    this.isRunning = false;
    if (this.timerId) clearTimeout(this.timerId);
  }
}
```

**UI Elements for Offline Status:**
```html
<!-- index.html -->
<div id="connection-status" class="status-badge status-online">
  Online
</div>

<div id="warning-banner" class="banner banner-warning" style="display: none;">
  ⚠️ Connection issue. Retrying...
</div>

<div id="error-banner" class="banner banner-error" style="display: none;">
  🔴 Backend unreachable. Showing cached data.
</div>
```

**CSS for Offline Indicators:**
```css
/* styles/offline.css */
.status-badge {
  position: fixed;
  top: 10px;
  right: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  z-index: 1000;
}

.status-online {
  background-color: #4caf50;
  color: white;
}

.status-offline {
  background-color: #ff9800;
  color: white;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.7; }
}

.banner {
  position: fixed;
  top: 50px;
  left: 0;
  right: 0;
  padding: 12px 20px;
  text-align: center;
  font-size: 14px;
  z-index: 999;
}

.banner-warning {
  background-color: #fff9c4;
  color: #f57f17;
  border-bottom: 2px solid #f57f17;
}

.banner-error {
  background-color: #ffebee;
  color: #c62828;
  border-bottom: 2px solid #c62828;
}
```

**Disabling Operations When Offline:**
```javascript
// When offline, disable refill/admin buttons
function updateUIForOfflineState(isOffline) {
  const refillBtn = document.getElementById('refill-button');
  const adminBtn = document.getElementById('admin-button');
  const deleteBtn = document.getElementById('delete-button');

  if (isOffline) {
    refillBtn.disabled = true;
    adminBtn.disabled = true;
    deleteBtn.disabled = true;
    refillBtn.title = 'Unavailable when offline';
    adminBtn.title = 'Unavailable when offline';
    deleteBtn.title = 'Unavailable when offline';
  } else {
    refillBtn.disabled = false;
    adminBtn.disabled = false;
    deleteBtn.disabled = false;
    refillBtn.title = '';
    adminBtn.title = '';
    deleteBtn.title = '';
  }
}

// Watch offline state
AppState.subscribe((state) => {
  updateUIForOfflineState(offlineManager.isOffline);
});
```

**Cache Validation on App Start:**
```javascript
// On app initialization
async function initializeApp() {
  // Check if cached data is too old
  if (offlineManager.isDataStale()) {
    console.warn('Cached inventory is stale. Forcing refresh.');
    offlineManager.reset();
  }

  // Try to fetch fresh data
  try {
    const machines = await api.getMachines();
    StorageManager.setMachines(machines);
  } catch (error) {
    // Fall back to cached machines
    const cached = StorageManager.getMachines();
    if (cached.length === 0) {
      showErrorBanner('Cannot load machines and no cache available');
      return;
    }
    console.warn('Using cached machines list');
  }

  startApp();
}
```

---

## 8. Multi-Machine Switching

### Decision

**Pause polling on current machine, cancel pending refills, switch context, resume polling on new machine. Show confirmation dialog if operation in progress.**

### Rationale

1. **Prevent Data Corruption**: Don't refill machine A while viewing machine B's inventory.

2. **Clear UX**: "Are you sure? Refill in progress on Machine A" dialog prevents accidental switches mid-operation.

3. **State Isolation**: Each machine gets its own polling interval and API client context.

4. **Recovery on Switch**: If refill fails mid-switch, user sees error clearly on original machine.

5. **Operator Safety**: Dual-machine usage is rare; graceful degradation (pause operations) is acceptable.

### Alternatives Considered

| Approach | Issues | Status |
|----------|--------|--------|
| **Pause polling, cancel operations, switch** | ✅ **Chosen** | Simple, safe. Operator loses a few refill/readings but data stays consistent. |
| **Queue operations for after switch** | ⚠️ Complex | "Refill A, then switch, then refill B" queue logic is brittle. Easy to lose operations. |
| **Allow parallel operations** | ❌ Rejected | Operator refills machine A stock on display B. Confusion and errors. |
| **Lock machine during refill** | ✅ Good add-on | Prevent switching until refill completes. Better UX. |
| **Sync state across machines** | ⚠️ Overkill | Cache each machine's inventory separately. Added complexity. Skip v1. |

### Implementation Notes

**Operation State Manager:**
```javascript
// js/operations.js
class OperationManager {
  static OPERATIONS = {
    REFILL: 'refill',
    TRANSACTION: 'transaction',
    ADMIN_UPDATE: 'admin_update'
  };

  constructor(api) {
    this.api = api;
    this.currentOperation = null;
    this.operationStartTime = null;
    this.listeners = [];
  }

  async startRefill(coilId) {
    if (this.currentOperation) {
      throw new Error(`Cannot start refill: ${this.currentOperation} in progress`);
    }

    this.currentOperation = this.OPERATIONS.REFILL;
    this.operationStartTime = Date.now();
    this._notify({ status: 'started', operation: this.OPERATIONS.REFILL });

    try {
      const result = await this.api.refillCoil(coilId);
      this._notify({ status: 'completed', operation: this.OPERATIONS.REFILL, data: result });
      return result;
    } catch (error) {
      this._notify({ status: 'failed', operation: this.OPERATIONS.REFILL, error });
      throw error;
    } finally {
      this.currentOperation = null;
      this.operationStartTime = null;
    }
  }

  canSwitchMachine() {
    return !this.currentOperation;
  }

  getOperationStatus() {
    if (!this.currentOperation) return null;

    const elapsed = Date.now() - this.operationStartTime;
    return {
      operation: this.currentOperation,
      elapsedSeconds: Math.round(elapsed / 1000)
    };
  }

  subscribe(listener) {
    this.listeners.push(listener);
    return () => this.listeners.splice(this.listeners.indexOf(listener), 1);
  }

  _notify(event) {
    this.listeners.forEach(l => l(event));
  }
}

const opManager = new OperationManager(api);
```

**Machine Switcher with Guards:**
```javascript
// js/pages/machine-selector.js
class MachineSwitcher {
  static async switchMachine(machineId) {
    // Check if operation in progress
    if (!opManager.canSwitchMachine()) {
      const status = opManager.getOperationStatus();
      const confirmed = await this._showConfirmDialog(
        `${status.operation} in progress on current machine (${status.elapsedSeconds}s elapsed). Switch anyway?`,
        'warning'
      );

      if (!confirmed) {
        console.log('Switch cancelled by user');
        return;
      }
    }

    // Pause current polling
    const currentMachine = AppState.get('currentMachine');
    if (currentMachine) {
      this._pausePolling(currentMachine.id);
    }

    // Switch machine
    const newMachine = this._findMachine(machineId);
    AppState.set('currentMachine', newMachine);

    // Update storage
    const uiState = StorageManager.getUIState();
    uiState.currentMachine = machineId;
    StorageManager.setUIState(uiState);

    // Resume polling on new machine
    this._startPolling(newMachine.id);

    // Show success
    this._showToast(`Switched to ${newMachine.name}`);
  }

  static _pausePolling(machineId) {
    const poller = this._getPoller(machineId);
    if (poller) {
      poller.stop();
      console.log(`Polling paused for ${machineId}`);
    }
  }

  static _startPolling(machineId) {
    const machine = AppState.get('currentMachine');
    const api = new MachineAPI(machine);

    const poller = new ResilientPollingManager(api, offlineManager);
    poller.start();

    // Store poller reference for later pause
    this._pollers[machineId] = poller;
    console.log(`Polling started for ${machineId}`);
  }

  static _showConfirmDialog(message, level) {
    return new Promise((resolve) => {
      const dialog = document.createElement('div');
      dialog.className = `confirm-dialog confirm-${level}`;
      dialog.innerHTML = `
        <div class="dialog-content">
          <p>${message}</p>
          <div class="dialog-buttons">
            <button class="btn-cancel">Cancel</button>
            <button class="btn-confirm">Proceed</button>
          </div>
        </div>
      `;

      document.body.appendChild(dialog);

      dialog.querySelector('.btn-cancel').addEventListener('click', () => {
        dialog.remove();
        resolve(false);
      });

      dialog.querySelector('.btn-confirm').addEventListener('click', () => {
        dialog.remove();
        resolve(true);
      });
    });
  }

  static _findMachine(machineId) {
    const machines = StorageManager.getMachines();
    return machines.find(m => m.id === machineId);
  }

  static _getPoller(machineId) {
    return this._pollers[machineId];
  }

  static _pollers = {};

  static _showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast toast-success';
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.animation = 'slideOut 0.3s ease-out';
      setTimeout(() => toast.remove(), 300);
    }, 2000);
  }
}
```

**Refill Operation with Machine Lock:**
```javascript
class SafeRefillOperation {
  static async refillCoil(coilId) {
    const machine = AppState.get('currentMachine');

    // Lock machine for refill
    this._lockMachine(machine.id);

    try {
      // Show progress
      const progressEl = this._showProgress(`Refilling ${coilId}...`);

      const result = await opManager.startRefill(coilId);

      // Update UI
      GridPage.updateCoilQuantity(coilId, result.newQuantity);
      this._showToast(`Refilled ${coilId} to ${result.newQuantity}`);

      return result;
    } catch (error) {
      this._showError(`Refill failed: ${error.message}`);
      throw error;
    } finally {
      this._unlockMachine(machine.id);
    }
  }

  static _lockMachine(machineId) {
    const btn = document.getElementById(`machine-${machineId}`);
    if (btn) {
      btn.disabled = true;
      btn.setAttribute('title', 'Operation in progress. Cannot switch.');
      btn.classList.add('locked');
    }
  }

  static _unlockMachine(machineId) {
    const btn = document.getElementById(`machine-${machineId}`);
    if (btn) {
      btn.disabled = false;
      btn.removeAttribute('title');
      btn.classList.remove('locked');
    }
  }

  static _showProgress(message) {
    const el = document.getElementById('operation-progress');
    el.textContent = message;
    el.style.display = 'block';
    return el;
  }

  static _showError(message) {
    document.getElementById('error-banner').innerHTML = `❌ ${message}`;
    document.getElementById('error-banner').style.display = 'block';
  }

  static _showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast toast-success';
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
  }
}
```

**State Recovery on Crash:**
```javascript
// On app restart, check for incomplete operations
async function recoverFromCrash() {
  const uiState = StorageManager.getUIState();

  // If app crashed during refill, check current machine state
  if (uiState.lastOperation?.status === 'in_progress') {
    console.warn('Incomplete operation detected:', uiState.lastOperation);

    const machine = StorageManager.getMachines()
      .find(m => m.id === uiState.currentMachine);

    if (machine) {
      try {
        // Check if operation actually completed on backend
        const api = new MachineAPI(machine);
        const result = await api.checkOperationStatus(uiState.lastOperation.id);

        if (result.status === 'completed') {
          console.log('Operation completed successfully');
        } else if (result.status === 'failed') {
          console.error('Operation failed:', result.error);
          showErrorBanner(`Previous refill failed: ${result.error}`);
        }
      } catch (error) {
        console.error('Cannot verify operation status:', error);
        showWarningBanner('Previous operation status unknown. Check inventory.');
      }
    }

    // Clear incomplete operation flag
    uiState.lastOperation = null;
    StorageManager.setUIState(uiState);
  }
}

// Track operation in progress
opManager.subscribe((event) => {
  const uiState = StorageManager.getUIState();
  uiState.lastOperation = {
    id: event.operationId,
    status: event.status,
    timestamp: Date.now()
  };
  StorageManager.setUIState(uiState);
});
```

**CSS for Locked Machines:**
```css
/* styles/operations.css */
.machine-button.locked {
  opacity: 0.6;
  cursor: not-allowed;
  border: 2px solid #ff9800;
  background-color: #fff8e1;
}

.confirm-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  padding: 24px;
  max-width: 400px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.confirm-warning .dialog-content {
  border-left: 4px solid #ff9800;
}

.dialog-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  justify-content: flex-end;
}

.btn-cancel, .btn-confirm {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #333;
}

.btn-confirm {
  background-color: #2196f3;
  color: white;
}

.toast {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 4px;
  background-color: #4caf50;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  animation: slideIn 0.3s ease-out;
  z-index: 1000;
}

.toast-success {
  background-color: #4caf50;
}

.toast-error {
  background-color: #f44336;
}

@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideOut {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(400px);
    opacity: 0;
  }
}
```

---

## Summary: Technology Stack

| Decision | Technology | Rationale |
|----------|-----------|-----------|
| **Framework** | Vanilla JavaScript (ES6+) | No build infrastructure, simple state mgmt, direct browser access |
| **CORS** | Go backend middleware + localhost:3000 portal | ADB port forwarding enables direct backend calls |
| **Polling** | `setInterval` (5s) + debounce | Non-visual task; simple, reliable error recovery |
| **Storage** | `localStorage` (JSON schema) | <100KB data, offline cache, machine configs persist |
| **Testing** | Jest (unit) + MSW (mocks) + Playwright (E2E) | 70% unit / 20% integration / 10% E2E coverage |
| **Grid Rendering** | CSS Grid + HTML table + DOM updates | <100ms render time, fully accessible, responsive |
| **Offline** | Cache 30 min stale data, pause polling after 3 failures | Graceful degradation, operator awareness |
| **Machine Switch** | Pause polling, cancel operations, lock UI during refill | Prevent data corruption, clear UX |

---

## References & Sources

- [React vs Vue vs Svelte 2025 Comparison](https://www.frontendtools.tech/blog/best-frontend-frameworks-2025-comparison)
- [Vue vs React: Complete 2025 Guide](https://alokai.com/blog/vue-vs-react)
- [ADB Port Forwarding Guide](https://til.magmalabs.io/posts/d2a0b9bbc2-you-can-forwardreverse-ports-on-android-device-using-adb)
- [Chrome DevTools Port Forwarding](https://developer.chrome.com/docs/devtools/remote-debugging/local-server)
- [requestAnimationFrame vs setInterval (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame)
- [Server-Sent Events vs WebSockets 2025](https://dev.to/haraf/server-sent-events-sse-vs-websockets-vs-long-polling-whats-best-in-2025-5ep8)
- [localStorage Best Practices](https://blog.logrocket.com/localstorage-javascript-complete-guide/)
- [Client-Side Storage: LocalStorage vs IndexedDB](https://www.frontendtools.tech/blog/client-side-storage-guide-localstorage-sessionstorage-indexeddb)
- [Testing Strategy: Unit, Integration, E2E (2025)](https://talent500.com/blog/fullstack-app-testing-unit-integration-e2e-2025/)
- [Mock Service Worker Guide](https://mswjs.io/)
- [CSS Grid vs HTML Tables for Data](https://uiverse.io/blog/html-tables-vs-css-grid-which-layout-option-is-best)
- [Graceful Degradation Patterns](https://blog.logrocket.com/guide-graceful-degradation-web-development/)
- [REST API Caching Strategies](https://restfulapi.net/caching/)
- [Cache-Control Headers (MDN)](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control)

---

**Document Status:** Complete
**Last Updated:** December 28, 2025
**Review Status:** Ready for development team review
