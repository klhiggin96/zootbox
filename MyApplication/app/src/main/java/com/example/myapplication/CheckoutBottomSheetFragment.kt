package com.example.myapplication

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

/**
 * Checkout state for the bottom sheet
 */
enum class CheckoutState {
    PAY_NOW,        // Initial state - user needs to verify ID first
    PAYMENT_READY   // Post-ID verification - waiting for card tap
}

/**
 * Bottom sheet checkout fragment that slides up with product summary and price breakdown.
 *
 * Shows:
 * - Product thumbnail, name, and price
 * - Subtotal, tax, and total calculations
 * - "Complete Purchase" CTA button
 *
 * Usage:
 * ```kotlin
 * val bottomSheet = CheckoutBottomSheetFragment.newInstance(
 *     productName = "SPEARMINT 6MG",
 *     productCategory = "PREMIUM POUCHES",
 *     productPrice = 24.99,
 *     quantity = 1
 * )
 * bottomSheet.setOnCheckoutComplete {
 *     // Handle checkout completion
 * }
 * bottomSheet.show(supportFragmentManager, "CheckoutBottomSheet")
 * ```
 */
class CheckoutBottomSheetFragment : BottomSheetDialogFragment() {

    private var productName: String = ""
    private var productCategory: String = ""
    private var productPrice: Double = 0.0
    private var quantity: Int = 1
    private var checkoutState: CheckoutState = CheckoutState.PAY_NOW

    private var onCheckoutComplete: (() -> Unit)? = null
    private var onDismissed: (() -> Unit)? = null
    private var onPayNowClicked: (() -> Unit)? = null
    private var onPaymentCancelled: (() -> Unit)? = null

    // View references for state management
    private var payNowContainer: LinearLayout? = null
    private var paymentStateContainer: LinearLayout? = null
    private var btnCancelPayment: Button? = null
    private var btnClose: ImageButton? = null
    private var textTitle: TextView? = null

    // Auto-dismiss handler for PAYMENT_READY state
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null
    private val PAYMENT_TIMEOUT_MS = 30_000L  // 30 seconds

    // Bottom sheet behavior reference for controlling dragging
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    // Tax rate (7.5%)
    private val taxRate = 0.075

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            productName = it.getString(ARG_PRODUCT_NAME, "")
            productCategory = it.getString(ARG_PRODUCT_CATEGORY, "PREMIUM POUCHES")
            productPrice = it.getDouble(ARG_PRODUCT_PRICE, 0.0)
            quantity = it.getInt(ARG_QUANTITY, 1)
            val stateStr = it.getString(ARG_INITIAL_STATE, CheckoutState.PAY_NOW.name)
            checkoutState = try {
                CheckoutState.valueOf(stateStr)
            } catch (e: Exception) {
                CheckoutState.PAY_NOW
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_checkout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        val textProductName = view.findViewById<TextView>(R.id.text_product_name)
        val textProductCategory = view.findViewById<TextView>(R.id.text_product_category)
        val textProductPrice = view.findViewById<TextView>(R.id.text_product_price)
        val textSubtotal = view.findViewById<TextView>(R.id.text_subtotal)
        val textTax = view.findViewById<TextView>(R.id.text_tax)
        val textTotal = view.findViewById<TextView>(R.id.text_total)
        val btnCompletePurchase = view.findViewById<Button>(R.id.btn_complete_purchase)

        // Bind state-related views
        textTitle = view.findViewById(R.id.text_checkout_title)
        payNowContainer = view.findViewById(R.id.pay_now_container)
        paymentStateContainer = view.findViewById(R.id.payment_state_container)
        btnCancelPayment = view.findViewById(R.id.btn_cancel_payment)
        btnClose = view.findViewById(R.id.btn_close)

        // Calculate prices
        val subtotal = productPrice * quantity
        val tax = subtotal * taxRate
        val total = subtotal + tax

        // Set product info
        textProductName.text = productName.uppercase()
        textProductCategory.text = productCategory.uppercase()
        textProductPrice.text = currencyFormatter.format(productPrice)

        // Set price breakdown
        textSubtotal.text = currencyFormatter.format(subtotal)
        textTax.text = currencyFormatter.format(tax)
        textTotal.text = currencyFormatter.format(total)

        // Close button
        btnClose?.setOnClickListener {
            dismiss()
        }

        // PAY NOW / Complete purchase button
        btnCompletePurchase.setOnClickListener {
            when (checkoutState) {
                CheckoutState.PAY_NOW -> {
                    // User taps PAY NOW -> trigger ID scan
                    onPayNowClicked?.invoke()
                }
                CheckoutState.PAYMENT_READY -> {
                    // Fallback: user taps button in payment state (shouldn't happen)
                    dismiss()
                    onCheckoutComplete?.invoke()
                }
            }
        }

        // Cancel payment button
        btnCancelPayment?.setOnClickListener {
            dismiss()
            onPaymentCancelled?.invoke()
        }

        // Apply initial state
        updateUIForState()
    }

    /**
     * Update UI based on current checkout state
     */
    private fun updateUIForState() {
        when (checkoutState) {
            CheckoutState.PAY_NOW -> {
                textTitle?.text = "PAY NOW"
                payNowContainer?.visibility = View.VISIBLE
                paymentStateContainer?.visibility = View.GONE
                btnClose?.visibility = View.VISIBLE
                // Allow dismissing in PAY_NOW state
                isCancelable = true
                bottomSheetBehavior?.isDraggable = true
                // Cancel any pending auto-dismiss
                cancelAutoDismiss()
            }
            CheckoutState.PAYMENT_READY -> {
                textTitle?.text = "TAP CARD TO PAY"
                payNowContainer?.visibility = View.GONE
                paymentStateContainer?.visibility = View.VISIBLE
                btnClose?.visibility = View.GONE
                // Prevent dismissing in PAYMENT_READY state
                isCancelable = false
                bottomSheetBehavior?.isDraggable = false
                // Start 30-second auto-dismiss timer
                startAutoDismissTimer()
            }
        }
    }

    /**
     * Start auto-dismiss timer for PAYMENT_READY state
     */
    private fun startAutoDismissTimer() {
        cancelAutoDismiss()
        autoDismissRunnable = Runnable {
            if (isAdded && checkoutState == CheckoutState.PAYMENT_READY) {
                onPaymentCancelled?.invoke()
                dismiss()
            }
        }
        handler.postDelayed(autoDismissRunnable!!, PAYMENT_TIMEOUT_MS)
    }

    /**
     * Cancel pending auto-dismiss timer
     */
    private fun cancelAutoDismiss() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        autoDismissRunnable = null
    }

    /**
     * Set callback for when user taps PAY NOW button (triggers ID scan)
     */
    fun setOnPayNowClicked(callback: () -> Unit) {
        onPayNowClicked = callback
    }

    /**
     * Set callback for when user cancels payment
     */
    fun setOnPaymentCancelled(callback: () -> Unit) {
        onPaymentCancelled = callback
    }

    /**
     * Change the checkout state and update UI
     */
    fun setCheckoutState(state: CheckoutState) {
        checkoutState = state
        if (view != null) {
            updateUIForState()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                bottomSheetBehavior = behavior

                // Set peek height to ~65% of screen height
                val screenHeight = resources.displayMetrics.heightPixels
                behavior.peekHeight = (screenHeight * 0.65).toInt()

                // Start expanded
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                // Set draggable based on state (disabled in PAYMENT_READY)
                behavior.isDraggable = checkoutState == CheckoutState.PAY_NOW
                behavior.skipCollapsed = true

                // Set background to transparent so our rounded corners show
                sheet.setBackgroundResource(android.R.color.transparent)
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelAutoDismiss()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        cancelAutoDismiss()
        onDismissed?.invoke()
    }

    /**
     * Set callback for when user taps "Complete Purchase"
     */
    fun setOnCheckoutComplete(callback: () -> Unit) {
        onCheckoutComplete = callback
    }

    /**
     * Set callback for when bottom sheet is dismissed (close button, swipe down, tap outside)
     */
    fun setOnDismissed(callback: () -> Unit) {
        onDismissed = callback
    }

    companion object {
        private const val ARG_PRODUCT_NAME = "product_name"
        private const val ARG_PRODUCT_CATEGORY = "product_category"
        private const val ARG_PRODUCT_PRICE = "product_price"
        private const val ARG_QUANTITY = "quantity"
        private const val ARG_INITIAL_STATE = "initial_state"

        /**
         * Create a new instance of CheckoutBottomSheetFragment
         *
         * @param productName The product name (e.g., "SPEARMINT 6MG")
         * @param productCategory The product category (e.g., "PREMIUM POUCHES")
         * @param productPrice The unit price of the product
         * @param quantity The quantity being purchased
         * @param initialState The initial checkout state (PAY_NOW or PAYMENT_READY)
         */
        @JvmStatic
        fun newInstance(
            productName: String,
            productCategory: String = "PREMIUM POUCHES",
            productPrice: Double,
            quantity: Int = 1,
            initialState: CheckoutState = CheckoutState.PAY_NOW
        ): CheckoutBottomSheetFragment {
            return CheckoutBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRODUCT_NAME, productName)
                    putString(ARG_PRODUCT_CATEGORY, productCategory)
                    putDouble(ARG_PRODUCT_PRICE, productPrice)
                    putInt(ARG_QUANTITY, quantity)
                    putString(ARG_INITIAL_STATE, initialState.name)
                }
            }
        }
    }
}
