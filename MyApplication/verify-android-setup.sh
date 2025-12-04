#!/bin/bash
# Android Environment Verification Script
# Checks if Android SDK, tools, and emulator are properly configured

set -e

HAS_ERRORS=false

echo "========================================"
echo "Android Environment Verification"
echo "========================================"
echo ""

# Function to display status
write_status() {
    local message=$1
    local status=$2
    
    if [ "$status" = "OK" ]; then
        echo -e "\033[32m[✓]\033[0m $message"
    elif [ "$status" = "FAIL" ]; then
        echo -e "\033[31m[✗]\033[0m $message"
        HAS_ERRORS=true
    elif [ "$status" = "WARN" ]; then
        echo -e "\033[33m[!]\033[0m $message"
    else
        echo -e "\033[90m[?]\033[0m $message"
    fi
}

# Step 1: Detect Android SDK path
echo "1. Checking Android SDK path..."
SDK_PATH=""

# Try to read from local.properties
if [ -f "local.properties" ]; then
    SDK_PATH=$(grep "^sdk.dir=" local.properties | sed 's/^sdk.dir=//' | sed 's/\\//\//g')
    SDK_PATH=$(echo "$SDK_PATH" | sed 's/^C:/\/c/' | sed 's/\\/\//g')
fi

# Try common locations if not found
if [ -z "$SDK_PATH" ] || [ ! -d "$SDK_PATH" ]; then
    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
        SDK_PATH="$ANDROID_HOME"
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
        SDK_PATH="$ANDROID_SDK_ROOT"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        SDK_PATH="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        SDK_PATH="$HOME/Android/Sdk"
    fi
fi

if [ -n "$SDK_PATH" ] && [ -d "$SDK_PATH" ]; then
    write_status "SDK Path found: $SDK_PATH" "OK"
    export ANDROID_HOME="$SDK_PATH"
    export ANDROID_SDK_ROOT="$SDK_PATH"
else
    write_status "Android SDK path not found" "FAIL"
    echo "  Please install Android Studio or set ANDROID_HOME environment variable"
    exit 1
fi

# Step 2: Check SDK tools
echo ""
echo "2. Checking Android SDK tools..."

PLATFORM_TOOLS_PATH="$SDK_PATH/platform-tools"
EMULATOR_PATH="$SDK_PATH/emulator"
TOOLS_PATH="$SDK_PATH/tools"
TOOLS_BIN_PATH="$SDK_PATH/tools/bin"

ADB_PATH="$PLATFORM_TOOLS_PATH/adb"
EMULATOR_EXE_PATH="$EMULATOR_PATH/emulator"

if [ -f "$ADB_PATH" ]; then
    write_status "adb found: $ADB_PATH" "OK"
    export PATH="$PLATFORM_TOOLS_PATH:$PATH"
else
    write_status "adb not found at $ADB_PATH" "FAIL"
fi

if [ -f "$EMULATOR_EXE_PATH" ]; then
    write_status "emulator found: $EMULATOR_EXE_PATH" "OK"
    export PATH="$EMULATOR_PATH:$PATH"
else
    write_status "emulator not found at $EMULATOR_EXE_PATH" "WARN"
fi

# Step 3: Check if tools are in PATH
echo ""
echo "3. Checking PATH configuration..."

if command -v adb >/dev/null 2>&1; then
    ADB_VERSION=$(adb version 2>&1 | head -n 1)
    write_status "adb accessible in PATH: $ADB_VERSION" "OK"
else
    write_status "adb not in PATH" "WARN"
    echo "  Add to PATH: $PLATFORM_TOOLS_PATH"
fi

if command -v emulator >/dev/null 2>&1; then
    write_status "emulator accessible in PATH" "OK"
else
    write_status "emulator not in PATH" "WARN"
    echo "  Add to PATH: $EMULATOR_PATH"
fi

# Step 4: List available AVDs
echo ""
echo "4. Checking available Android Virtual Devices (AVDs)..."

AVD_MANAGER_PATH="$TOOLS_BIN_PATH/avdmanager"
if [ ! -f "$AVD_MANAGER_PATH" ]; then
    TOOLS_BIN_PATH="$SDK_PATH/cmdline-tools/latest/bin"
    AVD_MANAGER_PATH="$TOOLS_BIN_PATH/avdmanager"
fi

if [ -f "$AVD_MANAGER_PATH" ]; then
    export PATH="$TOOLS_BIN_PATH:$PATH"
    AVD_LIST=$("$AVD_MANAGER_PATH" list avd 2>&1 || true)
    AVD_COUNT=0
    AVD_NAMES=()
    
    while IFS= read -r line; do
        if [[ $line =~ Name:[[:space:]]+(.+) ]]; then
            AVD_NAMES+=("${BASH_REMATCH[1]}")
            ((AVD_COUNT++))
        fi
    done <<< "$AVD_LIST"
    
    if [ $AVD_COUNT -gt 0 ]; then
        write_status "Found $AVD_COUNT AVD(s)" "OK"
        echo ""
        for name in "${AVD_NAMES[@]}"; do
            echo "  - $name"
        done
    else
        write_status "No AVDs found" "WARN"
        echo "  Create an AVD in Android Studio: Tools > Device Manager > Create Device"
    fi
else
    write_status "avdmanager not found" "WARN"
fi

# Step 5: Check for running emulator
echo ""
echo "5. Checking for running emulator..."

if command -v adb >/dev/null 2>&1; then
    DEVICES=$(adb devices 2>&1 || true)
    RUNNING_DEVICES=0
    
    while IFS= read -r line; do
        if [[ $line =~ ^[^[:space:]]+[[:space:]]+device$ ]]; then
            DEVICE_ID=$(echo "$line" | awk '{print $1}')
            write_status "Emulator running: $DEVICE_ID" "OK"
            ((RUNNING_DEVICES++))
        fi
    done <<< "$DEVICES"
    
    if [ $RUNNING_DEVICES -eq 0 ]; then
        write_status "No emulator currently running" "WARN"
        echo "  Start an emulator using: ./start-emulator.sh or from Android Studio"
    fi
else
    write_status "Cannot check devices (adb not available)" "WARN"
fi

# Step 6: Check Gradle wrapper
echo ""
echo "6. Checking Gradle wrapper..."

if [ -f "gradlew" ]; then
    write_status "Gradle wrapper found (gradlew)" "OK"
else
    write_status "Gradle wrapper not found" "FAIL"
fi

# Summary
echo ""
echo "========================================"
if [ "$HAS_ERRORS" = true ]; then
    echo -e "\033[31mVerification completed with ERRORS\033[0m"
    echo ""
    echo "Please fix the errors above before proceeding."
    exit 1
else
    echo -e "\033[32mVerification completed successfully!\033[0m"
    echo ""
    echo "Next steps:"
    echo "  1. If emulator is not running, start it with: ./start-emulator.sh"
    echo "  2. Build and install app with: ./build-and-preview.sh"
fi
echo "========================================"

