#!/system/bin/sh
# Auto-grant USB permissions for vending machine

# Wait for system to fully boot
sleep 30

# Grant permission to scanner
pm grant com.example.myapplication android.permission.MANAGE_USB 2>/dev/null

# Force grant via dumpsys
dumpsys usb grant-permission 1027 24577 com.example.myapplication 0
dumpsys usb grant-permission 9969 22096 com.example.myapplication 0

# Restart app to apply
am force-stop com.example.myapplication
sleep 2
am start -n com.example.myapplication/.MainActivity
