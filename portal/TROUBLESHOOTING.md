# Troubleshooting Guide

## Quick Diagnostics

Run these commands to quickly identify common issues:

```powershell
# Check if portal server is running
netstat -ano | findstr :3000

# Check Tailscale VPN status
tailscale status

# Test backend connectivity
curl http://100.y.y.y:8080/health

# Check browser console for errors
# Open Chrome → F12 → Console tab
```

## Common Issues

### 1. Portal Won't Load in Browser

**Symptoms:**
- Browser shows "This site can't be reached"
- "Connection refused" error
- Blank white page

**Diagnosis:**
```powershell
# Check if server is running on port 3000
netstat -ano | findstr :3000
# Should show: TCP    0.0.0.0:3000    0.0.0.0:0    LISTENING    [PID]
```

**Solutions:**

**A. Server Not Running**
```powershell
# Start the server
cd C:\ZootBox\portal
python -m http.server 3000
```

**B. Port Already in Use**
```powershell
# Find what's using port 3000
netstat -ano | findstr :3000

# Kill the process (replace [PID] with actual process ID)
taskkill /PID [PID] /F

# Or use a different port
python -m http.server 8000
# Then access: http://localhost:8000
```

**C. Firewall Blocking Port**
```powershell
# Add firewall rule (Run as Administrator)
netsh advfirewall firewall add rule name="ZootBox Portal" dir=in action=allow protocol=TCP localport=3000
```

**D. Wrong Directory**
```powershell
# Verify you're in the correct directory
cd C:\ZootBox\portal
dir
# Should see: index.html, js/, build-metadata.json, etc.
```

### 2. Can't Connect to Backend

**Symptoms:**
- Status indicator shows red "Offline"
- Inventory grid empty or shows "No machine configured"
- Toast notifications: "Failed to fetch inventory"
- Stale data banner appears immediately

**Diagnosis:**
```powershell
# Step 1: Check Tailscale VPN
tailscale status
# Should show both Operator PC and Tablet online

# Step 2: Ping backend
ping 100.y.y.y
# Replace 100.y.y.y with your tablet's Tailscale IP

# Step 3: Test backend API
curl http://100.y.y.y:8080/health
# Should return: {"status":"ok","timestamp":"..."}
```

**Solutions:**

**A. Tailscale Not Connected**
```powershell
# Check Tailscale status
tailscale status

# If disconnected, reconnect
tailscale up

# Verify both machines show online
tailscale status | findstr "online"
```

**B. Backend Server Not Running on Tablet**
1. On Android tablet, check if backend app is running
2. Look for ZootBox app in recent apps
3. If not running, launch the app
4. Verify backend started (check app logs)

**C. Wrong Backend URL Configured**
1. Open portal: `http://localhost:3000`
2. Go to "Machine Settings" (sidebar)
3. Check endpoint URL (should be `http://100.y.y.y:8080`)
4. Edit if incorrect
5. Return to "Inventory Map"

**D. Backend API Crashed**
1. Restart backend app on tablet
2. Check tablet logs for errors:
   ```bash
   adb logcat | grep ZootBox
   ```
3. If database locked, reboot tablet

**E. Network Firewall Blocking Traffic**
1. Check Tailscale ACLs (admin console)
2. Verify operator PC can reach tablet on port 8080
3. Temporarily disable Windows Defender Firewall to test:
   ```powershell
   # Disable (for testing only!)
   netsh advfirewall set allprofiles state off

   # Re-enable after testing
   netsh advfirewall set allprofiles state on
   ```

### 3. Stale Data Banner Won't Dismiss

**Symptoms:**
- Yellow banner at top says "Viewing Offline Data"
- Banner reappears after dismissing
- Data not updating

**Diagnosis:**
```powershell
# Check if backend is reachable
curl http://100.y.y.y:8080/coils
# Should return JSON with coil data

# Check browser console (F12 → Console)
# Look for errors like "Failed to fetch"
```

**Solutions:**

**A. Backend Actually Offline**
1. Verify backend is running (see issue #2)
2. Once backend is back online, banner will auto-dismiss within 30 seconds
3. Or manually dismiss and refresh (Ctrl+F5)

**B. Auto-Refresh Disabled**
1. Check browser console for errors
2. Restart portal server
3. Hard refresh browser (Ctrl+Shift+R)

**C. Cached Offline State**
```javascript
// In browser console (F12):
localStorage.clear();
location.reload();
```

### 4. Inventory Data Not Updating

**Symptoms:**
- Grid shows old data
- Changes made on tablet don't appear in portal
- Auto-refresh seems stuck

**Diagnosis:**
```powershell
# Check if data is actually changing on backend
curl http://100.y.y.y:8080/coils
# Note inventory values

# Wait 30 seconds, run again
curl http://100.y.y.y:8080/coils
# Compare values
```

**Solutions:**

**A. Auto-Refresh Paused**
1. Check if status indicator shows "Checking..." (yellow)
2. Hard refresh browser (Ctrl+Shift+R)
3. Check browser console for JavaScript errors

**B. Browser Cache**
```javascript
// Clear cache in browser console
localStorage.clear();
sessionStorage.clear();
location.reload(true);
```

**C. Backend Database Locked**
1. On tablet, restart backend app
2. If persistent, reboot tablet:
   ```bash
   adb reboot
   ```

**D. Auto-Refresh Interval Too Long**
```javascript
// Check current refresh interval (browser console)
localStorage.getItem('zootbox.uiState');
// Should show refreshInterval: 30 (seconds)
```

### 5. Can't Edit Coil Inventory

**Symptoms:**
- Clicking coil does nothing
- Edit panel doesn't open
- Save button grayed out or spinning

**Diagnosis:**
1. Open browser console (F12 → Console)
2. Click a coil
3. Look for JavaScript errors

**Solutions:**

**A. JavaScript Error**
1. Check console for red error messages
2. Note the error and file/line number
3. Hard refresh browser (Ctrl+Shift+R)
4. If persists, rebuild portal:
   ```bash
   npm run build
   ```

**B. Backend API Error**
```powershell
# Test inventory update manually
curl -X POST http://100.y.y.y:8080/admin/coils/A1 -H "Content-Type: application/json" -d "{\"inventory\":5}"
```

**C. CORS Error**
- Check backend CORS configuration
- Backend must allow `localhost:3000` origin
- Look for CORS errors in browser console

### 6. Refill All Button Not Working

**Symptoms:**
- Button shows loading spinner forever
- Toast error: "Failed to refill coils"
- Some coils update, others don't

**Diagnosis:**
```powershell
# Test bulk refill manually
curl -X POST http://100.y.y.y:8080/admin/refill-all
```

**Solutions:**

**A. Backend Timeout**
- Backend may be processing slowly
- Wait 10-15 seconds
- Refresh page manually (F5)

**B. Database Transaction Failed**
1. Check backend logs on tablet
2. Restart backend app
3. Try refilling coils individually instead

**C. Network Timeout**
```javascript
// Increase API timeout (browser console)
// Note: This is temporary, resets on page reload
import('./js/api/client.js').then(({apiClient}) => {
  apiClient.defaultTimeout = 30000; // 30 seconds
});
```

### 7. Machine Configuration Lost

**Symptoms:**
- Portal forgets machine settings after browser close
- Need to re-add machine every time
- "No machine configured" message

**Diagnosis:**
```javascript
// Check localStorage in browser console
localStorage.getItem('zootbox.machines');
// Should return JSON array of machines
```

**Solutions:**

**A. LocalStorage Disabled**
1. Check browser settings
2. Enable cookies and site data
3. Chrome: Settings → Privacy → Site Settings → Cookies
   - Allow "localhost" to set cookies

**B. Incognito/Private Mode**
- Don't use incognito mode (localStorage is session-only)
- Use normal browser window

**C. Browser Clearing Data**
1. Check browser settings
2. Disable "Clear data on exit"
3. Chrome: Settings → Privacy → Clear browsing data

**D. Manual Backup/Restore**
```javascript
// Backup (copy this output to a file)
console.log(localStorage.getItem('zootbox.machines'));

// Restore (paste your backup JSON)
localStorage.setItem('zootbox.machines', 'YOUR_BACKUP_JSON_HERE');
location.reload();
```

### 8. Console.log Messages in Production

**Symptoms:**
- See debug messages in browser console
- "console.log" output visible
- Performance degraded

**Diagnosis:**
```javascript
// Check if DEBUG mode enabled
localStorage.getItem('DEBUG');
// Should be null or 'false' in production
```

**Solutions:**

**A. Debug Mode Enabled**
```javascript
// Disable debug mode
localStorage.removeItem('DEBUG');
location.reload();
```

**B. Using Development Build**
1. Verify using production build (dist/ folder)
2. Check build metadata:
   ```javascript
   fetch('/build-metadata.json').then(r => r.json()).then(console.log);
   // Should show environment: "production"
   ```
3. If showing "development", rebuild:
   ```bash
   npm run build
   # Then serve from dist/ folder
   ```

### 9. Slow Performance / Laggy UI

**Symptoms:**
- Grid takes long to load
- Clicking feels delayed
- Scrolling stutters

**Diagnosis:**
1. Open browser console (F12)
2. Go to Performance tab
3. Click Record, interact with portal, stop recording
4. Look for long tasks (>50ms)

**Solutions:**

**A. Too Many Browser Extensions**
1. Disable unnecessary Chrome extensions
2. Test in Chrome Incognito (Ctrl+Shift+N)
3. If faster in incognito, identify problematic extension

**B. Backend Slow**
```powershell
# Test backend response time
Measure-Command {curl http://100.y.y.y:8080/coils}
# Should complete in < 500ms
```

**C. Large Cache**
```javascript
// Clear cached data
sessionStorage.clear();
location.reload();
```

**D. Hardware Acceleration Disabled**
1. Chrome → Settings → System
2. Enable "Use hardware acceleration when available"
3. Restart browser

### 10. Dark Mode Not Working

**Symptoms:**
- Toggle switch doesn't change theme
- Always light or always dark
- Theme resets on page reload

**Diagnosis:**
```javascript
// Check theme in localStorage
localStorage.getItem('zootbox-theme');
// Should return 'light' or 'dark'
```

**Solutions:**

**A. LocalStorage Issue**
```javascript
// Reset theme
localStorage.removeItem('zootbox-theme');
location.reload();
```

**B. CSS Not Loaded**
1. Hard refresh (Ctrl+Shift+R)
2. Check browser console for CSS errors
3. Verify Tailwind CSS CDN is accessible:
   ```powershell
   curl https://cdn.tailwindcss.com
   ```

## Browser Console Errors

### Common Error Messages

**"Failed to fetch"**
- Backend unreachable
- Check Tailscale VPN
- Verify backend URL

**"NetworkError when attempting to fetch resource"**
- CORS issue
- Backend CORS must allow localhost:3000
- Check backend CORS configuration

**"Unexpected token < in JSON"**
- Backend returning HTML instead of JSON
- Usually means 404 or 500 error
- Check backend endpoint URL

**"Cannot read property 'id' of undefined"**
- Data structure mismatch
- Backend API changed
- Check backend version compatibility

## Advanced Diagnostics

### Enable Verbose Logging

```javascript
// Enable debug mode
localStorage.setItem('DEBUG', 'true');
location.reload();

// View all localStorage data
console.table(localStorage);

// Monitor API requests
// Chrome → F12 → Network tab → Filter: XHR
```

### Export Diagnostic Data

```javascript
// Collect diagnostic info
const diagnostics = {
  userAgent: navigator.userAgent,
  viewport: {width: window.innerWidth, height: window.innerHeight},
  localStorage: {...localStorage},
  sessionStorage: {...sessionStorage},
  buildMetadata: await fetch('/build-metadata.json').then(r => r.json()),
  timestamp: new Date().toISOString()
};
console.log(JSON.stringify(diagnostics, null, 2));
// Copy output for support ticket
```

### Test Backend Connectivity

```powershell
# Complete backend connectivity test
Write-Host "Testing Tailscale..." -ForegroundColor Yellow
tailscale status

Write-Host "`nTesting Backend Ping..." -ForegroundColor Yellow
ping -n 4 100.y.y.y

Write-Host "`nTesting Backend Health..." -ForegroundColor Yellow
curl http://100.y.y.y:8080/health

Write-Host "`nTesting Inventory API..." -ForegroundColor Yellow
curl http://100.y.y.y:8080/coils

Write-Host "`nDone!" -ForegroundColor Green
```

## Getting Help

If issues persist after trying solutions:

1. **Collect Information:**
   - Browser console errors (F12 → Console)
   - Network tab errors (F12 → Network)
   - Tailscale status output
   - Backend logs from tablet
   - Screenshot of issue

2. **Check Documentation:**
   - [README.md](README.md) - Portal overview
   - [SECURITY.md](SECURITY.md) - Security model
   - [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide

3. **Contact Support:**
   - Include diagnostic data (see above)
   - Describe exact steps to reproduce
   - Note when issue started
   - Mention any recent changes

## Emergency Recovery

### Complete Reset

If portal is completely broken:

```powershell
# 1. Stop server
# Press Ctrl+C in terminal

# 2. Backup data
xcopy /E /I C:\ZootBox\portal C:\ZootBox\portal-backup

# 3. Clear browser data
# Chrome → Settings → Privacy → Clear browsing data
# Select: Cookies, Cached images, Local storage
# Time range: All time → Clear data

# 4. Redeploy portal
rmdir /S /Q C:\ZootBox\portal
# Copy fresh dist/ folder from backup or rebuild

# 5. Restart server
cd C:\ZootBox\portal
python -m http.server 3000

# 6. Reconfigure machine
# Open http://localhost:3000
# Go to Machine Settings
# Add backend machine again
```

### Restore from Backup

```powershell
# Stop server
# Restore previous version
xcopy /E /I C:\ZootBox\portal-backup C:\ZootBox\portal /Y

# Restart server
cd C:\ZootBox\portal
python -m http.server 3000
```

## Prevention Tips

- **Backup machine configs weekly** (export from LocalStorage)
- **Test updates in non-production first**
- **Keep Tailscale updated** (check for updates monthly)
- **Monitor disk space** (backend SQLite database grows over time)
- **Restart backend weekly** (prevents memory leaks)
- **Document all configuration changes**

## Known Issues

### Issue: Auto-refresh stops after 24+ hours
**Status:** Known issue
**Workaround:** Refresh browser once daily (F5)
**Root cause:** Browser tab throttling for background tabs

### Issue: LocalStorage quota exceeded (rare)
**Status:** Known issue
**Workaround:** Clear old cache data
```javascript
sessionStorage.clear();
```

## Additional Resources

- **Tailscale Support:** https://tailscale.com/contact/support
- **Chrome DevTools Guide:** https://developer.chrome.com/docs/devtools/
- **HTTP Status Codes:** https://httpstatuses.com/
