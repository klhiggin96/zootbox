# Kiosk Mode Configuration

## Current Production Settings

The app runs in kiosk (lock task) mode for production deployment.

## Enable Kiosk Mode (Production)

### Step 1: Enable in code (MainActivity.kt lines 118-122)
Uncomment the kiosk mode code:
```kotlin
android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
     enableAppPinning()
}, 30000) // 30 seconds delay
```

### Step 2: Set system setting
```bash
adb shell settings put global lock_task_packages com.example.myapplication
adb reboot
```

## Disable Kiosk Mode (Development)

### Step 1: Comment out in code (MainActivity.kt lines 118-122)
The code should look like:
```kotlin
// android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
//      enableAppPinning()
// }, 30000) // 30 seconds delay
```

### Step 2: Remove system setting
```bash
adb shell settings delete global lock_task_packages
adb reboot
```

## Check Current Status
```bash
adb shell settings get global lock_task_packages
```

## How It Works

1. The system setting `lock_task_packages` whitelists the app for lock task mode
2. The app is registered as HOME launcher in AndroidManifest.xml
3. After 30 seconds, `enableAppPinning()` calls `startLockTask()`
4. When lock task is active, users cannot exit the app or access other apps

## Notes

- **BOTH** the code change AND system setting are required for kiosk mode
- Reboot is required after changing the setting for it to take full effect
- During development, disable kiosk mode to avoid install/restart conflicts
