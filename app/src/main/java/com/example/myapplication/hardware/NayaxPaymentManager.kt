package com.example.myapplication.hardware

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

enum class PaymentState {
    IDLE,
    INITIALIZING,
    WAITING_FOR_CARD,
    PROCESSING,
    APPROVED,
    DECLINED,
    ERROR,
    CANCELLED
}

data class PaymentResult(
    val success: Boolean,
    val amount: Double? = null,
    val transactionId: String? = null,
    val error: String? = null
)

class NayaxPaymentManager(
    private val usbManager: UsbManager,
    private val usbDevice: UsbDevice,
    private val devicePath: String,
    private val scope: CoroutineScope
) {
    private var serialPort: UsbSerialPort? = null
    private val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState: StateFlow<PaymentState> = _paymentState
    
    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult
    
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
                if (driver !is CdcAcmSerialDriver) {
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
                _paymentState.value = PaymentState.IDLE
                startReading()
            } catch (e: Exception) {
                _isReady.value = false
                _paymentState.value = PaymentState.ERROR
                _paymentResult.value = PaymentResult(false, error = e.message)
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
                        processPaymentData(data)
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        delay(100)
                    }
                }
            }
        }
    }
    
    private fun processPaymentData(data: String) {
        scope.launch(Dispatchers.Default) {
            // Nayax protocol handling
            // This is a simplified implementation - actual protocol would need
            // proper message framing, ACK/NAK handling, etc.
            when (_paymentState.value) {
                PaymentState.WAITING_FOR_CARD -> {
                    // Card detected, process authorization
                    _paymentState.value = PaymentState.PROCESSING
                }
                PaymentState.PROCESSING -> {
                    // Parse response from Nayax
                    if (data.contains("APPROVED", ignoreCase = true)) {
                        _paymentState.value = PaymentState.APPROVED
                        _paymentResult.value = PaymentResult(
                            success = true,
                            transactionId = extractTransactionId(data)
                        )
                    } else if (data.contains("DECLINED", ignoreCase = true)) {
                        _paymentState.value = PaymentState.DECLINED
                        _paymentResult.value = PaymentResult(
                            success = false,
                            error = "Payment declined"
                        )
                    }
                }
                else -> {
                    // Handle other states
                }
            }
        }
    }
    
    private fun extractTransactionId(data: String): String? {
        // Extract transaction ID from Nayax response
        // This would need proper protocol parsing
        return "TXN_${System.currentTimeMillis()}"
    }
    
    suspend fun initiatePayment(amount: Double): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isReady.value) return@withContext false
                
                _paymentState.value = PaymentState.INITIALIZING
                
                // Send INIT command to Nayax
                sendCommand("INIT")
                delay(500)
                
                // Enable card reader
                sendCommand("ENABLE")
                delay(500)
                
                _paymentState.value = PaymentState.WAITING_FOR_CARD
                
                // Wait for card or timeout
                val timeout = 60000L // 60 seconds
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() - startTime < timeout) {
                    if (_paymentState.value == PaymentState.APPROVED) {
                        return@withContext true
                    }
                    if (_paymentState.value == PaymentState.DECLINED || 
                        _paymentState.value == PaymentState.ERROR) {
                        return@withContext false
                    }
                    delay(100)
                }
                
                _paymentState.value = PaymentState.CANCELLED
                _paymentResult.value = PaymentResult(false, error = "Payment timeout")
                false
            } catch (e: Exception) {
                _paymentState.value = PaymentState.ERROR
                _paymentResult.value = PaymentResult(false, error = e.message)
                false
            }
        }
    }
    
    private suspend fun sendCommand(command: String) {
        val port = serialPort ?: return
        try {
            val data = command.toByteArray()
            port.write(data, 1000)
        } catch (e: IOException) {
            throw e
        }
    }
    
    fun cancelPayment() {
        scope.launch {
            _paymentState.value = PaymentState.CANCELLED
            sendCommand("CANCEL")
        }
    }
    
    fun reset() {
        scope.launch {
            _paymentState.value = PaymentState.IDLE
            _paymentResult.value = null
        }
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
            _paymentState.value = PaymentState.IDLE
        }
    }
}

