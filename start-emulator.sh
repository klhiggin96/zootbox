#!/bin/bash
# Start Android Emulator Script
# Automatically detects and starts an available AVD

echo "========================================"
echo "Android Emulator Launcher"
echo "========================================"
echo ""

# Check if emulator is already running
if command -v adb >/dev/null 2>&1; then
    if adb devices | grep -q "device$"; then
        echo "[OK] An emulator is already running."
        adb devices
        echo ""
        echo "If you want to start a different emulator, please stop the current one first."
        exit 0
    fi
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

SDK_PATH=$(find_sdk_path)

if [ -z "$SDK_PATH" ] || [ ! -d "$SDK_PATH" ]; then
    echo "[ERROR] Could not find Android SDK path."
    echo "Please ensure Android Studio is installed or set ANDROID_HOME environment variable."
    exit 1
fi

echo "[OK] Found SDK at: $SDK_PATH"
echo ""

# Add emulator to PATH
export PATH="$SDK_PATH/emulator:$SDK_PATH/platform-tools:$PATH"

# Check if emulator command is available
if ! command -v emulator >/dev/null 2>&1; then
    echo "[ERROR] emulator command not found. Is the Android SDK properly installed?"
    exit 1
fi

# List available AVDs
echo "Listing available Android Virtual Devices..."
echo ""

AVD_LIST=$(emulator -list-avds 2>&1)
AVD_COUNT=0
AVD_ARRAY=()

while IFS= read -r line; do
    if [ -n "$line" ]; then
        AVD_ARRAY+=("$line")
        echo "  [$AVD_COUNT] $line"
        ((AVD_COUNT++))
    fi
done <<< "$AVD_LIST"

if [ $AVD_COUNT -eq 0 ]; then
    echo ""
    echo "[ERROR] No AVDs found. Please create an AVD first:"
    echo "  1. Open Android Studio"
    echo "  2. Go to: Tools > Device Manager"
    echo "  3. Click: Create Device"
    echo ""
    exit 1
fi

echo ""
if [ $AVD_COUNT -eq 1 ]; then
    SELECTED_AVD="${AVD_ARRAY[0]}"
    echo "[OK] Found 1 AVD. Starting: $SELECTED_AVD"
else
    SELECTED_AVD="${AVD_ARRAY[0]}"
    echo "Found $AVD_COUNT AVD(s). Starting the first one: $SELECTED_AVD"
    echo ""
    echo "To start a different AVD, run:"
    echo "  emulator -avd <AVD_NAME>"
    echo ""
fi

echo ""
echo "========================================"
echo "Starting emulator: $SELECTED_AVD"
echo "========================================"
echo ""
echo "This may take a minute or two. The emulator window will open when ready."
echo ""
echo "To check when the emulator is ready, run: adb devices"
echo ""

# Start emulator in background
emulator -avd "$SELECTED_AVD" > /dev/null 2>&1 &

echo "Emulator is starting in the background..."
echo ""
echo "You can check emulator status by running: adb devices"
echo ""
echo "Once the emulator shows \"device\" status, you can build and install with:"
echo "  ./build-and-preview.sh"
echo ""

sleep 3

