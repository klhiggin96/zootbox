# Build and Install Debug APK via ADB on port 43123
# This script builds the debug APK and installs it via ADB
# It does NOT touch the existing VendingClient.apk in shippable_apk folder
# Run from c:\dev directory

$devicePort = "43123"
$deviceIp = "192.168.7.246"
$deviceAddress = "${deviceIp}:${devicePort}"
$projectRoot = "c:\dev"
$debugApkPath = "MyApplication\app\build\outputs\apk\debug\app-debug.apk"
$existingApkPath = "MyApplication\shippable_apk\VendingClient.apk"

# Change to project root
Set-Location $projectRoot

Write-Host "========================================"
Write-Host "Building and Installing Debug APK via ADB"
Write-Host "========================================"
Write-Host ""
Write-Host "Working directory: $(Get-Location)"
Write-Host "Target device: $deviceAddress"
Write-Host "Note: Existing APK at '$existingApkPath' will be preserved"
Write-Host ""

# Check if adb is available
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] adb not found in PATH"
    Write-Host "Please add Android SDK platform-tools to your PATH"
    exit 1
}

# Check for device connection
Write-Host "Checking for device at $deviceAddress..."
$devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }

if ($devices.Count -eq 0) {
    Write-Host "[ERROR] No devices/emulators found"
    Write-Host "Please connect a device or start an emulator"
    Write-Host "Trying to connect to $deviceAddress..."
    
    # Try to connect if device is not listed
    adb connect $deviceAddress
    Start-Sleep -Seconds 2
    
    $devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }
    if ($devices.Count -eq 0) {
        Write-Host "[ERROR] Could not connect to device at $deviceAddress"
        exit 1
    }
}

Write-Host "[OK] Device found:"
adb devices
Write-Host ""

# Build the debug APK (this goes to MyApplication\app\build\outputs\apk\debug\app-debug.apk)
Write-Host "========================================"
Write-Host "Step 1: Building debug APK..."
Write-Host "========================================"
.\MyApplication\gradlew.bat assembleDebug -p MyApplication

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build failed!"
    exit 1
}

Write-Host ""
Write-Host "[OK] Build successful"
Write-Host ""

# Verify debug APK exists
if (-not (Test-Path $debugApkPath)) {
    Write-Host "[ERROR] Debug APK not found at: $debugApkPath"
    exit 1
}

# Verify existing APK is still there (just a safety check)
if (Test-Path $existingApkPath) {
    Write-Host "[OK] Existing APK preserved at: $existingApkPath"
} else {
    Write-Host "[INFO] No existing APK found at: $existingApkPath (this is OK if it didn't exist)"
}
Write-Host ""

# Install debug APK via Gradle (targeting specific device)
Write-Host "========================================"
Write-Host "Step 2: Installing debug APK via ADB..."
Write-Host "========================================"
Write-Host "Installing to device: $deviceAddress"
Write-Host ""

# Set ANDROID_SERIAL to target the specific device
$env:ANDROID_SERIAL = $deviceAddress
.\MyApplication\gradlew.bat installDebug -p MyApplication

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Installation failed!"
    exit 1
}

Write-Host ""
Write-Host "[OK] Installation successful"
Write-Host ""

# Launch the app using the specific device
Write-Host "========================================"
Write-Host "Step 3: Launching app..."
Write-Host "========================================"
adb -s $deviceAddress shell am start -n com.example.myapplication/.MainActivity

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[OK] App launched successfully!"
} else {
    Write-Host ""
    Write-Host "[WARNING] Could not automatically launch app"
    Write-Host "You can open it manually from the device launcher"
}

Write-Host ""
Write-Host "========================================"
Write-Host "Installation completed!"
Write-Host "========================================"
Write-Host ""
Write-Host "Debug APK installed to: $deviceAddress"
Write-Host "Debug APK location: $debugApkPath"
Write-Host "Existing APK preserved: $existingApkPath"
Write-Host ""
