# ZootBox Auto-Start Boot Sequence Documentation

**Last Updated**: December 28, 2025
**Status**: ✅ Fully Tested and Working

---

## Table of Contents

1. [Boot Sequence Overview](#boot-sequence-overview)
2. [Components Involved](#components-involved)
3. [Step-by-Step Replication Guide](#step-by-step-replication-guide)
4. [File Deployment Checklist](#file-deployment-checklist)
5. [Configuration Steps](#configuration-steps)
6. [Verification & Testing](#verification--testing)
7. [Troubleshooting](#troubleshooting)

---

## Boot Sequence Overview

### What Happens When Tablet Powers On

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Tablet Power On                                          │
│    └─► Android OS Boots                                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. System Boots to Home Screen                              │
│    └─► MyApplication is the default launcher                │
│    └─► MyApplication.MainActivity automatically launches    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. MainActivity.onCreate() Executes                         │
│    └─► Calls BootManager.startZootBoxServices(this)         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. BootManager Starts Services (in background thread)       │
│    ├─► Starts Tailscale VPN                                 │
│    │   ├─► Start Tailscale service                          │
│    │   ├─► Send connect broadcast                           │
│    │   ├─► Launch Tailscale activity with autoconnect       │
│    │   └─► Execute 'tailscale up' CLI command               │
│    │                                                         │
│    ├─► Waits 5 seconds for Tailscale to establish           │
│    │                                                         │
│    └─► Checks if backend is running                         │
│        ├─► If NOT running: Start backend                    │
│        │   └─► Execute: cd /data/data/com.termux/.../zootbox│
│        │       DB_PATH=.../inventory.db HTTP_HOST=0.0.0.0   │
│        │       nohup ./backend > backend.log 2>&1 &         │
│        └─► If running: Skip (already started)               │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. All Services Running                                     │
│    ├─► MyApplication (foreground - customer UI)             │
│    ├─► Backend API (localhost:8080 + 0.0.0.0:8080)          │
│    └─► Tailscale VPN (100.120.168.44 connected)             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                   Machine Ready for Use
```

### Timeline (Approximate)

| Time | Event |
|------|-------|
| 0:00 | Power button pressed |
| 0:05 | Android boot animation starts |
| 0:45 | Android system fully booted |
| 0:48 | MyApplication launches (home screen) |
| 0:49 | BootManager starts Tailscale |
| 0:54 | Tailscale VPN connected (after 5s delay) |
| 0:55 | Backend checks begin |
| 0:56 | Backend starts (if not running) |
| 0:57 | **System fully operational** |

**Total boot time**: ~57 seconds from power on to fully operational

---

## Components Involved

### 1. MyApplication (Android App)

**Role**: Customer-facing vending UI + service orchestration

**Key Files**:
```
MyApplication/app/src/main/java/com/example/myapplication/
├── MainActivity.kt              # Main UI, calls BootManager
├── BootManager.kt              # Auto-start orchestration logic
├── BootReceiver.kt             # Boot broadcast listener (backup)
└── AndroidManifest.xml         # Launcher config + permissions
```

**Responsibilities**:
- Display vending UI to customers
- Auto-start backend and Tailscale on launch
- Act as default home screen (launcher)

### 2. BootManager.kt

**Role**: Service startup orchestration

**Functions**:
```kotlin
fun startZootBoxServices(context: Context?)
    ├─► startTailscale(context)     # Connect VPN
    └─► startBackend()               # Launch Go backend

private fun isBackendRunning(): Boolean
    └─► Checks: ps -ef | grep backend

private fun startBackend()
    └─► Executes backend with su privileges

private fun startTailscale(context: Context?)
    ├─► Method 1: am startservice (IPNService)
    ├─► Method 2: am broadcast (CONNECT)
    ├─► Method 3: Launch activity with autoconnect
    └─► Method 4: tailscale up (CLI)
```

**Why Multiple Tailscale Methods?**
Different Android versions and Tailscale versions support different connection methods. Using all 4 ensures maximum compatibility.

### 3. Backend (Go Service)

**Location**: `/data/data/com.termux/files/home/zootbox/backend`

**Configuration** (via environment variables):
```bash
DB_PATH=/data/data/com.termux/files/home/zootbox/data/inventory.db
HTTP_HOST=0.0.0.0  # CRITICAL: Must be 0.0.0.0 for Tailscale access
HTTP_PORT=8080
```

**Listens On**:
- `localhost:8080` - For MyApplication (local vending)
- `0.0.0.0:8080` - For Tailscale (remote portal)

### 4. Tailscale VPN

**Purpose**: Secure remote access to backend API

**Network**:
- Tablet Tailscale IP: `100.120.168.44`
- Operator PC Tailscale IP: `100.127.201.126`

**Connection Methods** (BootManager tries all):
1. **Service Start**: `am startservice -n com.tailscale.ipn/.IPNService`
2. **Connect Broadcast**: `am broadcast -a com.tailscale.ipn.CONNECT`
3. **Activity Launch**: Launch MainActivity with `autoconnect=true` extra
4. **CLI Command**: `tailscale up` (if Tailscale CLI installed)

---

## Step-by-Step Replication Guide

### Prerequisites

**On Operator PC**:
- ✅ ADB installed and in PATH
- ✅ Tablet connected via USB
- ✅ USB debugging enabled on tablet
- ✅ Root access (su) on tablet
- ✅ Go 1.22.8+ installed (for building backend)
- ✅ Android Studio / Gradle for building APK

**On Tablet**:
- ✅ Android 8.0+ (API level 26+)
- ✅ Termux installed (for backend runtime)
- ✅ Root access (su command available)
- ✅ 200+ MB free storage

---

## File Deployment Checklist

### Step 1: Prepare Backend Binary

#### Option A: Build from Source (Recommended)

```bash
# On your PC
cd C:/dev/Backend

# Cross-compile for Android ARM64
GOOS=linux GOARCH=arm64 CGO_ENABLED=0 go build -o backend ./cmd/server

# Verify binary
file backend
# Should show: backend: ELF 64-bit LSB executable, ARM aarch64
```

#### Option B: Use Pre-built Binary

Use the existing backend binary from a working tablet.

### Step 2: Deploy Backend to Tablet

```bash
# Create directory structure
adb shell "su -c 'mkdir -p /data/data/com.termux/files/home/zootbox/data'"

# Push backend binary
adb push backend /sdcard/backend
adb shell "su -c 'mv /sdcard/backend /data/data/com.termux/files/home/zootbox/backend'"
adb shell "su -c 'chmod +x /data/data/com.termux/files/home/zootbox/backend'"

# Push entire backend directory (if you have other files)
cd C:/dev/Backend
tar -czf backend.tar.gz .
adb push backend.tar.gz /sdcard/
adb shell "su -c 'cd /data/data/com.termux/files/home/zootbox && tar -xzf /sdcard/backend.tar.gz'"
adb shell "su -c 'chmod +x /data/data/com.termux/files/home/zootbox/backend'"
```

### Step 3: Install Tailscale on Tablet

```bash
# Download Tailscale APK (F-Droid or official)
curl -L -o tailscale.apk "https://f-droid.org/repo/com.tailscale.ipn_526.apk"

# Install via ADB
adb install tailscale.apk

# Launch and sign in (IMPORTANT: Use the same Tailscale account on all devices)
adb shell "am start -n com.tailscale.ipn/.MainActivity"
```

**Manual Step**: On the tablet screen:
1. Tap "Log in" in Tailscale app
2. Sign in with your Tailscale account (same account used on operator PC)
3. Accept VPN configuration prompt
4. Note the Tailscale IP address (e.g., `100.120.168.44`)

### Step 4: Build and Install MyApplication

```bash
# Build APK
cd C:/dev/MyApplication
./gradlew assembleDebug

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 5: Set MyApplication as Default Launcher

```bash
# Set as home screen / launcher
adb shell "cmd package set-home-activity com.example.myapplication/.MainActivity"

# Verify it's set
adb shell "cmd package get-home-activities"
# Should show: com.example.myapplication/.MainActivity
```

### Step 6: Grant Necessary Permissions

```bash
# Grant boot permission (should already be in manifest)
adb shell "pm grant com.example.myapplication android.permission.RECEIVE_BOOT_COMPLETED"

# Verify MyApplication has root access (if needed for su commands)
adb shell "su -c 'id'"
```

---

## Configuration Steps

### 1. Backend Configuration

The backend reads configuration from environment variables. These are set in `BootManager.kt`:

```kotlin
DB_PATH=/data/data/com.termux/files/home/zootbox/data/inventory.db
HTTP_HOST=0.0.0.0  // CRITICAL: Must bind to all interfaces
HTTP_PORT=8080
```

**⚠️ CRITICAL**: `HTTP_HOST` must be `0.0.0.0` (not `127.0.0.1`) for Tailscale access to work.

### 2. Tailscale Network Configuration

**Same Account Requirement**: All devices (tablets and operator PCs) must be signed into the **same Tailscale account** to see each other.

**Device Naming**: Tailscale auto-assigns names based on hostname. Example:
- Tablet: `seco-c31-rk3399-android11` → IP `100.120.168.44`
- Operator PC: `nicks` → IP `100.127.201.126`

### 3. Portal Configuration

On the operator's PC, configure the portal to connect to the tablet:

**File**: `C:/dev/portal/` (access via browser at `http://localhost:3000`)

**Machine Settings**:
1. Open portal → "Machine Settings"
2. Click "Add Machine"
3. Enter:
   - **Name**: `Sarasota Venue` (or any descriptive name)
   - **Endpoint URL**: `http://100.120.168.44:8080` (use tablet's Tailscale IP)
4. Click "Test" to verify connection
5. Save

---

## Verification & Testing

### Test 1: Manual Service Start

**Before rebooting**, test that services can start manually:

```bash
# Connect to tablet
adb devices

# Launch MyApplication manually
adb shell "am start -n com.example.myapplication/.MainActivity"

# Wait 10 seconds for services to start
sleep 10

# Check backend is running
adb shell "ps -ef | grep backend | grep -v grep"
# Should show: backend process with PID

# Check Tailscale connectivity from PC
ping 100.120.168.44
# Should reply: Reply from 100.120.168.44: bytes=32 time=XXms

# Check backend health
curl http://100.120.168.44:8080/health
# Should return: {"status":"healthy","uptime_seconds":...,"database_ok":true}
```

### Test 2: Full Boot Sequence

**Reboot test** to ensure everything auto-starts:

```bash
# Reboot tablet
adb shell "su -c 'reboot'"

# Wait 75 seconds for full boot
sleep 75

# Reconnect ADB
adb devices

# Check MyApplication is foreground app
adb shell "dumpsys activity activities | grep 'mResumedActivity'"
# Should show: com.example.myapplication/.MainActivity

# Check backend is running
adb shell "ps -ef | grep backend | grep -v grep"
# Should show: backend process

# Test Tailscale connectivity
ping 100.120.168.44
# Should reply successfully

# Test backend API
curl http://100.120.168.44:8080/health
# Should return healthy status
```

### Test 3: Check Boot Logs

```bash
# View boot sequence logs
adb logcat -d | grep -E "BootManager|BootReceiver"

# Should show timeline:
# BootReceiver: Device booted, launching MyApplication
# BootManager: Tailscale service started
# BootManager: Tailscale connect broadcast sent
# BootManager: Tailscale activity launched with autoconnect
# BootManager: Tailscale CLI 'up' command executed
# BootManager: Backend not running, starting...
# BootManager: Backend started successfully
```

---

## Troubleshooting

### Issue 1: MyApplication Doesn't Auto-Launch on Boot

**Symptom**: After reboot, tablet shows default Android launcher

**Solution**:
```bash
# Verify MyApplication is set as home
adb shell "cmd package get-home-activities"

# If not set, run:
adb shell "cmd package set-home-activity com.example.myapplication/.MainActivity"

# Force reboot and test again
```

### Issue 2: Backend Doesn't Start

**Symptom**: Backend process not found after boot

**Check Logs**:
```bash
adb logcat -d | grep BootManager
```

**Common Causes**:

1. **Permission denied**:
   - Solution: Ensure root (su) is available
   - Test: `adb shell "su -c 'id'"` should work

2. **Backend binary missing or not executable**:
   ```bash
   adb shell "su -c 'ls -la /data/data/com.termux/files/home/zootbox/backend'"
   # Should show: -rwxr-xr-x ... backend

   # If not executable:
   adb shell "su -c 'chmod +x /data/data/com.termux/files/home/zootbox/backend'"
   ```

3. **Database directory doesn't exist**:
   ```bash
   adb shell "su -c 'mkdir -p /data/data/com.termux/files/home/zootbox/data'"
   ```

### Issue 3: Tailscale Doesn't Connect

**Symptom**: Ping to Tailscale IP times out

**Check Status**:
```bash
# Launch Tailscale and check status manually
adb shell "am start -n com.tailscale.ipn/.MainActivity"
```

**Common Causes**:

1. **Not signed in to Tailscale**:
   - Solution: Open Tailscale app on tablet, sign in

2. **Tailscale service not running**:
   - Check: `adb shell "dumpsys activity services | grep tailscale"`
   - Restart: `adb shell "am startservice -n com.tailscale.ipn/.IPNService"`

3. **VPN permission not granted**:
   - When first connecting, Android asks for VPN permission
   - Must tap "OK" on tablet screen

4. **Different Tailscale accounts**:
   - Tablet and PC must use the **same Tailscale account**
   - Verify in Tailscale admin console: https://login.tailscale.com/admin/machines

### Issue 4: Portal Can't Connect to Backend

**Symptom**: Portal shows "Connection Refused" or "Request Timeout"

**Checklist**:

1. **Verify Tailscale connectivity**:
   ```bash
   ping 100.120.168.44
   ```

2. **Verify backend is listening on 0.0.0.0**:
   ```bash
   adb shell "su -c 'netstat -tlnp | grep 8080'"
   # Should show: 0.0.0.0:8080 (NOT 127.0.0.1:8080)
   ```

3. **Test backend directly**:
   ```bash
   curl http://100.120.168.44:8080/health
   ```

4. **Check CORS configuration**:
   - Backend must allow requests from `http://localhost:3000`
   - File: `Backend/internal/api/middleware/cors.go`
   - Should have: `w.Header().Set("Access-Control-Allow-Origin", "*")` or properly handle origin header

### Issue 5: Backend Crashes on Start

**Check Logs**:
```bash
adb shell "su -c 'cat /data/data/com.termux/files/home/zootbox/backend.log'"
```

**Common Errors**:

1. **Database locked**:
   ```
   Error: database is locked
   ```
   - Solution: Kill any existing backend processes
   ```bash
   adb shell "su -c 'pkill backend'"
   ```

2. **Permission denied (database)**:
   ```
   Error: unable to open database file
   ```
   - Solution: Ensure directory permissions
   ```bash
   adb shell "su -c 'chown -R root:root /data/data/com.termux/files/home/zootbox'"
   adb shell "su -c 'chmod -R 755 /data/data/com.termux/files/home/zootbox'"
   ```

3. **Port already in use**:
   ```
   Error: bind: address already in use
   ```
   - Solution: Find and kill process using port 8080
   ```bash
   adb shell "su -c 'netstat -tlnp | grep 8080'"
   adb shell "su -c 'kill <PID>'"
   ```

---

## Replication Checklist for New Machines

Use this checklist when deploying to a new tablet:

### ☐ Pre-Deployment (PC)

- [ ] Backend binary compiled for ARM64
- [ ] MyApplication APK built
- [ ] Tailscale APK downloaded
- [ ] ADB installed and tablet connected
- [ ] Root access verified on tablet

### ☐ Backend Deployment (Tablet)

- [ ] Created directory: `/data/data/com.termux/files/home/zootbox/`
- [ ] Created directory: `/data/data/com.termux/files/home/zootbox/data/`
- [ ] Pushed backend binary
- [ ] Set executable permission: `chmod +x backend`
- [ ] Verified binary runs: `./backend --help` works

### ☐ Tailscale Setup (Tablet)

- [ ] Installed Tailscale APK
- [ ] Signed in to Tailscale (same account as operator PC)
- [ ] Accepted VPN configuration prompt
- [ ] Noted Tailscale IP address
- [ ] Verified connectivity: `ping <tablet-ip>` from PC works

### ☐ MyApplication Setup (Tablet)

- [ ] Installed MyApplication APK
- [ ] Set as default launcher: `cmd package set-home-activity`
- [ ] Granted RECEIVE_BOOT_COMPLETED permission
- [ ] Verified launcher: Press home button → MyApplication appears

### ☐ Testing (Tablet)

- [ ] **Manual start test**: Launch MyApplication → Backend starts
- [ ] **Tailscale test**: Ping tablet IP from PC
- [ ] **Backend API test**: `curl http://<tablet-ip>:8080/health`
- [ ] **Full reboot test**: Reboot → Wait 75s → All services running
- [ ] **Portal test**: Open portal on PC → Connect to machine → View inventory

### ☐ Portal Configuration (PC)

- [ ] Started portal server: `python -m http.server 3000`
- [ ] Opened portal: `http://localhost:3000`
- [ ] Added machine in "Machine Settings"
- [ ] Tested connection: Health check passes
- [ ] Verified inventory grid loads

---

## Quick Reference Commands

### Start Portal Server (PC)
```bash
cd C:/dev/portal
python -m http.server 3000
# Access at: http://localhost:3000
```

### Check Services (Tablet via ADB)
```bash
# Check MyApplication is foreground
adb shell "dumpsys activity activities | grep 'mResumedActivity'"

# Check backend is running
adb shell "ps -ef | grep backend | grep -v grep"

# Check backend logs
adb shell "su -c 'tail -50 /data/data/com.termux/files/home/zootbox/backend.log'"

# Check Tailscale status
adb shell "dumpsys activity services | grep tailscale"

# Test Tailscale connectivity (from PC)
ping 100.120.168.44

# Test backend API (from PC)
curl http://100.120.168.44:8080/health
```

### Restart Services (Tablet via ADB)
```bash
# Restart MyApplication (also restarts backend + Tailscale)
adb shell "am force-stop com.example.myapplication"
adb shell "am start -n com.example.myapplication/.MainActivity"

# Or reboot tablet
adb shell "su -c 'reboot'"
```

### Update Backend Binary (Tablet via ADB)
```bash
# Kill existing backend
adb shell "su -c 'pkill backend'"

# Push new binary
adb push backend /sdcard/backend
adb shell "su -c 'mv /sdcard/backend /data/data/com.termux/files/home/zootbox/backend'"
adb shell "su -c 'chmod +x /data/data/com.termux/files/home/zootbox/backend'"

# Restart MyApplication to auto-start new backend
adb shell "am force-stop com.example.myapplication"
adb shell "am start -n com.example.myapplication/.MainActivity"
```

### Update MyApplication (Tablet via ADB)
```bash
# Build APK
cd C:/dev/MyApplication
./gradlew assembleDebug

# Install update
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Restart app
adb shell "am start -n com.example.myapplication/.MainActivity"
```

---

## Important Notes

### ⚠️ Security Considerations

1. **Root Access Required**: The boot sequence requires root (su) on the tablet to start the backend
2. **No Authentication**: Backend has no authentication (security is at network level via Tailscale VPN)
3. **Tailscale Account Security**: Protect your Tailscale account - anyone with access can connect to all machines
4. **VPN Only**: Backend should ONLY be accessible via Tailscale VPN, never exposed to public internet

### 🔧 Customization Points

**Change Tablet Tailscale IP**:
- IP is assigned by Tailscale based on device
- To use a specific IP, configure static IP in Tailscale admin console
- Update portal machine settings with new IP

**Change Backend Port**:
- Update `BootManager.kt`: Modify `HTTP_PORT` environment variable
- Update portal machine settings: Use new port in endpoint URL

**Add More Machines**:
1. Repeat replication checklist for each new tablet
2. Install Tailscale on each (same account)
3. Note each tablet's Tailscale IP
4. Add each machine in portal "Machine Settings"

---

## File Locations Reference

### On Tablet:
```
/data/data/com.termux/files/home/zootbox/
├── backend                    # Go binary (executable)
├── backend.log                # Runtime logs
└── data/
    └── inventory.db           # SQLite database

/data/data/com.example.myapplication/
└── (APK installation files)

/data/data/com.tailscale.ipn/
└── (Tailscale VPN files)
```

### On PC:
```
C:/dev/
├── Backend/                   # Go source code
│   ├── backend                # Compiled ARM64 binary
│   └── BOOT_SEQUENCE_SETUP.md # This file
├── MyApplication/             # Android app source
│   └── app/build/outputs/apk/debug/app-debug.apk
├── portal/                    # Web portal
└── PORTAL_SYSTEM_OVERVIEW.md  # System documentation
```

---

## Support & Maintenance

### Regular Maintenance

**Weekly**:
- Check backend logs for errors: `adb shell "su -c 'tail -100 /data/data/com.termux/files/home/zootbox/backend.log'"`
- Verify Tailscale connectivity
- Test portal access

**Monthly**:
- Update Tailscale app if new version available
- Review backend performance metrics: `curl http://<tablet-ip>:8080/metrics`
- Check database size: `adb shell "su -c 'du -h /data/data/com.termux/files/home/zootbox/data/inventory.db'"`

### Backup Recommendations

**Critical Files to Backup**:
1. Database: `/data/data/com.termux/files/home/zootbox/data/inventory.db`
2. Backend binary: `/data/data/com.termux/files/home/zootbox/backend`
3. MyApplication APK: `C:/dev/MyApplication/app/build/outputs/apk/debug/app-debug.apk`

**Backup Command**:
```bash
# Create backup directory
mkdir -p C:/dev/backups/$(date +%Y%m%d)

# Pull database
adb pull /data/data/com.termux/files/home/zootbox/data/inventory.db C:/dev/backups/$(date +%Y%m%d)/

# Pull backend logs
adb shell "su -c 'cat /data/data/com.termux/files/home/zootbox/backend.log'" > C:/dev/backups/$(date +%Y%m%d)/backend.log

# Copy APK
cp C:/dev/MyApplication/app/build/outputs/apk/debug/app-debug.apk C:/dev/backups/$(date +%Y%m%d)/
```

---

## Revision History

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-28 | 1.0 | Initial documentation - Full boot sequence working |

---

**End of Boot Sequence Setup Documentation**

For system architecture overview, see: `C:/dev/PORTAL_SYSTEM_OVERVIEW.md`
