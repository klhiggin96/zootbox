package com.example.myapplication.hardware

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.bitmick.marshall.UsbSerialBridge
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
import com.bitmick.marshall.models.vmc_configuration
import com.bitmick.marshall.vmc.vmc_framework
import com.bitmick.marshall.vmc.vmc_link
import com.bitmick.marshall.vmc.vmc_vend_t
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Nayax VPOS Touch Payment Manager using Official Marshall SDK
 *
 * This implementation uses the Marshall SDK extracted from DMVI's APK.
 * The SDK handles the complex protocol communication, CRC calculations,
 * and state machine management for Nayax payment processing.
 *
 * Protocol: Marshall Protocol (Binary, 115200 bps, 8N1)
 * Hardware: Nayax VPOS Touch connected via USB CDC-ACM
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
    val transactionId: String? = null,
    val error: String? = null
)

class NayaxPaymentManager(
    private val usbManager: UsbManager,
    private val usbDevice: UsbDevice,
    private val serialPort: UsbSerialPort,      // Already-open port (DMVI pattern)
    private val connection: UsbDeviceConnection, // Keep reference for cleanup
    private val scope: CoroutineScope
) : vmc_vend_t.vend_callbacks_t, vmc_link.vmc_link_events_t {

    private val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    // Marshall SDK components
    private var framework: vmc_framework? = null
    private var usbBridge: UsbSerialBridge? = null

    // Current vend session
    private var pendingVendSession: vmc_vend_t.vend_session_t? = null
    private var pendingAmountCents: Int = 0
    private var pendingItemNumber: Int = 1

    // Continuation for async payment flow
    private var paymentContinuation: CancellableContinuation<Boolean>? = null

    companion object {
        private const val TAG = "NayaxPaymentManager"
        private const val MACHINE_SERIAL = "ZOOTBOX001"
        private const val MACHINE_MODEL = "ZootBox Kiosk"
        private const val SW_VERSION = "1.0.0"
        private const val HW_VERSION = "1.0"
        private const val MANUF_CODE = "ZOOTBOX"
    }

    /**
     * Initialize Marshall SDK connection to Nayax VPOS Touch
     *
     * DMVI pattern: The serial port is already opened before this call.
     * We pass the open port to the SDK via init() and config.port_vpos.
     */
    fun initialize() {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing Nayax payment manager with Marshall SDK (DMVI pattern)")
                _paymentState.value = PaymentState.INITIALIZING

                if (!usbManager.hasPermission(usbDevice)) {
                    Log.e(TAG, "No USB permission for Nayax device")
                    _isReady.value = false
                    _paymentState.value = PaymentState.ERROR
                    return@launch
                }

                // Create USB serial bridge and set the already-open port
                usbBridge = UsbSerialBridge(usbManager, usbDevice).apply {
                    // DMVI pattern: pass already-open port
                    setSerialPort(serialPort, connection)
                }

                // FIX 3: Wrap UsbSerialBridge in AndroidUsbPort for pull-based Marshall SDK access
                val androidUsbPort = AndroidUsbPort(usbBridge)

                // Create Marshall SDK configuration
                // Using EXACT DMVI values - these are likely pre-registered with Nayax
                val config = vmc_configuration().apply {
                    port_vpos = serialPort
                    port_vpos_baud = HardwareService.NAYAX_BAUD_RATE

                    // EXACT DMVI values - copy their registration
                    model = "android-marshall-demo"
                    serial = "1434324619381374"
                    sw_ver = "1.0.0.0"

                    // Feature flags - exact DMVI settings
                    mifare_approved_by_vmc_support = false
                    mag_card_approved_by_vmc_support = false
                    multi_vend_support = true
                    multi_session_support = false
                    price_not_final_support = false
                    reader_always_on = true  // Keep reader enabled to show "Tap Card" instead of "Cash Only"
                    always_idle = false
                    vend_denied_policy = 0

                    // Debug settings
                    dump_packets_level = 2
                    debug = true
                }

                // Create and configure the framework
                // DMVI pattern: use getInstance() for singleton, call link.start() directly
                framework = vmc_framework.getInstance().apply {
                    link.set_serial_port(androidUsbPort)
                        .set_lowlevel(usbBridge)
                        .configure(config)
                        .set_events(this@NayaxPaymentManager)

                    vend.register_callbacks(this@NayaxPaymentManager)
                }

                // CRITICAL: Start the USB bridge read thread FIRST
                // This is needed to fill AndroidUsbPort's CircularBuffer via DataCallback
                usbBridge?.start()
                Log.d(TAG, "UsbSerialBridge read thread started")

                // Start the Marshall SDK link - DMVI calls link.start() directly!
                framework?.link?.start()

                Log.i(TAG, "Marshall SDK link.start() called (DMVI pattern), waiting for Nayax connection...")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Nayax payment manager", e)
                _isReady.value = false
                _paymentState.value = PaymentState.ERROR
                _paymentResult.value = PaymentResult(false, error = e.message)
            }
        }
    }

    // ========== vmc_link.vmc_link_events_t callbacks ==========

    override fun onReady(config: vmc_link.vpos_config_t?) {
        Log.i(TAG, "Marshall SDK connected to Nayax VPOS Touch")
        config?.let {
            Log.d(TAG, "VPOS Serial: ${String(it.vpos_serial ?: byteArrayOf())}")
            Log.d(TAG, "Protocol Version: ${it.prot_ver_major}.${it.prot_ver_minor}")
        }

        scope.launch {
            _isReady.value = true
            _paymentState.value = PaymentState.READY
        }
    }

    override fun onCommError() {
        Log.e(TAG, "Marshall SDK communication error")
        scope.launch {
            _isReady.value = false
            _paymentState.value = PaymentState.ERROR
            _paymentResult.value = PaymentResult(false, error = "Communication error with Nayax")

            // Resume any waiting payment with failure
            paymentContinuation?.resume(false) {}
            paymentContinuation = null
        }
    }

    // ========== vmc_vend_t.vend_callbacks_t callbacks ==========

    override fun onReady(previousSession: vmc_vend_t.vend_session_t?) {
        Log.d(TAG, "Vend module ready")
        if (previousSession != null) {
            Log.d(TAG, "Previous session status: ${previousSession.session_status}")
        }
    }

    override fun onSessionBegin(fundsAvailable: Int) {
        Log.i(TAG, "Payment session started, funds available: $fundsAvailable cents")
        scope.launch {
            _paymentState.value = PaymentState.WAITING_FOR_CARD
        }
    }

    override fun onVendApproved(session: vmc_vend_t.vend_session_t?): Boolean {
        Log.i(TAG, "Payment APPROVED!")

        val transactionId = session?.data?.transaction_id?.let { "NYX_$it" }
            ?: "NYX_${System.currentTimeMillis()}"

        scope.launch {
            _paymentState.value = PaymentState.APPROVED
            _paymentResult.value = PaymentResult(
                success = true,
                amount = pendingAmountCents / 100.0,
                transactionId = transactionId
            )

            // Resume the waiting payment coroutine
            paymentContinuation?.resume(true) {}
            paymentContinuation = null
        }

        // Return true to indicate we will handle the vend
        return true
    }

    override fun onVendDenied(session: vmc_vend_t.vend_session_t?) {
        Log.w(TAG, "Payment DECLINED")

        scope.launch {
            _paymentState.value = PaymentState.DECLINED
            _paymentResult.value = PaymentResult(
                success = false,
                error = "Payment declined"
            )

            // Resume the waiting payment coroutine
            paymentContinuation?.resume(false) {}
            paymentContinuation = null
        }
    }

    override fun onTransactionInfo(data: vmc_vend_t.vend_session_data_t?) {
        data?.let {
            Log.d(TAG, "Transaction info received:")
            Log.d(TAG, "  Card type: ${it.card_type}")
            Log.d(TAG, "  Card entry mode: ${it.card_entry_mode}")
            Log.d(TAG, "  Last 4 digits: ${it.cc_last_4_digits}")
        }
    }

    override fun onSettlement(success: Boolean) {
        Log.d(TAG, "Settlement ${if (success) "completed" else "failed"}")
    }

    override fun onStatus(status: Int) {
        Log.d(TAG, "Status update: $status")
    }

    override fun onReaderState(enabled: Boolean) {
        Log.d(TAG, "Reader state: ${if (enabled) "enabled" else "disabled"}")
    }

    override fun onRemoteVend(productCode: Int, price: Int, quantity: Int) {
        Log.d(TAG, "Remote vend request: product=$productCode, price=$price, qty=$quantity")
    }

    override fun onReceipt(type: Int, data: String?) {
        Log.d(TAG, "Receipt received: type=$type, data=$data")
    }

    override fun onOpenedSessions(sessions: ShortArray?) {
        Log.d(TAG, "Opened sessions: ${sessions?.contentToString()}")
    }

    // ========== Public API ==========

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

                val vend = framework?.vend
                if (vend == null || !vend.is_ready) {
                    Log.e(TAG, "Vend module not ready")
                    return@withContext false
                }

                Log.i(TAG, "Initiating payment: $$amount (item #$itemNumber)")

                // Reset previous result
                _paymentResult.value = null
                _paymentState.value = PaymentState.INITIALIZING

                // Store pending vend info
                pendingAmountCents = (amount * 100).toInt()
                pendingItemNumber = itemNumber

                // Start a credit session
                vend.session_start(vmc_vend_t.session_type_credit_e)

                // Wait for session to begin, then send vend request
                delay(500) // Give the SDK time to enable the reader

                // Create vend session
                val vendSession = vmc_vend_t.vend_session_t(
                    itemNumber,           // product code
                    1,                    // quantity
                    vmc_vend_t.vend_item_t.UNIT_DONT_CARE.toByte(),
                    pendingAmountCents    // price in cents
                )
                pendingVendSession = vendSession

                // Request vend
                vend.vend_request(vendSession)
                Log.i(TAG, "Vend request sent: $pendingAmountCents cents for item $itemNumber")

                _paymentState.value = PaymentState.WAITING_FOR_CARD

                // Wait for payment result with timeout
                suspendCancellableCoroutine<Boolean> { continuation ->
                    paymentContinuation = continuation

                    // Set timeout
                    scope.launch {
                        delay(120000) // 120 seconds timeout
                        if (paymentContinuation != null) {
                            Log.w(TAG, "Payment timeout")
                            _paymentState.value = PaymentState.CANCELLED
                            _paymentResult.value = PaymentResult(false, error = "Payment timeout")
                            continuation.resume(false) {}
                            paymentContinuation = null
                        }
                    }
                }

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
     */
    suspend fun confirmVend(success: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val vend = framework?.vend ?: return@withContext
                val session = pendingVendSession ?: return@withContext

                if (success) {
                    Log.i(TAG, "Confirming successful vend")
                    session.session_status = vmc_vend_t.session_status_ok_e
                } else {
                    Log.w(TAG, "Confirming vend failure (will trigger refund)")
                    session.session_status = vmc_vend_t.session_status_fail_to_dispense_e
                }

                // Close the session with the result
                vend.session_close(session)

                _paymentState.value = if (success) PaymentState.READY else PaymentState.ERROR
                pendingVendSession = null

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
                framework?.vend?.session_cancel()
                _paymentState.value = PaymentState.CANCELLED

                paymentContinuation?.resume(false) {}
                paymentContinuation = null
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
            pendingVendSession = null
        }
    }

    /**
     * Close SDK connection
     */
    fun close() {
        scope.launch {
            try {
                Log.i(TAG, "Closing Nayax payment manager")

                // Cancel any pending payment
                paymentContinuation?.cancel()
                paymentContinuation = null

                // Stop Marshall SDK
                framework?.stop()
                framework = null

                // Close USB bridge
                usbBridge?.stop()
                usbBridge = null

                _isReady.value = false
                _paymentState.value = PaymentState.IDLE

            } catch (e: Exception) {
                Log.e(TAG, "Error closing payment manager", e)
            }
        }
    }
}
