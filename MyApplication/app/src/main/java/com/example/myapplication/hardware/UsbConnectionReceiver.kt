package com.example.myapplication.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * Monitors USB device attach/detach events for automatic reconnection
 * Enables seamless hardware reconnection when cables are toggled
 */
class UsbConnectionReceiver(
    private val onDeviceAttached: (UsbDevice) -> Unit,
    private val onDeviceDetached: (UsbDevice) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "UsbConnectionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "USB Device Attached: VID=${it.vendorId} (0x${it.vendorId.toString(16)}), PID=${it.productId} (0x${it.productId.toString(16)})")
                    onDeviceAttached(it)
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "USB Device Detached: VID=${it.vendorId} (0x${it.vendorId.toString(16)}), PID=${it.productId} (0x${it.productId.toString(16)})")
                    onDeviceDetached(it)
                }
            }
        }
    }
}
