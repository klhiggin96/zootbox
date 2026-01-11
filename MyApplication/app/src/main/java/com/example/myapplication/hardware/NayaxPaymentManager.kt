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
import kotlin.math.roundToInt
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
 * FLOW TYPE: Pre-Selection
 *   1. User selects product in app UI
 *   2. App calls initiatePayment(amount) -> sends vend_request()
 *   3. VPOS screen shows price and "Please Present Card"
 *   4. User taps card
 *   5. Payment authorization -> onVendApproved() callback
 *   6. Product dispensed -> confirmVend(success)
 *   7. Settlement -> onSettlement(success)
 *
 * Protocol: Marshall Protocol (Binary, 115200 bps, 8N1)
 * Hardware: Nayax VPOS Touch connected via USB FTDI interface
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

                    // Machine type - retail vending machine
                    machine_type = vmc_configuration.machine_type_type_retail

                    // Feature flags - exact DMVI settings
                    mifare_approved_by_vmc_support = false
                    mag_card_approved_by_vmc_support = false
                    multi_vend_support = false  // DISABLED: Use standard MDB vendRequest instead of Extended MDB to avoid Auth Status -1
                    multi_session_support = false
                    price_not_final_support = false
                    reader_always_on = true  // Keep reader enabled to show "Tap Card" instead of "Cash Only"
                    always_idle = true  // Enable Pre-Selection flow (app sends price before card tap)
                    vend_denied_policy = 0

                    // Note: This SDK version doesn't have explicit_vend_success field
                    // The SDK handles vend confirmation via session_close() call in confirmVend()

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
            // CRITICAL: Check decimal place configuration (0 = $108.00, 2 = $1.08)
            Log.e(TAG, "CONFIGURATION CHECK: Decimal Places = ${it.decimal_place}")
            if (it.decimal_place.toInt() == 0) {
                Log.e(TAG, "WARNING: Decimal place is 0! Sending 108 will charge $108.00, not $1.08")
            }
        }

        scope.launch {
            // NOTE: Do NOT call session_start(0) here!
            // The VPOS interprets session_start(0) as an invalid payment request,
            // which causes it to enter an error state and stop responding to keep-alives.
            // DMVI's reference implementation simply sets ready state without session manipulation.

            Log.i(TAG, "onReady() called - setting ready state")
            _isReady.value = true
            _paymentState.value = PaymentState.READY
            Log.i(TAG, "Ready to accept payments!")
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
        Log.d(TAG, "Vend module ready (session ended or reset)")
        if (previousSession != null) {
            Log.d(TAG, "Previous session status: ${previousSession.session_status}")
        }

        // CRITICAL: Handle user cancellation from VPOS (e.g., user pressed Cancel button)
        // If paymentContinuation is still active, it means the session ended without
        // approval/denial - this is a cancellation. Resume with false to unblock UI.
        scope.launch {
            if (paymentContinuation != null) {
                Log.w(TAG, "Session ended while payment pending - user cancelled or timeout")
                _paymentState.value = PaymentState.CANCELLED
                _paymentResult.value = PaymentResult(false, error = "Payment cancelled")
                paymentContinuation?.resume(false) {}
                paymentContinuation = null
            }
            // Reset to ready state
            _paymentState.value = PaymentState.READY
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

        // --- DEBUGGING: Extract denial reason codes ---
        var denialReason = "Payment declined"
        if (session != null) {
            // 1. Check the high-level session status (Expected: 4 for Denied)
            Log.e(TAG, "Session Status Code: ${session.session_status}")

            // 2. Check the authorization status from the VMC/Gateway
            if (session.data != null) {
                val authStatus = session.data.vmc_auth_status
                val authReason = when (authStatus) {
                    0 -> "Approved (Unexpected in onVendDenied)"
                    1 -> "Declined by Acquirer/Bank"
                    else -> "Unknown Auth Status: $authStatus"
                }
                Log.e(TAG, "VMC Auth Status: $authStatus ($authReason)")
                denialReason = authReason

                // 3. Log card details to ensure the card was actually read
                Log.e(TAG, "Card Type: ${session.data.card_type}")
                Log.e(TAG, "Last 4: ${session.data.cc_last_4_digits}")
            } else {
                Log.e(TAG, "Session Data is NULL (Denial happened before auth request?)")
                denialReason = "Denied before authorization"
            }
        }
        // --- END DEBUGGING ---

        scope.launch {
            _paymentState.value = PaymentState.DECLINED
            _paymentResult.value = PaymentResult(
                success = false,
                error = denialReason
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
        if (success) {
            Log.i(TAG, "Settlement completed successfully - payment captured")
        } else {
            Log.e(TAG, "Settlement FAILED - payment may not have been captured!")
            // Update payment result to reflect settlement failure
            // The product may have been dispensed but money not collected
            scope.launch {
                val currentResult = _paymentResult.value
                if (currentResult != null && currentResult.success) {
                    _paymentResult.value = currentResult.copy(
                        error = "Settlement failed - payment may not have been captured"
                    )
                }
            }
        }
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
                if (vend == null) {
                    Log.e(TAG, "Vend module is null")
                    return@withContext false
                }

                // Note: vend.is_ready is false during active sessions, so we don't check it here
                // The link being ready (_isReady.value) is sufficient to start a payment
                Log.i(TAG, "Initiating payment: $$amount (item #$itemNumber)")

                // Reset previous result
                _paymentResult.value = null
                _paymentState.value = PaymentState.INITIALIZING

                // Store pending vend info (round to match displayed price)
                pendingAmountCents = (amount * 100).roundToInt()
                pendingItemNumber = itemNumber

                // With reader_always_on = true and always_idle = true, the SDK manages session lifecycle.
                // The vend_request() call initiates the transaction in Pre-Selection mode.
                // Wait briefly for state machine to be ready
                delay(200)

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
