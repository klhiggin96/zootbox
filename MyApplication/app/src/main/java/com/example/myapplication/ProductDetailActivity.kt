package com.example.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.cart.CartManager
import com.example.myapplication.database.InventoryRepository
import com.example.myapplication.database.models.Coil
import com.example.myapplication.database.models.Product
import com.example.myapplication.database.models.Transaction
import com.example.myapplication.hardware.HardwareService
import com.example.myapplication.hardware.MotorControlManager
import com.example.myapplication.hardware.NayaxPaymentManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var imageView: ImageView
    private var videoFileName: String? = null

    // Product data
    private var basePrice: Double = 0.0
    private var previousPrice: Double = 0.0
    private var quantity: Int = 1
    private var ageRestriction: Int = -1
    private var productName: String = ""

    // UI References
    private lateinit var textQuantity: TextView
    private lateinit var textPrice: TextView
    private lateinit var textPreviousPrice: TextView
    private lateinit var btnQuantityMinus: ImageButton
    private lateinit var btnQuantityPlus: ImageButton
    private lateinit var btnAddToCart: Button
    private lateinit var outOfStockBanner: LinearLayout

    // Inventory
    private lateinit var inventoryRepo: InventoryRepository
    private lateinit var cartManager: CartManager
    private var assignedCoil: Coil? = null

    // Hardware Service
    private var hardwareService: HardwareService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? HardwareService.LocalBinder
            hardwareService = binder?.getService()
            serviceBound = true
            Log.d("ProductDetailActivity", "HardwareService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hardwareService = null
            serviceBound = false
            Log.d("ProductDetailActivity", "HardwareService disconnected")
        }
    }

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    private lateinit var idScanLauncher: ActivityResultLauncher<Intent>
    private var isAddToCartFlow = false  // Track whether we're in "Add to Cart" or "Buy Now" flow

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        // Path: /storage/emulated/0/Movies/ZootBox/
        private val VIDEO_BASE_PATH = Environment.getExternalStorageDirectory().absolutePath + "/Movies/ZootBox/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Initialize inventory repository and cart manager
        inventoryRepo = InventoryRepository.getInstance(this)
        cartManager = CartApplication.getInstance().cartManager

        // Get Intent data
        productName = intent.getStringExtra("name") ?: ""
        basePrice = intent.getDoubleExtra("price", 0.0)
        previousPrice = intent.getDoubleExtra("previousPrice", 0.0)
        val imageRes = intent.getIntExtra("imageRes", 0)
        val backgroundRes = intent.getIntExtra("backgroundRes", R.drawable.bg_card_gradient)
        val description = intent.getStringExtra("desc") ?: ""
        val flavor = intent.getStringExtra("flavor") ?: "SPEARMINT"
        val nicotineStrength = intent.getStringExtra("nicotineStrength") ?: "6mg"
        val rating = intent.getDoubleExtra("rating", 4.8)
        val reviewCount = intent.getIntExtra("reviewCount", 2847)
        val pouchesPerCan = intent.getStringExtra("pouchesPerCan") ?: "15 pouches"
        val flavorProfile = intent.getStringExtra("flavorProfile") ?: "Spearmint"
        val format = intent.getStringExtra("format") ?: "Slim"
        videoFileName = intent.getStringExtra("videoFileName")
        ageRestriction = intent.getIntExtra("ageRestriction", -1)

        // Initialize views
        initializeViews()

        // Determine which coil this product uses and check inventory
        assignedCoil = determineCoilForProduct(productName)
        checkInventoryAndUpdateUI()

        // Register Activity Result Launcher for ID Scan
        idScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Verification Successful!", Toast.LENGTH_SHORT).show()
                if (isAddToCartFlow) {
                    // Navigate to cart after successful ID verification
                    CartActivity.start(this)
                    finish()
                } else {
                    // Proceed to checkout for "Buy Now" flow
                    processCheckout()
                }
            } else {
                Toast.makeText(this, "Verification Failed or Cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup product information
        setupProductInfo(productName, basePrice, previousPrice, description)
        setupFlavorInfo(flavor, nicotineStrength)
        setupRating(rating, reviewCount)
        setupSpecifications(nicotineStrength, pouchesPerCan, flavorProfile, format)

        // Setup quantity controls
        setupQuantityControls()

        // Initialize button text with initial price
        updateQuantityDisplay()

        // Setup image/video
        setupMedia(imageRes)

        // Apply gradient background
        applyGradientBackground(backgroundRes)

        if (videoFileName != null) {
            checkAndRequestPermissions()
        }

        // Bind to HardwareService
        bindToHardwareService()
    }

    private fun bindToHardwareService() {
        val intent = Intent(this, HardwareService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun initializeViews() {
        imageView = findViewById(R.id.detail_image)
        videoView = findViewById(R.id.detail_video)
        textQuantity = findViewById(R.id.text_quantity)
        textPrice = findViewById(R.id.detail_price)
        textPreviousPrice = findViewById(R.id.detail_previous_price)
        btnQuantityMinus = findViewById(R.id.btn_quantity_minus)
        btnQuantityPlus = findViewById(R.id.btn_quantity_plus)
        btnAddToCart = findViewById(R.id.btn_add_to_cart)

        // Initialize out-of-stock banner (will be created programmatically if needed)
        outOfStockBanner = findViewById(R.id.out_of_stock_banner)

        // Setup back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
    
    private fun setupProductInfo(name: String, price: Double, prevPrice: Double, description: String) {
        findViewById<TextView>(R.id.detail_name).text = name.uppercase()
        textPrice.text = currencyFormatter.format(price)
        
        // Show previous price only if it exists and is different from current price
        if (prevPrice > 0.0 && prevPrice > price) {
            textPreviousPrice.text = currencyFormatter.format(prevPrice)
            textPreviousPrice.paintFlags = textPreviousPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            textPreviousPrice.visibility = View.VISIBLE
        } else {
            textPreviousPrice.visibility = View.GONE
        }

        // Set category if available
        val category = intent.getStringExtra("category") ?: "PREMIUM NICOTINE POUCHES"
        findViewById<TextView>(R.id.text_category).text = category
        
        // Set product snippet if available
        val snippet = intent.getStringExtra("snippet") ?: "Premium nicotine pouches designed for a smooth, satisfying experience. Tobacco-free and discreet, perfect for any occasion."
        findViewById<TextView>(R.id.text_product_snippet).text = snippet
    }
    
    private fun setupFlavorInfo(flavor: String, nicotineStrength: String) {
        findViewById<TextView>(R.id.text_flavor).text = flavor.uppercase()
        findViewById<TextView>(R.id.text_nicotine_strength).text = "$nicotineStrength Nicotine Strength"
    }
    
    private fun setupRating(rating: Double, reviewCount: Int) {
        findViewById<TextView>(R.id.text_rating).text = "${String.format(Locale.US, "%.1f", rating)}/5.0"
        findViewById<TextView>(R.id.text_review_count).text = "(${formatReviewCount(reviewCount)} reviews)"
        
        // Setup star display (4 filled, 1 outline based on rating)
        // This is a simplified version - you could make it more dynamic
        val filledStars = rating.toInt()
        // Stars are already set in XML, but we could update them dynamically if needed
    }
    
    private fun setupSpecifications(nicotineStrength: String, pouchesPerCan: String, flavorProfile: String, format: String) {
        findViewById<TextView>(R.id.spec_nicotine_value).text = nicotineStrength
        findViewById<TextView>(R.id.spec_pouches_value).text = pouchesPerCan
        findViewById<TextView>(R.id.spec_flavor_profile_value).text = flavorProfile
        findViewById<TextView>(R.id.spec_format_value).text = format
    }
    
    private fun setupQuantityControls() {
        updateQuantityDisplay()
        
        btnQuantityMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityDisplay()
            }
        }
        
        btnQuantityPlus.setOnClickListener {
            quantity++
            updateQuantityDisplay()
        }
        
        val addToCartBtn = findViewById<Button>(R.id.btn_add_to_cart)
        addToCartBtn.setOnClickListener {
            handleAddToCart()
        }

        // Optional: Add long-click for "Buy Now" immediate checkout
        addToCartBtn.setOnLongClickListener {
            handleBuyNow()
            true
        }
    }
    
    private fun updateQuantityDisplay() {
        textQuantity.text = quantity.toString()
        val totalPrice = basePrice * quantity
        // Update Add to Cart button text with price
        btnAddToCart.text = "ADD TO CART - ${currencyFormatter.format(totalPrice)}"
    }
    
    private fun setupMedia(imageRes: Int) {
        if (imageRes != 0) {
            imageView.setImageResource(imageRes)
        }
    }

    private fun applyGradientBackground(backgroundRes: Int) {
        // Extract colors from the original gradient
        val (startColor, centerColor, endColor) = extractGradientColors(backgroundRes)

        // Create a new gradient optimized for full screen
        val scaledGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, centerColor, endColor)
        )

        // Remove corner radius for full-screen application
        scaledGradient.cornerRadius = 0f

        // Apply to NestedScrollView
        val nestedScrollView = findViewById<View>(R.id.nested_scroll_view)
        nestedScrollView?.background = scaledGradient
    }

    private fun extractGradientColors(backgroundRes: Int): Triple<Int, Int, Int> {
        return when (backgroundRes) {
            R.drawable.bg_card_gradient -> {
                Triple(
                    Color.parseColor("#14A566"),  // green
                    Color.parseColor("#14A566"),  // green
                    Color.parseColor("#F0E0F5")   // pink
                )
            }
            R.drawable.bg_zyn_citrus_gradient -> {
                Triple(
                    Color.parseColor("#EAD93B"),  // yellow
                    Color.parseColor("#EAD93B"),  // yellow
                    Color.parseColor("#F0E0F5")   // pink
                )
            }
            else -> {
                // Default gradient (green to pink)
                Triple(
                    Color.parseColor("#14A566"),
                    Color.parseColor("#14A566"),
                    Color.parseColor("#F0E0F5")
                )
            }
        }
    }

    private fun formatReviewCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format(Locale.US, "%.1fM", count / 1000000.0)
            count >= 1000 -> String.format(Locale.US, "%.1fK", count / 1000.0)
            else -> count.toString()
        }.replace(".0", "")
    }

    private fun checkAndRequestPermissions() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        } else {
            playVideo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                playVideo()
            }
        }
    }

    private fun playVideo() {
        val fileName = videoFileName ?: return
        val videoFile = File(VIDEO_BASE_PATH, fileName)

        if (videoFile.exists()) {
            // Hide Image, Show Video
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE

            videoView.setVideoURI(Uri.fromFile(videoFile))
            videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                videoView.start()
            }
            videoView.setOnErrorListener { _, _, _ ->
                // Fallback to image on error
                videoView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                true
            }
        } else {
            // File doesn't exist, keep showing image
            // Optional: Log this or show a toast for debugging
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::videoView.isInitialized && videoView.visibility == View.VISIBLE && !videoView.isPlaying) {
            videoView.start()
        }
        // Refresh inventory when returning to screen
        checkInventoryAndUpdateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind from HardwareService
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    /**
     * Determine which coil this product uses
     * TODO: Replace with actual product-to-coil mapping from database
     */
    private fun determineCoilForProduct(productName: String): Coil? {
        // Simplified mapping: Hash product name to coil (A1-J1)
        val coilIds = listOf("A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1", "I1", "J1")
        val index = (productName.hashCode() and 0x7FFFFFFF) % coilIds.size
        val coilId = coilIds[index]

        Log.d("ProductDetailActivity", "Product '$productName' mapped to coil $coilId")
        return inventoryRepo.getCoil(coilId)
    }

    /**
     * Check inventory and update UI accordingly
     */
    private fun checkInventoryAndUpdateUI() {
        val coil = assignedCoil ?: return

        // Refresh coil data
        assignedCoil = inventoryRepo.getCoil(coil.id)
        val refreshedCoil = assignedCoil ?: return

        // Show/hide out-of-stock banner
        if (refreshedCoil.inventory == 0 || refreshedCoil.isJammed()) {
            outOfStockBanner?.visibility = View.VISIBLE
            btnAddToCart.isEnabled = false
            btnAddToCart.alpha = 0.5f

            val message = if (refreshedCoil.isJammed()) {
                "⚠ This item is temporarily unavailable"
            } else {
                "⚠ This item is currently out of stock"
            }
            outOfStockBanner?.findViewById<TextView>(R.id.out_of_stock_text)?.text = message
        } else {
            outOfStockBanner?.visibility = View.GONE
            btnAddToCart.isEnabled = true
            btnAddToCart.alpha = 1.0f
        }

        // Show low stock warning
        if (refreshedCoil.isLowStock()) {
            Toast.makeText(this, "Only ${refreshedCoil.inventory} units remaining", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle "Add to Cart" button click
     *
     * Adds product to cart and shows options to continue shopping or view cart
     */
    private fun handleAddToCart() {
        // Check inventory before proceeding
        val coil = assignedCoil
        if (coil == null) {
            Toast.makeText(this, "Product not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (coil.inventory == 0) {
            Toast.makeText(this, "This item is currently out of stock", Toast.LENGTH_SHORT).show()
            return
        }

        if (coil.isJammed()) {
            Toast.makeText(this, "This item is temporarily unavailable (jammed)", Toast.LENGTH_SHORT).show()
            return
        }

        if (coil.inventory < quantity) {
            Toast.makeText(this, "Only ${coil.inventory} units available", Toast.LENGTH_SHORT).show()
            return
        }

        // Create product from current data
        val product = Product(
            id = "PRD_TEMP_${productName.hashCode()}",  // TODO: Use real product ID from database
            name = productName,
            category = intent.getStringExtra("category") ?: "Unknown",
            price = basePrice,
            ageRestriction = ageRestriction,
            imageUrl = null,
            videoFilename = videoFileName,
            isDigital = false,
            active = true
        )

        // Add to cart
        val result = cartManager.addItem(product, quantity)

        if (result.isSuccess) {
            Toast.makeText(
                this,
                "Added ${quantity}x ${productName} to cart!",
                Toast.LENGTH_SHORT
            ).show()

            // Navigate to ID scan if age-restricted, otherwise go to cart
            if (ageRestriction > 0) {
                // Launch ID verification before going to cart
                isAddToCartFlow = true
                val intent = Intent(this, IdScanActivity::class.java)
                intent.putExtra("requiredAge", ageRestriction)
                idScanLauncher.launch(intent)
            } else {
                // No age restriction - go directly to cart
                CartActivity.start(this)
                finish()
            }
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Failed to add to cart"
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handle "Buy Now" button (long-click)
     *
     * Immediate checkout without adding to cart
     */
    private fun handleBuyNow() {
        // Check inventory before proceeding
        val coil = assignedCoil
        if (coil == null) {
            Toast.makeText(this, "Product not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (coil.inventory == 0) {
            Toast.makeText(this, "This item is currently out of stock", Toast.LENGTH_SHORT).show()
            return
        }

        if (coil.isJammed()) {
            Toast.makeText(this, "This item is temporarily unavailable (jammed)", Toast.LENGTH_SHORT).show()
            return
        }

        if (coil.inventory < quantity) {
            Toast.makeText(this, "Only ${coil.inventory} units available", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ProductDetailActivity", "Buy Now Clicked. Age Restriction: $ageRestriction")

        // FORCE AGE CHECK FOR DEMO
        val checkAge = if (ageRestriction > 0) ageRestriction else 21

        if (checkAge > 0) {
            Log.d("ProductDetailActivity", "Launching ID Scan")
            // Launch ID Verification (Buy Now flow - goes to checkout after verification)
            isAddToCartFlow = false
            val intent = Intent(this, IdScanActivity::class.java)
            intent.putExtra("requiredAge", checkAge)
            idScanLauncher.launch(intent)
        } else {
            Log.d("ProductDetailActivity", "Proceeding to Checkout (No Restriction)")
            processCheckout()
        }
    }

    /**
     * Process checkout after age verification (if required)
     *
     * Payment Flow:
     * 1. Initiate payment with Nayax VPOS Touch
     * 2. Wait for card tap (max 120 seconds)
     * 3. If approved:
     *    - Trigger motor vend for each quantity
     *    - Log transactions with payment data
     *    - Confirm vend to Nayax (or trigger refund if jam)
     * 4. If declined/cancelled: Show error message
     */
    private fun processCheckout() {
        val coil = assignedCoil ?: return

        val totalPrice = basePrice * quantity
        Log.d("ProductDetailActivity", "Processing checkout: $quantity units from coil ${coil.id}, total: ${currencyFormatter.format(totalPrice)}")

        // Get hardware managers
        val paymentManager = hardwareService?.getNayaxPaymentManager()
        val motorManager = hardwareService?.getMotorControlManager()

        // Check if payment hardware is available
        if (paymentManager == null) {
            Log.w("ProductDetailActivity", "Payment manager not available - using free vend mode")
            processFreeVend(coil)
            return
        }

        // Check if motor control is available
        if (motorManager == null) {
            Log.e("ProductDetailActivity", "Motor control manager not available")
            Toast.makeText(this, "Vending system unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button during payment
        btnAddToCart.isEnabled = false
        btnAddToCart.text = "PROCESSING PAYMENT..."

        // Launch payment flow in coroutine
        lifecycleScope.launch {
            try {
                Log.i("ProductDetailActivity", "Initiating payment: ${currencyFormatter.format(totalPrice)}")
                Toast.makeText(
                    this@ProductDetailActivity,
                    "Present Card - ${currencyFormatter.format(totalPrice)}",
                    Toast.LENGTH_LONG
                ).show()

                // Initiate payment (blocks until card tap or timeout)
                val paymentApproved = paymentManager.initiatePayment(totalPrice, itemNumber = quantity)

                if (paymentApproved) {
                    Log.i("ProductDetailActivity", "Payment APPROVED")

                    // Get Nayax transaction ID from payment result
                    val paymentResult = paymentManager.paymentResult.value
                    val nayaxTransactionId = paymentResult?.transactionId

                    // Process vend for each quantity
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Payment approved! Dispensing product...",
                        Toast.LENGTH_SHORT
                    ).show()

                    var allVendsSuccessful = true
                    for (index in 0 until quantity) {
                        Log.d("ProductDetailActivity", "Vending item ${index + 1}/$quantity from coil ${coil.id}")

                        val vendSuccess = motorManager.vendMotor(coil.id)

                        if (vendSuccess) {
                            // Decrement inventory
                            inventoryRepo.decrementInventory(coil.id)

                            // Log transaction with payment data
                            val transaction = Transaction.createWithPayment(
                                coilId = coil.id,
                                status = Transaction.STATUS_SUCCESS,
                                amount = basePrice,
                                paymentMethod = Transaction.PAYMENT_METHOD_CARD,
                                paymentStatus = Transaction.PAYMENT_STATUS_APPROVED,
                                nayaxTransactionId = nayaxTransactionId,
                                productId = null  // TODO: Get from product-coil mapping
                            )
                            inventoryRepo.saveTransaction(transaction)
                            Log.d("ProductDetailActivity", "Saved transaction: ${transaction.id}")
                        } else {
                            // Vend failed (motor jam)
                            Log.e("ProductDetailActivity", "Motor vend FAILED for coil ${coil.id}")
                            allVendsSuccessful = false

                            // Log JAM transaction
                            val jamTransaction = Transaction.createWithPayment(
                                coilId = coil.id,
                                status = Transaction.STATUS_JAM,
                                amount = basePrice,
                                paymentMethod = Transaction.PAYMENT_METHOD_CARD,
                                paymentStatus = Transaction.PAYMENT_STATUS_REFUNDED,
                                nayaxTransactionId = nayaxTransactionId,
                                productId = null
                            )
                            inventoryRepo.saveTransaction(jamTransaction)

                            // Create jam event for backend alerting
                            // inventoryRepo.createJamEvent(coil.id)

                            break  // Stop vending on first failure
                        }
                    }

                    if (allVendsSuccessful) {
                        // Confirm successful vend to Nayax (finalizes transaction)
                        paymentManager.confirmVend(success = true)

                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Purchase successful! Thank you!",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.i("ProductDetailActivity", "Checkout completed successfully")
                    } else {
                        // Partial failure - trigger refund
                        paymentManager.confirmVend(success = false)

                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Vend failed. Payment will be refunded.",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.w("ProductDetailActivity", "Checkout failed - payment refunded")
                    }
                } else {
                    // Payment declined or cancelled
                    Log.w("ProductDetailActivity", "Payment DECLINED or CANCELLED")
                    val paymentResult = paymentManager.paymentResult.value
                    val errorMessage = paymentResult?.error ?: "Payment declined"

                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Payment failed: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ProductDetailActivity", "Checkout error", e)
                Toast.makeText(
                    this@ProductDetailActivity,
                    "Checkout error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Re-enable button
                btnAddToCart.isEnabled = true
                updateQuantityDisplay()

                // Refresh inventory
                checkInventoryAndUpdateUI()
            }
        }
    }

    /**
     * Process free vend (no payment required)
     * Used when payment hardware is unavailable or for testing
     */
    private fun processFreeVend(coil: Coil) {
        val totalPrice = basePrice * quantity
        Log.d("ProductDetailActivity", "Processing FREE vend: $quantity units from coil ${coil.id}")

        val motorManager = hardwareService?.getMotorControlManager()
        if (motorManager == null) {
            Log.e("ProductDetailActivity", "Motor control manager not available")
            Toast.makeText(this, "Vending system unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        btnAddToCart.isEnabled = false
        btnAddToCart.text = "DISPENSING..."

        lifecycleScope.launch {
            try {
                var allVendsSuccessful = true
                for (index in 0 until quantity) {
                    Log.d("ProductDetailActivity", "Vending item ${index + 1}/$quantity from coil ${coil.id}")

                    val vendSuccess = motorManager.vendMotor(coil.id)

                    if (vendSuccess) {
                        inventoryRepo.decrementInventory(coil.id)

                        val transaction = Transaction.createWithPayment(
                            coilId = coil.id,
                            status = Transaction.STATUS_SUCCESS,
                            amount = 0.0,  // Free vend
                            paymentMethod = Transaction.PAYMENT_METHOD_FREE,
                            paymentStatus = Transaction.PAYMENT_STATUS_APPROVED,
                            nayaxTransactionId = null,
                            productId = null
                        )
                        inventoryRepo.saveTransaction(transaction)
                    } else {
                        allVendsSuccessful = false
                        val jamTransaction = Transaction.createWithPayment(
                            coilId = coil.id,
                            status = Transaction.STATUS_JAM,
                            amount = 0.0,
                            paymentMethod = Transaction.PAYMENT_METHOD_FREE,
                            paymentStatus = Transaction.PAYMENT_STATUS_APPROVED,
                            nayaxTransactionId = null,
                            productId = null
                        )
                        inventoryRepo.saveTransaction(jamTransaction)
                        break
                    }
                }

                if (allVendsSuccessful) {
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Vend successful! (Free mode)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Vend failed (motor jam)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ProductDetailActivity", "Free vend error", e)
                Toast.makeText(this@ProductDetailActivity, "Vend error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnAddToCart.isEnabled = true
                updateQuantityDisplay()
                checkInventoryAndUpdateUI()
            }
        }
    }
}