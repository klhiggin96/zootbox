# Quickstart Guide: ZootBox Inventory Web Portal

**Feature**: 001-inventory-portal
**Date**: 2025-12-28
**Audience**: Developers and operators

---

## Overview

This guide covers:
1. **Developer Setup**: Running the portal locally for development/testing
2. **Machine Configuration**: Adding vending machines to the portal
3. **ADB Port Forwarding**: Connecting to remote tablets
4. **Deployment**: Installing portal on operator's Windows PC
5. **Troubleshooting**: Common issues and solutions

---

## Prerequisites

### For Development
- **Windows PC** (Windows 10/11)
- **Web Browser**: Chrome 90+, Edge 90+, or Firefox 85+
- **Python 3.x** (for local HTTP server) OR any static web server
- **Git** (to clone repository)
- **ADB (Android Debug Bridge)**: For connecting to remote tablets
  - Download: https://developer.android.com/tools/releases/platform-tools
  - Add to PATH: `C:\platform-tools\adb.exe`

### For Deployment
- **Windows PC** (operator's workstation)
- **Web Browser**: Chrome/Edge/Firefox
- **Network Access**: ADB USB connection or VPN to tablet network

---

## Quick Start (5 Minutes)

### Step 1: Clone Repository

```bash
git clone https://github.com/bossmandlow523/zootbox.git
cd zootbox
```

### Step 2: Navigate to Portal Directory

```bash
cd portal
```

### Step 3: Start Local Web Server

**Option A: Python HTTP Server**
```bash
python -m http.server 3000
```

**Option B: Node.js http-server**
```bash
npx http-server -p 3000
```

**Option C: PHP Built-in Server**
```bash
php -S localhost:3000
```

### Step 4: Open Portal in Browser

Navigate to: **http://localhost:3000**

You should see the portal homepage (Inventory Grid page).

### Step 5: Configure a Machine

1. Click "Machine Settings" in the navigation bar
2. Click "Add Machine"
3. Fill in form:
   - **Name**: "Test Machine" (or any name)
   - **Endpoint URL**: http://localhost:8080 (if backend running locally)
4. Click "Save"

**Note**: If backend is NOT running locally, you'll see "Offline" status. That's okay for UI testing.

### Step 6: Test API Connection (Optional)

If you have the backend running locally:

```bash
# In another terminal
cd Backend
./backend

# Verify health
curl http://localhost:8080/health
# Should return: {"status":"healthy","uptime_seconds":123,"database_ok":true}
```

Now refresh the portal - machine status should change to "Online" and inventory grid populates with 100 coils.

---

## Developer Workflow

### Directory Structure

```
portal/
├── index.html              # Inventory Grid page (homepage)
├── machine-settings.html   # Machine configuration
├── jam-management.html     # Jam events page
├── product-links.html      # Product linking page
├── css/
│   ├── main.css            # Global styles
│   ├── grid.css            # 10x10 coil grid
│   ├── navigation.css      # Top nav bar
│   └── forms.css           # Forms & modals
├── js/
│   ├── api/
│   │   ├── client.js       # Fetch API wrapper
│   │   ├── coils.js        # Coil endpoints
│   │   ├── jams.js         # Jam endpoints
│   │   ├── products.js     # Product link endpoints
│   │   └── admin.js        # Admin endpoints
│   ├── components/
│   │   ├── CoilGrid.js     # 10x10 grid rendering
│   │   ├── MachineSelector.js # Dropdown selector
│   │   ├── StatusIndicator.js # Online/offline badge
│   │   └── Modal.js        # Reusable dialogs
│   ├── state/
│   │   ├── machines.js     # LocalStorage wrapper
│   │   ├── inventory.js    # Inventory state
│   │   └── sync.js         # Auto-refresh logic
│   └── utils/
│       ├── validation.js   # Input validation
│       └── formatting.js   # Date/time helpers
├── assets/
│   └── icons/              # SVG icons
└── README.md
```

### Making Changes

1. **Edit Files**: Use any text editor (VS Code, Notepad++, Sublime)
2. **Refresh Browser**: Changes take effect immediately (no build step)
3. **Test**: Click through portal, check browser console for errors
4. **Commit**: `git add . && git commit -m "Update: description"`

### Browser DevTools

**Open DevTools**: Press `F12` or right-click → Inspect

**Useful Tabs**:
- **Console**: See JavaScript errors and `console.log()` output
- **Network**: View API requests/responses (filter by "Fetch/XHR")
- **Application**: Inspect LocalStorage (`zootbox.machines`, `zootbox.uiState`)
- **Elements**: Inspect HTML/CSS for styling issues

**Common Debugging**:
- API call failing? Check Network tab for status code and response
- State not persisting? Check Application → LocalStorage
- UI not updating? Check Console for JavaScript errors

---

## Machine Configuration

### Adding a Machine

#### Local Backend (Development)
```
Name: Local Test Machine
Endpoint URL: http://localhost:8080
```

#### Remote Tablet (via ADB Port Forwarding)
```
Name: Downtown Vending Machine
Endpoint URL: http://localhost:8080
```

**Setup ADB forwarding first** (see next section).

#### Remote Tablet (via VPN/Network IP)
```
Name: Airport Terminal
Endpoint URL: http://192.168.1.100:8080
```

**Requirements**: Windows PC and tablet on same network, or VPN connection established.

### Editing a Machine

1. Click "Machine Settings"
2. Click "Edit" next to machine name
3. Update name or URL
4. Click "Save"

### Deleting a Machine

1. Click "Machine Settings"
2. Click "Delete" next to machine name
3. Confirm deletion
4. Machine configuration removed from LocalStorage

**Note**: Deleting a machine does NOT affect the tablet backend. It only removes the portal's saved configuration.

---

## ADB Port Forwarding (Connecting to Remote Tablets)

### Overview

ADB port forwarding creates a tunnel from your Windows PC's localhost:8080 to the tablet's localhost:8080. This allows the portal (running on your PC) to call the backend API (running on the tablet).

### Prerequisites

1. **Install ADB**: Download platform-tools and add to PATH
2. **Enable USB Debugging on Tablet**:
   - Settings → About Tablet → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable USB Debugging
3. **Connect Tablet via USB**: Use USB cable to connect tablet to PC

### Setup Steps

#### Step 1: Verify ADB Connection

```bash
adb devices
```

**Expected Output**:
```
List of devices attached
b0535a1f9f0f6ce0        device
```

If you see "unauthorized", check tablet screen for USB debugging permission dialog and tap "Allow".

#### Step 2: Forward Port 8080

```bash
adb forward tcp:8080 tcp:8080
```

**Expected Output**:
```
8080
```

This maps your PC's localhost:8080 → tablet's localhost:8080.

#### Step 3: Test Connection

```bash
curl http://localhost:8080/health
```

**Expected Output**:
```json
{"status":"healthy","uptime_seconds":3456,"database_ok":true}
```

If this works, the portal can now connect to the tablet backend!

#### Step 4: Configure Machine in Portal

Add machine with endpoint: `http://localhost:8080`

The portal will connect to your PC's localhost:8080, which ADB forwards to the tablet.

### Managing Multiple Tablets

**Problem**: ADB can only forward one tablet at a time to port 8080.

**Solution**: Use different ports for each tablet.

```bash
# Tablet 1 (downtown machine)
adb -s b0535a1f9f0f6ce0 forward tcp:8080 tcp:8080

# Tablet 2 (airport machine)
adb -s c1646b2g0g1g7df1 forward tcp:8081 tcp:8080
```

**Portal Configuration**:
- Machine 1: `http://localhost:8080`
- Machine 2: `http://localhost:8081`

### Persistent Forwarding

ADB forwarding persists until:
- Tablet disconnected
- ADB server restarted
- Windows rebooted

**Auto-reconnect**: Create a batch script (`connect-tablets.bat`):
```batch
@echo off
adb forward tcp:8080 tcp:8080
adb -s c1646b2g0g1g7df1 forward tcp:8081 tcp:8080
echo Port forwarding established
pause
```

Run this script whenever you reconnect tablets.

### Troubleshooting ADB

**Issue**: `adb devices` shows empty list

**Solutions**:
- Check USB cable (data cable, not charging-only)
- Re-enable USB debugging on tablet
- Restart ADB server: `adb kill-server && adb start-server`
- Try different USB port on PC

**Issue**: `curl http://localhost:8080/health` fails

**Solutions**:
- Check backend is running on tablet: `adb shell ps -A | grep backend`
- Verify port forwarding: `adb forward --list`
- Re-run: `adb forward tcp:8080 tcp:8080`

**Issue**: Portal shows "Offline" despite successful curl

**Solutions**:
- Check browser console for CORS errors
- Verify machine endpoint URL is exactly `http://localhost:8080` (no trailing slash)
- Clear browser cache: Ctrl+Shift+Delete → Clear data

---

## VPN/Network Connection (Alternative to ADB)

### When to Use

- Tablets on private network (not USB-connected)
- Multiple operators accessing same tablets
- Permanent deployment (no USB tethering)

### Requirements

1. **Tablets and PC on Same Network**: E.g., all devices on 192.168.1.0/24 subnet
2. **Tablets Have Static IPs**: Or use DHCP reservations (router assigns same IP every time)
3. **Firewall Rules**: Allow TCP port 8080 from PC to tablets

### Finding Tablet IP Address

**Method 1: ADB**
```bash
adb shell ip addr show wlan0
```

**Method 2: Tablet Settings**
- Settings → About Tablet → Status → IP Address
- Or Settings → Wi-Fi → Connected Network → IP Address

**Example Output**: `192.168.1.100`

### Configuring Machine in Portal

Add machine with endpoint: `http://192.168.1.100:8080`

The portal connects directly to the tablet's IP (no ADB needed).

### VPN Setup (For Remote Access)

If tablets are not on local network, use VPN:

1. **Setup VPN Server**: On tablet network's router or dedicated VPN server
2. **Connect PC to VPN**: Use Windows VPN client
3. **Configure Machine**: Use tablet's VPN IP (e.g., `http://10.8.0.5:8080`)

**Recommended VPN**: WireGuard (lightweight, fast, secure)

---

## Deployment (Operator's PC)

### Option 1: File Copy (Simplest)

1. **Copy Portal Directory**:
   ```
   Copy C:\dev\zootbox\portal\ to C:\ZootBoxPortal\
   ```

2. **Create Desktop Shortcut**:
   - Right-click Desktop → New → Shortcut
   - Target: `C:\ZootBoxPortal\index.html`
   - Name: "ZootBox Inventory Portal"

3. **Double-click Shortcut**: Opens portal in default browser

**Pros**: No server needed, works offline
**Cons**: `file://` origin may have CORS issues with some backends

### Option 2: Local Web Server (Recommended)

1. **Install Python** (if not already installed):
   - Download: https://www.python.org/downloads/
   - Check "Add Python to PATH" during installation

2. **Create Startup Script** (`C:\ZootBoxPortal\start-portal.bat`):
   ```batch
   @echo off
   cd /d C:\ZootBoxPortal
   start http://localhost:3000
   python -m http.server 3000
   ```

3. **Create Desktop Shortcut**:
   - Target: `C:\ZootBoxPortal\start-portal.bat`
   - Name: "ZootBox Inventory Portal"

4. **Double-click Shortcut**: Starts server and opens browser

**Pros**: Reliable CORS handling, fast performance
**Cons**: Requires Python installation

### Option 3: Portable Web Server (No Dependencies)

1. **Download Portable Server**:
   - **Option A**: Mongoose (https://mongoose.ws) - single .exe file
   - **Option B**: nginx for Windows (https://nginx.org/en/download.html)

2. **Extract to** `C:\ZootBoxPortal\server\`

3. **Configure Server** to serve `C:\ZootBoxPortal\` on port 3000

4. **Create Shortcut** to start server

**Pros**: No Python needed, professional server
**Cons**: More complex setup

### Choosing Default Browser

Portal works best in Chrome or Edge. To set default:

1. Windows Settings → Apps → Default Apps
2. Web Browser → Choose Chrome or Edge

---

## Operator Usage Guide

### Starting the Portal

1. **Connect Tablets** (if using ADB):
   - Plug in USB cables
   - Run `connect-tablets.bat` script
   - Wait for "Port forwarding established" message

2. **Launch Portal**:
   - Double-click "ZootBox Inventory Portal" desktop shortcut
   - Portal opens in browser at http://localhost:3000

3. **Select Machine**:
   - Click machine dropdown in top-left corner
   - Choose desired machine (e.g., "Downtown Location")
   - Wait for inventory grid to load (1-2 seconds)

### Monitoring Inventory

- **Green cells**: Normal stock (3-10 units)
- **Orange cells**: Low stock (1-2 units) - restock soon
- **Gray cells**: Empty (0 units) - restock immediately
- **Red cells**: Jammed - requires physical maintenance

**Auto-Refresh**: Grid updates every 5 seconds automatically.

### Performing Bulk Refill

1. **Navigate to Inventory page** (default home page)
2. Click "Refill All Coils" button (top-right)
3. Confirm dialog: "Set all 100 coils to inventory=10?"
4. Click "Yes"
5. Wait 2-5 seconds for operation to complete
6. Success message: "100 coils refilled successfully"
7. Grid updates to show all coils at "10"

### Manual Inventory Adjustment

1. Click on a coil cell (e.g., "A5")
2. Modal dialog opens showing current inventory
3. Enter new inventory (0-10)
4. Click "Save"
5. Coil cell updates immediately

**Use Cases**:
- Damaged product removed: Decrease inventory
- Manual sale (cash payment): Decrease inventory
- Inventory audit correction: Set to actual physical count

### Resolving Jams

1. Click "Jam Management" in navigation bar
2. Filter dropdown: "Open Jams Only"
3. Locate jam in table (shows coil ID and timestamp)
4. **Physically clear the jam** on the vending machine
5. Click "Resolve" button for that jam
6. Jam moves to "Resolved" status
7. Coil status changes to "Available" on Inventory page

### Managing Product Links

1. Click "Product Links" in navigation bar
2. View existing links (SKU → coil IDs)

**To Create Link**:
1. Click "Create Product Link"
2. Enter Product SKU (e.g., "COKE-001")
3. Select coil IDs from multi-select dropdown (e.g., A1, A2, A3)
4. Click "Save"
5. Link appears in table

**To Delete Link**:
1. Click "Delete" next to link in table
2. Confirm deletion
3. Link removed, coils are now independent

---

## Troubleshooting

### Portal Shows "Offline" for All Machines

**Possible Causes**:
1. Backend not running on tablets
2. ADB port forwarding not established
3. Network connectivity issue
4. Incorrect endpoint URLs

**Solutions**:
1. Check backend status:
   ```bash
   adb shell ps -A | grep backend
   ```
   If empty, backend is not running. Restart backend on tablet.

2. Re-establish port forwarding:
   ```bash
   adb forward tcp:8080 tcp:8080
   ```

3. Test connectivity:
   ```bash
   curl http://localhost:8080/health
   ```
   If fails, check firewall/network settings.

4. Verify endpoint URLs:
   - Machine Settings page
   - Ensure URLs match forwarded ports (e.g., http://localhost:8080)

---

### Inventory Grid Not Refreshing

**Possible Causes**:
1. Auto-refresh disabled
2. JavaScript error blocking updates
3. Backend returned error

**Solutions**:
1. Check refresh interval:
   - Machine Settings → Refresh Interval should be 5 seconds
   - If 0, change to 5

2. Check browser console (F12 → Console tab):
   - Look for red error messages
   - Common error: `Failed to fetch`
   - If present, backend connectivity issue (see "Offline" troubleshooting)

3. Manual refresh:
   - Click browser refresh button (F5)
   - If grid loads, auto-refresh is working

---

### "Coil was updated by another user" Error

**Cause**: Two operators edited same coil simultaneously (optimistic locking conflict).

**Solution**:
1. Click "Refresh" button in error dialog
2. View updated inventory value
3. Re-enter desired value if still needed
4. Save again

**Prevention**: Use bulk refill instead of manual edits when possible.

---

### LocalStorage Full Error

**Cause**: Browser storage limit exceeded (rare, requires 5MB+ data).

**Solutions**:
1. Delete old machines:
   - Machine Settings → Delete unused machines

2. Clear browser cache:
   - Settings → Privacy → Clear browsing data
   - Check "Cookies and site data"
   - Click "Clear data"

3. Export/import machine configs:
   - Machine Settings → Export to JSON file
   - Save file as backup
   - Clear LocalStorage
   - Re-import later if needed

---

### CORS Error in Console

**Error Message**:
```
Access to fetch at 'http://localhost:8080/api/v1/coils' from origin 'file://' has been blocked by CORS policy
```

**Cause**: Portal served from `file://` instead of `http://localhost:3000`.

**Solution**: Use Option 2 deployment (local web server) instead of Option 1 (file copy).

**Quick Fix**:
1. Open command prompt in portal directory
2. Run: `python -m http.server 3000`
3. Navigate to: http://localhost:3000
4. CORS error should disappear

---

### ADB "Offline" Device Status

**Error**: `adb devices` shows:
```
b0535a1f9f0f6ce0        offline
```

**Solutions**:
1. Unplug and replug USB cable
2. Restart ADB: `adb kill-server && adb start-server`
3. Check tablet screen for "Allow USB debugging" prompt
4. Try different USB port or cable

---

## Performance Optimization

### Slow Grid Rendering (>3 seconds)

**Solutions**:
1. Reduce refresh interval: Machine Settings → Refresh Interval: 10 seconds
2. Filter grid: Show only low-stock or jammed coils
3. Close other browser tabs (frees memory)
4. Upgrade browser to latest version

### High Memory Usage

**Check Memory**:
- Chrome: Settings → More tools → Task manager
- Look for "Tab: ZootBox Portal" - should be <100MB

**Solutions**:
1. Close and reopen portal (clears session cache)
2. Reduce number of configured machines (each machine adds 5-10KB)
3. Disable browser extensions (some extensions leak memory)

---

## Updating the Portal

### Minor Updates (Bug Fixes)

1. **Backup LocalStorage**:
   - Machine Settings → Export to JSON
   - Save `machines-backup.json`

2. **Pull Latest Code**:
   ```bash
   cd C:\dev\zootbox
   git pull origin main
   ```

3. **Copy Updated Files**:
   ```bash
   xcopy /E /Y portal C:\ZootBoxPortal
   ```

4. **Refresh Browser**:
   - Ctrl+Shift+R (hard refresh)
   - Clears cached JavaScript

5. **Verify**:
   - Check Machine Settings → machines still present
   - If missing, re-import from `machines-backup.json`

### Major Updates (New Features)

Follow same steps as minor updates, but also:

1. **Read CHANGELOG.md**: Check for breaking changes
2. **Test on Development PC**: Before deploying to operator's PC
3. **Backup Entire Portal Directory**: In case rollback needed

---

## Best Practices

### For Operators

1. **Add Machines Once**: Configure all machines in Machine Settings on first use
2. **Use Descriptive Names**: "Downtown Location" instead of "Machine 1"
3. **Regular Bulk Refills**: Use "Refill All" after route visits (faster than 100 manual edits)
4. **Monitor Low Stock**: Check orange cells daily, restock before empty
5. **Resolve Jams Promptly**: Jammed coils don't vend - fix and resolve ASAP
6. **Export Configs Weekly**: Machine Settings → Export → save backup

### For Developers

1. **Test Locally First**: Run portal on localhost:3000 before deploying
2. **Check Browser Console**: Look for JavaScript errors after every change
3. **Validate API Responses**: Use Network tab to verify JSON format
4. **Follow Coding Style**: See `portal/README.md` for conventions
5. **Test Offline Behavior**: Disconnect backend, ensure portal shows cached data
6. **Test Multi-Machine**: Add 3+ machines, switch between them rapidly

---

## Security Notes

- **No Authentication**: Portal relies on network-level security (localhost/ADB/VPN)
- **LocalStorage Visible**: Anyone with PC access can view machine configs (no sensitive data)
- **Backend Exposed**: Tablets must bind to localhost:8080 only (not 0.0.0.0)
- **HTTPS Not Required**: All traffic is on local network (no encryption needed)

**For Production**: If remote internet access needed, add VPN or reverse proxy with authentication.

---

## Support & Documentation

- **Portal README**: `portal/README.md`
- **API Documentation**: `specs/001-inventory-portal/contracts/api-endpoints.md`
- **Data Model**: `specs/001-inventory-portal/data-model.md`
- **Backend Source**: `Backend/internal/api/router.go`
- **Issues**: https://github.com/bossmandlow523/zootbox/issues

---

## Next Steps

1. ✅ **Complete Setup**: Follow Quick Start section
2. ✅ **Configure Machines**: Add your first machine in Machine Settings
3. ✅ **Test Connectivity**: Verify "Online" status and grid loads
4. ✅ **Explore Features**: Try bulk refill, manual edit, jam resolution
5. ✅ **Deploy to Operator**: Copy portal to production PC

**You're ready to use the ZootBox Inventory Portal!** 🎉
