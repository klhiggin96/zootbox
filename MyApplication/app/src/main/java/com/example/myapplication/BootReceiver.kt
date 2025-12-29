package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver that launches MyApplication on device boot
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device booted, launching MyApplication")
            
            // Launch MainActivity with flags to bring to foreground
            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            
            // Add delay to ensure system is ready
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    context.startActivity(launchIntent)
                    Log.i("BootReceiver", "MainActivity launched")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to launch MainActivity", e)
                }
            }, 3000) // 3 second delay after boot
        }
    }
}
