# MyApplication Vending Kiosk - Complete Implementation Guide

**Package:** `com.example.myapplication`
**Version:** 1.1.0 (versionCode 2)
**Target SDK:** Android 9.0 (API 28)
**Last Updated:** November 2025

---

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Hardware Components](#hardware-components)
3. [Software Architecture](#software-architecture)
4. [Communication Protocols](#communication-protocols)
5. [Complete User Flow](#complete-user-flow)
6. [Implementation Details](#implementation-details)
7. [Build & Deployment](#build--deployment)
8. [Troubleshooting](#troubleshooting)

---

## System Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Android Application                       │
│                    com.example.myapplication                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐    ┌──────────────────┐   ┌─────────────┐    │
│  │  MainActivity │───>│ProductDetailActiv│──>│IdScanActivity│   │
│  │  (Grid UI)   │    │     (Purchase)   │   │(Age Verify) │    │
│  └──────────────┘    └──────────────────┘   └─────────────┘    │
│                              │                      │            │
│                              │                      │            │
│  ┌──────────────────────────▼──────────────────────▼──────────┐ │
│  │              HardwareService (Foreground)                   │ │
│  │                                                              │ │
│  │  ┌─────────────────┐  ┌─────────────────┐ ┌──────────────┐│ │
│  │  │IdScannerManager │  │NayaxPaymentMgr  │ │MotorController││ │
│  │  └─────────────────┘  └─────────────────┘ └──────────────┘│ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                   │
└───────────┬───────────────────┬────────────────────┬─────────────┘
            │                   │                    │
            │ USB Serial        │ USB Serial         │ TCP/JSON
            │ FTDI              │ CDC-ACM            │ localhost
            │                   │                    │
    ┌───────▼────────┐  ┌──────▼───────┐   ┌────────▼──────────┐
    │  E-Seek M260   │  │ Nayax VPOS   │   │  DMVI Hardware    │
    │  ID Scanner    │  │   Payment    │   │     Service       │
    │                │  │  Terminal    │   │                   │
    │ VID: 0x0403    │  │ VID: 0x26f1  │   │ Port: Hardcoded   │
    │ PID: 0x6001    │  │ PID: 0x5650  │   │                   │
    └────────────────┘  └──────────────┘   └───────────────────┘
                                                     │
                                                     │ MDB Serial
                                                     │ /dev/ttyUSB1
                                                     │
                                            ┌────────▼──────────┐
                                            │  Motor Controller │
                                            │   (10 Coils)      │
                                            │                   │
                                            │  Row 1, Cols 1-10 │
                                            └───────────────────┘
```

### Key Design Principles

1. **Hardware Abstraction**: Each hardware device has a dedicated manager class
2. **Async Operations**: All hardware I/O uses Kotlin coroutines to prevent UI blocking
3. **State Management**: StateFlow for reactive state updates across components
4. **Error Isolation**: Hardware failures don't crash the app, only show user errors
5. **Separation of Concerns**: UI activities don't directly access hardware

---

## Hardware Components

### 1. E-Seek M260 ID Scanner

**Purpose:** Age verification for restricted products (tobacco, alcohol, adult items)

**Hardware Specifications:**
- **Manufacturer:** E-Seek
- **Model:** M260 Standalone ID Reader
- **Vendor ID:** 0x0403 (1027 decimal) - FTDI
- **Product ID:** 0x6001 (24577 decimal)
- **Connection:** USB 2.0 via FTDI FT232 chip
- **Baud Rate:** 115200
- **Driver:** FtdiSerialDriver (usb-serial-for-android library)
- **Kernel Node:** Auto-assigned by FTDI driver (no fixed /dev/ttyUSB*)

**Supported Formats:**
- PDF417 barcodes (Driver's licenses, state IDs)
- 2D barcodes (passports, some IDs)
- Magnetic stripe (older licenses)
- OCR for Machine Readable Zone (MRZ)

**Data Format:**
- AAMVA standard (American Association of Motor Vehicle Administrators)
- Extracts: Name, DOB, Address, Expiration Date, Issue Date, ID Number

**Physical Location:**
- Typically mounted at user eye-level on kiosk
- Must be accessible for users to scan ID

---

### 2. Nayax VPOS Touch Payment Terminal

**Purpose:** Process credit/debit card payments

**Hardware Specifications:**
- **Manufacturer:** Nayax
- **Model:** VPOS Touch
- **Vendor ID:** 0x26f1 (9969 decimal)
- **Product ID:** 0x5650 (22096 decimal)
- **Connection:** USB via CDC-ACM (Communication Device Class)
- **Kernel Node:** /dev/ttyACM0
- **Baud Rate:** 115200
- **Driver:** CdcAcmSerialDriver (built into Android kernel)

**Payment Features:**
- EMV chip card support
- Contactless (NFC) payments (Apple Pay, Google Pay)
- Magnetic stripe fallback
- PCI DSS compliant encryption
- Touchscreen display for PIN entry
- Multi-currency support

**Communication Protocol:**
- Proprietary Nayax protocol over serial
- Commands sent as ASCII text
- Responses include payment status, transaction ID

**Physical Location:**
- Mounted at chest height for card insertion
- Touchscreen accessible for PIN entry

---

### 3. Motor Control via DMVI Hardware Service

**Purpose:** Dispense physical products from vending machine coils

**Architecture:**
```
Your App (MyApplication)
    │
    │ TCP Socket: 127.0.0.1:<HARDCODED_PORT>
    │ Protocol: JSON-RPC 2.0
    │
    ▼
DMVI Hardware Service (com.digitalmediavending.hardware)
    │
    │ Serial: /dev/ttyUSB1 (or /dev/ttyS4)
    │ Baud: 115200
    │ Protocol: MDB Binary
    │
    ▼
Motor Controller Board (MDB-compatible)
    │
    └─> 10 Motors (Row 1, Columns 1-10)
```

**Why This Design?**
- DMVI service handles low-level MDB protocol (complex 7-byte binary commands)
- Your app sends simple JSON commands via localhost TCP
- Separation allows DMVI to manage hardware state, error recovery, motor timing
- Multiple apps can potentially communicate with same service

**MDB Protocol Details (for reference):**
- Multi-Drop Bus (industry standard for vending machines)
- 7-byte command structure
- Example vend command for Motor 5:
  ```
  Byte 1: 0x13 (Address/Command: MDB_VEND(3) + 16)
  Byte 2: 0x00 (Sub-command: VEND REQUEST)
  Byte 3-4: 0x00 0x00 (Item Price in cents, big-endian)
  Byte 5-6: 0x00 0x05 (Item Number: Motor 5, big-endian)
  Byte 7: 0x18 (Checksum: sum of bytes mod 256)
  ```

**JSON-RPC Command Format:**
```json
{
  "col": "5",
  "method": "requestProductVend",
  "row": "1",
  "jsonrpc": "2.0"
}
```

**Expected Response:**
```json
{
  "result": "success",
  "jsonrpc": "2.0"
}
```
or
```json
{
  "error": {
    "code": -32001,
    "message": "Motor jam detected"
  },
  "jsonrpc": "2.0"
}
```

---

## Software Architecture

### Component Hierarchy

```
Application Layer
├── MainActivity.kt              (Product grid display)
├── ProductDetailActivity.kt     (Purchase flow - MISSING, needs implementation)
└── IdScanActivity.kt           (Age verification UI)

Service Layer
└── HardwareService.kt          (Foreground service, lifecycle manager)

Hardware Abstraction Layer
├── IdScannerManager.kt         (E-Seek M260 driver)
├── NayaxPaymentManager.kt      (Nayax VPOS driver)
└── MotorController.kt          (DMVI TCP client - MISSING, needs implementation)

Data Models
├── Product.kt                  (Product data class - inline in MainActivity)
└── IdScanResult (data class in IdScannerManager)

UI Adapters
└── ProductAdapter.kt           (RecyclerView adapter for product grid)
```

---

### 1. MainActivity.kt

**Responsibility:** Display product grid and start hardware service

**Lifecycle:**
1. `onCreate()`: Start HardwareService as foreground service
2. Initialize RecyclerView with GridLayoutManager (2 columns)
3. Create product list (10 physical + 1 digital)
4. Set click listener to launch ProductDetailActivity

**Product Data Structure:**
```kotlin
data class Product(
    val id: String,              // "01" to "11"
    val name: String,            // "ZOOT VAPE X"
    val row: String,             // "1" for physical, "0" for digital
    val col: String,             // "1" to "10" for physical, "0" for digital
    val imageRes: Int,           // R.drawable.img_product_zyn
    val isDigital: Boolean = false,
    val price: Double = 0.0,
    val ageRestriction: Int? = null  // 18, 21, or null
)
```

**Product Mapping to Hardware:**
- Products 01-10: Row 1, Columns 1-10 (physical vending)
- Product 11: Digital donation (payment only, no vending)

**Key Code:**
```kotlin
// Start hardware service
val serviceIntent = Intent(this, HardwareService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    startForegroundService(serviceIntent)
} else {
    startService(serviceIntent)
}

// Product click handler
val adapter = ProductAdapter(products) { product ->
    val intent = Intent(this, ProductDetailActivity::class.java)
    intent.putExtra("name", product.name)
    intent.putExtra("row", product.row)
    intent.putExtra("col", product.col)
    intent.putExtra("price", product.price)
    intent.putExtra("ageRestriction", product.ageRestriction ?: -1)
    intent.putExtra("isDigital", product.isDigital)
    startActivity(intent)
}
```

---

### 2. ProductDetailActivity.kt ⚠️ MISSING - NEEDS IMPLEMENTATION

**Responsibility:** Orchestrate the complete purchase flow

**Expected Flow:**

```
1. Display product details
   ├─> Product name, price, image, description
   └─> "Purchase" button

2. User taps "Purchase"
   ├─> Check if age-restricted
   │   ├─> Yes: Launch IdScanActivity for age verification
   │   │   ├─> Age verified: Continue to step 3
   │   │   └─> Age denied: Show error, return to MainActivity
   │   └─> No: Continue to step 3

3. Initiate payment via NayaxPaymentManager
   ├─> Show "Insert card..." message
   ├─> Wait for payment (60s timeout)
   ├─> Payment approved: Continue to step 4
   └─> Payment declined/timeout: Show error, allow retry

4. Vend product (if not digital)
   ├─> Send JSON command to DMVI service via MotorController
   ├─> Wait for vend completion
   ├─> Success: Show "Enjoy your product!" message
   └─> Failure: Show error, initiate refund

5. Return to MainActivity
```

**Implementation Template:**
```kotlin
package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var productName: String
    private lateinit var productRow: String
    private lateinit var productCol: String
    private var productPrice: Double = 0.0
    private var ageRestriction: Int = -1
    private var isDigital: Boolean = false

    private lateinit var nameText: TextView
    private lateinit var priceText: TextView
    private lateinit var descText: TextView
    private lateinit var productImage: ImageView
    private lateinit var purchaseButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Get product details from intent
        productName = intent.getStringExtra("name") ?: ""
        productRow = intent.getStringExtra("row") ?: "1"
        productCol = intent.getStringExtra("col") ?: "1"
        productPrice = intent.getDoubleExtra("price", 0.0)
        ageRestriction = intent.getIntExtra("ageRestriction", -1)
        isDigital = intent.getBooleanExtra("isDigital", false)

        // Initialize views
        nameText = findViewById(R.id.product_name)
        priceText = findViewById(R.id.product_price)
        descText = findViewById(R.id.product_desc)
        productImage = findViewById(R.id.product_image)
        purchaseButton = findViewById(R.id.purchase_button)
        statusText = findViewById(R.id.status_text)

        // Display product info
        nameText.text = productName
        priceText.text = "$%.2f".format(productPrice)
        productImage.setImageResource(intent.getIntExtra("imageRes", 0))
        descText.text = intent.getStringExtra("desc") ?: ""

        // Purchase button handler
        purchaseButton.setOnClickListener {
            startPurchaseFlow()
        }
    }

    private fun startPurchaseFlow() {
        lifecycleScope.launch {
            try {
                // Step 1: Age verification if needed
                if (ageRestriction > 0) {
                    val ageVerified = verifyAge(ageRestriction)
                    if (!ageVerified) {
                        showError("Age verification failed")
                        return@launch
                    }
                }

                // Step 2: Process payment
                val paymentSuccess = processPayment(productPrice)
                if (!paymentSuccess) {
                    showError("Payment failed")
                    return@launch
                }

                // Step 3: Vend product (if physical)
                if (!isDigital) {
                    val vendSuccess = vendProduct(productRow, productCol)
                    if (!vendSuccess) {
                        showError("Vending failed - refund initiated")
                        // TODO: Initiate refund
                        return@launch
                    }
                }

                // Step 4: Success!
                showSuccess("Thank you for your purchase!")
                delay(2000)
                finish() // Return to MainActivity

            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private suspend fun verifyAge(requiredAge: Int): Boolean {
        return withContext(Dispatchers.Main) {
            val intent = Intent(this@ProductDetailActivity, IdScanActivity::class.java)
            intent.putExtra("requiredAge", requiredAge)
            startActivityForResult(intent, AGE_VERIFY_REQUEST)
            // Wait for result...
            // (Implementation depends on your result handling)
            true // Placeholder
        }
    }

    private suspend fun processPayment(amount: Double): Boolean {
        return withContext(Dispatchers.IO) {
            // Get NayaxPaymentManager from HardwareService
            // Call initiatePayment(amount)
            // Wait for response
            // Return true if approved
            true // Placeholder
        }
    }

    private suspend fun vendProduct(row: String, col: String): Boolean {
        return withContext(Dispatchers.IO) {
            // Get MotorController
            // Send vend command
            // Wait for completion
            true // Placeholder
        }
    }

    private fun showError(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
    }

    private fun showSuccess(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(android.R.color.holo_green_dark))
    }

    companion object {
        const val AGE_VERIFY_REQUEST = 1001
    }
}
```

---

### 3. IdScanActivity.kt ✅ EXISTS

**Responsibility:** Handle age verification via ID scanner

**Flow:**
1. Display "Scan ID" prompt
2. Wait for IdScannerManager to process scan
3. Parse birthdate from AAMVA data
4. Calculate age, compare to requirement
5. Return result to calling activity

**Key Features:**
- Observes IdScannerManager.scanResult StateFlow
- Parses PDF417 barcode data
- Extracts DOB field (AAMVA format: "DBA" field)
- Calculates age from current date
- Returns success/failure via Activity result

**Code Snippet:**
```kotlin
lifecycleScope.launch {
    IdScannerManager.scanResult.collect { result ->
        result?.let {
            val age = calculateAge(it.dateOfBirth)
            if (age >= requiredAge) {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                showError("You must be $requiredAge to purchase")
            }
        }
    }
}
```

---

### 4. HardwareService.kt ✅ EXISTS

**Responsibility:** Foreground service that manages hardware lifecycle

**Why Foreground Service?**
- Android requires foreground service for long-running hardware access
- Prevents system from killing the service while hardware is in use
- Shows persistent notification to user

**Lifecycle Management:**
1. `onCreate()`: Initialize all hardware managers
2. `onStartCommand()`: Start foreground notification
3. USB device attached: Initialize appropriate manager
4. USB device detached: Clean up manager
5. `onDestroy()`: Close all hardware connections

**Hardware Managers Controlled:**
- IdScannerManager (singleton)
- NayaxPaymentManager (singleton)
- MotorController (to be implemented)

**USB Device Hotplug Handling:**
```kotlin
// BroadcastReceiver for USB attach/detach
private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let { initializeDevice(it) }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                // Clean up managers
            }
        }
    }
}
```

---

### 5. IdScannerManager.kt ✅ EXISTS

**Responsibility:** Communicate with E-Seek M260 ID scanner via USB serial

**Architecture:**
- Singleton object (only one scanner)
- Uses `usb-serial-for-android` library (FtdiSerialDriver)
- Kotlin coroutines for async I/O
- StateFlow for reactive state updates

**Key Components:**
```kotlin
object IdScannerManager {
    // USB Serial Port
    private var serialPort: UsbSerialPort? = null

    // Scan result (observable by UI)
    val scanResult = MutableStateFlow<IdScanResult?>(null)

    // Connection state
    private var isConnected = false

    // USB Device IDs
    private const val VENDOR_ID = 0x0403  // FTDI
    private const val PRODUCT_ID = 0x6001
    private const val BAUD_RATE = 115200
}
```

**Initialization Flow:**
```kotlin
fun initialize(context: Context) {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    // Find E-Seek device
    val device = usbManager.deviceList.values.find {
        it.vendorId == VENDOR_ID && it.productId == PRODUCT_ID
    }

    device?.let {
        // Request permission if needed
        if (!usbManager.hasPermission(it)) {
            usbManager.requestPermission(it, permissionIntent)
            return
        }

        // Open connection
        val driver = FtdiSerialDriver(it)
        val connection = usbManager.openDevice(it)
        serialPort = driver.ports[0]
        serialPort?.open(connection)
        serialPort?.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        isConnected = true

        // Start reading loop
        startReadLoop()
    }
}
```

**Data Reading Loop:**
```kotlin
private fun startReadLoop() {
    CoroutineScope(Dispatchers.IO).launch {
        val buffer = ByteArray(1024)
        while (isConnected) {
            try {
                val bytesRead = serialPort?.read(buffer, READ_TIMEOUT) ?: 0
                if (bytesRead > 0) {
                    val data = buffer.copyOf(bytesRead)
                    parseIdData(data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Read error", e)
            }
        }
    }
}
```

**AAMVA Data Parsing:**
```kotlin
private fun parseIdData(data: ByteArray) {
    val dataString = String(data, Charsets.UTF_8)

    // Extract fields (AAMVA standard)
    val firstName = extractField(dataString, "DAC")   // First name
    val lastName = extractField(dataString, "DCS")    // Last name
    val dob = extractField(dataString, "DBB")         // Date of birth (MMDDYYYY)
    val expiration = extractField(dataString, "DBA")  // Expiration date

    val result = IdScanResult(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = parseDate(dob),
        expirationDate = parseDate(expiration)
    )

    scanResult.value = result
}

private fun extractField(data: String, fieldCode: String): String {
    val index = data.indexOf(fieldCode)
    if (index == -1) return ""

    val start = index + 3  // Field code is 3 chars
    val end = data.indexOf('\n', start)
    return if (end != -1) data.substring(start, end).trim() else ""
}
```

**Age Verification Helper:**
```kotlin
fun isAgeVerified(requiredAge: Int): Boolean {
    val result = scanResult.value ?: return false
    val age = calculateAge(result.dateOfBirth)
    return age >= requiredAge
}

private fun calculateAge(dob: Date): Int {
    val calendar = Calendar.getInstance()
    val today = calendar.time
    calendar.time = dob

    var age = Calendar.getInstance().get(Calendar.YEAR) - calendar.get(Calendar.YEAR)
    if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age
}
```

---

### 6. NayaxPaymentManager.kt ✅ EXISTS

**Responsibility:** Process payments via Nayax VPOS Touch terminal

**Payment State Machine:**
```
IDLE
  │
  ├─> initiatePayment(amount) called
  │
  ▼
WAITING_FOR_CARD
  │
  ├─> Card inserted/tapped
  │
  ▼
PROCESSING
  │
  ├─> Terminal processes transaction
  │
  ▼
APPROVED / DECLINED / ERROR / TIMEOUT
  │
  └─> Return to IDLE
```

**Architecture:**
```kotlin
object NayaxPaymentManager {
    // USB Serial Port
    private var serialPort: UsbSerialPort? = null

    // Payment state (observable)
    val paymentState = MutableStateFlow<PaymentState>(PaymentState.IDLE)

    // USB Device IDs
    private const val VENDOR_ID = 0x26f1   // Nayax
    private const val PRODUCT_ID = 0x5650  // VPOS Touch
    private const val BAUD_RATE = 115200

    // Timeout for card insertion
    private const val PAYMENT_TIMEOUT = 60_000L  // 60 seconds
}

enum class PaymentState {
    IDLE,
    WAITING_FOR_CARD,
    PROCESSING,
    APPROVED,
    DECLINED,
    ERROR,
    TIMEOUT
}
```

**Payment Initiation:**
```kotlin
suspend fun initiatePayment(amount: Double): Boolean = withContext(Dispatchers.IO) {
    if (!isConnected) {
        paymentState.value = PaymentState.ERROR
        return@withContext false
    }

    // Set state
    paymentState.value = PaymentState.WAITING_FOR_CARD

    // Send payment request to terminal
    val amountCents = (amount * 100).toInt()
    val command = buildPaymentCommand(amountCents)
    serialPort?.write(command, WRITE_TIMEOUT)

    // Wait for response (with timeout)
    val result = waitForPaymentResponse(PAYMENT_TIMEOUT)

    return@withContext result
}

private fun buildPaymentCommand(amountCents: Int): ByteArray {
    // Nayax protocol: <STX>PAY:<amount><ETX>
    val command = "PAY:$amountCents"
    return command.toByteArray(Charsets.UTF_8)
}

private suspend fun waitForPaymentResponse(timeout: Long): Boolean {
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeout) {
        when (paymentState.value) {
            PaymentState.APPROVED -> return true
            PaymentState.DECLINED, PaymentState.ERROR -> return false
            PaymentState.TIMEOUT -> return false
            else -> delay(100)  // Check every 100ms
        }
    }

    paymentState.value = PaymentState.TIMEOUT
    return false
}
```

**Response Parsing:**
```kotlin
private fun parsePaymentResponse(data: ByteArray) {
    val response = String(data, Charsets.UTF_8)

    when {
        response.contains("APPROVED") -> {
            paymentState.value = PaymentState.APPROVED
            // Extract transaction ID if needed
        }
        response.contains("DECLINED") -> {
            paymentState.value = PaymentState.DECLINED
        }
        response.contains("ERROR") -> {
            paymentState.value = PaymentState.ERROR
        }
    }
}
```

---

### 7. MotorController.kt ⚠️ MISSING - NEEDS IMPLEMENTATION

**Responsibility:** Send vend commands to DMVI hardware service via TCP/JSON

**Architecture:**
```kotlin
object MotorController {
    // Hardcoded port (you specify this)
    private const val DMVI_SERVICE_PORT = 57482  // CHANGE THIS TO ACTUAL PORT

    // Localhost address
    private const val HOST = "127.0.0.1"

    // Connection timeout
    private const val CONNECT_TIMEOUT = 5000  // 5 seconds

    // Vend result state
    val vendResult = MutableStateFlow<VendResult?>(null)
}

sealed class VendResult {
    object Success : VendResult()
    data class Failure(val error: String) : VendResult()
}
```

**Vend Command Implementation:**
```kotlin
suspend fun vendProduct(row: String, col: String): Boolean = withContext(Dispatchers.IO) {
    var socket: Socket? = null

    try {
        // Create TCP socket to DMVI service
        socket = Socket()
        socket.connect(InetSocketAddress(HOST, DMVI_SERVICE_PORT), CONNECT_TIMEOUT)

        // Build JSON-RPC command
        val command = JSONObject().apply {
            put("col", col)
            put("method", "requestProductVend")
            put("row", row)
            put("jsonrpc", "2.0")
        }

        // Send command
        val writer = PrintWriter(socket.getOutputStream(), true)
        writer.println(command.toString())

        // Read response (optional, depending on DMVI service)
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val response = reader.readLine()

        // Parse response
        val responseJson = JSONObject(response)
        val isSuccess = responseJson.optString("result") == "success"

        vendResult.value = if (isSuccess) {
            VendResult.Success
        } else {
            val error = responseJson.optJSONObject("error")?.optString("message") ?: "Unknown error"
            VendResult.Failure(error)
        }

        return@withContext isSuccess

    } catch (e: Exception) {
        Log.e(TAG, "Vend error", e)
        vendResult.value = VendResult.Failure(e.message ?: "Connection failed")
        return@withContext false

    } finally {
        socket?.close()
    }
}
```

**Alternative: Dynamic Port Discovery (if port changes):**
```kotlin
private suspend fun findDmviServicePort(): Int = withContext(Dispatchers.IO) {
    try {
        // Step 1: Find DMVI service PID
        val psProcess = Runtime.getRuntime().exec("ps -A")
        val psReader = BufferedReader(InputStreamReader(psProcess.inputStream))
        var pid = -1

        psReader.forEachLine { line ->
            if (line.contains("com.digitalmediavending.hardware")) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2) {
                    pid = parts[1].toIntOrNull() ?: -1
                }
            }
        }

        if (pid == -1) return@withContext -1

        // Step 2: Find port from netstat
        val netstatProcess = Runtime.getRuntime().exec("netstat -ltunp")
        val netstatReader = BufferedReader(InputStreamReader(netstatProcess.inputStream))
        var port = -1

        netstatReader.forEachLine { line ->
            if (line.contains(pid.toString())) {
                // Extract port from line like: ":::57482"
                val portRegex = """:(\\d+)""".toRegex()
                val match = portRegex.find(line)
                port = match?.groupValues?.get(1)?.toIntOrNull() ?: -1
            }
        }

        return@withContext port

    } catch (e: Exception) {
        Log.e(TAG, "Port discovery failed", e)
        return@withContext -1
    }
}
```

---

## Communication Protocols

### 1. USB Serial Communication (ID Scanner & Payment Terminal)

**Library:** `usb-serial-for-android` v3.5.1
- GitHub: https://github.com/mik3y/usb-serial-for-android
- License: LGPL

**Supported Drivers:**
- FtdiSerialDriver (FTDI FT232, FT2232, etc.) - Used by E-Seek M260
- CdcAcmSerialDriver (CDC-ACM class devices) - Used by Nayax VPOS
- Ch34xSerialDriver (CH340, CH341)
- Cp21xxSerialDriver (CP210x)
- ProlificSerialDriver (PL2303)

**Permission Requirements:**

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-feature android:name="android.hardware.usb.host" android:required="true" />

<!-- USB device filter -->
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>
    <meta-data
        android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
        android:resource="@xml/device_filter" />
</activity>
```

**res/xml/device_filter.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- E-Seek M260 ID Scanner -->
    <usb-device vendor-id="1027" product-id="24577" />

    <!-- Nayax VPOS Touch Payment Terminal -->
    <usb-device vendor-id="9969" product-id="22096" />
</resources>
```

---

### 2. TCP/JSON Communication (Motor Control)

**Protocol:** JSON-RPC 2.0
**Transport:** TCP socket over localhost
**Port:** Hardcoded (you specify) or dynamically discovered

**Request Format:**
```json
{
  "col": "5",
  "method": "requestProductVend",
  "row": "1",
  "jsonrpc": "2.0"
}
```

**Success Response:**
```json
{
  "result": "success",
  "jsonrpc": "2.0"
}
```

**Error Response:**
```json
{
  "error": {
    "code": -32001,
    "message": "Motor jam detected"
  },
  "jsonrpc": "2.0"
}
```

**Common Error Codes:**
- `-32001`: Motor jam or mechanical failure
- `-32002`: Invalid row/column
- `-32003`: Motor controller not responding
- `-32004`: Service busy (another vend in progress)

---

### 3. MDB Protocol (Reference Only - Handled by DMVI)

**You don't implement this directly**, but understanding it helps debugging:

**Command Structure (7 bytes):**
```
Byte 1: Address/Command (0x13 for VEND)
Byte 2: Sub-command (0x00 for VEND REQUEST)
Byte 3-4: Item Price (2 bytes, big-endian, cents)
Byte 5-6: Item Number (2 bytes, big-endian)
Byte 7: Checksum (sum of bytes 1-6, mod 256)
```

**Example: Vend Motor 5 (free vend):**
```
0x13 0x00 0x00 0x00 0x00 0x05 0x18
```

**Checksum Calculation:**
```
0x13 + 0x00 + 0x00 + 0x00 + 0x00 + 0x05 = 0x18 (24 decimal)
```

**Shell Command (for testing directly):**
```bash
echo -e "\x13\x00\x00\x00\x00\x05\x18" > /dev/ttyUSB1
```

---

## Complete User Flow

### Scenario 1: Purchase Age-Restricted Physical Product (e.g., ZYN Citrus)

```
1. User approaches kiosk
   └─> MainActivity displays product grid

2. User taps "ZYN CITRUS" (Product #3)
   └─> ProductDetailActivity launches
       ├─> Shows: Name, Price ($8.99), Image
       └─> "Purchase" button visible

3. User taps "Purchase"
   └─> ProductDetailActivity checks ageRestriction (21)
       └─> Launches IdScanActivity

4. IdScanActivity prompts "Scan ID"
   └─> User scans driver's license
       └─> IdScannerManager reads PDF417 barcode
           └─> Parses AAMVA data, extracts DOB
               ├─> Age < 21: "Age verification failed" → Return to MainActivity
               └─> Age >= 21: Return to ProductDetailActivity

5. ProductDetailActivity continues with payment
   └─> NayaxPaymentManager.initiatePayment($8.99)
       └─> Status: "Insert card..."
           └─> User inserts/taps card
               └─> Nayax terminal processes
                   ├─> Declined: "Payment failed" → Allow retry or exit
                   └─> Approved: Continue

6. ProductDetailActivity vends product
   └─> MotorController.vendProduct(row="1", col="3")
       └─> Sends: {"col":"3","method":"requestProductVend","row":"1","jsonrpc":"2.0"}
           └─> DMVI service → Motor controller → Motor 3 spins
               ├─> Motor jam: "Vending failed - refund initiated" → Error screen
               └─> Success: "Enjoy your product!" → Return to MainActivity

7. User collects product
   └─> Flow complete
```

**Timing:**
- ID scan: ~5-10 seconds
- Payment: ~10-30 seconds (depending on PIN entry)
- Vending: ~3-5 seconds
- **Total: 20-45 seconds per transaction**

---

### Scenario 2: Purchase Non-Restricted Physical Product (e.g., Red Bull)

```
1-2. Same as above (tap product)

3. User taps "Purchase"
   └─> No ageRestriction → Skip IdScanActivity
       └─> Go directly to payment

4-6. Same as Scenario 1 (payment + vending)

**Total: 15-35 seconds per transaction**
```

---

### Scenario 3: Digital Donation (No Vending)

```
1-2. Same as above (tap "Donate to Autism Fund")

3. User taps "Purchase"
   └─> No age restriction → Skip ID scan
       └─> Payment only

4. ProductDetailActivity processes payment
   └─> NayaxPaymentManager.initiatePayment($5.00)
       └─> Payment approved

5. ProductDetailActivity checks isDigital flag
   └─> isDigital == true → Skip motor vending
       └─> Show "Thank you for your donation!" → Return to MainActivity

**Total: 10-30 seconds**
```

---

## Implementation Details

### Step-by-Step Implementation Checklist

#### Phase 1: Project Cleanup ✓
- [ ] Delete `app/build/` directory (~45 MB)
- [ ] Delete `.gradle/` directory (~4.9 MB)
- [ ] Delete `.kotlin/` directory
- [ ] Verify source code intact after cleanup
- [ ] Run `gradlew clean` to confirm Gradle still works

#### Phase 2: Create ProductDetailActivity
- [ ] Create `activity_product_detail.xml` layout
  - [ ] Product image (ImageView)
  - [ ] Product name (TextView)
  - [ ] Product price (TextView)
  - [ ] Product description (TextView)
  - [ ] Purchase button (Button)
  - [ ] Status text (TextView for messages)
- [ ] Create `ProductDetailActivity.kt`
  - [ ] Extract intent extras (name, row, col, price, etc.)
  - [ ] Implement `startPurchaseFlow()`
  - [ ] Implement `verifyAge()` with startActivityForResult
  - [ ] Implement `processPayment()` calling NayaxPaymentManager
  - [ ] Implement `vendProduct()` calling MotorController
  - [ ] Add error handling and user feedback
- [ ] Add activity to `AndroidManifest.xml`
```xml
<activity android:name=".ProductDetailActivity" />
```

#### Phase 3: Create MotorController
- [ ] Create `MotorController.kt` in `hardware/` package
- [ ] Define hardcoded port constant (get from user)
- [ ] Implement `vendProduct(row, col)` function
  - [ ] Create TCP socket to 127.0.0.1:PORT
  - [ ] Build JSON-RPC command
  - [ ] Send command
  - [ ] Receive response
  - [ ] Parse result
  - [ ] Close socket
  - [ ] Return success/failure
- [ ] Add error handling for:
  - [ ] Connection refused (DMVI service not running)
  - [ ] Socket timeout
  - [ ] Invalid response
  - [ ] Motor jam errors
- [ ] Optional: Implement dynamic port discovery as fallback

#### Phase 4: Wire Up Components
- [ ] In HardwareService, initialize MotorController
- [ ] In ProductDetailActivity, get reference to managers:
  - [ ] Access IdScannerManager (singleton)
  - [ ] Access NayaxPaymentManager (singleton)
  - [ ] Access MotorController (singleton)
- [ ] Implement age verification flow:
  - [ ] Launch IdScanActivity with required age
  - [ ] Handle onActivityResult
  - [ ] Parse result code (RESULT_OK or RESULT_CANCELED)
- [ ] Implement payment flow:
  - [ ] Call NayaxPaymentManager.initiatePayment(amount)
  - [ ] Observe paymentState StateFlow
  - [ ] Show progress to user
  - [ ] Handle timeout/decline/approval
- [ ] Implement vend flow:
  - [ ] Check if digital product (skip if true)
  - [ ] Call MotorController.vendProduct(row, col)
  - [ ] Wait for result
  - [ ] Handle success/failure

#### Phase 5: Testing
- [ ] Test age verification flow
  - [ ] Scan real ID
  - [ ] Verify age calculation correct
  - [ ] Test under-age rejection
- [ ] Test payment flow
  - [ ] Test approved transaction
  - [ ] Test declined transaction
  - [ ] Test timeout (don't insert card)
- [ ] Test motor control
  - [ ] Verify DMVI service is running
  - [ ] Test each motor (1-10)
  - [ ] Verify products dispense correctly
  - [ ] Test digital product (no vend)
- [ ] Test error handling
  - [ ] Disconnect ID scanner during scan
  - [ ] Disconnect payment terminal during payment
  - [ ] Stop DMVI service during vend
  - [ ] Verify app doesn't crash, shows errors

#### Phase 6: Build & Deploy
- [ ] Run full build: `gradlew assembleDebug`
- [ ] Verify APK created: `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Connect to device: `adb connect 192.168.7.246:34643`
- [ ] Uninstall old version: `adb -s 192.168.7.246:34643 uninstall com.example.myapplication`
- [ ] Install new APK: `adb -s 192.168.7.246:34643 install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] Launch app: `adb -s 192.168.7.246:34643 shell am start -n com.example.myapplication/.MainActivity`
- [ ] Monitor logs: `adb -s 192.168.7.246:34643 logcat -s MyApplication`

---

## Build & Deployment

### Build Configuration

**File: `app/build.gradle.kts`**
```kotlin
android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24        // Android 7.0 Nougat
        targetSdk = 28     // Android 9.0 Pie (for hardware compatibility)
        versionCode = 2
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // USB Serial (CRITICAL for hardware)
    implementation("com.github.mik3y:usb-serial-for-android:3.5.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

**File: `settings.gradle.kts`**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Required for usb-serial library
    }
}
```

### Build Commands

**Clean build:**
```bash
cd C:\dev\MyApplication
.\gradlew clean
```

**Build debug APK:**
```bash
.\gradlew assembleDebug
```

**Build release APK (unsigned):**
```bash
.\gradlew assembleRelease
```

**Output location:**
```
C:\dev\MyApplication\app\build\outputs\apk\debug\app-debug.apk
```

### ADB Deployment

**1. Connect to device:**
```bash
adb connect 192.168.7.246:34643
```

**2. Verify connection:**
```bash
adb devices
# Should show: 192.168.7.246:34643   device
```

**3. Uninstall old version:**
```bash
adb -s 192.168.7.246:34643 uninstall com.example.myapplication
```

**4. Install new APK:**
```bash
adb -s 192.168.7.246:34643 install -r app\build\outputs\apk\debug\app-debug.apk
```

**5. Launch app:**
```bash
adb -s 192.168.7.246:34643 shell am start -n com.example.myapplication/.MainActivity
```

**6. View logs:**
```bash
adb -s 192.168.7.246:34643 logcat | grep -i myapplication
```

### One-Command Deployment Script

**Create `deploy.bat` in project root:**
```batch
@echo off
echo Building APK...
call gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)

echo Connecting to device...
adb connect 192.168.7.246:34643

echo Uninstalling old version...
adb -s 192.168.7.246:34643 uninstall com.example.myapplication

echo Installing new APK...
adb -s 192.168.7.246:34643 install -r app\build\outputs\apk\debug\app-debug.apk

echo Launching app...
adb -s 192.168.7.246:34643 shell am start -n com.example.myapplication/.MainActivity

echo Deployment complete!
```

**Usage:**
```bash
.\deploy.bat
```

---

## Troubleshooting

### Hardware Issues

#### ID Scanner Not Detected
**Symptoms:** IdScannerManager fails to initialize, "Device not found" error

**Checks:**
1. Verify USB cable connected
2. Check USB device appears: `adb shell ls /dev/bus/usb/`
3. Verify VID/PID: `adb shell lsusb` (should show 0403:6001)
4. Check permissions in `device_filter.xml` match
5. Try unplugging and replugging scanner

**Debug logging:**
```kotlin
Log.d("IdScanner", "Available USB devices:")
usbManager.deviceList.forEach { (name, device) ->
    Log.d("IdScanner", "$name: VID=${device.vendorId}, PID=${device.productId}")
}
```

#### Payment Terminal Not Responding
**Symptoms:** Payment stuck at "Waiting for card", no response

**Checks:**
1. Verify terminal powered on (display shows logo)
2. Check USB connection (terminal should beep when connected)
3. Verify VID/PID: `adb shell lsusb` (should show 26f1:5650)
4. Check /dev/ttyACM0 exists: `adb shell ls -l /dev/ttyACM0`
5. Try power cycling terminal (unplug power, wait 10s, replug)

**Test communication:**
```bash
# From ADB shell
adb shell
echo "STATUS" > /dev/ttyACM0
cat /dev/ttyACM0
# Should respond with terminal status
```

#### Motor Not Vending
**Symptoms:** Payment succeeds, but no motor movement

**Checks:**
1. Verify DMVI service running: `adb shell ps -A | grep dmvi`
2. Check motor port accessible: `adb shell ls -l /dev/ttyUSB1`
3. Test direct motor command:
   ```bash
   adb shell
   echo -e "\x13\x00\x00\x00\x00\x05\x18" > /dev/ttyUSB1
   ```
4. Verify row/col values correct (1-indexed, not 0-indexed)
5. Check DMVI service logs: `adb logcat | grep DMVI`

**Port discovery debugging:**
```bash
# Find DMVI service
adb shell ps -A | grep digitalmediavending

# Find listening ports
adb shell netstat -ltunp | grep <PID>
```

**Correct Motor Test Script (IPv6)**

If the `netstat` command shows the service is listening on `tcp6`, the standard `127.0.0.1` address will fail with "Connection refused". You must use the IPv6 loopback address `::1` instead.

Here is the correct sequence to find the port and test the motor:

1.  **Connect to the device shell:**
    ```bash
    adb shell
    ```

2.  **Find the listening port and PID:**
    ```bash
    netstat -lptn
    ```
    *Look for the line containing `com.digitalmediavending.hardware` to confirm the port and PID. Note if the protocol is `tcp6`.*

    Example output:
    ```
    Proto Recv-Q Send-Q Local Address    Foreign Address    State      PID/Program Name
    tcp6       0      0 :::57482         :::*               LISTEN     2353/com.digitalmediavending.hardware
    ```

3.  **Send the vend command using the IPv6 loopback address `::1`:**
    ```bash
    # Test motor for column 1
    echo '{"col":"1","method":"requestProductVend","row":"1","jsonrpc":"2.0"}' | nc ::1 57482
    ```

### Software Issues

#### App Crashes on Product Tap
**Symptom:** App crashes when tapping product in grid

**Cause:** ProductDetailActivity doesn't exist

**Fix:** Implement ProductDetailActivity.kt (see implementation section)

**Temporary workaround:**
```kotlin
// In MainActivity, comment out product tap handler
// val adapter = ProductAdapter(products) { product ->
//     val intent = Intent(this, ProductDetailActivity::class.java)
//     ...
// }
```

#### Gradle Build Fails
**Symptom:** "Could not resolve dependency" or similar errors

**Fixes:**
1. Check internet connection (Gradle downloads dependencies)
2. Verify JitPack repository in settings.gradle.kts
3. Sync project: `.\gradlew --refresh-dependencies`
4. Clear Gradle cache: `rm -rf .gradle/`
5. Invalidate Android Studio caches: File → Invalidate Caches → Restart

#### ADB Connection Refused
**Symptom:** "cannot connect to 192.168.7.246:34643"

**Fixes:**
1. Verify device on same network
2. Ping device: `ping 192.168.7.246`
3. Check ADB server running: `adb start-server`
4. Try USB connection instead:
   ```bash
   # Connect via USB
   adb usb
   # Then enable network debugging
   adb tcpip 5555
   ```

### Permission Issues

#### USB Permission Denied
**Symptom:** "Permission denied" when accessing USB device

**Fixes:**
1. Verify device_filter.xml includes device VID/PID
2. Check app requests permission:
   ```kotlin
   if (!usbManager.hasPermission(device)) {
       usbManager.requestPermission(device, permissionIntent)
   }
   ```
3. Grant permission manually: Settings → Apps → MyApplication → Permissions

#### Network Permission
**Symptom:** "Permission denied" when connecting to localhost

**Fix:** Verify AndroidManifest.xml includes:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Performance Considerations

### Optimize Coroutines Usage
```kotlin
// Use appropriate dispatcher
lifecycleScope.launch(Dispatchers.IO) {  // For I/O operations
    // USB serial, TCP socket operations
}

lifecycleScope.launch(Dispatchers.Main) {  // For UI updates
    // Update TextViews, show dialogs
}
```

### Prevent Memory Leaks
```kotlin
// Always close hardware connections
override fun onDestroy() {
    super.onDestroy()
    IdScannerManager.close()
    NayaxPaymentManager.close()
}

// Cancel coroutines when activity destroyed
private val job = Job()
private val scope = CoroutineScope(Dispatchers.IO + job)

override fun onDestroy() {
    super.onDestroy()
    job.cancel()
}
```

### Efficient State Management
```kotlin
// Use StateFlow for observable state (like LiveData but better)
val paymentState = MutableStateFlow<PaymentState>(PaymentState.IDLE)

// Collect in lifecycle-aware manner
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        NayaxPaymentManager.paymentState.collect { state ->
            updateUI(state)
        }
    }
}
```

---

## Security Considerations

1. **PCI DSS Compliance:**
   - Never log credit card data
   - Don't store payment info in SharedPreferences
   - Use Nayax terminal's built-in encryption
   - Terminal is PCI DSS certified, maintain that certification

2. **Age Verification Compliance:**
   - Log all age verification attempts (with consent)
   - Store logs securely (encrypted)
   - Retain logs per state law (typically 30-90 days)
   - Don't store full ID images (privacy violation)

3. **Network Security:**
   - Localhost-only for DMVI service (no external exposure)
   - Use HTTPS for any backend API calls (if added)
   - Validate all JSON responses (prevent injection)

---

## Future Enhancements

1. **Backend Integration:**
   - Send transaction logs to cloud server
   - Inventory management API
   - Remote monitoring and alerts

2. **Enhanced UI:**
   - Product videos/animations
   - Customer loyalty program
   - QR code payment support

3. **Analytics:**
   - Track popular products
   - Peak usage times
   - Failure rates by motor

4. **Maintenance Mode:**
   - Admin panel (password protected)
   - Manual motor testing
   - View transaction history
   - Clear error states

---

## Appendix A: Full File Listing

### Source Code Files
```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt                  ✅ EXISTS
├── ProductDetailActivity.kt         ❌ MISSING (needs implementation)
├── IdScanActivity.kt               ✅ EXISTS
├── ProductAdapter.kt               ✅ EXISTS
└── hardware/
    ├── HardwareService.kt          ✅ EXISTS
    ├── IdScannerManager.kt         ✅ EXISTS
    ├── NayaxPaymentManager.kt      ✅ EXISTS
    └── MotorController.kt          ❌ MISSING (needs implementation)
```

### Layout Files
```
app/src/main/res/layout/
├── activity_main.xml               ✅ EXISTS (product grid)
├── activity_product_detail.xml     ❌ MISSING (needs creation)
├── activity_id_scan.xml           ✅ EXISTS
└── item_product.xml               ✅ EXISTS (grid item)
```

### Configuration Files
```
app/
├── build.gradle.kts               ✅ EXISTS
├── src/main/AndroidManifest.xml   ✅ EXISTS
└── src/main/res/xml/
    └── device_filter.xml          ✅ EXISTS
```

### Documentation
```
C:\dev\MyApplication\
├── IMPLEMENTATION_GUIDE.md        ⬅️ THIS FILE
├── app/motorcontext.md           ✅ Motor protocol reference
├── app/context.txt              ✅ Service discovery docs
└── shippable_apk/
    └── VendingClient.apk         ✅ Working APK
```

---

## Appendix B: Quick Reference

### Hardware Specs Summary
| Device | VID | PID | Driver | Baud | Node |
|--------|-----|-----|--------|------|------|
| E-Seek M260 | 0x0403 | 0x6001 | FTDI | 115200 | Auto |
| Nayax VPOS | 0x26f1 | 0x5650 | CDC-ACM | 115200 | /dev/ttyACM0 |
| Motor (MDB) | N/A | N/A | DMVI Service | 115200 | /dev/ttyUSB1 |

### Product Mapping
| Product ID | Name | Row | Col | Age | Price |
|------------|------|-----|-----|-----|-------|
| 01 | ZOOT VAPE X | 1 | 1 | 21 | $29.99 |
| 02 | NIGHT OWL CAM | 1 | 2 | - | $15.99 |
| 03 | ZYN CITRUS | 1 | 3 | 21 | $8.99 |
| 04 | RED BULL 12OZ | 1 | 4 | - | $4.99 |
| 05 | LIGHTER GOLD | 1 | 5 | 18 | $2.99 |
| 06 | ROLLING PAPERS | 1 | 6 | 18 | $3.99 |
| 07 | ENERGY SHOT | 1 | 7 | - | $3.49 |
| 08 | GUM MINT | 1 | 8 | - | $1.99 |
| 09 | CONDOM PACK | 1 | 9 | - | $5.99 |
| 10 | WATER 500ML | 1 | 10 | - | $2.49 |
| 11 | Autism Donation | 0 | 0 | - | $5.00 |

### ADB Commands
```bash
# Connect
adb connect 192.168.7.246:34643

# Install
adb -s 192.168.7.246:34643 install -r app-debug.apk

# Launch
adb -s 192.168.7.246:34643 shell am start -n com.example.myapplication/.MainActivity

# Logs
adb -s 192.168.7.246:34643 logcat

# Shell access
adb -s 192.168.7.246:34643 shell
```

---

**End of Implementation Guide**

For questions or issues, refer to the troubleshooting section or contact the development team.
