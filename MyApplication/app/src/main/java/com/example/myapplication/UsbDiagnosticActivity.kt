package com.example.myapplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * USB Diagnostic Activity
 *
 * Shows all connected USB devices and their vendor/product IDs
 * Helps troubleshoot ID scanner connection issues
 */
class UsbDiagnosticActivity : AppCompatActivity() {

    private lateinit var diagnosticText: TextView
    private lateinit var refreshButton: Button
    private lateinit var requestPermissionButton: Button
    private lateinit var backButton: Button
    private lateinit var usbManager: UsbManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_diagnostic)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        diagnosticText = findViewById(R.id.diagnostic_text)
        refreshButton = findViewById(R.id.refresh_button)
        requestPermissionButton = findViewById(R.id.request_permission_button)
        backButton = findViewById(R.id.back_button)

        refreshButton.setOnClickListener {
            refreshUsbDevices()
        }

        requestPermissionButton.setOnClickListener {
            requestPermissionsForAllDevices()
        }

        backButton.setOnClickListener {
            finish()
        }

        // Initial scan
        refreshUsbDevices()
    }

    private fun refreshUsbDevices() {
        val deviceList = usbManager.deviceList
        val output = StringBuilder()

        output.append("=== USB DEVICE DIAGNOSTIC ===\n\n")
        output.append("Total devices found: ${deviceList.size}\n\n")

        if (deviceList.isEmpty()) {
            output.append("No USB devices detected.\n")
            output.append("\nTroubleshooting:\n")
            output.append("1. Check if scanner is connected via USB\n")
            output.append("2. Try using a USB OTG adapter if needed\n")
            output.append("3. Ensure device is powered on\n")
        } else {
            deviceList.values.forEachIndexed { index, device ->
                output.append("--- Device ${index + 1} ---\n")
                output.append("Name: ${device.deviceName}\n")
                output.append("Vendor ID: ${device.vendorId} (0x${device.vendorId.toString(16).uppercase()})\n")
                output.append("Product ID: ${device.productId} (0x${device.productId.toString(16).uppercase()})\n")
                output.append("Device Class: ${device.deviceClass}\n")
                output.append("Subclass: ${device.deviceSubclass}\n")
                output.append("Protocol: ${device.deviceProtocol}\n")
                output.append("Interface Count: ${device.interfaceCount}\n")

                // Safe access to fields that require permission
                val hasPermission = usbManager.hasPermission(device)
                output.append("Has Permission: $hasPermission\n")

                try {
                    output.append("Manufacturer: ${device.manufacturerName ?: "N/A"}\n")
                    output.append("Product: ${device.productName ?: "N/A"}\n")
                    output.append("Serial: ${device.serialNumber ?: "N/A"}\n")
                } catch (e: SecurityException) {
                    output.append("Manufacturer: [Permission Required]\n")
                    output.append("Product: [Permission Required]\n")
                    output.append("Serial: [Permission Required]\n")
                }

                // Check if this matches ID scanner config
                if (device.vendorId == 0x0403 && device.productId == 0x6001) {
                    output.append("✓ MATCHES ID SCANNER CONFIG!\n")
                }

                output.append("\n")
            }

            output.append("\n=== EXPECTED SCANNER ===\n")
            output.append("E-Seek M260/M210-DL Scanner:\n")
            output.append("  Vendor ID: 1027 (0x0403)\n")
            output.append("  Product ID: 24577 (0x6001)\n")
        }

        diagnosticText.text = output.toString()
    }

    private fun requestPermissionsForAllDevices() {
        val deviceList = usbManager.deviceList

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No USB devices found", Toast.LENGTH_SHORT).show()
            return
        }

        var permissionRequested = false

        deviceList.values.forEach { device ->
            if (!usbManager.hasPermission(device)) {
                val permissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent("com.example.myapplication.USB_PERMISSION"),
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(device, permissionIntent)
                permissionRequested = true
            }
        }

        if (permissionRequested) {
            Toast.makeText(this, "Permission request sent. Please approve.", Toast.LENGTH_LONG).show()

            // Refresh after a delay
            refreshButton.postDelayed({
                refreshUsbDevices()
            }, 2000)
        } else {
            Toast.makeText(this, "All devices already have permission", Toast.LENGTH_SHORT).show()
        }
    }
}
