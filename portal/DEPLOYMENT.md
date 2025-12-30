# Deployment Guide

## Overview

This guide covers deploying the ZootBox Inventory Portal to a production environment for remote operator access via Tailscale VPN.

## Deployment Architecture

```
┌─────────────────────────────────────────────────┐
│         Tailscale VPN Mesh Network              │
│                                                 │
│  ┌──────────────┐      VPN      ┌────────────┐ │
│  │   Operator   │◄──────────────►│   Tablet   │ │
│  │      PC      │                │  Backend   │ │
│  │              │                │            │ │
│  │  Portal      │   HTTP 8080    │  SQLite    │ │
│  │  (Browser)   │                │  Database  │ │
│  └──────────────┘                └────────────┘ │
│                                                 │
│  Access via Tailscale IP: http://100.x.x.x:3000 │
└─────────────────────────────────────────────────┘
```

## Prerequisites

### Operator PC (Windows 10/11)
- [ ] Tailscale installed and authenticated
- [ ] Chrome browser (version 90+)
- [ ] Python 3.x or Node.js (for serving portal)
- [ ] Network access to Tailscale VPN

### Backend Tablet (Android)
- [ ] Backend server running on port 8080
- [ ] Tailscale installed and authenticated
- [ ] Connected to same Tailscale network as Operator PC

## Deployment Steps

### Step 1: Install Tailscale

**On Operator PC:**
1. Download Tailscale: https://tailscale.com/download/windows
2. Install and sign in with your Tailscale account
3. Note your machine's Tailscale IP (e.g., `100.x.x.x`)
4. Verify connection:
   ```powershell
   tailscale status
   ```

**On Android Tablet:**
1. Install Tailscale from Google Play Store
2. Sign in with same Tailscale account
3. Enable "Run on VPN start" in settings
4. Note the tablet's Tailscale IP (e.g., `100.y.y.y`)

**Verify Connectivity:**
```powershell
# From Operator PC, ping the tablet
ping 100.y.y.y

# Test backend API
curl http://100.y.y.y:8080/health
```

### Step 2: Build Production Version

On your development machine:

```bash
cd portal

# Install dependencies (first time only)
npm install

# Build production bundle
npm run build

# Verify build output
ls dist/
```

The `dist/` folder now contains:
- Minified JavaScript (56% smaller)
- HTML files
- Build metadata
- All console.log statements removed

### Step 3: Deploy to Operator PC

**Option A: Copy via Network Share**
```powershell
# From development machine
xcopy /E /I portal\dist \\OPERATOR-PC\C$\ZootBox\portal
```

**Option B: Copy via USB Drive**
1. Copy entire `dist/` folder to USB drive
2. On Operator PC, copy to: `C:\ZootBox\portal\`

**Option C: Git Clone (if available)**
```bash
git clone <repository-url>
cd portal
npm install
npm run build
```

### Step 4: Configure Backend Endpoint

On Operator PC, edit `dist/index.html` if you need to change default backend URL:

**Method 1: Via Portal UI (Recommended)**
1. Open portal in browser
2. Go to "Machine Settings"
3. Click "Add Machine"
4. Enter backend endpoint: `http://100.y.y.y:8080`
5. Click "Save"

**Method 2: Via LocalStorage (Advanced)**
```javascript
// In browser console
localStorage.setItem('zootbox.machines', JSON.stringify([{
  id: crypto.randomUUID(),
  name: "Main Vending Machine",
  endpointUrl: "http://100.y.y.y:8080",
  status: "offline",
  createdAt: new Date().toISOString()
}]));
location.reload();
```

### Step 5: Start Portal Server

**Option A: Python HTTP Server (Recommended)**
```powershell
cd C:\ZootBox\portal
python -m http.server 3000
```

**Option B: Node.js HTTP Server**
```powershell
cd C:\ZootBox\portal
npx http-server -p 3000
```

**Option C: Production npm script**
```powershell
cd C:\ZootBox\portal
npm run serve:prod
```

### Step 6: Access Portal

1. Open Chrome browser on Operator PC
2. Navigate to: `http://localhost:3000`
3. Portal should load and display inventory grid

**First-Time Setup:**
1. Click "Machine Settings" in sidebar
2. Add backend machine with Tailscale IP
3. Return to "Inventory Map"
4. Verify data loads from backend

### Step 7: Verify Deployment

**Test Checklist:**
- [ ] Portal loads in browser
- [ ] Can add/edit machine configurations
- [ ] Inventory grid displays 10 coils (A1-J1)
- [ ] Status indicator shows "Online" (green)
- [ ] Can edit individual coil inventory
- [ ] "Refill All" button works
- [ ] Auto-refresh updates data every 30 seconds
- [ ] Stale data banner appears when machine offline
- [ ] No console.log output (production build)

**Network Test:**
```powershell
# From Operator PC
curl http://100.y.y.y:8080/health
curl http://100.y.y.y:8080/coils
```

### Step 8: Create Desktop Shortcut (Optional)

Create a shortcut on Operator PC desktop for easy access:

```powershell
# Create shortcut to launch Chrome in app mode
$WshShell = New-Object -comObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$Home\Desktop\ZootBox Portal.lnk")
$Shortcut.TargetPath = "chrome.exe"
$Shortcut.Arguments = "--app=http://localhost:3000"
$Shortcut.Save()
```

## Running as Windows Service (Advanced)

To run the portal server as a background service that starts automatically:

**Using NSSM (Non-Sucking Service Manager):**

1. Download NSSM: https://nssm.cc/download
2. Extract to `C:\Tools\nssm`
3. Install service:
   ```powershell
   C:\Tools\nssm\nssm.exe install ZootBoxPortal "C:\Python3\python.exe" "-m http.server 3000"
   C:\Tools\nssm\nssm.exe set ZootBoxPortal AppDirectory "C:\ZootBox\portal"
   C:\Tools\nssm\nssm.exe set ZootBoxPortal Start SERVICE_AUTO_START
   ```
4. Start service:
   ```powershell
   net start ZootBoxPortal
   ```

## Tailscale Configuration

### Access Control Lists (ACLs)

Restrict portal access to specific users/devices:

```json
{
  "acls": [
    {
      "action": "accept",
      "src": ["operator-pc"],
      "dst": ["tablet-backend:8080"]
    },
    {
      "action": "accept",
      "src": ["operator-pc"],
      "dst": ["operator-pc:3000"]
    }
  ]
}
```

### MagicDNS

Enable MagicDNS for friendly hostnames:
- Instead of: `http://100.y.y.y:8080`
- Use: `http://tablet-backend:8080`

**Enable in Tailscale Admin Console:**
1. Go to DNS settings
2. Enable MagicDNS
3. Devices become accessible via hostname

## Updating the Portal

To deploy updates:

```bash
# On development machine
cd portal
npm run build

# Copy dist/ to Operator PC
xcopy /E /I dist \\OPERATOR-PC\C$\ZootBox\portal

# On Operator PC
# Refresh browser (Ctrl+F5) to clear cache
```

**For zero-downtime updates:**
1. Deploy to temporary directory
2. Test new version
3. Swap directories when ready
4. Refresh browser

## Backup & Recovery

### Backup Machine Configurations

Machine configurations are stored in browser LocalStorage:

**Export:**
```javascript
// In browser console
const backup = {
  machines: localStorage.getItem('zootbox.machines'),
  uiState: localStorage.getItem('zootbox.uiState'),
  metadata: localStorage.getItem('zootbox.metadata'),
  exportDate: new Date().toISOString()
};
console.log(JSON.stringify(backup, null, 2));
// Copy output to safe location
```

**Restore:**
```javascript
// In browser console
const backup = /* paste backup JSON here */;
localStorage.setItem('zootbox.machines', backup.machines);
localStorage.setItem('zootbox.uiState', backup.uiState);
localStorage.setItem('zootbox.metadata', backup.metadata);
location.reload();
```

### Backup Portal Files

```powershell
# Create backup
$date = Get-Date -Format "yyyy-MM-dd"
Compress-Archive -Path "C:\ZootBox\portal" -DestinationPath "C:\Backups\portal-$date.zip"
```

## Troubleshooting

### Portal Won't Load

**Symptoms:** Browser shows "Connection refused" or blank page

**Solutions:**
1. Verify server is running:
   ```powershell
   netstat -ano | findstr :3000
   ```
2. Check firewall isn't blocking port 3000
3. Try different port (e.g., 8000, 8080)
4. Check browser console for errors (F12)

### Can't Connect to Backend

**Symptoms:** Status indicator shows "Offline", no inventory data

**Solutions:**
1. Verify Tailscale is connected:
   ```powershell
   tailscale status
   ```
2. Ping backend tablet:
   ```powershell
   ping 100.y.y.y
   ```
3. Test backend API directly:
   ```powershell
   curl http://100.y.y.y:8080/health
   ```
4. Check backend logs on tablet
5. Verify machine endpoint URL in portal settings

### Stale Data Warning

**Symptoms:** Yellow banner saying "Viewing Offline Data"

**Cause:** Backend machine went offline, portal showing cached data

**Solutions:**
1. Check backend tablet connectivity
2. Restart backend service
3. Verify Tailscale VPN connection
4. Banner will dismiss automatically when connection restored

### Console Errors in Production

**If you see console.log output:**
1. Verify you're using production build (`dist/` folder)
2. Check build-metadata.json for environment
3. Rebuild: `npm run build`

**Enable debug logging temporarily:**
```javascript
localStorage.setItem('DEBUG', 'true');
location.reload();
```

### Permission Denied Errors

**On Windows:**
```powershell
# Run as Administrator
Start-Process powershell -Verb RunAs
cd C:\ZootBox\portal
python -m http.server 3000
```

## Monitoring

### Health Checks

**Portal Health:**
```powershell
curl http://localhost:3000/index.html
```

**Backend Health:**
```powershell
curl http://100.y.y.y:8080/health
```

**Tailscale Status:**
```powershell
tailscale status
```

### Logs

**Portal Server Logs:**
- Python http.server: Logs to console/stdout
- Redirect to file: `python -m http.server 3000 > portal.log 2>&1`

**Browser Console:**
- F12 → Console tab
- Look for errors (red) or warnings (yellow)

## Security Checklist

Before going live:

- [ ] Verify portal only accessible via Tailscale VPN
- [ ] Confirm backend not exposed to public internet
- [ ] Test that only authorized Tailscale users can access
- [ ] Review Tailscale ACLs to restrict access
- [ ] Verify production build strips console.log statements
- [ ] Backup machine configurations
- [ ] Document Tailscale account credentials (securely)
- [ ] Test failover scenarios (offline mode)

## Rollback Procedure

If deployment fails:

1. Stop portal server (Ctrl+C or `net stop ZootBoxPortal`)
2. Restore previous version:
   ```powershell
   rmdir /S /Q C:\ZootBox\portal
   xcopy /E /I C:\Backups\portal-previous C:\ZootBox\portal
   ```
3. Restart server
4. Verify portal functionality
5. Investigate issues in development environment

## Support Contacts

For deployment issues:
- **Technical Issues**: Contact development team
- **Tailscale Issues**: https://tailscale.com/contact/support
- **Network Issues**: Contact network administrator

## References

- [Tailscale Documentation](https://tailscale.com/kb/)
- [Portal README](README.md)
- [Security Documentation](SECURITY.md)
- [NSSM Documentation](https://nssm.cc/usage)
