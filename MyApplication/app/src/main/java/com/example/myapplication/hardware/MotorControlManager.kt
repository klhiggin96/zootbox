package com.example.myapplication.hardware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * Motor Control Manager using JSON-RPC 2.0 over TCP
 *
 * Communicates with DMVI Hardware Service (com.digitalmediavending.hardware)
 * to trigger motor vends via Wall Machine Protocol.
 *
 * Architecture:
 * - JSON-RPC 2.0 over TCP (NOT raw serial)
 * - Address: IPv6 loopback ::1 (NOT 127.0.0.1)
 * - Port: 57482 (default)
 * - Service: com.digitalmediavending.hardware/.wallcoilmachine.WallCoilMachineService
 *
 * Command Format:
 * {"col":"5", "method":"requestProductVend", "row":"1", "jsonrpc":"2.0"}
 *
 * Critical Notes:
 * - Columns are 1-indexed strings: A1=1, B1=2, C1=3, etc.
 * - DMVI service MUST be running as foreground service
 * - This is IPC via TCP loopback, NOT inter-app communication
 */
class MotorControlManager(
    private val scope: CoroutineScope
) {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _lastVendStatus = MutableStateFlow<VendStatus?>(null)
    val lastVendStatus: StateFlow<VendStatus?> = _lastVendStatus

    private val gson = Gson()

    companion object {
        private const val TAG = "MotorControlManager"

        // DMVI Hardware Service connection details
        const val DMVI_SERVICE_HOST = "::1"  // IPv6 loopback (CRITICAL!)
        const val DMVI_SERVICE_PORT = 57482   // Default port
        const val SERVICE_PACKAGE = "com.digitalmediavending.hardware"
        const val SERVICE_CLASS = ".wallcoilmachine.WallCoilMachineService"

        // Timeouts
        const val VEND_TIMEOUT_MS = 10000L    // 10 second socket timeout
        const val SERVICE_START_TIMEOUT_MS = 5000L  // 5 seconds to start service
        const val MOTOR_RUNTIME_MS = 5000L    // 5 seconds for motor to complete rotation
    }

    /**
     * JSON-RPC 2.0 vend request structure
     */
    data class VendRequest(
        val col: String,           // Column number as string: "1" to "10"
        val method: String = "requestProductVend",
        val row: String = "1",     // Row number as string (typically "1")
        val jsonrpc: String = "2.0"
    )

    /**
     * Vend status result
     */
    data class VendStatus(
        val success: Boolean,
        val coilId: String,
        val error: String? = null
    )

    /**
     * Initialize motor control manager
     * Checks if DMVI service is running
     */
    fun initialize(context: Context) {
        scope.launch {
            try {
                Log.d(TAG, "Initializing motor control manager")
                val serviceRunning = ensureServiceRunning(context)
                _isReady.value = serviceRunning

                if (serviceRunning) {
                    Log.i(TAG, "Motor control manager initialized successfully")
                } else {
                    Log.w(TAG, "DMVI service not running - motor vending will not work")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize motor control manager", e)
                _isReady.value = false
            }
        }
    }

    /**
     * Ensure DMVI Hardware Service is running
     *
     * @return true if service is running, false otherwise
     */
    suspend fun ensureServiceRunning(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if service is already running
                if (isServiceRunning()) {
                    Log.d(TAG, "DMVI service is already running")
                    return@withContext true
                }

                Log.d(TAG, "Starting DMVI Hardware Service")

                // Start the service as foreground service
                val intent = Intent()
                intent.component = ComponentName(
                    SERVICE_PACKAGE,
                    "$SERVICE_PACKAGE$SERVICE_CLASS"
                )

                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start DMVI service: ${e.message}")
                    return@withContext false
                }

                // Wait for service to start (max 5 seconds)
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < SERVICE_START_TIMEOUT_MS) {
                    delay(500)
                    if (isServiceRunning()) {
                        val elapsed = System.currentTimeMillis() - startTime
                        Log.d(TAG, "DMVI service started after ${elapsed}ms")
                        return@withContext true
                    }
                }

                Log.e(TAG, "DMVI service failed to start within ${SERVICE_START_TIMEOUT_MS}ms")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error ensuring DMVI service is running: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Check if DMVI service is running by attempting TCP connection
     *
     * @return true if connection successful, false otherwise
     */
    private fun isServiceRunning(): Boolean {
        return try {
            Socket(DMVI_SERVICE_HOST, DMVI_SERVICE_PORT).use { socket ->
                socket.soTimeout = 1000
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vend a motor by coil ID
     *
     * Sends JSON-RPC command to DMVI service and waits for motor to complete.
     *
     * @param coilId Coil identifier (e.g., "A1", "B1", "J1")
     * @return true if vend successful, false otherwise
     */
    suspend fun vendMotor(coilId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isReady.value) {
                    Log.e(TAG, "Motor control manager not ready")
                    _lastVendStatus.value = VendStatus(
                        success = false,
                        coilId = coilId,
                        error = "Motor control manager not ready"
                    )
                    return@withContext false
                }

                val columnNumber = coilIdToColumnNumber(coilId)
                if (columnNumber < 1 || columnNumber > 10) {
                    Log.e(TAG, "Invalid coil ID: $coilId (column: $columnNumber)")
                    _lastVendStatus.value = VendStatus(
                        success = false,
                        coilId = coilId,
                        error = "Invalid coil ID: $coilId"
                    )
                    return@withContext false
                }

                val request = VendRequest(col = columnNumber.toString())
                val json = gson.toJson(request)

                Log.d(TAG, "Vending coil $coilId (column $columnNumber)")
                Log.d(TAG, "JSON-RPC request: $json")

                // Connect to DMVI service via TCP
                Socket(DMVI_SERVICE_HOST, DMVI_SERVICE_PORT).use { socket ->
                    socket.soTimeout = VEND_TIMEOUT_MS.toInt()

                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    // Send JSON-RPC command
                    writer.println(json)
                    Log.d(TAG, "Sent vend command for coil $coilId")

                    // Read response (optional - DMVI service may not return anything)
                    try {
                        val response = reader.readLine()
                        if (response != null && response.isNotEmpty()) {
                            Log.d(TAG, "Response: $response")
                        }
                    } catch (e: Exception) {
                        Log.v(TAG, "No response from DMVI service (normal)")
                    }

                    // Wait for motor to complete rotation (3-5 seconds typical)
                    Log.d(TAG, "Waiting ${MOTOR_RUNTIME_MS}ms for motor to complete")
                    delay(MOTOR_RUNTIME_MS)

                    Log.i(TAG, "Motor vend completed for coil $coilId")
                    _lastVendStatus.value = VendStatus(
                        success = true,
                        coilId = coilId
                    )
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vend failed for coil $coilId: ${e.message}", e)
                _lastVendStatus.value = VendStatus(
                    success = false,
                    coilId = coilId,
                    error = e.message ?: "Unknown error"
                )
                false
            }
        }
    }

    /**
     * Vend multiple motors sequentially
     *
     * Used for multi-item shopping cart purchases.
     *
     * @param coilIds List of coil IDs to vend
     * @return Map of coilId to success status
     */
    suspend fun vendMultipleMotors(coilIds: List<String>): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, Boolean>()

            for (coilId in coilIds) {
                Log.d(TAG, "Vending coil $coilId (${coilIds.indexOf(coilId) + 1}/${coilIds.size})")
                val success = vendMotor(coilId)
                results[coilId] = success

                if (!success) {
                    Log.e(TAG, "Multi-vend stopped: Coil $coilId failed")
                    break  // Stop on first failure
                }

                // Brief delay between vends
                if (coilId != coilIds.last()) {
                    delay(500)
                }
            }

            results
        }
    }

    /**
     * Convert coil ID to column number (1-indexed)
     *
     * Mapping:
     * A1 → 1
     * B1 → 2
     * C1 → 3
     * D1 → 4
     * E1 → 5
     * F1 → 6
     * G1 → 7
     * H1 → 8
     * I1 → 9
     * J1 → 10
     *
     * @param coilId Coil identifier (e.g., "A1", "B1")
     * @return Column number (1-indexed)
     */
    private fun coilIdToColumnNumber(coilId: String): Int {
        if (coilId.isEmpty()) return -1

        val letter = coilId.first().uppercaseChar()
        return letter - 'A' + 1
    }

    /**
     * Test motor connectivity
     *
     * Attempts to connect to DMVI service without vending.
     *
     * @return true if DMVI service is reachable, false otherwise
     */
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket(DMVI_SERVICE_HOST, DMVI_SERVICE_PORT).use { socket ->
                    socket.soTimeout = 2000
                    Log.d(TAG, "Motor control connection test successful")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Motor control connection test failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Reset manager state
     */
    fun reset() {
        _lastVendStatus.value = null
    }
}
