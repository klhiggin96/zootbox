# ZootBox Auto-Start Boot Sequence Documentation

**Last Updated**: December 30, 2025
**Status**: ✅ Fully Tested and Working - Production Ready

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
│ 2. Android VPN Service Auto-Starts                          │
│    └─► Tailscale VPN connects automatically                 │
│        (via "Always-on VPN" setting - no UI needed)         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. BOOT_COMPLETED Broadcast Sent                            │
│    └─► BootReceiver.onReceive() triggers                    │
│    └─► Launches MyApplication immediately                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. System Goes to Home Screen                               │
│    └─► MyApplication is set as default launcher             │
│    └─► MyApplication.MainActivity appears on screen         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. MainActivity.onCreate() Executes                         │
│    └─► Calls BootManager.startZootBoxServices(this)         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. BootManager Starts Backend (in background thread)        │
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
│ 7. All Services Running                                     │
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
| 0:46 | Tailscale VPN auto-connects (Always-on VPN) |
| 0:47 | BOOT_COMPLETED broadcast sent |
| 0:47 | BootReceiver launches MyApplication immediately |
| 0:48 | MyApplication appears on screen (home screen) |
| 0:49 | BootManager checks backend status |
| 0:50 | Backend starts (if not running) |
| 0:51 | **System fully operational** |

**Total boot time**: ~51 seconds from power on to fully operational

---

## Components Involved

### 1. MyApplication (Android App)

**Role**: Customer-facing vending UI + service orchestration

**Key Files**:
```
MyApplication/app/src/main/java/com/example/myapplication/
├── MainActivity.kt              # Main UI, calls BootManager
├── BootManager.kt              # Auto-start backend logic
├── BootReceiver.kt             # Boot broadcast listener - launches MainActivity
└── AndroidManifest.xml         # Launcher config + permissions
```

**Responsibilities**:
- Display vending UI to customers
- Auto-start backend on launch
- Act as default home screen (launcher)
- Launch immediately on boot via BootReceiver

### 2. BootReceiver.kt

**Role**: Boot broadcast listener that launches MyApplication immediately

**Functions**:
```kotlin
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        // Launch MyApplication immediately
        val launchIntent = Intent(context, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
```

**Why it's needed**:
- Ensures MyApplication launches as soon as Android finishes booting
- Works in conjunction with MyApplication being set as default launcher
- No delays - launches immediately for fastest boot time

### 3. BootManager.kt

**Role**: Backend service startup orchestration

**Functions**:
```kotlin
fun startZootBoxServices(context: Context?)
    └─► startBackend()               # Launch Go backend

private fun isBackendRunning(): Boolean
    └─► Checks: ps -ef | grep backend

private fun startBackend()
    └─► Executes backend with su privileges
```

**Note**: Tailscale is NOT started by BootManager - it auto-connects via Android's "Always-on VPN" feature.

### 4. Backend (Go Service)

**Location**: `/data/data/com.termux/files/home/zootbox/backend`

**Configuration** (via environment variables):
```bash
DB_PATH=/data/data/com.termux/files/home/zootbox/data/inventory.db
HTTP_HOST=0.0.0.0  # ⚠️ CRITICAL: Must be 0.0.0.0 for Tailscale access
HTTP_PORT=8080
```

**⚠️ CRITICAL: HTTP_HOST Configuration**
- `HTTP_HOST=0.0.0.0` - Backend accepts connections from ALL interfaces (required for portal)
- `HTTP_HOST=127.0.0.1` - Backend only accepts localhost connections (portal will fail)

If the portal shows "Connection Refused", verify the backend started with `HTTP_HOST=0.0.0.0`.

**Start Command**:
```bash
cd /data/data/com.termux/files/home/zootbox
DB_PATH=/data/data/com.termux/files/home/zootbox/data/inventory.db HTTP_HOST=0.0.0.0 nohup ./backend > backend.log 2>&1 &
```

**Listens On**:
- `0.0.0.0:8080` - All interfaces (localhost + Tailscale)

**Database**: 10 coils (A1-J1), matching the 10 motors in the vending machine

### 5. Tailscale VPN

**Purpose**: Secure remote access to backend API

**Network**:
- Tablet Tailscale IP: `100.120.168.44`
- Operator PC Tailscale IP: `100.127.201.126`

**✅ PRODUCTION-READY**: The official Tailscale Android app **DOES** support fully automatic connection on boot when configured as "Always-on VPN" with the screen lock disabled. This is now the **RECOMMENDED** production configuration.

**Three Options** (in order of recommendation):

#### Option A: Official Tailscale App with Always-on VPN (RECOMMENDED ✅)

**What it is**: The official Tailscale Android app configured with Android's "Always-on VPN" feature to auto-connect on every boot.

**Why it's the best option**:
- ✅ Fully automatic - no manual activation required after initial setup
- ✅ Uses official Tailscale app - no need for Magisk or root
- ✅ Connects before apps launch (via Android VPN service)
- ✅ Perfect for headless/kiosk devices
- ✅ Easy to configure via Android Settings
- ✅ Most reliable and well-tested method

**Installation & Configuration**:

1. **Install Tailscale App**:
   ```bash
   # Download from F-Droid or official source
   curl -L -o tailscale.apk "https://f-droid.org/repo/com.tailscale.ipn_526.apk"
   adb install tailscale.apk
   ```

2. **Sign In (One-Time Setup)**:
   - Open Tailscale app on tablet
   - Sign in with your Tailscale account
   - Accept VPN configuration prompt
   - Note the assigned Tailscale IP (e.g., `100.120.168.44`)

3. **Enable Always-on VPN** (CRITICAL):
   - Go to: **Settings > Network & internet > VPN**
   - Tap the gear icon next to **Tailscale**
   - Toggle ON: **Always-on VPN**
   - Toggle ON: **Block connections without VPN** (recommended)

4. **Disable Screen Lock** (Required for headless operation):
   - Go to: **Settings > Security > Screen lock**
   - Set to: **None** or **Swipe**
   - **Why**: Android's File-Based Encryption locks Tailscale's identity keys until the device is unlocked after boot. Disabling screen lock bypasses this.

5. **Disable Battery Optimization** (Prevents Android from killing Tailscale):
   - Go to: **Settings > Apps > Tailscale > Battery**
   - Set to: **Unrestricted**
   - **Why**: Prevents Android's "Doze" mode from killing the Tailscale background process.

6. **Test Auto-Connect**:
   ```bash
   # Reboot tablet
   adb reboot

   # Wait 60 seconds for boot
   sleep 60

   # Test connectivity from PC
   ping 100.120.168.44
   # Should reply successfully!
   ```

**Post-Installation Behavior**:
- Tailscale connects automatically on every boot
- No UI flashing or manual intervention
- Connection established within ~5 seconds of boot
- MyApplication launches immediately and appears on screen
- Backend starts automatically
- **Total boot time**: ~51 seconds to fully operational

**Advantages over Magisk Tailscaled**:
- No root required (besides for backend startup)
- Uses official, well-maintained app
- Automatic updates via app store
- Simpler setup for non-technical operators
- No module compatibility issues

#### Option B: Magisk Tailscaled Module (Alternative for Advanced Users)

**What it is**: A Magisk/KernelSU module that runs the Tailscale daemon (tailscaled) at system level.

**Why use this**:
- Want to keep screen lock enabled (though not recommended for kiosks)
- Need CLI control via `tailscale` command
- Prefer system-level daemon over app-based VPN

**Installation**:
1. Download latest release: [Magisk-Tailscaled Releases](https://github.com/anasfanani/Magisk-Tailscaled/releases)
2. Push to tablet: `adb push magisk-tailscaled-*.zip /sdcard/`
3. Install via Magisk Manager → Modules → Install from storage
4. Reboot tablet
5. Authenticate: `adb shell "su -c 'tailscale login'"`
6. Open the URL shown and authorize
7. Verify: `adb shell "su -c 'tailscale status'"`

**Note**: This option is more complex and requires Magisk to be installed. Option A (Always-on VPN) is recommended for most users.

#### Option C: Manual Connection Only (Development/Testing)

**⚠️ NOT SUITABLE FOR PRODUCTION**: Requires manual activation on every boot.

**Why it doesn't work**: Without "Always-on VPN" or Magisk module, Tailscale starts but doesn't connect until you manually open the app and tap "Connect". This is unacceptable for unattended vending machines.

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

### Step 3: Install and Configure Tailscale on Tablet

**RECOMMENDED**: Use **Option A (Always-on VPN)** for production vending machines. See the detailed configuration in the "Components Involved > Tailscale VPN" section above.

#### Quick Setup for Always-on VPN (RECOMMENDED ✅)

```bash
# 1. Install Tailscale APK
curl -L -o tailscale.apk "https://f-droid.org/repo/com.tailscale.ipn_526.apk"
adb install tailscale.apk

# 2. Launch Tailscale and sign in (one-time)
adb shell "am start -n com.tailscale.ipn/.MainActivity"
```

**Manual Configuration on Tablet Screen**:

1. **Sign In to Tailscale**:
   - Open Tailscale app
   - Sign in with your Tailscale account
   - Accept VPN configuration prompt
   - Note the assigned Tailscale IP (e.g., `100.120.168.44`)

2. **Enable Always-on VPN** (CRITICAL):
   - Open: **Settings > Network & internet > VPN**
   - Tap the gear icon next to **Tailscale**
   - Toggle ON: **Always-on VPN**
   - Toggle ON: **Block connections without VPN**

3. **Disable Screen Lock** (Required):
   - Open: **Settings > Security > Screen lock**
   - Set to: **None**
   - Confirm the change

4. **Disable Battery Optimization**:
   - Open: **Settings > Apps > Tailscale > Battery**
   - Set to: **Unrestricted**

5. **Test Auto-Connect**:
   ```bash
   # Reboot tablet
   adb reboot

   # Wait 60 seconds
   sleep 60

   # Test connectivity
   ping 100.120.168.44
   # Should work!
   ```

**Post-Configuration**:
- Tailscale connects automatically on every boot
- No UI flashing - completely silent
- Connection established within ~5 seconds
- Perfect for unattended vending machines
- No manual intervention ever needed

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
| 2026-01-10 | 2.1 | **SYNC VERIFIED**: End-to-end inventory sync working. HTTP_HOST=0.0.0.0 required for portal. Backend DB fixed to 10 coils (A1-J1). |
| 2025-12-30 | 2.0 | **PRODUCTION READY**: Tailscale Always-on VPN for headless operation. Boot time ~51 seconds. |
| 2025-12-28 | 1.0 | Initial documentation - Full boot sequence working |

---

**End of Boot Sequence Setup Documentation**

For system architecture overview, see: `C:/dev/PORTAL_SYSTEM_OVERVIEW.md`
