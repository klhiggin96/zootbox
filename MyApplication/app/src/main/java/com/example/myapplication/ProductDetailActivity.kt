package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    
    // UI References
    private lateinit var textQuantity: TextView
    private lateinit var textPrice: TextView
    private lateinit var textPreviousPrice: TextView
    private lateinit var btnQuantityMinus: Button
    private lateinit var btnQuantityPlus: Button
    private lateinit var btnAddToCart: Button
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    private lateinit var idScanLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        // Path: /storage/emulated/0/Movies/ZootBox/
        private val VIDEO_BASE_PATH = Environment.getExternalStorageDirectory().absolutePath + "/Movies/ZootBox/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Get Intent data
        val name = intent.getStringExtra("name") ?: ""
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
        
        // Register Activity Result Launcher for ID Scan
        idScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Verification Successful! Proceeding to checkout...", Toast.LENGTH_SHORT).show()
                // Proceed with checkout logic (e.g., add to cart, open checkout screen)
                val totalPrice = basePrice * quantity
                Toast.makeText(this, "Added to cart! Total: ${currencyFormatter.format(totalPrice)}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Verification Failed or Cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup product information
        setupProductInfo(name, basePrice, previousPrice, description)
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
        
        findViewById<TextView>(R.id.detail_description).text = description
        
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
            // Toast.makeText(this, "Checking Age: $ageRestriction", Toast.LENGTH_SHORT).show()
            Log.d("ProductDetailActivity", "Buy Now Clicked. Age Restriction: $ageRestriction")
            
            // FORCE AGE CHECK FOR DEMO
            val checkAge = if (ageRestriction > 0) ageRestriction else 21
            
            if (checkAge > 0) {
                Log.d("ProductDetailActivity", "Launching ID Scan")
                // Launch ID Verification
                val intent = Intent(this, IdScanActivity::class.java)
                intent.putExtra("requiredAge", checkAge)
                idScanLauncher.launch(intent)
            } else {
                Log.d("ProductDetailActivity", "Proceeding to Cart (No Restriction)")
                // No age restriction, proceed to cart
                val totalPrice = basePrice * quantity
                Toast.makeText(this, "Added to cart! Total: ${currencyFormatter.format(totalPrice)}", Toast.LENGTH_SHORT).show()
            }
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
    }
}