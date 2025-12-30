# ZootBox Inventory Web Portal

A web-based inventory management portal for ZootBox vending machine operators to remotely monitor and manage multiple machines from a Windows desktop computer.

## Features

- **Real-Time Inventory Monitoring**: Display 10 coils per machine in a single-column grid (A1-J1) with 5-second auto-refresh
- **Multi-Machine Management**: Switch between and manage multiple vending machines from a single portal
- **Bulk Refill Operations**: Set all 10 coils to maximum inventory (10 units) with one click
- **Manual Inventory Adjustment**: Edit individual coil inventory levels (0-10)
- **Jam Event Resolution**: View and resolve jam events to restore coil availability
- **Product Link Configuration**: Link multiple coils to a single product SKU for automatic coil selection

## Quick Start

### 1. Start Local Web Server

```bash
# Navigate to portal directory
cd portal

# Option A: Python HTTP Server
python -m http.server 3000

# Option B: Node.js http-server
npx http-server -p 3000
```

### 2. Open Portal in Browser

Navigate to: **http://localhost:3000**

### 3. Configure a Machine

1. Click "Machine Settings" in the navigation bar
2. Click "Add Machine"
3. Fill in form:
   - **Name**: Your machine name (e.g., "Downtown Location")
   - **Endpoint URL**: Backend API URL (e.g., http://localhost:8080)
4. Click "Save"

### 4. Connect to Remote Tablet Backend

If the backend is running on a tablet, use ADB port forwarding:

```bash
# Connect tablet via USB
adb devices

# Forward port 8080 to tablet
adb forward tcp:8080 tcp:8080

# Verify connection
curl http://localhost:8080/health
```

## Project Structure

```
portal/
├── index.html              # Main entry point (Inventory Grid page)
├── machine-settings.html   # Machine configuration page
├── jam-management.html     # Jam events page
├── product-links.html      # Product linking page
├── css/                    # Stylesheets
│   ├── main.css            # Global styles & layout
│   ├── grid.css            # Coil grid styles (10 rows)
│   ├── navigation.css      # Top nav bar styles
│   └── forms.css           # Forms & modals
├── js/                     # JavaScript modules
│   ├── api/                # Backend API clients
│   │   ├── client.js       # Fetch wrapper (CORS, error handling)
│   │   ├── coils.js        # Coil API endpoints
│   │   ├── jams.js         # Jam event API endpoints
│   │   ├── products.js     # Product link API endpoints
│   │   └── admin.js        # Admin API endpoints
│   ├── components/         # UI components
│   │   ├── CoilGrid.js     # Coil grid component (10 rows)
│   │   ├── MachineSelector.js  # Machine dropdown
│   │   ├── StatusIndicator.js  # Online/offline badge
│   │   └── Modal.js        # Reusable modal dialog
│   ├── state/              # State management
│   │   ├── machines.js     # Machine config (LocalStorage)
│   │   ├── inventory.js    # Inventory cache (in-memory)
│   │   └── sync.js         # Auto-refresh & polling logic
│   └── utils/              # Utilities
│       ├── validation.js   # Input validation
│       └── formatting.js   # Date/time formatting
└── assets/
    └── icons/              # SVG icons for coil states
```

## Technology Stack

- **Frontend**: Vanilla JavaScript (ES2020+), HTML5, CSS3 - no framework required
- **Storage**: Browser LocalStorage for machine configurations
- **API**: REST API (existing backend at http://localhost:8080)
- **Deployment**: Static HTML/CSS/JS served locally or from file://

## Browser Compatibility

- Chrome 90+
- Edge 90+
- Firefox 85+
- Minimum screen resolution: 1024px width

## Documentation

For detailed documentation, see:

- **Feature Specification**: `specs/001-inventory-portal/spec.md`
- **Implementation Plan**: `specs/001-inventory-portal/plan.md`
- **API Endpoints**: `specs/001-inventory-portal/contracts/api-endpoints.md`
- **Data Model**: `specs/001-inventory-portal/data-model.md`
- **Quickstart Guide**: `specs/001-inventory-portal/quickstart.md`
- **Implementation Tasks**: `specs/001-inventory-portal/tasks.md`

## Development

### Prerequisites

- Windows 10/11
- Web browser (Chrome/Edge/Firefox)
- Python 3.x or Node.js (for local HTTP server)
- ADB (Android Debug Bridge) for remote tablet connections

### Running Tests

Manual acceptance testing per user story:

- **US1**: Load portal, verify all 10 coils displayed with inventory counts
- **US2**: Click "Refill All", verify all 10 coils set to inventory=10
- **US3**: Edit coil A5 inventory to 7, verify update persists
- **US4**: Resolve open jam, verify coil status changes to available
- **US5**: Create product link, verify multiple coils linked to SKU

### Deployment

Copy the entire `portal/` directory to the operator's Windows PC and open `index.html` in a browser, or serve via any static web server.

## Security

### Production Deployment Model

This portal is designed for deployment in a **secure, VPN-only environment**:

- **Network Security**: Tailscale VPN provides authentication and access control
- **No Public Internet**: Portal is NOT accessible from public internet
- **Single Operator**: Designed for single-user operation on trusted PC

### Security Features

- ✅ **XSS Protection**: All user inputs HTML-escaped to prevent script injection
- ✅ **Production Build**: Console logging stripped, code minified
- ✅ **Input Validation**: All form inputs validated before processing
- ✅ **CORS**: Backend restricts requests to localhost origins

### Security Documentation

For comprehensive security information, see **[SECURITY.md](SECURITY.md)**:
- Network security model
- Authentication design decisions
- XSS protections implemented
- Data storage security
- Known limitations and risks
- Deployment security checklist

### Quick Security Notes

- **No Authentication**: Portal relies on network-level security (Tailscale VPN)
- **CORS**: Backend restricts requests from localhost origins only
- **Data Storage**: LocalStorage contains only machine names and endpoint URLs (no sensitive data)
- **Production Mode**: Run `npm run build` to create production bundle with security hardening

## Support

For issues or questions, refer to the documentation in `specs/001-inventory-portal/` or contact the development team.

## License

Proprietary - ZootBox Vending System
