package com.example.myapplication

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var section1: ConstraintLayout
    private lateinit var section2: ConstraintLayout
    private lateinit var section3: ConstraintLayout
    private lateinit var section4: ConstraintLayout
    private lateinit var darkModeToggle: FrameLayout
    private lateinit var watermark1: TextView
    private lateinit var watermark2: TextView
    private lateinit var watermark3: TextView
    private lateinit var watermark4: TextView
    private lateinit var orbIcon: ImageView
    private lateinit var devLabel1: TextView
    private lateinit var devLabel2: TextView
    private lateinit var devLabel3: TextView
    private lateinit var devLabel4: TextView

    private val colorSchemes = ColorSchemes.getAll()
    private var currentSchemeIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable edge-to-edge and hide system bars (must be after setContentView)
        setupFullScreen()

        // Initialize Views
        rootLayout = findViewById(R.id.root_layout)
        section1 = findViewById(R.id.section1)
        section2 = findViewById(R.id.section2)
        section3 = findViewById(R.id.section3)
        section4 = findViewById(R.id.section4)
        darkModeToggle = findViewById(R.id.darkModeToggle)
        watermark1 = findViewById(R.id.watermark1)
        watermark2 = findViewById(R.id.watermark2)
        watermark3 = findViewById(R.id.watermark3)
        watermark4 = findViewById(R.id.watermark4)
        orbIcon = findViewById(R.id.orbIcon)
        devLabel1 = findViewById(R.id.devLabel1)
        devLabel2 = findViewById(R.id.devLabel2)
        devLabel3 = findViewById(R.id.devLabel3)
        devLabel4 = findViewById(R.id.devLabel4)

        // Apply initial color scheme
        applyColorScheme(colorSchemes[currentSchemeIndex])

        // Setup Color Scheme Toggle
        darkModeToggle.setOnClickListener {
            cycleColorScheme()
        }
        
        // Setup Navigation Buttons
        findViewById<Button>(R.id.btn_s1).setOnClickListener { openCategory("ZyNS") }
        findViewById<Button>(R.id.btn_s2).setOnClickListener { openCategory("VAPES") }
        findViewById<Button>(R.id.btn_s3).setOnClickListener { openCategory("CIGERATES") }
        findViewById<Button>(R.id.btn_s4).setOnClickListener { openCategory("ZOOTBOX LEGENDARY LOOT") }

        // Start hardware service
        try {
            val serviceIntent = Intent(this, com.example.myapplication.hardware.HardwareService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                @Suppress("DEPRECATION")
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(this, "USB Connection Active", Toast.LENGTH_LONG).show()
        
        // Setup simple animations for entrance
        setupEntranceAnimations()
    }
    
    private fun openCategory(categoryName: String) {
        val intent = Intent(this, ProductGridActivity::class.java)
        intent.putExtra("category_name", categoryName)
        startActivity(intent)
    }
    
    private fun cycleColorScheme() {
        currentSchemeIndex = (currentSchemeIndex + 1) % colorSchemes.size
        applyColorScheme(colorSchemes[currentSchemeIndex])
    }

    private fun applyColorScheme(scheme: ColorScheme) {
        // Update section backgrounds
        section1.backgroundTintList = ColorStateList.valueOf(scheme.containerColors[0])
        section2.backgroundTintList = ColorStateList.valueOf(scheme.containerColors[1])
        section3.backgroundTintList = ColorStateList.valueOf(scheme.containerColors[2])
        section4.backgroundTintList = ColorStateList.valueOf(scheme.containerColors[3])

        // Apply ghost text colors with 25% opacity to all schemes
        val ghostOpacity = (0.25f * 255).toInt()  // 25% opacity
        watermark1.setTextColor(ColorUtils.setAlphaComponent(scheme.ghostColors[0], ghostOpacity))
        watermark2.setTextColor(ColorUtils.setAlphaComponent(scheme.ghostColors[1], ghostOpacity))
        watermark3.setTextColor(ColorUtils.setAlphaComponent(scheme.ghostColors[2], ghostOpacity))
        watermark4.setTextColor(ColorUtils.setAlphaComponent(scheme.ghostColors[3], ghostOpacity))

        // Update dev labels with unique numbers for each color
        val baseNumber = currentSchemeIndex * 4
        devLabel1.text = (baseNumber + 1).toString()
        devLabel2.text = (baseNumber + 2).toString()
        devLabel3.text = (baseNumber + 3).toString()
        devLabel4.text = (baseNumber + 4).toString()

        // Update orb icon
        orbIcon.setImageResource(scheme.orbDrawable)
    }

    private fun setupEntranceAnimations() {
        // Slide up animation for sections
        val sections = listOf(section4, section3, section2, section1) // Bottom to top
        
        sections.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 100f
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(index * 150L) // Staggered
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
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

data class ColorScheme(
    val name: String,
    val orbDrawable: Int,
    val containerColors: List<Int>,
    val ghostColors: List<Int>
)

object ColorSchemes {
    fun getAll(): List<ColorScheme> = listOf(
        // Scheme 0: Current Default
        ColorScheme(
            name = "Original",
            orbDrawable = R.drawable.orb_sun,
            containerColors = listOf(
                Color.parseColor("#8b5cf6"),  // Purple
                Color.parseColor("#fb7185"),  // Coral
                Color.parseColor("#4ade80"),  // Green
                Color.parseColor("#38bdf8")   // Blue
            ),
            ghostColors = listOf(
                Color.parseColor("#FFFFFF"),  // White (for purple bg)
                Color.parseColor("#000000"),  // Black (for coral bg)
                Color.parseColor("#000000"),  // Black (for green bg)
                Color.parseColor("#FFFFFF")   // White (for blue bg)
            )
        ),

        // Scheme 1: Neon Mint/Navy Pop
        ColorScheme(
            name = "Neon Mint/Navy Pop",
            orbDrawable = R.drawable.orb_tropical,
            containerColors = listOf(
                Color.parseColor("#40FFA7"),  // Mint Green
                Color.parseColor("#050761"),  // Deep Navy
                Color.parseColor("#C1FF1A"),  // Lime Neon
                Color.parseColor("#00FBEA")   // Aqua Cyan
            ),
            ghostColors = listOf(
                Color.parseColor("#050761"),  // Deep Navy
                Color.parseColor("#7DFFD0"),  // Soft Mint
                Color.parseColor("#1F2F1F"),  // Charcoal Green
                Color.parseColor("#0B2E3D")   // Midnight Blue
            )
        ),

        // Scheme 2: Hot Pink/Purple Shock
        ColorScheme(
            name = "Hot Pink/Purple Shock",
            orbDrawable = R.drawable.orb_purple,
            containerColors = listOf(
                Color.parseColor("#000000"),  // Black
                Color.parseColor("#FF2687"),  // Hot Pink
                Color.parseColor("#340043"),  // Dark Plum
                Color.parseColor("#C68DFE")   // Lavender Neon
            ),
            ghostColors = listOf(
                Color.parseColor("#340043"),  // Dark Plum
                Color.parseColor("#5A1C63"),  // Muted Violet
                Color.parseColor("#FF9BE8"),  // Soft Pink
                Color.parseColor("#4B2A6A")   // Deep Grape
            )
        ),

        // Scheme 3: Citrus Pop
        ColorScheme(
            name = "Citrus Pop",
            orbDrawable = R.drawable.orb_sun,
            containerColors = listOf(
                Color.parseColor("#FFD019"),  // Lemon Yellow
                Color.parseColor("#FE9A34"),  // Melon Orange
                Color.parseColor("#FF544C"),  // Watermelon Pink
                Color.parseColor("#E4FE37")   // Zest Lime
            ),
            ghostColors = listOf(
                Color.parseColor("#FE9A34"),  // Warm Orange
                Color.parseColor("#C9472D"),  // Burnt Coral
                Color.parseColor("#6B1F1F"),  // Deep Berry
                Color.parseColor("#2F5A1E")   // Forest Green
            )
        ),

        // Scheme 4: Neo Pastel Contrast
        ColorScheme(
            name = "Neo Pastel Contrast",
            orbDrawable = R.drawable.orb_sunset,
            containerColors = listOf(
                Color.parseColor("#FF97D0"),  // Pastel Magenta
                Color.parseColor("#C3F380"),  // Light Lime
                Color.parseColor("#F1FFBA"),  // Baby Pink
                Color.parseColor("#C68DFE")   // Pastel Violet
            ),
            ghostColors = listOf(
                Color.parseColor("#9C4C6A"),  // Muted Rose
                Color.parseColor("#4F6A2E"),  // Olive Shadow
                Color.parseColor("#8A6A6A"),  // Dusty Mauve
                Color.parseColor("#5C4A7A")   // Slate Purple
            )
        ),

        // Scheme 5: Dark Core Neon
        ColorScheme(
            name = "Dark Core Neon",
            orbDrawable = R.drawable.orb_ocean,
            containerColors = listOf(
                Color.parseColor("#000000"),  // Black
                Color.parseColor("#FF0DCB"),  // Electric Pink
                Color.parseColor("#2AB0A3"),  // Turquoise
                Color.parseColor("#FBFF00")   // Neon Yellow
            ),
            ghostColors = listOf(
                Color.parseColor("#FBFF00"),  // Electric Yellow
                Color.parseColor("#1A1A1A"),  // Deep Charcoal
                Color.parseColor("#0E4F4A"),  // Dark Teal
                Color.parseColor("#111111")   // Ink Black
            )
        ),

        // Scheme 6: Cool Modern Contrast
        ColorScheme(
            name = "Cool Modern Contrast",
            orbDrawable = R.drawable.orb_tropical,
            containerColors = listOf(
                Color.parseColor("#00FBEA"),  // Offbeat Blue
                Color.parseColor("#133464"),  // Navy Blue
                Color.parseColor("#D8FF76"),  // Lime Green
                Color.parseColor("#7039FB")   // Violet
            ),
            ghostColors = listOf(
                Color.parseColor("#1B5F63"),  // Steel Blue
                Color.parseColor("#7FEFFF"),  // Soft Cyan
                Color.parseColor("#3F5F1F"),  // Deep Moss
                Color.parseColor("#C2B3FF")   // Muted Lilac
            )
        ),

        // Scheme 7: Brandboard Bold
        ColorScheme(
            name = "Brandboard Bold",
            orbDrawable = R.drawable.orb_sun,
            containerColors = listOf(
                Color.parseColor("#FBFF00"),  // Lemon Glacier
                Color.parseColor("#FF2F87"),  // Electric Pink
                Color.parseColor("#4AE1D7"),  // Turquoise Blue
                Color.parseColor("#FFFFFF")   // Pure White
            ),
            ghostColors = listOf(
                Color.parseColor("#8A8A8A"),  // Cool Gray
                Color.parseColor("#4A0F2A"),  // Dark Wine
                Color.parseColor("#0E4C4A"),  // Deep Ocean
                Color.parseColor("#000000")   // Jet Black
            )
        ),

        // Scheme 8: Custom Mix (9, 5, 19, 28)
        ColorScheme(
            name = "Custom Mix",
            orbDrawable = R.drawable.orb_purple,
            containerColors = listOf(
                Color.parseColor("#000000"),  // Black (from #9)
                Color.parseColor("#40FFA7"),  // Mint Green (from #5)
                Color.parseColor("#F1FFBA"),  // Baby Pink (from #19)
                Color.parseColor("#7039FB")   // Violet (from #28)
            ),
            ghostColors = listOf(
                Color.parseColor("#340043"),  // Dark Plum
                Color.parseColor("#050761"),  // Deep Navy
                Color.parseColor("#8A6A6A"),  // Dusty Mauve
                Color.parseColor("#C2B3FF")   // Muted Lilac
            )
        )
    )
}
