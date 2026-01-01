package com.example.myapplication.hardware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class HardwareService : Service() {
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var usbManager: UsbManager
    private var idScannerManager: IdScannerManager? = null
    private var nayaxPaymentManager: NayaxPaymentManager? = null
    private var motorControlManager: MotorControlManager? = null
    private var usbReceiver: UsbConnectionReceiver? = null
    private var permissionReceiver: BroadcastReceiver? = null
    
    companion object {
        private const val TAG = "HardwareService"
        private const val CHANNEL_ID = "HardwareServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_USB_PERMISSION = "com.example.myapplication.USB_PERMISSION"

        // USB Vendor/Product IDs from hardware analysis
        const val ID_SCANNER_VID = 0x0403
        const val ID_SCANNER_PID = 0x6001
        const val NAYAX_VID = 0x26f1
        const val NAYAX_PID = 0x5650
        const val NAYAX_TTY_ACM = "/dev/ttyACM0"

        // Baud rates - CRITICAL: Different devices require different speeds!
        const val ID_SCANNER_BAUD_RATE = 9600    // E-Seek M260 requires 9600 bps
        const val NAYAX_BAUD_RATE = 115200       // Marshall Protocol requires 115200 bps
        @Deprecated("Use device-specific baud rates", ReplaceWith("ID_SCANNER_BAUD_RATE or NAYAX_BAUD_RATE"))
        const val SERIAL_BAUD_RATE = 115200      // Kept for backwards compatibility
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): HardwareService = this@HardwareService
    }
    
    override fun onCreate() {
        super.onCreate()
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupPermissionReceiver()

        // Initialize motor control (no USB device needed - uses TCP)
        motorControlManager = MotorControlManager(serviceScope)
        motorControlManager?.initialize(this)

        initializeHardware()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()

        // Unregister USB receiver
        usbReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        usbReceiver = null

        // Unregister permission receiver
        permissionReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        permissionReceiver = null

        // Close hardware connections
        idScannerManager?.close()
        nayaxPaymentManager?.close()
        motorControlManager?.reset()

        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hardware Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Manages ID scanner, payment reader, and motor control"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hardware Service")
            .setContentText("ID Scanner, Payment Reader, and Motor Control Active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun setupPermissionReceiver() {
        permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_USB_PERMISSION) {
                    synchronized(this) {
                        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                Log.d(TAG, "USB permission granted for ${it.deviceName}")
                                // Initialize the hardware that got permission
                                when {
                                    it.vendorId == ID_SCANNER_VID && it.productId == ID_SCANNER_PID -> {
                                        idScannerManager = IdScannerManager(usbManager, it, serviceScope)
                                        idScannerManager?.initialize()
                                    }
                                    it.vendorId == NAYAX_VID && it.productId == NAYAX_PID -> {
                                        nayaxPaymentManager = NayaxPaymentManager(usbManager, it, NAYAX_TTY_ACM, serviceScope)
                                        nayaxPaymentManager?.initialize()
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "USB permission denied for ${device?.deviceName}")
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(permissionReceiver, filter)
    }

    private fun initializeHardware() {
        // Initialize ID Scanner (E-Seek M260)
        val idScannerDevice = findUsbDevice(ID_SCANNER_VID, ID_SCANNER_PID)
        if (idScannerDevice != null) {
            if (usbManager.hasPermission(idScannerDevice)) {
                Log.d(TAG, "ID Scanner: Permission already granted")
                idScannerManager = IdScannerManager(usbManager, idScannerDevice, serviceScope)
                idScannerManager?.initialize()
            } else {
                Log.d(TAG, "ID Scanner: Requesting permission")
                requestUsbPermission(idScannerDevice)
            }
        } else {
            Log.w(TAG, "ID Scanner device not found")
        }

        // Initialize Nayax Payment Reader
        val nayaxDevice = findUsbDevice(NAYAX_VID, NAYAX_PID)
        if (nayaxDevice != null) {
            if (usbManager.hasPermission(nayaxDevice)) {
                Log.d(TAG, "Nayax: Permission already granted")
                nayaxPaymentManager = NayaxPaymentManager(usbManager, nayaxDevice, NAYAX_TTY_ACM, serviceScope)
                nayaxPaymentManager?.initialize()
            } else {
                Log.d(TAG, "Nayax: Requesting permission")
                requestUsbPermission(nayaxDevice)
            }
        } else {
            Log.w(TAG, "Nayax device not found")
        }

        // Setup USB connection monitoring
        setupUsbMonitoring()
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    private fun findUsbDevice(vid: Int, pid: Int): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == vid && device.productId == pid
        }
    }

    private fun setupUsbMonitoring() {
        usbReceiver = UsbConnectionReceiver(
            onDeviceAttached = { device ->
                when {
                    device.vendorId == ID_SCANNER_VID && device.productId == ID_SCANNER_PID -> {
                        // Re-initialize ID Scanner
                        // idScannerManager?.reconnect() // TODO: implement reconnect
                    }
                    device.vendorId == NAYAX_VID && device.productId == NAYAX_PID -> {
                        // Re-initialize Nayax Payment Reader
                        // nayaxPaymentManager?.reconnect() // TODO: implement reconnect
                    }
                }
            },
            onDeviceDetached = { device ->
                when {
                    device.vendorId == ID_SCANNER_VID && device.productId == ID_SCANNER_PID -> {
                        idScannerManager?.close()
                    }
                    device.vendorId == NAYAX_VID && device.productId == NAYAX_PID -> {
                        nayaxPaymentManager?.close()
                    }
                }
            }
        )

        // Register receiver for USB events
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)
    }

    fun getIdScannerManager(): IdScannerManager? = idScannerManager
    fun getNayaxPaymentManager(): NayaxPaymentManager? = nayaxPaymentManager
    fun getMotorControlManager(): MotorControlManager? = motorControlManager
}

