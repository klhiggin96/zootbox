package com.example.myapplication

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class ScreensaverActivity : AppCompatActivity() {

    private lateinit var bgVideoView1: FullScreenVideoView
    private lateinit var bgVideoView2: FullScreenVideoView

    private val fadeDuration = 1500L // 1.5s cross-fade
    private var activePlayer = 1 // 1 or 2
    private val loopHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var loopRunnable: Runnable? = null
    private val playbackSpeed = 1.0f // Normal playback speed
    private var videoDuration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screensaver)

        // Enable edge-to-edge and hide system bars
        setupFullScreen()

        // Initialize Video Views
        bgVideoView1 = findViewById(R.id.bg_video_view_1)
        bgVideoView2 = findViewById(R.id.bg_video_view_2)

        // Rotate videos 180 degrees
        bgVideoView1.rotation = 180f
        bgVideoView2.rotation = 180f

        // Setup Video Background
        setupDualVideoBackground()
    }

    private fun setupDualVideoBackground() {
        // Ensure video views are visible
        bgVideoView1.visibility = View.VISIBLE
        bgVideoView2.visibility = View.VISIBLE

        val videoPath = "android.resource://" + packageName + "/" + R.raw.download
        val uri = android.net.Uri.parse(videoPath)

        android.util.Log.d("ScreensaverActivity", "Video URI: $videoPath")
        android.util.Log.d("ScreensaverActivity", "Resource ID: ${R.raw.download}")

        // Initialize Player 1
        bgVideoView1.setVideoURI(uri)
        bgVideoView1.setOnPreparedListener { mp ->
            android.util.Log.d("ScreensaverActivity", "Video 1 prepared, duration: ${mp.duration}")
            mp.setVolume(0f, 0f)
            // Set playback speed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {
                    android.util.Log.e("ScreensaverActivity", "Error setting playback speed", e)
                }
            }

            videoDuration = mp.duration
            bgVideoView1.start()
            android.util.Log.d("ScreensaverActivity", "Video 1 started")
            scheduleNextLoop(videoDuration)
        }

        bgVideoView1.setOnErrorListener { mp, what, extra ->
            android.util.Log.e("ScreensaverActivity", "Video 1 error: what=$what, extra=$extra")
            false
        }

        // Initialize Player 2 (don't start yet)
        bgVideoView2.setVideoURI(uri)
        bgVideoView2.setOnPreparedListener { mp ->
            android.util.Log.d("ScreensaverActivity", "Video 2 prepared")
            mp.setVolume(0f, 0f)
            // Set playback speed for P2 as well
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {
                    android.util.Log.e("ScreensaverActivity", "Error setting playback speed for P2", e)
                }
            }
        }

        bgVideoView2.setOnErrorListener { mp, what, extra ->
            android.util.Log.e("ScreensaverActivity", "Video 2 error: what=$what, extra=$extra")
            false
        }
    }

    private fun scheduleNextLoop(durationMs: Int) {
        if (durationMs <= 0) return

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
        if (activePlayer == 1) bgVideoView1.start() else bgVideoView2.start()
    }

    override fun onPause() {
        super.onPause()
        loopRunnable?.let { loopHandler.removeCallbacks(it) }
        bgVideoView1.pause()
        bgVideoView2.pause()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Any touch exits the screensaver
        if (event.action == MotionEvent.ACTION_DOWN) {
            finish()
            return true
        }
        return super.onTouchEvent(event)
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
