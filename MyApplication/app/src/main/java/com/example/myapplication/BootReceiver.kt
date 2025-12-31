package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver that launches MyApplication on device boot
 *
 * NOTE: MyApplication is set as HOME app and auto-launches during boot.
 * This receiver serves as a backup to ensure the app starts if something goes wrong.
 *
 * HardwareService is started by MainActivity after a 10-second delay to allow
 * USB permission database to load.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device booted, launching MyApplication")

            // Launch MyApplication (backup - app is also HOME and auto-launches)
            try {
                val launchIntent = Intent(context, MainActivity::class.java)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launchIntent)
                Log.i("BootReceiver", "MyApplication launch intent sent")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to launch MainActivity", e)
            }
        }
    }
}
