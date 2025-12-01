# Android Environment Verification Script
# Checks if Android SDK, tools, and emulator are properly configured

$ErrorActionPreference = "Continue"
$script:HasErrors = $false

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Android Environment Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if a command exists
function Test-Command {
    param([string]$Command)
    $null = Get-Command $Command -ErrorAction SilentlyContinue
    return $?
}

# Function to display status
function Write-Status {
    param(
        [string]$Message,
        [string]$Status
    )
    if ($Status -eq "OK") {
        Write-Host "[✓] $Message" -ForegroundColor Green
    } elseif ($Status -eq "FAIL") {
        Write-Host "[✗] $Message" -ForegroundColor Red
        $script:HasErrors = $true
    } elseif ($Status -eq "WARN") {
        Write-Host "[!] $Message" -ForegroundColor Yellow
    } else {
        Write-Host "[?] $Message" -ForegroundColor Gray
    }
}

# Step 1: Detect Android SDK path
Write-Host "1. Checking Android SDK path..." -ForegroundColor Yellow
$sdkPath = $null

# Try to read from local.properties
if (Test-Path "local.properties") {
    $localProps = Get-Content "local.properties"
    foreach ($line in $localProps) {
        if ($line -match '^sdk\.dir=(.+)$') {
            $sdkPath = $matches[1] -replace '\\', '\'
            break
        }
    }
}

# Try common locations if not found
if (-not $sdkPath -or -not (Test-Path $sdkPath)) {
    $commonPaths = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:USERPROFILE\AppData\Local\Android\Sdk",
        "C:\Android\Sdk"
    )
    
    foreach ($path in $commonPaths) {
        if (Test-Path $path) {
            $sdkPath = $path
            break
        }
    }
}

if ($sdkPath -and (Test-Path $sdkPath)) {
    Write-Status "SDK Path found: $sdkPath" "OK"
    $env:ANDROID_HOME = $sdkPath
    $env:ANDROID_SDK_ROOT = $sdkPath
} else {
    Write-Status "Android SDK path not found" "FAIL"
    Write-Host "  Please install Android Studio or set ANDROID_HOME environment variable" -ForegroundColor Red
    exit 1
}

# Step 2: Check SDK tools
Write-Host ""
Write-Host "2. Checking Android SDK tools..." -ForegroundColor Yellow

$platformToolsPath = Join-Path $sdkPath "platform-tools"
$emulatorPath = Join-Path $sdkPath "emulator"
$toolsPath = Join-Path $sdkPath "tools"
$toolsBinPath = Join-Path $sdkPath "tools\bin"

$adbPath = Join-Path $platformToolsPath "adb.exe"
$emulatorExePath = Join-Path $emulatorPath "emulator.exe"

if (Test-Path $adbPath) {
    Write-Status "adb found: $adbPath" "OK"
    $env:PATH = $platformToolsPath + ';' + $env:PATH
} else {
    Write-Status "adb not found at $adbPath" "FAIL"
}

if (Test-Path $emulatorExePath) {
    Write-Status "emulator found: $emulatorExePath" "OK"
    $env:PATH = $emulatorPath + ';' + $env:PATH
} else {
    Write-Status "emulator not found at $emulatorExePath" "WARN"
}

# Step 3: Check if tools are in PATH
Write-Host ""
Write-Host "3. Checking PATH configuration..." -ForegroundColor Yellow

if (Test-Command "adb") {
    $adbVersion = & adb version 2>&1 | Select-Object -First 1
    Write-Status "adb accessible in PATH: $adbVersion" "OK"
} else {
    Write-Status "adb not in PATH" "WARN"
    Write-Host "  Add to PATH: $platformToolsPath" -ForegroundColor Yellow
}

if (Test-Command "emulator") {
    Write-Status "emulator accessible in PATH" "OK"
} else {
    Write-Status "emulator not in PATH" "WARN"
    Write-Host "  Add to PATH: $emulatorPath" -ForegroundColor Yellow
}

# Step 4: List available AVDs
Write-Host ""
Write-Host "4. Checking available Android Virtual Devices (AVDs)..." -ForegroundColor Yellow

$avdManagerPath = Join-Path $toolsBinPath "avdmanager.bat"
if (-not (Test-Path $avdManagerPath)) {
    $toolsBinPath = Join-Path $sdkPath "cmdline-tools\latest\bin"
    $avdManagerPath = Join-Path $toolsBinPath "avdmanager.bat"
}

if (Test-Path $avdManagerPath) {
    $env:PATH = $toolsBinPath + ';' + $env:PATH
    try {
        $avdList = & $avdManagerPath list avd 2>&1
        $avdCount = 0
        $avdNames = @()
        
        foreach ($line in $avdList) {
            if ($line -match 'Name:\s+(.+)') {
                $avdNames += $matches[1].Trim()
                $avdCount++
            }
        }
        
        if ($avdCount -gt 0) {
            Write-Status "Found $avdCount AVD(s)" "OK"
            Write-Host ""
            foreach ($name in $avdNames) {
                Write-Host "  - $name" -ForegroundColor Cyan
            }
        } else {
            Write-Status "No AVDs found" "WARN"
            Write-Host '  Create an AVD in Android Studio: Tools > Device Manager > Create Device' -ForegroundColor Yellow
        }
    } catch {
        Write-Status "Could not list AVDs: $_" "WARN"
    }
} else {
    Write-Status "avdmanager not found" "WARN"
}

# Step 5: Check for running emulator
Write-Host ""
Write-Host "5. Checking for running emulator..." -ForegroundColor Yellow

if (Test-Command "adb") {
    try {
        $devices = & adb devices 2>&1
        $runningDevices = 0
        
        foreach ($line in $devices) {
            if ($line -match '^\S+\s+device$') {
                $runningDevices++
                $deviceId = ($line -split '\s+')[0]
                Write-Status "Emulator running: $deviceId" "OK"
            }
        }
        
        if ($runningDevices -eq 0) {
            Write-Status "No emulator currently running" "WARN"
            Write-Host "  Start an emulator using: start-emulator.bat or from Android Studio" -ForegroundColor Yellow
        }
    } catch {
        Write-Status "Could not check devices: $_" "WARN"
    }
} else {
    Write-Status "Cannot check devices (adb not available)" "WARN"
}

# Step 6: Check Gradle wrapper
Write-Host ""
Write-Host "6. Checking Gradle wrapper..." -ForegroundColor Yellow

if (Test-Path "gradlew.bat") {
    Write-Status "Gradle wrapper found (gradlew.bat)" "OK"
} else {
    Write-Status "Gradle wrapper not found" "FAIL"
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($script:HasErrors) {
    Write-Host "Verification completed with ERRORS" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please fix the errors above before proceeding." -ForegroundColor Red
    exit 1
} else {
    Write-Host "Verification completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. If emulator is not running, start it with: .\start-emulator.bat" -ForegroundColor White
    Write-Host "  2. Build and install app with: .\build-and-preview.bat" -ForegroundColor White
}
Write-Host "========================================" -ForegroundColor Cyan

