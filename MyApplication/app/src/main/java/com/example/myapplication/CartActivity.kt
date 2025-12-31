package com.example.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.cart.CartManager
import com.example.myapplication.database.InventoryRepository
import com.example.myapplication.database.models.Transaction
import com.example.myapplication.hardware.HardwareService
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var cartManager: CartManager
    private lateinit var inventoryRepo: InventoryRepository

    // Hardware Service
    private var hardwareService: HardwareService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? HardwareService.LocalBinder
            hardwareService = binder?.getService()
            serviceBound = true
            Log.d(TAG, "HardwareService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hardwareService = null
            serviceBound = false
            Log.d(TAG, "HardwareService disconnected")
        }
    }

    // UI References
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyCartView: LinearLayout
    private lateinit var cartContentView: LinearLayout
    private lateinit var textTotal: TextView
    private lateinit var textItemCount: TextView
    private lateinit var btnCheckout: Button
    private lateinit var btnClearCart: Button

    private lateinit var cartAdapter: CartAdapter

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    companion object {
        private const val TAG = "CartActivity"

        fun start(context: Context) {
            val intent = Intent(context, CartActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Initialize dependencies
        inventoryRepo = InventoryRepository.getInstance(this)
        cartManager = CartApplication.getInstance().cartManager

        // Initialize UI
        initializeViews()
        setupRecyclerView()
        setupButtons()
        observeCart()

        // Bind to HardwareService
        bindToHardwareService()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.cart_recycler_view)
        emptyCartView = findViewById(R.id.empty_cart_view)
        cartContentView = findViewById(R.id.cart_content_view)
        textTotal = findViewById(R.id.text_total)
        textItemCount = findViewById(R.id.text_item_count)
        btnCheckout = findViewById(R.id.btn_checkout)
        btnClearCart = findViewById(R.id.btn_clear_cart)

        // Setup back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { itemId, newQuantity ->
                cartManager.updateQuantity(itemId, newQuantity)
            },
            onRemoveItem = { itemId ->
                cartManager.removeItem(itemId)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun setupButtons() {
        btnCheckout.setOnClickListener {
            processCheckout()
        }

        btnClearCart.setOnClickListener {
            cartManager.clear()
            Toast.makeText(this, "Cart cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeCart() {
        lifecycleScope.launch {
            cartManager.cartItems.collect { items ->
                updateUI(items)
            }
        }

        lifecycleScope.launch {
            cartManager.totalAmount.collect { total ->
                textTotal.text = currencyFormatter.format(total)
                btnCheckout.text = "CHECKOUT - ${currencyFormatter.format(total)}"
            }
        }

        lifecycleScope.launch {
            cartManager.itemCount.collect { count ->
                textItemCount.text = if (count == 1) "1 item" else "$count items"
            }
        }
    }

    private fun updateUI(items: List<CartManager.CartItem>) {
        if (items.isEmpty()) {
            emptyCartView.visibility = View.VISIBLE
            cartContentView.visibility = View.GONE
        } else {
            emptyCartView.visibility = View.GONE
            cartContentView.visibility = View.VISIBLE
            cartAdapter.submitList(items)
        }
    }

    private fun bindToHardwareService() {
        val intent = Intent(this, HardwareService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Process multi-item checkout
     *
     * Flow:
     * 1. Validate cart (inventory, no jams)
     * 2. Initiate single payment for total amount
     * 3. If approved:
     *    - Vend each item sequentially
     *    - Log transactions for each item
     *    - Confirm vend to Nayax (or refund if any fail)
     * 4. Clear cart on success
     */
    private fun processCheckout() {
        // Validate cart
        val validationResult = cartManager.validateCart()
        if (validationResult.isFailure) {
            Toast.makeText(
                this,
                "Cart validation failed: ${validationResult.exceptionOrNull()?.message}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Get hardware managers
        val paymentManager = hardwareService?.getNayaxPaymentManager()
        val motorManager = hardwareService?.getMotorControlManager()

        // Check if payment hardware is available
        if (paymentManager == null) {
            Log.w(TAG, "Payment manager not available - using free vend mode")
            processFreeCheckout()
            return
        }

        // Check if motor control is available
        if (motorManager == null) {
            Log.e(TAG, "Motor control manager not available")
            Toast.makeText(this, "Vending system unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = cartManager.totalAmount.value
        val itemCount = cartManager.itemCount.value

        Log.i(TAG, "Processing checkout: $itemCount items, total: ${currencyFormatter.format(totalAmount)}")

        // Disable buttons during checkout
        btnCheckout.isEnabled = false
        btnClearCart.isEnabled = false
        btnCheckout.text = "PROCESSING PAYMENT..."

        lifecycleScope.launch {
            try {
                Toast.makeText(
                    this@CartActivity,
                    "Present Card - ${currencyFormatter.format(totalAmount)}",
                    Toast.LENGTH_LONG
                ).show()

                // Initiate payment (blocks until card tap or timeout)
                val paymentApproved = paymentManager.initiatePayment(totalAmount, itemNumber = itemCount)

                if (paymentApproved) {
                    Log.i(TAG, "Payment APPROVED")

                    // Get Nayax transaction ID
                    val paymentResult = paymentManager.paymentResult.value
                    val nayaxTransactionId = paymentResult?.transactionId

                    Toast.makeText(
                        this@CartActivity,
                        "Payment approved! Dispensing products...",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Get vend list (coilId, product, quantity for each item)
                    val vendList = cartManager.getVendList()
                    var allVendsSuccessful = true
                    var vendedCount = 0

                    for ((index, vendItem) in vendList.withIndex()) {
                        val (coilId, product, _) = vendItem
                        Log.d(TAG, "Vending item ${index + 1}/${vendList.size}: ${product.name} from coil $coilId")

                        val vendSuccess = motorManager.vendMotor(coilId)

                        if (vendSuccess) {
                            vendedCount++
                            inventoryRepo.decrementInventory(coilId)

                            // Log transaction with payment data
                            val transaction = Transaction.createWithPayment(
                                coilId = coilId,
                                status = Transaction.STATUS_SUCCESS,
                                amount = product.price,
                                paymentMethod = Transaction.PAYMENT_METHOD_CARD,
                                paymentStatus = Transaction.PAYMENT_STATUS_APPROVED,
                                nayaxTransactionId = nayaxTransactionId,
                                productId = product.id
                            )
                            inventoryRepo.saveTransaction(transaction)
                            Log.d(TAG, "Saved transaction: ${transaction.id}")
                        } else {
                            // Vend failed (motor jam)
                            Log.e(TAG, "Motor vend FAILED for coil $coilId")
                            allVendsSuccessful = false

                            // Log JAM transaction
                            val jamTransaction = Transaction.createWithPayment(
                                coilId = coilId,
                                status = Transaction.STATUS_JAM,
                                amount = product.price,
                                paymentMethod = Transaction.PAYMENT_METHOD_CARD,
                                paymentStatus = Transaction.PAYMENT_STATUS_REFUNDED,
                                nayaxTransactionId = nayaxTransactionId,
                                productId = product.id
                            )
                            inventoryRepo.saveTransaction(jamTransaction)

                            break  // Stop vending on first failure
                        }
                    }

                    if (allVendsSuccessful) {
                        // Confirm successful vend to Nayax
                        paymentManager.confirmVend(success = true)

                        Toast.makeText(
                            this@CartActivity,
                            "Purchase successful! $vendedCount items dispensed. Thank you!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Clear cart
                        cartManager.clear()

                        Log.i(TAG, "Checkout completed successfully")
                    } else {
                        // Partial failure - trigger refund
                        paymentManager.confirmVend(success = false)

                        val message = if (vendedCount > 0) {
                            "Partial vend: $vendedCount/${vendList.size} items dispensed. Payment will be refunded."
                        } else {
                            "Vend failed. Payment will be refunded."
                        }

                        Toast.makeText(
                            this@CartActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()

                        Log.w(TAG, "Checkout failed - payment refunded (vended $vendedCount/${vendList.size})")
                    }
                } else {
                    // Payment declined or cancelled
                    Log.w(TAG, "Payment DECLINED or CANCELLED")
                    val paymentResult = paymentManager.paymentResult.value
                    val errorMessage = paymentResult?.error ?: "Payment declined"

                    Toast.makeText(
                        this@CartActivity,
                        "Payment failed: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Checkout error", e)
                Toast.makeText(
                    this@CartActivity,
                    "Checkout error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Re-enable buttons
                btnCheckout.isEnabled = true
                btnClearCart.isEnabled = true
                btnCheckout.text = "CHECKOUT - ${currencyFormatter.format(totalAmount)}"
            }
        }
    }

    /**
     * Process free checkout (no payment required)
     * Used when payment hardware unavailable or for testing
     */
    private fun processFreeCheckout() {
        val motorManager = hardwareService?.getMotorControlManager()
        if (motorManager == null) {
            Toast.makeText(this, "Vending system unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        btnCheckout.isEnabled = false
        btnClearCart.isEnabled = false
        btnCheckout.text = "DISPENSING..."

        lifecycleScope.launch {
            try {
                val vendList = cartManager.getVendList()
                var allVendsSuccessful = true
                var vendedCount = 0

                for ((index, vendItem) in vendList.withIndex()) {
                    val (coilId, product, _) = vendItem
                    Log.d(TAG, "Free vending item ${index + 1}/${vendList.size}: ${product.name}")

                    val vendSuccess = motorManager.vendMotor(coilId)

                    if (vendSuccess) {
                        vendedCount++
                        inventoryRepo.decrementInventory(coilId)

                        val transaction = Transaction.createWithPayment(
                            coilId = coilId,
                            status = Transaction.STATUS_SUCCESS,
                            amount = 0.0,
                            paymentMethod = Transaction.PAYMENT_METHOD_FREE,
                            paymentStatus = Transaction.PAYMENT_STATUS_APPROVED,
                            nayaxTransactionId = null,
                            productId = product.id
                        )
                        inventoryRepo.saveTransaction(transaction)
                    } else {
                        allVendsSuccessful = false
                        break
                    }
                }

                if (allVendsSuccessful) {
                    Toast.makeText(
                        this@CartActivity,
                        "Vend successful! $vendedCount items dispensed (Free mode)",
                        Toast.LENGTH_SHORT
                    ).show()
                    cartManager.clear()
                } else {
                    Toast.makeText(
                        this@CartActivity,
                        "Vend failed after $vendedCount items (motor jam)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Free checkout error", e)
                Toast.makeText(this@CartActivity, "Checkout error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnCheckout.isEnabled = true
                btnClearCart.isEnabled = true
                val totalAmount = cartManager.totalAmount.value
                btnCheckout.text = "CHECKOUT - ${currencyFormatter.format(totalAmount)}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind from HardwareService
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
