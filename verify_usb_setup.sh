#!/bin/bash
# USB Auto-Grant Setup Verification Script
# Run this to verify all changes are in place

echo "========================================="
echo "USB Auto-Grant Setup Verification"
echo "========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_file() {
    local file=$1
    local desc=$2

    if adb shell "su -c 'test -f $file && echo exists'" 2>/dev/null | grep -q "exists"; then
        echo -e "${GREEN}✓${NC} $desc"
        echo "  Location: $file"
        return 0
    else
        echo -e "${RED}✗${NC} $desc"
        echo "  Missing: $file"
        return 1
    fi
}

check_system_app() {
    if adb shell "pm list packages -s | grep myapplication" 2>/dev/null | grep -q "myapplication"; then
        echo -e "${GREEN}✓${NC} App is installed as system app"
        adb shell "pm path com.example.myapplication" 2>/dev/null | sed 's/^/  /'
        return 0
    else
        echo -e "${RED}✗${NC} App is NOT a system app"
        return 1
    fi
}

check_scanner_connection() {
    if adb shell "lsusb | grep 0403:6001" 2>/dev/null | grep -q "M260"; then
        echo -e "${GREEN}✓${NC} Scanner is connected"
        adb shell "lsusb | grep 0403" 2>/dev/null | sed 's/^/  /'
        return 0
    else
        echo -e "${YELLOW}⚠${NC} Scanner not detected (might be disconnected)"
        return 1
    fi
}

check_scanner_initialized() {
    if adb logcat -d 2>/dev/null | grep -q "Scanner initialized and ready"; then
        echo -e "${GREEN}✓${NC} Scanner initialized successfully"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} Scanner not initialized yet"
        return 1
    fi
}

check_baud_rate() {
    local apk_path=$(adb shell "pm path com.example.myapplication" 2>/dev/null | cut -d: -f2 | tr -d '\r')
    if [ -n "$apk_path" ]; then
        echo -e "${GREEN}✓${NC} Checking baud rate in APK..."
        echo "  APK location: $apk_path"
        echo "  Note: Verify SERIAL_BAUD_RATE = 9600 in source code"
    else
        echo -e "${YELLOW}⚠${NC} Cannot verify baud rate (APK not found)"
    fi
}

# Check device connection
echo "1. Device Connection"
echo "-------------------"
if adb get-state 2>/dev/null | grep -q "device"; then
    echo -e "${GREEN}✓${NC} Device connected via ADB"
    echo ""
else
    echo -e "${RED}✗${NC} Device not connected"
    echo ""
    echo "Please connect device and try again."
    exit 1
fi

# Check system files
echo "2. System Files"
echo "---------------"
check_file "/system/bin/auto_usb_grant.sh" "Auto-tap script"
check_file "/system/etc/init/auto_usb_grant.rc" "Init service config"
check_file "/data/system/users/0/usb_device_manager.xml" "USB device preferences"
check_file "/data/system/usb/device_permissions.xml" "USB permission filter"
echo ""

# Check system app
echo "3. System App Status"
echo "--------------------"
check_system_app
echo ""

# Check scanner hardware
echo "4. Scanner Hardware"
echo "-------------------"
check_scanner_connection
echo ""

# Check scanner initialization
echo "5. Scanner Initialization"
echo "-------------------------"
check_scanner_initialized
echo ""

# Check baud rate
echo "6. Baud Rate Configuration"
echo "--------------------------"
check_baud_rate
echo ""

# Check BootReceiver
echo "7. BootReceiver Status"
echo "----------------------"
if adb logcat -d 2>/dev/null | grep -q "HardwareService started"; then
    echo -e "${GREEN}✓${NC} BootReceiver starts HardwareService on boot"
    adb logcat -d 2>/dev/null | grep "BootReceiver" | tail -3 | sed 's/^/  /'
else
    echo -e "${YELLOW}⚠${NC} BootReceiver logs not found (may need reboot or rebuild)"
fi
echo ""

# Check USB permissions
echo "8. USB Permission Status"
echo "------------------------"
echo "Current USB device preferences:"
adb shell "su -c 'cat /data/system/users/0/usb_device_manager.xml 2>/dev/null'" | grep "usb-device" | sed 's/^/  /'
echo ""

# Check whitelist
echo "9. Battery Optimization"
echo "-----------------------"
if adb shell "dumpsys deviceidle whitelist 2>/dev/null" | grep -q "myapplication"; then
    echo -e "${GREEN}✓${NC} App is whitelisted for background starts"
else
    echo -e "${YELLOW}⚠${NC} App not whitelisted (may need: dumpsys deviceidle whitelist +com.example.myapplication)"
fi
echo ""

# Summary
echo "========================================="
echo "Verification Complete"
echo "========================================="
echo ""
echo "Next Steps:"
echo "1. If scanner not initialized, check logs: adb logcat | grep IdScannerManager"
echo "2. If permission dialog appears on boot, configure AirDroid Business"
echo "3. Test scanner by scanning a driver's license in the app"
echo ""
echo "For detailed setup: See c:/dev/AIRDROID_USB_SETUP.md"
echo ""
