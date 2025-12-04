# DMVI Vending Machine Hardware Protocol Reference

**Last Updated:** 2025-01-XX  
**Device:** DMVI LY-230918 Vending Machine  
**Controller:** SECO SYS-C31-DMV Board (Rockchip RK3399K)  
**OS:** Android 11 (API 30), userdebug build with root access

---

## Table of Contents

1. [Serial Port Assignments](#serial-port-assignments)
2. [Motor Control Protocol](#motor-control-protocol)
3. [Payment/MDB Protocol (Nayax)](#paymentmdb-protocol-nayax)
4. [ID Scanner Protocol](#id-scanner-protocol)
5. [QR Code Scanner Protocol](#qr-code-scanner-protocol)
6. [Response Parsing](#response-parsing)
7. [Boot Sequence & Kiosk Mode](#boot-sequence--kiosk-mode)
8. [Implementation Notes](#implementation-notes)

---

## Serial Port Assignments

| Port | Device | Baud Rate | Purpose |
|------|--------|-----------|---------|
| `/dev/ttyS0` | UART (Physical) | 115200 | Motor Control |
| `/dev/ttyUSB1` or `/dev/ttyUSB2` | USB-to-Serial (FTDI) | 115200 | Payment Terminal (Nayax) |
| `/dev/ttyUSB3`, `/dev/ttyUSB4` | USB-to-Serial | - | LTE Modem (RIL) |

**Note:** Motor port requires root access. Payment terminal uses USB serial drivers.

---

## Motor Control Protocol

### Overview
- **Baud Rate:** 115200
- **Data Bits:** 8
- **Stop Bits:** 1
- **Parity:** None (0)
- **Flow Control:** None
- **Library:** `android-serialport-api` (licheedev)

### Command Format

All commands follow this structure:
```
[Header] [Length] [Command] [Data...] [Checksum]
```

#### Vend Command (Dispense Product)
```
[0xAA] [0x03] [0x01] [coil] [duration] [checksum]
```

**Bytes:**
- `0xAA` (170): Header/Start byte
- `0x03` (3): Length field
- `0x01` (1): Command type (FUNCTION_VENDING_BIT)
- `coil`: Coil number (0-based in code, but typically 1-based for users)
- `duration`: Duration/quantity parameter
- `checksum`: Sum of all previous bytes, lower 8 bits (sum & 0xFF)

**Example - Vend Coil 1:**
```
0xAA 0x03 0x01 0x01 0x01 0xB0
```
Calculation: 170 + 3 + 1 + 1 + 1 = 176 (0xB0)

#### Door Lock Command
```
[0xAA] [0x03] [0x02] [action] [param] [checksum]
```

**Bytes:**
- `0x02`: Command type (FUNCTION_ELECTROMAGNETIC_DOOR_BIT)

#### Scan Trays Command
```
[0xAA] [0x02] [0x03] [tray] [checksum]
```

**Bytes:**
- `0x03`: Command type (FUNCTION_SCAN_TRAY_BIT)
- `tray`: Tray number (0 = all trays)

#### Reset Trays Command
```
[0xAA] [0x03] [0x04] [tray1] [tray2] [checksum]
```

**Bytes:**
- `0x04`: Command type (FUNCTION_RESET_TRAY_BIT)
- `tray1`, `tray2`: Tray numbers (255 = all trays)

### Response Format

All responses follow the same structure:
```
[0xAA] [length] [command_type] [data...] [checksum]
```

#### Vending Response (command_type = 0x01)
```
[0xAA] [length] [0x01] [row] [col] [status] [checksum]
```

**Status Codes:**
- `0x00`: SUCCESS
- `0x01`: FAILED
- `0x02`: TIMED_OUT

**Example Response:**
```
0xAA 0x05 0x01 0x01 0x01 0x00 0xB1
```
- Row: 1, Col: 1, Status: SUCCESS

#### Heartbeat Response (command_type = 0x00)
```
[0xAA] [length] [0x00] [status_byte] [checksum]
```

**Status Byte (Binary bits):**
- Bits 0-2: RESERVE_BIT
- Bit 3: ACCESS_CONTROL (1=TRIGGERED, 0=RELEASED)
- Bit 4: LOCK_1_OUT_STRETCHED (1=EXTENDED, 0=NA)
- Bit 5: LOCK_1_RETRACTED (1=RETRACTED, 0=NA)
- Bit 6: LOCK_2_OUT_STRETCHED (1=EXTENDED, 0=NA)
- Bit 7: LOCK_2_RETRACTED (1=RETRACTED, 0=NA)

#### Door Lock Response (command_type = 0x02)
```
[0xAA] [length] [0x02] [status] [checksum]
```

**Status Codes:**
- `0x00`: EXECUTED_IMMEDIATELY
- `0x01`: DEVICE_BUSY

#### Scan Trays Response (command_type = 0x03)
```
[0xAA] [length] [0x03] [tray_data...] [checksum]
```

**Format:** 34 bytes total
- Bytes 3-32: Tray data (10 trays × 3 bytes each)
- Each tray: 3 bytes converted to 24-bit binary string
- Each bit represents a column (1 = occupied, 0 = empty)

**Parsing Example:**
```kotlin
// For each tray (i = 3 to 32, step 3):
val trayNum = i / 3
val binary = intToBin(data[i]) + intToBin(data[i+1]) + intToBin(data[i+2])
// Read bits from right to left (LSB first)
for (bitPos in binary.length-1 downTo 0) {
    if (binary[bitPos] == '1') {
        val col = binary.length - bitPos
        // Tray trayNum, Column col is occupied
    }
}
```

#### Reset Trays Response (command_type = 0x04)
```
[0xAA] [length] [0x04] [status] [checksum]
```

**Status Codes:**
- `0x00`: RESET_SUCCESSFUL
- `0x01`: RESET_FAILED

### Checksum Algorithm

```kotlin
fun calculateChecksum(data: List<Int>): Int {
    var sum = 0
    for (value in data) {
        sum += value
    }
    return sum and 0xFF  // Lower 8 bits
}
```

**Note:** This matches `Constants.DecToBin(sum, true) -> BinToDec(...)` which extracts lower 8 bits.

### Constants Reference

From `ConstantsWallMachine`:
- `HEADER_BIT = 170` (0xAA)
- `FUNCTION_HEARTBEAT_BIT = 0`
- `FUNCTION_VENDING_BIT = 1`
- `FUNCTION_ELECTROMAGNETIC_DOOR_BIT = 2`
- `FUNCTION_SCAN_TRAY_BIT = 3`
- `FUNCTION_RESET_TRAY_BIT = 4`

---

## Payment/MDB Protocol (Nayax)

### Overview
- **Baud Rate:** 115200
- **Data Bits:** 8
- **Stop Bits:** 1
- **Parity:** None (0)
- **Protocol:** MDB (Multi-Drop Bus) - handled by Nayax SDK
- **Library:** `com.bitmick.marshall.vmc.vmc_framework` (Nayax SDK)

### Hardware Connection
- **Type:** USB-to-Serial (FTDI chip)
- **Port Discovery:** Uses `UsbSerialProber` to find FTDI drivers
- **Port Selection:** Iterates through available USB serial devices until connection succeeds

### SDK Initialization

```kotlin
// Configuration
val vmc_config = vmc_configuration()
vmc_config.port_vpos = usbSerialPort  // UsbSerialPort from UsbSerialProber
vmc_config.port_vpos_baud = 115200
vmc_config.model = "android-marshall-demo"
vmc_config.serial = "1434324619381374"
vmc_config.sw_ver = "1.0.0.0"
vmc_config.multi_vend_support = true
vmc_config.multi_session_support = false
vmc_config.reader_always_on = false
vmc_config.always_idle = false

// Initialize framework
val m_vmc = vmc_framework.getInstance()
m_vmc.link.set_lowlevel(lowlevel_serial_ftdi())
m_vmc.link.configure(vmc_config)
m_vmc.vend.register_callbacks(vend_callbacks)
m_vmc.link.start()
```

### Key SDK Methods

#### Start Payment Session
```kotlin
m_vmc.vend.session_start(0)  // 0 = credit session
```

#### Request Vend
```kotlin
val items = ArrayList<vend_item_t>()
items.add(vend_item_t(1, priceInCents, quantity, 1))
val session = vend_session_t(items)
m_vmc.vend.vend_request(session)
```

#### Cancel Session
```kotlin
m_vmc.vend.session_cancel()
```

#### Check Connection
```kotlin
val isReady = m_vmc.link.is_ready()
```

### Callback Events

The SDK provides callbacks through `vend_callbacks_t`:

- `onReady(session)`: Payment terminal ready
- `onSessionBegin(sessionType)`: Payment session started
- `onVendApproved(session)`: Vend approved by payment terminal
- `onVendDenied(session)`: Vend denied
- `onSettlement(success)`: Settlement complete
- `onTransactionInfo(data)`: Transaction details received
- `onReaderState(enabled)`: Card reader state changed

### Low-Level Serial Bridge

The SDK uses `lowlevel_serial_ftdi` which:
- Implements `lowlevel_i` interface
- Uses `UsbSerialPort` for communication
- Handles MDB protocol framing automatically
- Reads responses with length prefix (first 2 bytes = length)

**Transmit Method:**
```kotlin
fun transmit(data: ByteArray, len: Int): Boolean {
    val buffer = ByteArray(len)
    System.arraycopy(data, 0, buffer, 0, len)
    m_serial_port_ftdi.write(buffer, 0)
    return true
}
```

**Note:** MDB protocol details are abstracted by the SDK. Direct byte-level commands are not needed.

---

## ID Scanner Protocol

### Overview
- **Type:** USB FTDI Device
- **Vendor ID:** 1027 (0x0403)
- **Product ID:** 24577 (0x6001)
- **SDK:** `net.idscan.components.android.hwreaders` (IDScan SDK)
- **Parser:** `net.idscan.android.dlparser.DLParser`

### Document Types Supported
- PDF417 (2D barcode)
- Magnetic Stripe
- OCR MRZ (Machine Readable Zone)

### Initialization

```kotlin
val ftdiMgr = FTDIMgr.INSTANCE.create(context)
ftdiMgr.setAcceptableDocumentTypes(
    HashSet(Arrays.asList(
        DocumentType.PDF417,
        DocumentType.MAGNETIC_STRIPE,
        DocumentType.OCR_MRZ
    ))
)
ftdiMgr.enable()

// Find device by VID/PID
val devices = ftdiMgr.getDeviceList()
for (device in devices) {
    if (device.getVid() == 1027 && device.getPid() == 24577) {
        scannerDevice = device
        scannerDevice.connect()
        break
    }
}
```

### Data Parsing

```kotlin
val parser = DLParser()
parser.setup(context, vendorID)  // vendorID = license key
val result = parser.parse(observedData)

// Extracted fields:
result.firstName
result.middleName
result.lastName
result.address1
result.address2
result.birthdate
result.expirationDate
```

### Response Format

```json
{
  "responseType": "idScan",
  "userInfo": {
    "firstName": "...",
    "middleName": "...",
    "lastName": "...",
    "address1": "...",
    "address2": "...",
    "birthdate": "...",
    "expirationDate": "...",
    "parserVersion": "..."
  }
}
```

**Note:** Requires commercial IDScan SDK license. Cannot be replicated without SDK.

---

## QR Code Scanner Protocol

### Overview
- **Type:** USB Serial Device
- **Baud Rate:** 9600
- **Data Bits:** 8
- **Stop Bits:** 1
- **Parity:** None (0)
- **Flow Control:** None (0)
- **Library:** `com.felhr.usbserial` (felhr USB Serial library)

### Supported Devices

Multiple VID/PID combinations:
- VID: 10205, PID: 2
- VID: 259, PID: 24673
- VID: 9969, PID: 22096
- VID: 9969, PID: 53249
- VID: 1155, PID: 22336

### Initialization

```kotlin
val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
val deviceList = usbManager.deviceList

// Find matching device
for (device in deviceList.values) {
    val vid = device.vendorId
    val pid = device.productId
    if (isValidDevice(vid, pid)) {
        val connection = usbManager.openDevice(device)
        val serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
        serialPort.open()
        serialPort.setBaudRate(9600)
        serialPort.setDataBits(8)
        serialPort.setStopBits(1)
        serialPort.setParity(0)
        serialPort.setFlowControl(0)
        serialPort.read(readCallback)
        break
    }
}
```

### Data Format

- **Encoding:** UTF-8 string
- **Read Callback:** Receives byte array, converts to string
- **Auto-disconnect:** Disconnects after successful read (500ms delay)

### Response Format

```json
{
  "responseType": "qrCodeScan",
  "status": "SUCCESS",
  "message": "READ_DATA_CALL_BACK",
  "usbDevice": "[DeviceName: ..., VID: ..., PID: ...]",
  "qrCodeData": "scanned_string_data"
}
```

---

## Response Parsing

### Motor Response Parser

```kotlin
fun parseMotorResponse(data: List<Int>): MotorResponse? {
    if (data.size < 5 || data[0] != 0xAA) {
        return null  // Invalid header
    }
    
    val commandType = data[2]
    when (commandType) {
        0x00 -> parseHeartbeat(data)
        0x01 -> parseVendingResponse(data)
        0x02 -> parseDoorLockResponse(data)
        0x03 -> parseScanTraysResponse(data)
        0x04 -> parseResetTraysResponse(data)
        else -> null
    }
}

fun parseVendingResponse(data: List<Int>): VendingResponse {
    return VendingResponse(
        row = data[3],
        col = data[4],
        status = when (data[5]) {
            0 -> "SUCCESS"
            1 -> "FAILED"
            2 -> "TIMED_OUT"
            else -> "UNKNOWN"
        }
    )
}
```

### Checksum Validation

```kotlin
fun validateChecksum(data: List<Int>): Boolean {
    if (data.size < 2) return false
    
    var sum = 0
    for (i in 0 until data.size - 1) {
        sum += data[i]
    }
    val calculatedChecksum = sum and 0xFF
    val receivedChecksum = data[data.size - 1]
    
    return calculatedChecksum == receivedChecksum
}
```

---

## Boot Sequence & Kiosk Mode

### Launcher Activity
- **Main Activity:** `com.digitalmediavending.ui.main.view.SplashActivity`
- **Intent Filter:** `android.intent.action.MAIN` + `android.intent.category.LAUNCHER`

### Boot Receiver
- Uses AndroidX Work's `RescheduleReceiver` for `BOOT_COMPLETED`
- No custom boot receiver found in decompiled code

### Kiosk Mode
- **Device Admin:** `com.digitalmediavending.kiosk.KioskDeviceAdminReceiver`
- **Action:** `android.app.action.DEVICE_ADMIN_ENABLED`
- Uses Android Device Admin API to lock device

### Implementation Notes for Custom App

To implement kiosk mode in React Native:

1. **Set as Launcher:**
   ```xml
   <activity android:name=".MainActivity">
       <intent-filter>
           <action android:name="android.intent.action.MAIN" />
           <category android:name="android.intent.category.LAUNCHER" />
           <category android:name="android.intent.category.HOME" />
           <category android:name="android.intent.category.DEFAULT" />
       </intent-filter>
   </activity>
   ```

2. **Device Admin (Optional):**
   - Create DeviceAdminReceiver
   - Request device admin permission
   - Lock task mode: `setLockTaskMode(true)`

3. **Prevent Back Button:**
   ```kotlin
   override fun onBackPressed() {
       // Do nothing or show admin unlock
   }
   ```

4. **Auto-start on Boot:**
   ```xml
   <receiver android:name=".BootReceiver">
       <intent-filter>
           <action android:name="android.intent.action.BOOT_COMPLETED" />
       </intent-filter>
   </receiver>
   ```

---

## Implementation Notes

### Serial Port Permissions

Motor control port (`/dev/ttyS0`) requires root access:

```kotlin
// Try to gain permissions
if (!device.canRead() || !device.canWrite()) {
    val su = Runtime.getRuntime().exec("su")
    val os = su.outputStream
    os.write("chmod 666 $path\n".toByteArray())
    os.write("exit\n".toByteArray())
    os.flush()
    su.waitFor()
}
```

**Note:** Device is `userdebug` build, so `adb root` works for development.

### Error Handling

- **Motor Timeout:** Responses should arrive within reasonable time (5-10 seconds)
- **Payment Timeout:** Nayax SDK handles timeouts internally
- **USB Disconnection:** Monitor USB device attach/detach events
- **Checksum Errors:** Discard invalid responses, request retry

### Threading

- **Serial Reading:** Use separate thread with blocking I/O
- **Motor Commands:** Send synchronously, wait for response
- **Payment SDK:** Callbacks run on SDK's internal threads
- **UI Updates:** Use `runOnUiThread()` or React Native bridge

### React Native Bridge

Example usage:

```typescript
import { NativeModules, NativeEventEmitter } from 'react-native';

const { SerialPortModule } = NativeModules;
const serialEmitter = new NativeEventEmitter(SerialPortModule);

// Open port
await SerialPortModule.open('/dev/ttyS0', 115200);

// Listen for responses
serialEmitter.addListener('onSerialData', (hexData: string) => {
    const bytes = parseHexString(hexData);
    const response = parseMotorResponse(bytes);
    // Handle response
});

// Send vend command
await SerialPortModule.vendCoil(1, 1);
```

---

## File Structure Reference

### Original DMVI Apps
- `com.digitalmediavending`: Main UI app
- `com.digitalmediavending.hardware`: Hardware service
- `com.digitalmediavending.watchdog`: Watchdog service

### Key Classes
- `WallCoilMachine`: Motor control wrapper
- `DataSender`: Motor command sender
- `DataReceiver`: Motor response receiver
- `DataParser`: Response parser
- `WallCoilMachineService`: Main hardware service
- `lowlevel_serial_ftdi`: Payment terminal bridge
- `vmc_framework`: Nayax SDK wrapper

---

## Testing Checklist

- [ ] Motor vend command (coil 1, duration 1)
- [ ] Motor response parsing (success/failed/timeout)
- [ ] Door lock command
- [ ] Scan trays command
- [ ] Reset trays command
- [ ] Payment terminal initialization
- [ ] Payment session start
- [ ] Payment vend request
- [ ] QR scanner connection
- [ ] QR code reading
- [ ] ID scanner connection (if available)
- [ ] Boot sequence (kiosk mode)
- [ ] Error handling (timeouts, disconnections)

---

## Troubleshooting

### Motor Not Responding
1. Check port permissions (`ls -l /dev/ttyS0`)
2. Verify baud rate (115200)
3. Check if DMVI hardware service is disabled
4. Verify checksum calculation
5. Check for response timeout

### Payment Terminal Not Connecting
1. Verify USB device is connected
2. Check USB serial driver (FTDI)
3. Verify baud rate (115200)
4. Check Nayax SDK initialization
5. Monitor USB attach/detach events

### QR Scanner Not Working
1. Verify device VID/PID matches supported list
2. Check baud rate (9600)
3. Verify USB permissions
4. Check read callback is registered

---

## References

- **Motor Protocol:** `com.digitalmediavending.wallcoilmachine.inner.DataSender`
- **Response Parser:** `com.digitalmediavending.wallcoilmachine.inner.DataParser`
- **Constants:** `com.digitalmediavending.wallcoilmachine.utils.Constants`
- **Payment SDK:** `com.bitmick.marshall.vmc.vmc_framework`
- **Serial Library:** `com.github.licheedev:Android-SerialPort-API:2.1.0`

---

**End of Document**

