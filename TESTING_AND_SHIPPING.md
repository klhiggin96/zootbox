# Testing & Shipping Guide

This guide explains how to build, install, and test the application on the target device using ADB.

## 1. Build & Install (Debug)

The project currently uses the **Debug** build variant for development and testing. This APK is automatically signed with a debug key.

### Command:
Run this from the project root directory (`c:\dev\`):

```powershell
.\MyApplication\gradlew.bat installDebug -p MyApplication
```

This command will:
1. Clean and rebuild the project.
2. Generate `app-debug.apk`.
3. Install it on all connected ADB devices.

---

## 2. Connect to Device

If the device is not listed in `adb devices`, connect to it using its IP and Port.

**Check for devices:**
```powershell
adb devices
```

**Connect (Replace PORT with the current wireless debugging port):**
```powershell
adb connect 192.168.7.246:PORT
```
*Note: The port number changes if the device reboots or wireless debugging is toggled.*

---

## 3. Launch the App

To start the application manually via ADB:

```powershell
adb -s 192.168.7.246:PORT shell am start -n com.example.myapplication/.MainActivity
```

**Example (if port is 43123):**
```powershell
adb -s 192.168.7.246:43123 shell am start -n com.example.myapplication/.MainActivity
```

---

## 4. File Locations

*   **Debug APK Output:**
    `MyApplication\app\build\outputs\apk\debug\app-debug.apk`

*   **Video Assets (On Device):**
    The app looks for product videos in this folder on the device:
    `/storage/emulated/0/Movies/ZootBox/`

---

## 5. Troubleshooting

*   **"Device not found":** Run `adb disconnect` then `adb connect <IP>:<PORT>` again.
*   **"Connection refused":** The port has likely changed. Check the device's Developer Options > Wireless Debugging to find the new port.
*   **App Crash:** Run `adb logcat -d *:E` to see the error logs.

