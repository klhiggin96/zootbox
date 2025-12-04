package com.example.myapplication

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.hardware.HardwareService
import com.example.myapplication.hardware.IdScanResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IdScanActivity : AppCompatActivity() {
    private var hardwareService: HardwareService? = null
    private var isBound = false
    
    // UI Elements
    private lateinit var layoutIdle: ConstraintLayout
    private lateinit var layoutScanning: ConstraintLayout
    private lateinit var layoutSuccess: ConstraintLayout
    
    // Scanning Elements
    private lateinit var scanningLine: View
    private lateinit var progressBar: View
    private lateinit var textProgress: TextView
    
    // Animations
    private var scanningLineAnimator: ObjectAnimator? = null
    private var progressAnimator: ValueAnimator? = null
    private var currentProgress = 0
    private var scanSimulationJob: Job? = null
    
    private enum class ScanState {
        IDLE, SCANNING, SUCCESS
    }
    
    private var currentState = ScanState.IDLE
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HardwareService.LocalBinder
            hardwareService = binder.getService()
            isBound = true
            setupHardwareScanning()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            hardwareService = null
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_scan)
        
        initializeViews()
        setScanningState(ScanState.IDLE)
        
        // Bind to hardware service
        val intent = Intent(this, HardwareService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun initializeViews() {
        layoutIdle = findViewById(R.id.layout_idle)
        layoutScanning = findViewById(R.id.layout_scanning)
        layoutSuccess = findViewById(R.id.layout_success)
        
        scanningLine = findViewById(R.id.scanning_line)
        progressBar = findViewById(R.id.progress_bar)
        textProgress = findViewById(R.id.text_progress)
        
        // Setup click listener to cancel/finish
        // Note: The new design doesn't have a visible cancel button, 
        // but we might want to handle back press or add a hidden one.
        // For now, standard back button works.
    }
    
    private fun setScanningState(state: ScanState) {
        currentState = state
        
        // Transition animations could be added here (fade out old, fade in new)
        layoutIdle.isVisible = state == ScanState.IDLE
        layoutScanning.isVisible = state == ScanState.SCANNING
        layoutSuccess.isVisible = state == ScanState.SUCCESS
        
        when (state) {
            ScanState.IDLE -> {
                stopScanningAnimations()
                // Auto-start scanning after 1 second
                lifecycleScope.launch {
                    delay(1000)
                    if (currentState == ScanState.IDLE) {
                        setScanningState(ScanState.SCANNING)
                    }
                }
            }
            ScanState.SCANNING -> {
                startScanningAnimations()
                startProgressSimulation()
            }
            ScanState.SUCCESS -> {
                stopScanningAnimations()
                lifecycleScope.launch {
                    delay(3000) // Show success screen for 3 seconds
                    finishWithSuccess()
                }
            }
        }
    }
    
    private fun startScanningAnimations() {
        // Scanning line animation
        scanningLine.post {
            val height = (scanningLine.parent as View).height.toFloat()
            scanningLineAnimator = ObjectAnimator.ofFloat(scanningLine, "translationY", 0f, height).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = LinearInterpolator()
                start()
            }
        }
    }
    
    private fun stopScanningAnimations() {
        scanningLineAnimator?.cancel()
        scanningLineAnimator = null
        progressAnimator?.cancel()
        progressAnimator = null
        scanSimulationJob?.cancel()
    }
    
    private fun startProgressSimulation() {
        // Reset progress
        currentProgress = 0
        updateProgressUI(0)
        
        // Simulate progress FULLY for frontend demo
        scanSimulationJob = lifecycleScope.launch {
            while (currentProgress < 100 && currentState == ScanState.SCANNING) {
                delay(50)
                currentProgress += 2
                // Cap at 100
                if (currentProgress > 100) currentProgress = 100
                updateProgressUI(currentProgress)
            }
            
            if (currentProgress >= 100) {
                setScanningState(ScanState.SUCCESS)
            }
        }
    }
    
    private fun updateProgressUI(progress: Int) {
        textProgress.text = "$progress%"
        // Update width of progress bar
        val params = progressBar.layoutParams
        val containerWidth = (progressBar.parent as View).width
        params.width = (containerWidth * (progress / 100f)).toInt()
        progressBar.layoutParams = params
    }
    
    private fun setupHardwareScanning() {
        val scannerManager = hardwareService?.getIdScannerManager()
        if (scannerManager == null) {
            // Hardware not available - Silent fail for UI demo
            return
        }
        
        scannerManager.clearScanResult()
        
        lifecycleScope.launch {
            scannerManager.scanResult.collect { result ->
                result?.let { handleScanResult(it) }
            }
        }
    }
    
    private fun handleScanResult(result: IdScanResult) {
        if (currentState != ScanState.SCANNING && currentState != ScanState.IDLE) return
        
        if (result.success) {
            val requiredAge = intent.getIntExtra("requiredAge", 21)
            val scannerManager = hardwareService?.getIdScannerManager()
            
            if (scannerManager?.isAgeVerified(requiredAge) == true) {
                // Complete progress to 100%
                updateProgressUI(100)
                setScanningState(ScanState.SUCCESS)
            } else {
                // Verification failed
                Toast.makeText(this, "You must be $requiredAge or older", Toast.LENGTH_LONG).show()
                resetScanning()
            }
        } else {
            // Scan failed
            Toast.makeText(this, "Scan failed: ${result.error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            resetScanning()
        }
    }
    
    private fun resetScanning() {
        // Wait a bit then go back to scanning or idle
        lifecycleScope.launch {
            delay(2000)
            val scannerManager = hardwareService?.getIdScannerManager()
            scannerManager?.clearScanResult()
            setScanningState(ScanState.SCANNING) // Restart scanning loop
        }
    }
    
    private fun finishWithSuccess() {
        setResult(RESULT_OK)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopScanningAnimations()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}