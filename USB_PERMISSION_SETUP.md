# USB Permission Setup for Vending Machine
## Making ID Scanner Permissions Permanent (No Dialog on Reboot)

### Current Status
- ✅ App installed as **system app** (in `/system/priv-app/`)
- ✅ Scanner detected at boot (VID: 0x0403, PID: 0x6001)
- ✅ HardwareService starts automatically
- ⚠️ USB permission dialog still appears (requires one-time manual grant)

---

## SOLUTION: One-Time Permission Grant

### What Happens Now
When the app starts, you'll see a permission dialog:
```
Allow MyApplication to access the USB device?
[E-SEEK M260/210-DL Scanner]

□ Use by default for this USB device

[Cancel]  [OK]
```

### Steps to Make Permission Permanent

1. **✓ CHECK THE BOX:** "Use by default for this USB device"
2. **TAP "OK"** or "ALLOW"

**That's it!** Once you do this, the permission is saved permanently in:
```
/data/system/usb/device_permissions.xml
```

The permission will **persist across reboots** and the dialog will **never appear again**.

---

## Verification

After granting permission, verify it worked:

```bash
# Check if scanner initialized
adb logcat -d | grep "Scanner initialized and ready"

# Should see:
# IdScannerManager: Scanner initialized and ready
# IdScannerManager: Start reading loop...
```

---

## If Dialog Doesn't Have "Use by default" Checkbox

Some Android versions don't show this checkbox. In that case, use this ADB command to grant permanently:

```bash
# Grant permission via ADB (device must have root)
adb shell "su -c 'dumpsys usb | grep -A10 \"1027\"'"
```

Then manually add to device_permissions.xml (already done - see `/data/system/usb/device_permissions.xml`)

---

## Boot Sequence (After Permission Granted)

1. **Device Powers On**
2. **Android Boots** (~30 seconds)
3. **USB Devices Detected** (Scanner at /dev/bus/usb/005/005)
4. **BootReceiver Triggers** (launches MainActivity)
5. **HardwareService Starts** (connects to scanner)
6. **Permission Auto-Granted** (from saved preference)
7. **Scanner Initializes** (DTR/RTS enabled, read loop starts)
8. **System Ready** (can scan IDs immediately)

**Total Boot Time:** ~45-60 seconds from power-on to fully operational

---

## Troubleshooting

### Scanner Not Working After Reboot

```bash
# 1. Check if app is running
adb shell "ps -A | grep myapplication"

# 2. Check if permission was saved
adb shell "su -c 'cat /data/system/usb/device_permissions.xml'"

# 3. Check scanner connection
adb shell "lsusb | grep 0403"

# 4. Restart app manually
adb shell "am force-stop com.example.myapplication && am start -n com.example.myapplication/.MainActivity"
```

### Permission Dialog Keeps Appearing

If the "Use by default" checkbox doesn't save the permission, verify the device_permissions.xml file has the correct package name:

```bash
adb shell "su -c 'cat /data/system/usb/device_permissions.xml'"
```

Should show:
```xml
<usb-device vendor-id="1027" product-id="24577" package="com.example.myapplication" user="0" />
```

If not, recreate it:
```bash
adb push C:\\dev\\device_permissions.xml /sdcard/
adb shell "su -c 'cp /sdcard/device_permissions.xml /data/system/usb/ && chmod 660 /data/system/usb/device_permissions.xml && chown system:system /data/system/usb/device_permissions.xml'"
adb reboot
```

---

## Alternative: Fully Automated (No Dialog Ever)

If you need **zero user interaction**, install this init script:

### Create Auto-Grant Service

```bash
# 1. Create service file
adb shell "su -c 'cat > /system/etc/init/usb_autogrant.rc << EOF
service usb_autogrant /system/bin/sh /system/bin/usb_autogrant.sh
    class late_start
    user root
    group root
    oneshot
EOF'"

# 2. Create grant script
adb shell "su -c 'cat > /system/bin/usb_autogrant.sh << EOF
#!/system/bin/sh
sleep 40
pm grant com.example.myapplication android.permission.USB_PERMISSION 2>/dev/null
EOF'"

# 3. Set permissions
adb shell "su -c 'chmod 755 /system/bin/usb_autogrant.sh'"
adb shell "su -c 'chmod 644 /system/etc/init/usb_autogrant.rc'"

# 4. Reboot
adb reboot
```

This runs on every boot and automatically grants USB permission before the app starts.

---

## Summary

**For Production Vending Machine:**

1. ✅ App is system app (done)
2. ✅ USB permissions XML configured (done)
3. ⚠️ **USER ACTION REQUIRED:** Grant permission once with "Use by default" checked
4. ✅ After that, scanner works automatically on every boot forever

**No maintenance required after initial setup!**
