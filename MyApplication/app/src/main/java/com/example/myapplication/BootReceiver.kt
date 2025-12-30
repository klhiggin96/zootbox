package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver that launches MyApplication on device boot
 *
 * NOTE: Tailscale is configured as "Always-on VPN" and will auto-connect in background
 * No need to launch Tailscale UI - it connects automatically via Android VPN service
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device booted, launching MyApplication (Tailscale auto-connects via Always-on VPN)")

            // Launch MyApplication immediately
            try {
                val launchIntent = Intent(context, MainActivity::class.java)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launchIntent)
                Log.i("BootReceiver", "MyApplication launched immediately - Tailscale connecting in background")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to launch MainActivity", e)
            }
        }
    }
}
