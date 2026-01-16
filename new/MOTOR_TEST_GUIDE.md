# Motor Testing Guide - Quick Reference

**Last Updated:** 2025-12-09
**Device:** DMVI Vending Machine
**Status:** ✅ WORKING

---

## Quick Start - Test Motors

### 1. Start the DMVI Hardware Service

```bash
adb shell "am start-foreground-service com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"
```

**Wait 2-3 seconds for service to start**

### 2. Verify Service is Running

```bash
adb shell "netstat -lptn 2>/dev/null"
```

**Expected Output:**
```
tcp6  0  0 :::57482  :::*  LISTEN  2846/com.digitalmediavending.hardware
```

**Port:** `57482` (default, may vary)

### 3. Test Motor Command

**General Format:**
```bash
adb shell "echo '{\"col\":\"<COLUMN>\",\"method\":\"requestProductVend\",\"row\":\"<ROW>\",\"jsonrpc\":\"2.0\"}' | nc ::1 57482"
```

**Examples:**

```bash
# Test Motor at Row 1, Column 1
adb shell "echo '{\"col\":\"1\",\"method\":\"requestProductVend\",\"row\":\"1\",\"jsonrpc\":\"2.0\"}' | nc ::1 57482"

# Test Motor at Row 1, Column 5
adb shell "echo '{\"col\":\"5\",\"method\":\"requestProductVend\",\"row\":\"1\",\"jsonrpc\":\"2.0\"}' | nc ::1 57482"

# Test Motor at Row 2, Column 3
adb shell "echo '{\"col\":\"3\",\"method\":\"requestProductVend\",\"row\":\"2\",\"jsonrpc\":\"2.0\"}' | nc ::1 57482"
```

---

## Important Notes

### Protocol Details
- **Communication:** JSON-RPC 2.0 over TCP
- **Address:** IPv6 loopback `::1` (NOT `127.0.0.1`)
- **Port:** 57482 (verify with netstat)
- **Service:** `com.digitalmediavending.hardware`
- **Class:** `WallCoilMachineService`

### Row/Column Format
- **1-indexed** (not 0-indexed)
- Row: Typically 1 (unless multi-row vending machine)
- Column: 1-10 (or however many motors you have)

### Command Structure
```json
{
  "col": "5",                        // Column number (string)
  "method": "requestProductVend",    // Method name (fixed)
  "row": "1",                        // Row number (string)
  "jsonrpc": "2.0"                   // JSON-RPC version (fixed)
}
```

---

## Troubleshooting

### Service Not Running

**Symptom:** `Connection refused`

**Fix:**
```bash
# Start the service
adb shell "am start-foreground-service com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"

# Wait 3 seconds
sleep 3

# Verify it's running
adb shell "ps -A | grep digitalmedia"
```

### Wrong Port

**Symptom:** Connection times out or refuses

**Fix:** Find the actual port
```bash
adb shell "netstat -lptn 2>/dev/null | grep digitalmedia"
```

Look for the port number in the output (usually 57482)

### Service Crashes

**Check logs:**
```bash
adb logcat -d | grep -i -E "vend|motor|wall|coil" | tail -30
```

**Common Issue:** USB permission error
- The service may crash if it can't access USB devices
- Check logs for: `User has not given permission to access device`

### No Motor Movement

**Checks:**
1. Verify service is running: `adb shell "ps -A | grep digitalmedia"`
2. Check serial ports exist: `adb shell "ls -l /dev/ttyS0 /dev/ttyUSB*"`
3. Verify motor controller connected
4. Check DMVI service logs for errors

---

## Hardware Information

### Serial Ports
| Port | Device | Baud Rate | Purpose |
|------|--------|-----------|---------|
| `/dev/ttyS0` | UART | 115200 | Motor Controller |
| `/dev/ttyUSB0` | USB-Serial | 115200 | CP2102N bridge |
| `/dev/ttyUSB1-4` | USB-Serial | 115200 | Quectel LTE modem |
| `/dev/ttyACM0` | CDC-ACM | 115200 | Nayax Payment Terminal |

### Installed Packages
```bash
# Check what's installed
adb shell "pm list packages | grep -E 'dmvi|digitalmedia|myapplication'"
```

**Expected:**
- `package:com.digitalmediavending.hardware` ✅ (Required)
- `package:com.example.myapplication` (Your custom app)

---

## Advanced Commands

### Monitor Live Logs
```bash
adb logcat | grep -i -E "vend|motor|wall|coil"
```

### Stop Service
```bash
adb shell "am stopservice com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"
```

### Restart Service
```bash
# Stop
adb shell "am stopservice com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"

# Wait
sleep 2

# Start
adb shell "am start-foreground-service com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"
```

### Test All Motors (Loop)
```bash
# Test motors 1-10
for i in {1..10}; do
  echo "Testing motor $i..."
  adb shell "echo '{\"col\":\"$i\",\"method\":\"requestProductVend\",\"row\":\"1\",\"jsonrpc\":\"2.0\"}' | nc ::1 57482"
  sleep 5
done
```

---

## Windows PowerShell Version

For Windows users running PowerShell:

```powershell
# Start service
adb shell "am start-foreground-service com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"

# Test motor 1
adb shell "echo '{`"col`":`"1`",`"method`":`"requestProductVend`",`"row`":`"1`",`"jsonrpc`":`"2.0`"}' | nc ::1 57482"
```

**Note:** PowerShell requires backtick `` ` `` to escape quotes instead of backslash.

---

## Alternative: Direct Serial Port Method

If DMVI service is not available, you can send commands directly to the serial port:

### Wall Machine Protocol (0xAA format)
```bash
# Vend command for coil 1
adb shell "echo -ne '\xAA\x03\x01\x01\x01\xB0' > /dev/ttyS0"
```

**Format:**
```
[0xAA] [0x03] [0x01] [coil] [duration] [checksum]
 Header Length  Vend  Coil#  Qty/Time  Sum&0xFF
```

**Example - Coil 5:**
```bash
adb shell "echo -ne '\xAA\x03\x01\x05\x01\xB4' > /dev/ttyS0"
```

**Checksum:** 0xAA + 0x03 + 0x01 + coil + 0x01 = checksum

---

## Summary Cheat Sheet

```bash
# 1. Start service
adb shell "am start-foreground-service com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService"

# 2. Wait 3 seconds
sleep 3

# 3. Test motor (e.g., column 1)
adb shell "echo '{\"col\":\"1\",\"method\":\"requestProductVend\",\"row\":\"1\",\"jsonrpc\":\"2.0\"}' | nc ::1 57482"
```

**That's it!** 🎉

---

## References

- Full Protocol Documentation: [DMVI_Hardware_Protocol_Reference.md](DMVI_Hardware_Protocol_Reference.md)
- Implementation Guide: [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- System Analysis: [COMPREHENSIVE_SYSTEM_ANALYSIS.md](COMPREHENSIVE_SYSTEM_ANALYSIS.md)

---

**Document Version:** 1.0
**Status:** Verified Working
**Date:** December 9, 2025
