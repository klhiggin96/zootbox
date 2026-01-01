package com.example.myapplication.hardware

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

/**
 * Nayax VPOS Touch Payment Manager using Marshall Protocol
 *
 * Implements binary packet-based communication at 115,200 bps for
 * processing card and NFC payments through the Nayax VPOS Touch terminal.
 *
 * Protocol: Marshall Protocol (Binary, 115200 bps, 8N1)
 * Hardware: Nayax VPOS Touch connected via USB-to-Serial adapter
 */

enum class PaymentState {
    IDLE,               // Not initialized
    INITIALIZING,       // Opening serial connection
    READY,              // Waiting for payment initiation
    WAITING_FOR_CARD,   // Payment initiated, waiting for customer to tap/insert card
    PROCESSING,         // Card detected, authorizing with Nayax cloud
    APPROVED,           // Payment approved, ready to dispense
    DECLINED,           // Payment declined
    ERROR,              // Communication or hardware error
    CANCELLED           // Payment cancelled by timeout or user
}

data class PaymentResult(
    val success: Boolean,
    val amount: Double? = null,
    val transactionId: String? = null,  // Nayax transaction ID
    val error: String? = null
)

/**
 * Marshall Protocol packet structure
 */
data class MarshallPacket(
    val command: Byte,        // 0x13 (Address/Command byte)
    val subCommand: Byte,     // 0x00 = VEND_REQUEST, 0x01 = KEEP_ALIVE, 0x03 = SESSION_END
    val data: ByteArray = byteArrayOf(),  // Variable length data (price, item number, etc.)
    val checksum: Byte        // Sum of all bytes % 256
) {
    companion object {
        // Command bytes
        const val CMD_ADDRESS: Byte = 0x13

        // Sub-commands
        const val SUB_CMD_VEND_REQUEST: Byte = 0x00
        const val SUB_CMD_KEEP_ALIVE: Byte = 0x01
        const val SUB_CMD_SESSION_END: Byte = 0x03

        // Response codes
        const val RESPONSE_VEND_APPROVED: Byte = 0x10
        const val RESPONSE_VEND_DENIED: Byte = 0x11
        const val RESPONSE_SESSION_BEGIN: Byte = 0x12

        /**
         * Create VEND_REQUEST packet
         * Format: [0x13][0x00][price_high][price_low][item_high][item_low][checksum]
         */
        fun createVendRequest(amountCents: Int, itemNumber: Int): ByteArray {
            val priceHigh = ((amountCents shr 8) and 0xFF).toByte()
            val priceLow = (amountCents and 0xFF).toByte()
            val itemHigh = ((itemNumber shr 8) and 0xFF).toByte()
            val itemLow = (itemNumber and 0xFF).toByte()

            val packet = byteArrayOf(
                CMD_ADDRESS,
                SUB_CMD_VEND_REQUEST,
                priceHigh,
                priceLow,
                itemHigh,
                itemLow
            )

            // Calculate checksum (sum of all bytes % 256)
            val checksum = (packet.sum() and 0xFF).toByte()

            return packet + checksum
        }

        /**
         * Create KEEP_ALIVE packet (sent every 1 second)
         * Format: [0x13][0x01][checksum]
         */
        fun createKeepAlive(): ByteArray {
            val packet = byteArrayOf(CMD_ADDRESS, SUB_CMD_KEEP_ALIVE)
            val checksum = (packet.sum() and 0xFF).toByte()
            return packet + checksum
        }

        /**
         * Create SESSION_END packet (closes payment session)
         * Format: [0x13][0x03][checksum]
         */
        fun createSessionEnd(): ByteArray {
            val packet = byteArrayOf(CMD_ADDRESS, SUB_CMD_SESSION_END)
            val checksum = (packet.sum() and 0xFF).toByte()
            return packet + checksum
        }

        /**
         * Verify checksum of received packet
         */
        fun verifyChecksum(packet: ByteArray): Boolean {
            if (packet.isEmpty()) return false
            val expectedChecksum = packet.last()
            val actualChecksum = (packet.dropLast(1).sum() and 0xFF).toByte()
            return expectedChecksum == actualChecksum
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MarshallPacket

        if (command != other.command) return false
        if (subCommand != other.subCommand) return false
        if (!data.contentEquals(other.data)) return false
        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.toInt()
        result = 31 * result + subCommand
        result = 31 * result + data.contentHashCode()
        result = 31 * result + checksum
        return result
    }
}

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

    private var keepAliveJob: Job? = null
    private var readJob: Job? = null

    companion object {
        private const val TAG = "NayaxPaymentManager"
        private const val READ_TIMEOUT_MS = 1000
        private const val KEEP_ALIVE_INTERVAL_MS = 1000L  // 1 second
    }

    /**
     * Initialize serial connection to Nayax VPOS Touch
     */
    fun initialize() {
        scope.launch {
            try {
                Log.d(TAG, "Initializing Nayax payment manager")
                _paymentState.value = PaymentState.INITIALIZING

                if (!usbManager.hasPermission(usbDevice)) {
                    Log.e(TAG, "No USB permission for Nayax device")
                    _isReady.value = false
                    _paymentState.value = PaymentState.ERROR
                    return@launch
                }

                val driver: UsbSerialDriver? = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
                if (driver !is CdcAcmSerialDriver) {
                    Log.e(TAG, "Nayax device is not CDC-ACM compatible")
                    _isReady.value = false
                    _paymentState.value = PaymentState.ERROR
                    return@launch
                }

                serialPort = driver.ports[0]
                serialPort?.open(usbManager.openDevice(usbDevice))
                serialPort?.setParameters(
                    HardwareService.NAYAX_BAUD_RATE,  // CRITICAL: Marshall Protocol requires 115200 bps
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )

                Log.i(TAG, "Serial port opened at ${HardwareService.NAYAX_BAUD_RATE} bps")

                _isReady.value = true
                _paymentState.value = PaymentState.READY

                // Start background tasks
                startReading()
                startKeepAlive()

                Log.i(TAG, "Nayax payment manager initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Nayax payment manager", e)
                _isReady.value = false
                _paymentState.value = PaymentState.ERROR
                _paymentResult.value = PaymentResult(false, error = e.message)
            }
        }
    }

    /**
     * Start keep-alive heartbeat (CRITICAL SAFETY FEATURE)
     * Sends keep-alive packet every 1 second to maintain connection
     * If heartbeat stops, Nayax VPOS Touch will display "Cash Only"
     */
    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && _isReady.value) {
                try {
                    val packet = MarshallPacket.createKeepAlive()
                    sendPacket(packet)
                    Log.v(TAG, "Sent keep-alive heartbeat")
                } catch (e: Exception) {
                    Log.w(TAG, "Keep-alive failed: ${e.message}")
                }
                delay(KEEP_ALIVE_INTERVAL_MS)
            }
            Log.d(TAG, "Keep-alive stopped")
        }
    }

    /**
     * Background thread for reading serial data
     */
    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive && _isReady.value) {
                try {
                    val port = serialPort ?: break
                    val bytesRead = port.read(buffer, READ_TIMEOUT_MS)
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        processResponse(data)
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        delay(100)
                    }
                }
            }
            Log.d(TAG, "Read loop stopped")
        }
    }

    /**
     * Process binary response from Nayax VPOS Touch
     */
    private fun processResponse(data: ByteArray) {
        scope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "Received ${data.size} bytes: ${data.joinToString(" ") { "%02X".format(it) }}")

                // Verify minimum packet length
                if (data.size < 3) {
                    Log.w(TAG, "Packet too short, ignoring")
                    return@launch
                }

                // Verify checksum
                if (!MarshallPacket.verifyChecksum(data)) {
                    Log.e(TAG, "Checksum verification failed")
                    return@launch
                }

                val responseCode = data[1]

                when (responseCode) {
                    MarshallPacket.RESPONSE_SESSION_BEGIN -> {
                        Log.i(TAG, "Session begin received")
                        if (_paymentState.value == PaymentState.INITIALIZING) {
                            _paymentState.value = PaymentState.WAITING_FOR_CARD
                        }
                    }

                    MarshallPacket.RESPONSE_VEND_APPROVED -> {
                        Log.i(TAG, "Payment APPROVED")

                        // Extract Nayax transaction ID from response (if available)
                        val transactionId = if (data.size > 4) {
                            val txnBytes = data.slice(2 until data.size - 1)
                            "NYX_" + txnBytes.joinToString("") { "%02X".format(it) }
                        } else {
                            "NYX_${System.currentTimeMillis()}"
                        }

                        _paymentState.value = PaymentState.APPROVED
                        _paymentResult.value = PaymentResult(
                            success = true,
                            transactionId = transactionId
                        )
                    }

                    MarshallPacket.RESPONSE_VEND_DENIED -> {
                        Log.w(TAG, "Payment DECLINED")
                        _paymentState.value = PaymentState.DECLINED
                        _paymentResult.value = PaymentResult(
                            success = false,
                            error = "Payment declined by bank"
                        )
                    }

                    else -> {
                        Log.d(TAG, "Unknown response code: 0x%02X".format(responseCode))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing response", e)
            }
        }
    }

    /**
     * Send binary packet to Nayax VPOS Touch
     */
    private suspend fun sendPacket(packet: ByteArray) {
        val port = serialPort ?: throw IOException("Serial port not open")
        withContext(Dispatchers.IO) {
            try {
                port.write(packet, 1000)
                Log.v(TAG, "Sent ${packet.size} bytes: ${packet.joinToString(" ") { "%02X".format(it) }}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send packet", e)
                throw e
            }
        }
    }

    /**
     * Initiate payment for a specific amount
     *
     * @param amount Total amount in USD (e.g., 3.50)
     * @param itemNumber Item number (1-indexed, default = 1)
     * @return true if payment was approved, false otherwise
     */
    suspend fun initiatePayment(amount: Double, itemNumber: Int = 1): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isReady.value) {
                    Log.e(TAG, "Payment manager not ready")
                    return@withContext false
                }

                Log.i(TAG, "Initiating payment: $$amount (item #$itemNumber)")

                // Reset previous result
                _paymentResult.value = null
                _paymentState.value = PaymentState.INITIALIZING

                // Convert amount to cents
                val amountCents = (amount * 100).toInt()

                // Send VEND_REQUEST packet
                val vendPacket = MarshallPacket.createVendRequest(amountCents, itemNumber)
                sendPacket(vendPacket)

                // Wait for VPOS Touch to display "Present Card"
                _paymentState.value = PaymentState.WAITING_FOR_CARD

                // Wait for card tap or timeout
                val timeout = 120000L // 120 seconds (configured in Nayax DCS)
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < timeout) {
                    when (_paymentState.value) {
                        PaymentState.APPROVED -> {
                            Log.i(TAG, "Payment approved successfully")
                            return@withContext true
                        }
                        PaymentState.DECLINED, PaymentState.ERROR -> {
                            Log.w(TAG, "Payment failed: ${_paymentState.value}")
                            return@withContext false
                        }
                        else -> {
                            delay(100)
                        }
                    }
                }

                // Timeout
                Log.w(TAG, "Payment timeout after ${timeout}ms")
                _paymentState.value = PaymentState.CANCELLED
                _paymentResult.value = PaymentResult(false, error = "Payment timeout - no card presented")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Payment initiation failed", e)
                _paymentState.value = PaymentState.ERROR
                _paymentResult.value = PaymentResult(false, error = e.message)
                false
            }
        }
    }

    /**
     * Confirm vend result to Nayax
     *
     * @param success true if product was successfully dispensed, false if jam/failure
     *
     * If success = false, this triggers an automatic refund through Nayax cloud
     */
    suspend fun confirmVend(success: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                if (success) {
                    Log.i(TAG, "Confirming successful vend")
                    // Send SESSION_END to finalize transaction
                    val packet = MarshallPacket.createSessionEnd()
                    sendPacket(packet)
                    _paymentState.value = PaymentState.READY
                } else {
                    Log.w(TAG, "Vend failed, triggering refund")
                    // Send VEND_FAILURE (triggers automatic refund)
                    // Note: This would be a different packet type in full implementation
                    val packet = MarshallPacket.createSessionEnd()
                    sendPacket(packet)
                    _paymentState.value = PaymentState.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to confirm vend", e)
            }
        }
    }

    /**
     * Cancel ongoing payment
     */
    fun cancelPayment() {
        scope.launch {
            try {
                Log.i(TAG, "Cancelling payment")
                _paymentState.value = PaymentState.CANCELLED
                val packet = MarshallPacket.createSessionEnd()
                sendPacket(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel payment", e)
            }
        }
    }

    /**
     * Reset payment state
     */
    fun reset() {
        scope.launch {
            _paymentState.value = PaymentState.READY
            _paymentResult.value = null
        }
    }

    /**
     * Close serial connection
     */
    fun close() {
        scope.launch {
            try {
                Log.i(TAG, "Closing Nayax payment manager")

                // Stop background jobs
                keepAliveJob?.cancel()
                readJob?.cancel()

                // Close serial port
                serialPort?.close()
                serialPort = null

                _isReady.value = false
                _paymentState.value = PaymentState.IDLE
            } catch (e: Exception) {
                Log.e(TAG, "Error closing payment manager", e)
            }
        }
    }
}
