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
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialDriver
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CdcAcmSerialDriver
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
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
        const val NAYAX_PID = 0x5650        // Standard VPOS Touch
        const val NAYAX_PID_ALT = 0x222a    // Alternate PID (model variant)
        const val NAYAX_TTY_ACM = "/dev/ttyACM0"

        // FTDI VID/PID - Nayax VPOS uses internal FTDI chip!
        const val FTDI_VID = 0x0403
        const val NAYAX_FTDI_PID = 0x6015   // Nayax VPOS internal FTDI chip

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
                                    isNayaxDevice(it) -> {
                                        Log.d(TAG, "Initializing Nayax with Marshall SDK (DMVI pattern)")
                                        val portInfo = openNayaxSerialPort(it)
                                        if (portInfo != null) {
                                            val (serialPort, connection, _) = portInfo
                                            nayaxPaymentManager = NayaxPaymentManager(
                                                usbManager, it, serialPort, connection, serviceScope
                                            )
                                            nayaxPaymentManager?.initialize()
                                        } else {
                                            Log.e(TAG, "Failed to open Nayax serial port after permission grant")
                                        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(permissionReceiver, filter)
        }
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

        // Initialize Nayax VPOS Touch using official Marshall SDK
        // The SDK was extracted from DMVI's APK and properly implements the protocol
        // CRITICAL: The VPOS has TWO USB interfaces - prefer FTDI (0403:6015) over CDC-ACM (26f1:5650)!
        // The FTDI interface is the one that actually works for Marshall protocol communication.
        val nayaxDevice = findUsbDevice(FTDI_VID, NAYAX_FTDI_PID)  // Try FTDI first!
            ?: findUsbDevice(NAYAX_VID, NAYAX_PID)
            ?: findUsbDevice(NAYAX_VID, NAYAX_PID_ALT)
        Log.i(TAG, "Nayax device search: FTDI=${findUsbDevice(FTDI_VID, NAYAX_FTDI_PID) != null}, CDC=${findUsbDevice(NAYAX_VID, NAYAX_PID) != null}")
        if (nayaxDevice != null) {
            if (usbManager.hasPermission(nayaxDevice)) {
                Log.d(TAG, "Nayax: Permission already granted, opening serial port (DMVI pattern)")
                val portInfo = openNayaxSerialPort(nayaxDevice)
                if (portInfo != null) {
                    val (serialPort, connection, _) = portInfo
                    nayaxPaymentManager = NayaxPaymentManager(
                        usbManager, nayaxDevice, serialPort, connection, serviceScope
                    )
                    nayaxPaymentManager?.initialize()
                } else {
                    Log.e(TAG, "Failed to open Nayax serial port")
                }
            } else {
                Log.d(TAG, "Nayax: Requesting USB permission")
                requestUsbPermission(nayaxDevice)
            }
        } else {
            Log.w(TAG, "Nayax VPOS Touch device not found")
        }

        // Setup USB connection monitoring
        setupUsbMonitoring()
    }

    private fun requestUsbPermission(device: UsbDevice) {
        Log.i(TAG, "Requesting USB permission for VID=0x${device.vendorId.toString(16)}, PID=0x${device.productId.toString(16)}, name=${device.deviceName}")
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            device.deviceId,  // Use device ID as request code to differentiate devices
            Intent(ACTION_USB_PERMISSION).apply {
                putExtra(UsbManager.EXTRA_DEVICE, device)
            },
            PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    private fun findUsbDevice(vid: Int, pid: Int): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == vid && device.productId == pid
        }
    }

    /**
     * Check if a USB device is a Nayax VPOS (either CDC-ACM or FTDI variant)
     */
    private fun isNayaxDevice(device: UsbDevice): Boolean {
        // Check for Nayax VID with various PIDs
        if (device.vendorId == NAYAX_VID &&
            (device.productId == NAYAX_PID || device.productId == NAYAX_PID_ALT)) {
            return true
        }
        // Check for FTDI-based Nayax (VID 0403, PID 6015)
        // NOTE: Must not match ID Scanner (VID 0403, PID 6001)
        if (device.vendorId == FTDI_VID && device.productId == NAYAX_FTDI_PID) {
            return true
        }
        return false
    }

    /**
     * Open USB serial port for Nayax device.
     * CRITICAL: Nayax VPOS uses internal FTDI chip - must use FtdiSerialDriver!
     *
     * @return Triple of (UsbSerialPort, UsbDeviceConnection, Driver) or null if failed
     */
    private fun openNayaxSerialPort(device: UsbDevice): Triple<UsbSerialPort, UsbDeviceConnection, Any>? {
        try {
            // Debug: log device info
            Log.i(TAG, "Opening Nayax device: VID=0x${device.vendorId.toString(16)}, PID=0x${device.productId.toString(16)}")
            Log.d(TAG, "Nayax device interface count: ${device.interfaceCount}")
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                Log.d(TAG, "Interface $i: class=${iface.interfaceClass}, subclass=${iface.interfaceSubclass}")
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    Log.d(TAG, "  Endpoint $j: addr=0x${ep.address.toString(16)}, dir=${if (ep.direction == 128) "IN" else "OUT"}, type=${ep.type}")
                }
            }

            // Choose driver based on VID - device reports differently on different platforms!
            // - VID 0x0403 (FTDI): Use FtdiSerialDriver
            // - VID 0x26f1 (Nayax CDC): Use CdcAcmSerialDriver
            val isFtdiMode = (device.vendorId == FTDI_VID)
            val driver: UsbSerialDriver = if (isFtdiMode) {
                Log.i(TAG, "Using FtdiSerialDriver for Nayax (FTDI mode)")
                FtdiSerialDriver(device)
            } else {
                Log.i(TAG, "Using CdcAcmSerialDriver for Nayax (CDC-ACM mode)")
                CdcAcmSerialDriver(device)
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.e(TAG, "Failed to open USB device connection for Nayax")
                return null
            }

            val port = driver.ports[0]
            port.open(connection)

            // Set baud rate to 115200 for Marshall Protocol
            port.setParameters(
                NAYAX_BAUD_RATE,  // 115200
                8,                // Data bits
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            Log.i(TAG, "Nayax port opened: ${NAYAX_BAUD_RATE} baud, 8N1 (${if (isFtdiMode) "FTDI" else "CDC-ACM"} mode)")

            // Purge any stale data in the buffers
            try {
                port.purgeHwBuffers(true, true)
                Log.d(TAG, "Purged Nayax hardware buffers")
            } catch (e: Exception) {
                Log.w(TAG, "Could not purge buffers (normal for CDC-ACM): ${e.message}")
            }

            Log.i(TAG, "Nayax serial port opened successfully!")
            return Triple(port, connection, driver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Nayax serial port", e)
            return null
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
                    isNayaxDevice(device) -> {
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
                    isNayaxDevice(device) -> {
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

