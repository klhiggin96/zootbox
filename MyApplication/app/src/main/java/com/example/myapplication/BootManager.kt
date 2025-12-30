package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manages auto-starting ZootBox backend and Tailscale on app launch
 *
 * IMPORTANT: This assumes you're using Magisk Tailscaled module for automatic VPN connection.
 * If using the official Tailscale app, you'll need to manually connect on first boot.
 *
 * See: Backend/BOOT_SEQUENCE_SETUP.md for installation instructions
 */
object BootManager {
    private const val TAG = "BootManager"

    fun startZootBoxServices(context: Context? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if Magisk Tailscaled is installed and running
                val tailscaledRunning = checkMagiskTailscaled()
                if (!tailscaledRunning) {
                    Log.w(TAG, "Magisk Tailscaled not detected - trying official Tailscale app methods")
                    startTailscale(context)
                } else {
                    Log.i(TAG, "Magisk Tailscaled detected and running")
                }

                // Wait for Tailscale to establish connection
                delay(5000)

                // Check if backend is already running
                if (!isBackendRunning()) {
                    Log.i(TAG, "Backend not running, starting...")
                    startBackend()
                } else {
                    Log.i(TAG, "Backend already running")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error starting ZootBox services", e)
            }
        }
    }

    private fun checkMagiskTailscaled(): Boolean {
        return try {
            // Check if Magisk Tailscaled daemon is running
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pgrep tailscaled"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()

            val isRunning = output.isNotBlank()
            if (isRunning) {
                Log.i(TAG, "Magisk Tailscaled daemon is running (PID: ${output.trim()})")
            }
            isRunning
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Magisk Tailscaled status", e)
            false
        }
    }
    
    private fun isBackendRunning(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ps -ef | grep backend | grep -v grep"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            output.contains("backend")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking backend status", e)
            false
        }
    }
    
    private fun startBackend() {
        try {
            val backendCommand = """
                cd /data/data/com.termux/files/home/zootbox && \
                DB_PATH=/data/data/com.termux/files/home/zootbox/data/inventory.db \
                HTTP_HOST=0.0.0.0 \
                nohup ./backend > backend.log 2>&1 &
            """.trimIndent()
            
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", backendCommand))
            process.waitFor()
            Log.i(TAG, "Backend started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting backend", e)
        }
    }
    
    /**
     * Fallback method for official Tailscale app (NOT RECOMMENDED for production)
     *
     * WARNING: This method attempts to start the official Tailscale Android app,
     * but it WILL NOT establish a full VPN connection without manual interaction.
     * For production vending machines, use Magisk Tailscaled module instead.
     *
     * See: Backend/BOOT_SEQUENCE_SETUP.md for Magisk Tailscaled installation
     */
    private fun startTailscale(context: Context?) {
        try {
            Log.w(TAG, "WARNING: Using official Tailscale app methods (may not auto-connect)")
            Log.w(TAG, "For production use, install Magisk Tailscaled module")

            // Method 1: Start Tailscale service (background only, no UI)
            val process = Runtime.getRuntime().exec(arrayOf(
                "am", "startservice", "-n", "com.tailscale.ipn/.IPNService"
            ))
            process.waitFor()
            Log.i(TAG, "Tailscale service started (but may not be connected)")

            // Method 2: Send broadcast to connect Tailscale (background only)
            try {
                Runtime.getRuntime().exec(arrayOf(
                    "am", "broadcast",
                    "-a", "com.tailscale.ipn.CONNECT",
                    "-n", "com.tailscale.ipn/.ConnectivityChangeReceiver"
                )).waitFor()
                Log.i(TAG, "Tailscale connect broadcast sent")
            } catch (e: Exception) {
                Log.w(TAG, "Connect broadcast failed (may not be supported)", e)
            }

            // Method 3: Use Tailscale CLI if available (only works with Magisk Tailscaled)
            try {
                Runtime.getRuntime().exec(arrayOf(
                    "su", "-c",
                    "tailscale up"
                )).waitFor()
                Log.i(TAG, "Tailscale CLI 'up' command executed")
            } catch (e: Exception) {
                Log.w(TAG, "Tailscale CLI not available (install Magisk Tailscaled)", e)
            }

            // NOTE: We do NOT launch the Tailscale UI activity to avoid stealing focus
            // However, the official app still requires manual "Connect" tap on first boot

        } catch (e: Exception) {
            Log.e(TAG, "Error starting Tailscale", e)
        }
    }
}
