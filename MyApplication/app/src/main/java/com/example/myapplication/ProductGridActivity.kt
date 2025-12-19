package com.example.myapplication

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.sin

class ProductGridActivity : AppCompatActivity() {

    private lateinit var productRecycler: RecyclerView
    private lateinit var bgVideoView1: FullScreenVideoView
    private lateinit var bgVideoView2: FullScreenVideoView

    private val fadeDuration = 1500L // 1.5s cross-fade
    private var activePlayer = 1 // 1 or 2
    private val loopHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var loopRunnable: Runnable? = null
    private val playbackSpeed = 1.0f // Normal playback speed

    // Idle screensaver
    private val idleTimeout = 30000L // 30 seconds
    private val idleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var idleRunnable: Runnable? = null

    // Category selection
    private var categoryId: String = ""
    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_grid)

        // Get category from intent
        categoryId = intent.getStringExtra("category_id") ?: ""
        categoryName = intent.getStringExtra("category_name") ?: ""

        // Enable edge-to-edge and hide system bars (must be after setContentView)
        setupFullScreen()

        // Initialize Views
        productRecycler = findViewById(R.id.product_recycler)
        bgVideoView1 = findViewById(R.id.bg_video_view_1)
        bgVideoView2 = findViewById(R.id.bg_video_view_2)

        // Setup back button
        val backButton = findViewById<MaterialButton>(R.id.backButton)
        backButton.bringToFront() // Ensure button is on top of all views
        backButton.setOnClickListener {
            finish() // Return to CategorySelectionActivity
        }

        // Video background disabled - white background used instead
        // setupDualVideoBackground()

        // Setup ZootBox Product Grid (2 Columns)
        productRecycler.layoutManager = GridLayoutManager(this, 2)

        // Performance optimizations
        productRecycler.setHasFixedSize(true)
        productRecycler.setItemViewCacheSize(20)

        // Simple placeholder products (same for all categories)
        // In a real app, filter based on categoryName
        val allProducts = listOf(
            Product("01", "ZOOT VAPE X", "1", "1", R.drawable.zyn, price = 29.99, ageRestriction = 21, videoFileName = "zoot_vape_x.mp4", category = "VAPES"),
            Product("02", "NIGHT OWL CAM", "1", "2", R.drawable.zyn, price = 15.99, videoFileName = "night_owl_cam.mp4", category = "ZOOTBOX LEGENDARY LOOT"),
            Product("03", "ZYN CITRUS", "1", "3", R.drawable.img_zyn_citrus, backgroundRes = R.drawable.bg_zyn_citrus_gradient, price = 8.99, ageRestriction = 21, scaleX = 0.95f, scaleY = 0.95f, videoFileName = "zyn_citrus.mp4", category = "ZyNS"),
            Product("04", "RED BULL 12OZ", "1", "4", R.drawable.zyn, price = 4.99, videoFileName = "red_bull.mp4", category = "ZOOTBOX LEGENDARY LOOT"),
            Product("05", "LIGHTER GOLD", "1", "5", R.drawable.zyn, price = 2.99, ageRestriction = 18, videoFileName = "lighter_gold.mp4", category = "CIGERATES"),
            Product("06", "ROLLING PAPERS", "1", "6", R.drawable.zyn, price = 3.99, ageRestriction = 18, videoFileName = "rolling_papers.mp4", category = "CIGERATES"),
            Product("07", "ENERGY SHOT", "1", "7", R.drawable.zyn, price = 3.49, videoFileName = "energy_shot.mp4", category = "ZOOTBOX LEGENDARY LOOT"),
            Product("08", "GUM MINT", "1", "8", R.drawable.zyn, price = 1.99, videoFileName = "gum_mint.mp4", category = "ZyNS"),
            Product("09", "CONDOM PACK", "1", "9", R.drawable.zyn, price = 5.99, videoFileName = "condom_pack.mp4", category = "ZOOTBOX LEGENDARY LOOT"),
            Product("10", "WATER 500ML", "1", "10", R.drawable.zyn, price = 2.49, videoFileName = "water.mp4", category = "ZOOTBOX LEGENDARY LOOT"),
            Product("11", "Donate to the Autism Fund", "0", "0", R.drawable.img_donate, isDigital = true, price = 5.00, videoFileName = "donate.mp4", category = "ZOOTBOX LEGENDARY LOOT")
        )
        
        val filteredProducts = if (categoryName.isNotEmpty()) {
            allProducts.filter { it.category == categoryName }
        } else {
            allProducts
        }

        val adapter = ProductAdapter(filteredProducts) { product ->
            // Launch Product Detail Activity
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("name", product.name)
            intent.putExtra("row", product.row)
            intent.putExtra("col", product.col)
            intent.putExtra("imageRes", product.imageRes)
            intent.putExtra("backgroundRes", product.backgroundRes)
            intent.putExtra("isDigital", product.isDigital)
            intent.putExtra("price", product.price)
            intent.putExtra("ageRestriction", product.ageRestriction ?: -1)
            intent.putExtra("desc", if(product.isDigital) "Support the cause. All proceeds go directly to the Autism Fund." else "Premium nightlife selection. High quality.")
            intent.putExtra("videoFileName", product.videoFileName)
            startActivity(intent)
        }
        productRecycler.adapter = adapter
    }

    private var videoDuration = 0

    private fun setupDualVideoBackground() {
        // Ensure video views are visible
        bgVideoView1.visibility = View.VISIBLE
        bgVideoView2.visibility = View.VISIBLE
        
        val videoPath = "android.resource://" + packageName + "/" + R.raw.download
        val uri = android.net.Uri.parse(videoPath)

        android.util.Log.d("ProductGridActivity", "Video URI: $videoPath")
        android.util.Log.d("ProductGridActivity", "Resource ID: ${R.raw.download}")

        // Initialize Player 1
        bgVideoView1.setVideoURI(uri)
        bgVideoView1.setOnPreparedListener { mp ->
            android.util.Log.d("ProductGridActivity", "Video 1 prepared, duration: ${mp.duration}")
            mp.setVolume(0f, 0f)
            // Set playback speed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {
                    android.util.Log.e("ProductGridActivity", "Error setting playback speed", e)
                }
            }

            videoDuration = mp.duration
            bgVideoView1.start()
            android.util.Log.d("ProductGridActivity", "Video 1 started")
            scheduleNextLoop(videoDuration)
        }
        
        bgVideoView1.setOnErrorListener { mp, what, extra ->
            android.util.Log.e("ProductGridActivity", "Video 1 error: what=$what, extra=$extra")
            false
        }

        // Initialize Player 2 (don't start yet)
        bgVideoView2.setVideoURI(uri)
        bgVideoView2.setOnPreparedListener { mp ->
            android.util.Log.d("ProductGridActivity", "Video 2 prepared")
            mp.setVolume(0f, 0f)
            // Set playback speed for P2 as well
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {
                    android.util.Log.e("ProductGridActivity", "Error setting playback speed for P2", e)
                }
            }
        }

        bgVideoView2.setOnErrorListener { mp, what, extra ->
            android.util.Log.e("ProductGridActivity", "Video 2 error: what=$what, extra=$extra")
            false
        }
        // Ensure P2 is ready (it will auto-prepare on setVideoURI)
    }

    private fun scheduleNextLoop(durationMs: Int) {
        if (durationMs <= 0) return

        // Schedule cross-fade before end (playing at normal 1.0x speed now)
        val triggerTime = (durationMs - fadeDuration).coerceAtLeast(0).toLong()

        loopRunnable = Runnable {
            performCrossFade()
        }
        loopHandler.postDelayed(loopRunnable!!, triggerTime)
    }

    private fun performCrossFade() {
        val outgoing = if (activePlayer == 1) bgVideoView1 else bgVideoView2
        val incoming = if (activePlayer == 1) bgVideoView2 else bgVideoView1

        // Start incoming player from beginning
        incoming.seekTo(0)
        incoming.start()

        // Cross-fade animations
        incoming.animate().alpha(1f).setDuration(fadeDuration).start()
        outgoing.animate().alpha(0f).setDuration(fadeDuration).withEndAction {
            // Reset outgoing player for next cycle
            outgoing.pause()
            outgoing.seekTo(0)
        }.start()

        // Swap active player
        activePlayer = if (activePlayer == 1) 2 else 1

        // Schedule next loop
        if (videoDuration > 0) {
            scheduleNextLoop(videoDuration)
        }
    }

    override fun onResume() {
        super.onResume()
        // Video background disabled
        // if (activePlayer == 1) bgVideoView1.start() else bgVideoView2.start()
        resetIdleTimer()
    }

    override fun onPause() {
        super.onPause()
        loopRunnable?.let { loopHandler.removeCallbacks(it) }
        idleRunnable?.let { idleHandler.removeCallbacks(it) }
        // Video background disabled
        // bgVideoView1.pause()
        // bgVideoView2.pause()
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        resetIdleTimer()
        return super.dispatchTouchEvent(event)
    }

    private fun resetIdleTimer() {
        idleRunnable?.let { idleHandler.removeCallbacks(it) }
        idleRunnable = Runnable { launchScreensaver() }
        idleHandler.postDelayed(idleRunnable!!, idleTimeout)
    }

    private fun launchScreensaver() {
        val intent = Intent(this, ScreensaverActivity::class.java)
        startActivity(intent)
    }

    private fun setupFullScreen() {
        // Make the app edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide the navigation bar and status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullScreen()
        }
    }
}