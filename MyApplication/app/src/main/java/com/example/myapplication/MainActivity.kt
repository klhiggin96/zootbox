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
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var section1: ConstraintLayout
    private lateinit var section2: ConstraintLayout
    private lateinit var section3: ConstraintLayout
    private lateinit var section4: ConstraintLayout
    private lateinit var darkModeToggle: FrameLayout

    private var isDarkMode = false

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

        // Setup Dark Mode Toggle
        darkModeToggle.setOnClickListener {
            toggleDarkMode()
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
    
    private fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        
        // Define colors
        val bgLight = ContextCompat.getColor(this, R.color.grid_bg_light)
        val bgDark = ContextCompat.getColor(this, R.color.grid_bg_dark)
        
        // Section 1: Purple
        val s1Light = ContextCompat.getColor(this, R.color.grid_card_1_light)
        val s1Dark = ContextCompat.getColor(this, R.color.grid_card_1_dark)
        
        // Section 2: Coral (Primary) -> Red 900
        val s2Light = ContextCompat.getColor(this, R.color.grid_primary)
        val s2Dark = Color.parseColor("#7f1d1d") // Red 900
        
        // Section 3: Green
        val s3Light = ContextCompat.getColor(this, R.color.grid_card_3_light)
        val s3Dark = ContextCompat.getColor(this, R.color.grid_card_3_dark)
        
        // Section 4: Blue
        val s4Light = ContextCompat.getColor(this, R.color.grid_card_4_light)
        val s4Dark = ContextCompat.getColor(this, R.color.grid_card_4_dark)

        // Apply
        rootLayout.setBackgroundColor(if (isDarkMode) bgDark else bgLight)
        
        section1.backgroundTintList = ColorStateList.valueOf(if (isDarkMode) s1Dark else s1Light)
        section2.backgroundTintList = ColorStateList.valueOf(if (isDarkMode) s2Dark else s2Light)
        section3.backgroundTintList = ColorStateList.valueOf(if (isDarkMode) s3Dark else s3Light)
        section4.backgroundTintList = ColorStateList.valueOf(if (isDarkMode) s4Dark else s4Light)
        
        // In a real app, we would also update text colors here by finding all TextViews 
        // or using a proper Theme attribute system.
        // For this mockup, the background change is the main visual indicator.
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
