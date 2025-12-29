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
 */
object BootManager {
    private const val TAG = "BootManager"
    
    fun startZootBoxServices(context: Context? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start Tailscale first (needs time to establish connection)
                startTailscale(context)
                
                // Wait for Tailscale to initialize
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
    
    private fun startTailscale(context: Context?) {
        try {
            // Method 1: Start Tailscale service
            val process = Runtime.getRuntime().exec(arrayOf(
                "am", "startservice", "-n", "com.tailscale.ipn/.IPNService"
            ))
            process.waitFor()
            Log.i(TAG, "Tailscale service started")
            
            // Method 2: Send broadcast to connect Tailscale
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
            
            // Method 3: Launch Tailscale activity with auto-connect intent
            try {
                if (context != null) {
                    val intent = Intent()
                    intent.setClassName("com.tailscale.ipn", "com.tailscale.ipn.MainActivity")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("autoconnect", true)
                    context.startActivity(intent)
                    Log.i(TAG, "Tailscale activity launched with autoconnect")
                    
                    // Close it after 2 seconds
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        try {
                            Runtime.getRuntime().exec(arrayOf(
                                "am", "force-stop", "com.tailscale.ipn"
                            ))
                        } catch (e: Exception) {
                            // Ignore if force-stop fails
                        }
                    }
                } else {
                    // Fallback: Use am start
                    Runtime.getRuntime().exec(arrayOf(
                        "am", "start",
                        "-n", "com.tailscale.ipn/.MainActivity",
                        "--ez", "autoconnect", "true"
                    )).waitFor()
                    Log.i(TAG, "Tailscale activity launched via am")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch Tailscale activity", e)
            }
            
            // Method 4: Use Tailscale CLI if available
            try {
                Runtime.getRuntime().exec(arrayOf(
                    "su", "-c",
                    "tailscale up"
                )).waitFor()
                Log.i(TAG, "Tailscale CLI 'up' command executed")
            } catch (e: Exception) {
                Log.w(TAG, "Tailscale CLI not available or failed", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Tailscale", e)
        }
    }
}
