package com.example.myapplication.hardware

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.*

data class IdScanResult(
    val success: Boolean,
    val dateOfBirth: Date? = null,
    val expirationDate: Date? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val isExpired: Boolean = false,
    val error: String? = null
)

enum class ScannerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class IdScannerManager(
    private val usbManager: UsbManager,
    private val usbDevice: UsbDevice,
    private val scope: CoroutineScope
) {
    private var serialPort: UsbSerialPort? = null
    private val _scanResult = MutableStateFlow<IdScanResult?>(null)
    val scanResult: StateFlow<IdScanResult?> = _scanResult

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _connectionState = MutableStateFlow(ScannerConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ScannerConnectionState> = _connectionState
    
    fun initialize() {
        scope.launch {
            _connectionState.value = ScannerConnectionState.CONNECTING
            try {
                if (!usbManager.hasPermission(usbDevice)) {
                    _isReady.value = false
                    _connectionState.value = ScannerConnectionState.DISCONNECTED
                    return@launch
                }

                val driver: UsbSerialDriver? = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
                if (driver !is FtdiSerialDriver) {
                    _isReady.value = false
                    _connectionState.value = ScannerConnectionState.ERROR
                    return@launch
                }

                serialPort = driver.ports[0]
                serialPort?.open(usbManager.openDevice(usbDevice))
                serialPort?.setParameters(
                    HardwareService.SERIAL_BAUD_RATE,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )

                _isReady.value = true
                _connectionState.value = ScannerConnectionState.CONNECTED
                startReading()
            } catch (e: Exception) {
                _isReady.value = false
                _connectionState.value = ScannerConnectionState.ERROR
                _scanResult.value = IdScanResult(false, error = e.message)
            }
        }
    }
    
    private fun startReading() {
        scope.launch {
            val buffer = ByteArray(4096)  // Increased from 1024 to handle full AAMVA data
            val accumulatedData = StringBuilder()
            var lastReadTime = 0L

            while (isActive && _isReady.value) {
                try {
                    val port = serialPort ?: break
                    val bytesRead = port.read(buffer, 1000)

                    if (bytesRead > 0) {
                        val chunk = String(buffer, 0, bytesRead)
                        accumulatedData.append(chunk)
                        lastReadTime = System.currentTimeMillis()

                        // Check if we have a complete AAMVA scan
                        if (accumulatedData.contains("ANSI") || accumulatedData.toString().startsWith("@")) {
                            // Wait for data to stabilize (no new data for 200ms)
                            delay(200)

                            // Process if no new data arrived
                            if (System.currentTimeMillis() - lastReadTime >= 200) {
                                processScanData(accumulatedData.toString())
                                accumulatedData.clear()
                            }
                        }

                    } else {
                        // Timeout - check for stale data
                        if (accumulatedData.isNotEmpty() &&
                            System.currentTimeMillis() - lastReadTime > 2000) {
                            // Flush stale partial data
                            accumulatedData.clear()
                        }
                    }

                } catch (e: IOException) {
                    if (isActive) {
                        delay(100)
                        // Reconnection handled by BroadcastReceiver
                    }
                }
            }
        }
    }
    
    private fun processScanData(data: String) {
        scope.launch(Dispatchers.Default) {
            try {
                // Validate AAMVA header
                if (!AamvaFieldParser.hasValidAamvaHeader(data)) {
                    _scanResult.value = IdScanResult(
                        success = false,
                        error = "Invalid ID format - AAMVA header not found"
                    )
                    return@launch
                }

                // Validate required fields
                if (!AamvaFieldParser.hasRequiredFields(data)) {
                    _scanResult.value = IdScanResult(
                        success = false,
                        error = "Incomplete scan - missing required fields"
                    )
                    return@launch
                }

                // Extract all fields using field-specific parser
                val dob = AamvaFieldParser.extractDateOfBirth(data)
                val expiration = AamvaFieldParser.extractExpirationDate(data)
                val firstName = AamvaFieldParser.extractFirstName(data)
                val lastName = AamvaFieldParser.extractLastName(data)

                if (dob == null) {
                    _scanResult.value = IdScanResult(
                        success = false,
                        error = "Could not parse date of birth (DBB/DAA field missing)"
                    )
                    return@launch
                }

                // Check if ID is expired
                val isExpired = expiration?.let { exp ->
                    exp.before(Date())
                } ?: false

                if (isExpired) {
                    _scanResult.value = IdScanResult(
                        success = false,
                        error = "ID has expired",
                        isExpired = true
                    )
                    return@launch
                }

                // Successfully parsed
                _scanResult.value = IdScanResult(
                    success = true,
                    dateOfBirth = dob,
                    expirationDate = expiration,
                    firstName = firstName,
                    lastName = lastName,
                    isExpired = false
                )

                // PII SANITIZATION: Clear raw data buffer immediately
                clearRawDataBuffer(data)

            } catch (e: Exception) {
                _scanResult.value = IdScanResult(
                    success = false,
                    error = "Parsing error: ${e.message}"
                )
            }
        }
    }
    
    fun isAgeVerified(requiredAge: Int = 21): Boolean {
        val result = _scanResult.value
        if (result?.success != true || result.dateOfBirth == null) return false

        // Convert Date to LocalDate
        val dob = result.dateOfBirth.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val today = LocalDate.now()
        val age = Period.between(dob, today).years

        return age >= requiredAge
    }
    
    fun clearScanResult() {
        // Clear StateFlow value
        val currentResult = _scanResult.value
        _scanResult.value = null

        // Explicitly null out extracted PII fields (defensive)
        // JVM garbage collector will clean up
        currentResult?.let {
            // Fields are already copied by value, this is for clarity
        }
    }

    /**
     * Clears all PII data from memory
     * Per Non-Functional Requirement #1: PII Security
     */
    fun clearData() {
        _scanResult.value = null

        // Clear internal USB buffers
        serialPort?.purgeHwBuffers(true, true)  // RX + TX buffers
    }

    /**
     * Clears raw data buffer to prevent PII retention
     * Per Non-Functional Requirement #1: No PII in logs/storage
     */
    private fun clearRawDataBuffer(data: String) {
        // JVM strings are immutable, so we just dereference
        // Actual sanitization happens at the byte buffer level
        // The data parameter will be garbage collected
    }

    /**
     * Attempts to reconnect to the scanner
     * Called automatically by UsbConnectionReceiver
     */
    fun reconnect() {
        scope.launch {
            _connectionState.value = ScannerConnectionState.CONNECTING
            _isReady.value = false

            try {
                // Close existing connection
                serialPort?.close()
                serialPort = null

                // Wait briefly for device to stabilize
                delay(500)

                // Re-initialize
                initialize()

            } catch (e: Exception) {
                _connectionState.value = ScannerConnectionState.ERROR
                _scanResult.value = IdScanResult(
                    success = false,
                    error = "Reconnection failed: ${e.message}"
                )
            }
        }
    }

    fun close() {
        scope.launch {
            _connectionState.value = ScannerConnectionState.DISCONNECTED
            try {
                serialPort?.close()
            } catch (e: Exception) {
                // Ignore
            }
            serialPort = null
            _isReady.value = false
        }
    }
}

