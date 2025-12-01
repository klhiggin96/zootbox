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
import java.text.SimpleDateFormat
import java.util.*

data class IdScanResult(
    val success: Boolean,
    val dateOfBirth: Date? = null,
    val expirationDate: Date? = null,
    val rawData: String? = null,
    val error: String? = null
)

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
    
    fun initialize() {
        scope.launch {
            try {
                if (!usbManager.hasPermission(usbDevice)) {
                    _isReady.value = false
                    return@launch
                }
                
                val driver: UsbSerialDriver? = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
                if (driver !is FtdiSerialDriver) {
                    _isReady.value = false
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
                startReading()
            } catch (e: Exception) {
                _isReady.value = false
                _scanResult.value = IdScanResult(false, error = e.message)
            }
        }
    }
    
    private fun startReading() {
        scope.launch {
            val buffer = ByteArray(1024)
            while (isActive && _isReady.value) {
                try {
                    val port = serialPort ?: break
                    val bytesRead = port.read(buffer, 1000)
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead)
                        processScanData(data)
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        delay(100)
                    }
                }
            }
        }
    }
    
    private fun processScanData(data: String) {
        scope.launch(Dispatchers.Default) {
            try {
                // Parse AAMVA format (PDF417 barcode data)
                // Format: @\nANSI 636... (AAMVA standard)
                val lines = data.trim().split("\n", "\r")
                if (lines.isEmpty()) return@launch
                
                // Look for AAMVA header
                val aamvaLine = lines.find { it.startsWith("ANSI") || it.startsWith("@") }
                if (aamvaLine == null) {
                    _scanResult.value = IdScanResult(false, error = "Invalid ID format")
                    return@launch
                }
                
                // Extract DOB (typically in DLN segment, field DAA)
                // Format varies by state, but common pattern: YYYYMMDD
                val dob = extractDateOfBirth(lines)
                val expiration = extractExpirationDate(lines)
                
                if (dob != null) {
                    _scanResult.value = IdScanResult(
                        success = true,
                        dateOfBirth = dob,
                        expirationDate = expiration,
                        rawData = data
                    )
                } else {
                    _scanResult.value = IdScanResult(false, error = "Could not parse date of birth")
                }
            } catch (e: Exception) {
                _scanResult.value = IdScanResult(false, error = e.message)
            }
        }
    }
    
    private fun extractDateOfBirth(lines: List<String>): Date? {
        // AAMVA format: DAA field contains DOB (YYYYMMDD)
        // Search for pattern matching date format
        val datePattern = Regex("""(\d{8})""")
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        
        for (line in lines) {
            val match = datePattern.find(line)
            if (match != null) {
                try {
                    val dateStr = match.value
                    val date = dateFormat.parse(dateStr)
                    // Validate reasonable date range (1900-2100)
                    if (date != null && date.after(Date(0)) && date.before(Date(4102444800000L))) {
                        return date
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }
    
    private fun extractExpirationDate(lines: List<String>): Date? {
        // Similar to DOB extraction, look for expiration date field
        // DBA field typically contains expiration date
        return extractDateOfBirth(lines) // Simplified - would need proper field parsing
    }
    
    fun isAgeVerified(requiredAge: Int = 21): Boolean {
        val result = _scanResult.value
        if (result?.success != true || result.dateOfBirth == null) return false
        
        val today = Calendar.getInstance()
        val dob = Calendar.getInstance().apply { time = result.dateOfBirth }
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        
        val monthDiff = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH)
        if (monthDiff < 0 || (monthDiff == 0 && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--
        }
        
        return age >= requiredAge
    }
    
    fun clearScanResult() {
        _scanResult.value = null
    }
    
    fun close() {
        scope.launch {
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

