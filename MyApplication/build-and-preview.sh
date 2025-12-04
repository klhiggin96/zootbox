#!/bin/bash
# Build and Install Script for Android Preview
# Builds the debug APK and installs it to a running emulator

set -e

echo "========================================"
echo "Android Build and Preview"
echo "========================================"
echo ""

ERROR_OCCURRED=0

# Check if Gradle wrapper exists
if [ ! -f "gradlew" ]; then
    echo "[ERROR] gradlew not found. Are you in the project root directory?"
    exit 1
fi

# Function to find SDK path
find_sdk_path() {
    # Try to read from local.properties
    if [ -f "local.properties" ]; then
        SDK_PATH=$(grep "^sdk.dir=" local.properties | sed 's/^sdk.dir=//' | sed 's/\\//\//g')
        SDK_PATH=$(echo "$SDK_PATH" | sed 's/^C:/\/c/' | sed 's/\\/\//g')
    fi
    
    # Try environment variables
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
    
    echo "$SDK_PATH"
}

# Check if adb is available
if ! command -v adb >/dev/null 2>&1; then
    echo "[WARNING] adb not found in PATH. Trying to find it..."
    
    SDK_PATH=$(find_sdk_path)
    
    if [ -n "$SDK_PATH" ] && [ -d "$SDK_PATH/platform-tools" ]; then
        export PATH="$SDK_PATH/platform-tools:$PATH"
        echo "Found SDK at: $SDK_PATH"
    else
        echo "[ERROR] Could not find Android SDK. Please add platform-tools to PATH."
        exit 1
    fi
fi

# Check for running emulator
echo "Checking for running emulator..."
if ! adb devices | grep -q "device$"; then
    echo "[ERROR] No emulator is currently running."
    echo ""
    echo "Please start an emulator first:"
    echo "  1. Run: ./start-emulator.sh"
    echo "  2. Or start from Android Studio: Tools > Device Manager"
    echo ""
    exit 1
fi

echo "[OK] Emulator found"
echo ""

# Build the debug APK
echo "========================================"
echo "Step 1: Building debug APK..."
echo "========================================"
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Build failed. Please check the errors above."
    ERROR_OCCURRED=1
else
    echo ""
    echo "[OK] Build successful"
    echo ""
fi

if [ $ERROR_OCCURRED -eq 0 ]; then
    # Install to emulator
    echo "========================================"
    echo "Step 2: Installing to emulator..."
    echo "========================================"
    ./gradlew installDebug
    if [ $? -ne 0 ]; then
        echo ""
        echo "[ERROR] Installation failed. Please check the errors above."
        ERROR_OCCURRED=1
    else
        echo ""
        echo "[OK] Installation successful"
        echo ""
    fi
fi

if [ $ERROR_OCCURRED -eq 0 ]; then
    # Launch the app
    echo "========================================"
    echo "Step 3: Launching app..."
    echo "========================================"
    adb shell monkey -p com.example.myapplication -c android.intent.category.LAUNCHER 1
    if [ $? -eq 0 ]; then
        echo ""
        echo "[OK] App launched successfully!"
    else
        echo ""
        echo "[WARNING] Could not automatically launch app. You can open it manually from the emulator launcher."
    fi
fi

echo ""
echo "========================================"
if [ $ERROR_OCCURRED -eq 1 ]; then
    echo "Build/Install completed with ERRORS"
    echo "Please fix the errors above."
    exit 1
else
    echo "Build/Install completed successfully!"
    echo ""
    echo "The app should now be visible on your emulator."
    echo "If it didn't launch automatically, find it in the app launcher."
fi
echo "========================================"

