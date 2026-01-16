#!/system/bin/sh
# Auto-grant USB permissions on boot without user interaction
# Handles MULTIPLE USB devices (ID Scanner + Nayax Chipi-X)
# Place in /system/bin/ and call from init.rc

LOG_TAG="USB_AUTO_GRANT"

# Log start
log -t $LOG_TAG "Starting USB auto-grant service (multi-device support)"

# Grant USB permission via input tap (simulates clicking "Allow")
# Coordinates for 1080x1920 display (rotated 180 degrees)
# Checkbox: 539, 973 | OK button: 906, 1083

# Track how many dialogs we've clicked
DIALOGS_CLICKED=0
MAX_DIALOGS=5  # Safety limit - don't click forever

# Track attempts without seeing a dialog
NO_DIALOG_COUNT=0
MAX_NO_DIALOG=10  # Exit after 10 seconds of no dialogs

# Total runtime limit
TOTAL_ATTEMPTS=0
MAX_TOTAL_ATTEMPTS=60  # Max 60 seconds total runtime

while [ $TOTAL_ATTEMPTS -lt $MAX_TOTAL_ATTEMPTS ] && [ $DIALOGS_CLICKED -lt $MAX_DIALOGS ]; do
    DIALOG_CHECK=$(dumpsys window | grep -i "UsbPermission" | wc -l)

    if [ "$DIALOG_CHECK" -gt 0 ]; then
        log -t $LOG_TAG "Permission dialog detected (attempt $TOTAL_ATTEMPTS), auto-clicking Allow"

        # Tap "Always open" checkbox at center of bounds
        input tap 539 973
        sleep 0.5

        # Tap "OK" button at center of bounds
        input tap 906 1083
        sleep 0.5

        DIALOGS_CLICKED=$((DIALOGS_CLICKED + 1))
        log -t $LOG_TAG "Auto-granted USB permission #$DIALOGS_CLICKED"

        # Reset the no-dialog counter - more dialogs may come
        NO_DIALOG_COUNT=0

        # Wait a moment for the next dialog to potentially appear
        sleep 1
    else
        # No dialog visible - increment counter
        NO_DIALOG_COUNT=$((NO_DIALOG_COUNT + 1))

        # If we've already clicked at least one dialog and haven't seen
        # another one for MAX_NO_DIALOG seconds, we're done
        if [ $DIALOGS_CLICKED -gt 0 ] && [ $NO_DIALOG_COUNT -ge $MAX_NO_DIALOG ]; then
            log -t $LOG_TAG "No new dialogs for $NO_DIALOG_COUNT seconds after granting $DIALOGS_CLICKED permissions"
            break
        fi

        # Check every 1 second
        sleep 1
    fi

    TOTAL_ATTEMPTS=$((TOTAL_ATTEMPTS + 1))
done

if [ $DIALOGS_CLICKED -eq 0 ]; then
    log -t $LOG_TAG "No permission dialogs appeared after $TOTAL_ATTEMPTS seconds"
else
    log -t $LOG_TAG "USB auto-grant service completed: $DIALOGS_CLICKED permission(s) granted"
fi
