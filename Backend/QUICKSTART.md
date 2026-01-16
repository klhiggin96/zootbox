# ZootBox Quick Start Guide

## Prerequisites
- Node.js installed (for portal)
- Tailscale VPN connected (for backend access)
- Backend already deployed on tablet

## Start the Portal (Web UI)

```bash
cd c:\dev\portal
npm run dev
```

This starts the portal at **http://localhost:3000** and auto-opens in your browser.

## Backend (API Server)

The backend runs on the Android tablet and is accessible via Tailscale VPN:

**Endpoint**: `http://100.120.168.44:8080`

### Verify Backend is Running

```bash
curl http://100.120.168.44:8080/health
```

Expected response:
```json
{"status":"healthy","uptime_seconds":225,"database_ok":true}
```

### If Backend Needs to be Started (via ADB)

1. Connect tablet via USB
2. Run:
```bash
adb shell "su -c 'cd /data/data/com.termux/files/home/zootbox && DB_PATH=./data/inventory.db HTTP_HOST=0.0.0.0 nohup ./backend > backend.log 2>&1 &'"
```

3. Verify:
```bash
adb shell "ps -A | grep backend"
```

## Portal Configuration

The portal reads the backend URL from localStorage. If connecting to the tablet backend:

1. Open portal in browser
2. Add a machine with endpoint: `http://100.120.168.44:8080`
3. Select that machine as active

Or set default in `portal/js/config.js`:
```js
DEFAULT_BACKEND_URL: 'http://100.120.168.44:8080'
```

## Useful Commands

| Action | Command |
|--------|---------|
| Check backend health | `curl http://100.120.168.44:8080/health` |
| Get all coils | `curl http://100.120.168.44:8080/api/v1/coils` |
| View backend logs | `adb shell "su -c 'tail -f /data/data/com.termux/files/home/zootbox/backend.log'"` |
| Stop backend | `adb shell "su -c 'pkill backend'"` |
| Check ADB connection | `adb devices` |

## Ports Summary

| Service | Port | URL |
|---------|------|-----|
| Portal (local) | 3000 | http://localhost:3000 |
| Backend (tablet) | 8080 | http://100.120.168.44:8080 |
