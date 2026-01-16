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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HardwareService : Service() {
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Hardware initialization status for loading screen
    private val _hardwareStatus = MutableStateFlow<HardwareStatus>(HardwareStatus.ConnectingNayax)
    val hardwareStatus: StateFlow<HardwareStatus> = _hardwareStatus.asStateFlow()

    private lateinit var usbManager: UsbManager
    private var idScannerManager: IdScannerManager? = null
    private var nayaxPaymentManager: NayaxPaymentManager? = null
    private var motorControlManager: MotorControlManager? = null
    private var usbReceiver: UsbConnectionReceiver? = null
    private var permissionReceiver: BroadcastReceiver? = null

    // Queue for USB permission requests - process one at a time to avoid dialog conflicts
    private val pendingPermissionDevices = mutableListOf<UsbDevice>()
    
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
                                            observeNayaxReadyState()
                                        } else {
                                            Log.e(TAG, "Failed to open Nayax serial port after permission grant")
                                            _hardwareStatus.value = HardwareStatus.Error("Failed to open payment reader")
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "USB permission denied for ${device?.deviceName}")
                        }
                        
                        // Process next device in permission queue (if any)
                        processNextPermissionRequest()
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
        // IMPORTANT: Initialize Nayax FIRST, then ID Scanner
        // Permission dialogs must be sequenced - only one at a time!

        // Update status for loading screen
        _hardwareStatus.value = HardwareStatus.ConnectingNayax

        // Initialize Nayax VPOS Touch using official Marshall SDK (Chipi-X FTDI interface)
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
                    observeNayaxReadyState()
                } else {
                    Log.e(TAG, "Failed to open Nayax serial port")
                    _hardwareStatus.value = HardwareStatus.Error("Failed to open payment reader")
                }
            } else {
                Log.d(TAG, "Nayax: Queuing permission request (will show FIRST)")
                queuePermissionRequest(nayaxDevice)
            }
        } else {
            Log.w(TAG, "Nayax VPOS Touch device not found")
            // Continue without Nayax - will show timeout on loading screen
        }

        // Update status for loading screen
        _hardwareStatus.value = HardwareStatus.ConnectingScanner

        // Initialize ID Scanner (E-Seek M260) - SECOND priority
        val idScannerDevice = findUsbDevice(ID_SCANNER_VID, ID_SCANNER_PID)
        if (idScannerDevice != null) {
            if (usbManager.hasPermission(idScannerDevice)) {
                Log.d(TAG, "ID Scanner: Permission already granted")
                idScannerManager = IdScannerManager(usbManager, idScannerDevice, serviceScope)
                idScannerManager?.initialize()
            } else {
                Log.d(TAG, "ID Scanner: Queuing permission request (will show SECOND)")
                queuePermissionRequest(idScannerDevice)
            }
        } else {
            Log.w(TAG, "ID Scanner device not found")
        }

        // Start processing the permission queue (one at a time)
        processNextPermissionRequest()

        // Setup USB connection monitoring
        setupUsbMonitoring()
    }

    /**
     * Observe NayaxPaymentManager.isReady and update hardware status when ready.
     * Called after Nayax is initialized to track when the payment system becomes available.
     */
    private fun observeNayaxReadyState() {
        serviceScope.launch {
            nayaxPaymentManager?.isReady?.collect { isReady ->
                if (isReady) {
                    Log.i(TAG, "Nayax payment system ready - updating hardware status")
                    _hardwareStatus.value = HardwareStatus.Ready
                }
            }
        }
    }
    
    /**
     * Add a device to the permission request queue.
     * Permissions are requested one at a time to avoid Android dialog conflicts.
     */
    private fun queuePermissionRequest(device: UsbDevice) {
        synchronized(pendingPermissionDevices) {
            pendingPermissionDevices.add(device)
            Log.d(TAG, "Queued permission request for ${device.deviceName} (queue size: ${pendingPermissionDevices.size})")
        }
    }
    
    /**
     * Process the next device in the permission queue.
     * Called after each permission is granted/denied to request the next one.
     */
    private fun processNextPermissionRequest() {
        synchronized(pendingPermissionDevices) {
            if (pendingPermissionDevices.isNotEmpty()) {
                val device = pendingPermissionDevices.removeAt(0)
                Log.i(TAG, "Processing permission request for ${device.deviceName} (${pendingPermissionDevices.size} remaining)")
                requestUsbPermissionInternal(device)
            } else {
                Log.d(TAG, "Permission queue empty - all devices processed")
            }
        }
    }

    /**
     * Internal method to actually request USB permission.
     * Called by processNextPermissionRequest() to ensure only one dialog at a time.
     */
    private fun requestUsbPermissionInternal(device: UsbDevice) {
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

