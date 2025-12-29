package com.example.myapplication.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.myapplication.database.InventoryRepository
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background service for syncing local inventory to backend
 *
 * Uses WorkManager for periodic background sync (every 1 hour)
 * Non-blocking, best-effort sync - failures are logged but don't block app functionality
 */
class BackgroundSyncService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = InventoryRepository.getInstance(context)

    companion object {
        private const val TAG = "BackgroundSyncService"
        private const val WORK_NAME = "inventory_sync"
        const val BACKEND_URL = "http://localhost:8080/api/v1/sync/inventory"

        /**
         * Schedule periodic sync (every 1 hour)
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncService>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )

            Log.d(TAG, "Periodic sync scheduled (every 1 hour)")
        }

        /**
         * Trigger immediate one-time sync
         */
        fun syncNow(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<BackgroundSyncService>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "Immediate sync triggered")
        }

        /**
         * Cancel all sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Sync work cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting inventory sync")

        return try {
            // Get current inventory state
            val coils = repository.getAllCoils()
            val unsyncedTransactions = repository.getUnsyncedTransactions()

            if (coils.isEmpty() && unsyncedTransactions.isEmpty()) {
                Log.d(TAG, "No data to sync")
                return Result.success()
            }

            // Build sync request
            val syncRequest = buildSyncRequest(coils, unsyncedTransactions)

            // Send to backend
            val success = sendSyncRequest(syncRequest)

            if (success) {
                // Mark transactions as synced
                unsyncedTransactions.forEach { transaction ->
                    repository.markTransactionSynced(transaction.id)
                }

                Log.d(TAG, "Sync successful: ${coils.size} coils, ${unsyncedTransactions.size} transactions")
                Result.success()
            } else {
                Log.w(TAG, "Sync failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            Result.retry()
        }
    }

    private fun buildSyncRequest(
        coils: List<com.example.myapplication.database.models.Coil>,
        transactions: List<com.example.myapplication.database.models.Transaction>
    ): JSONObject {
        val request = JSONObject()
        request.put("source", "android")
        request.put("timestamp", System.currentTimeMillis() / 1000)

        // Add coils
        val coilsArray = JSONArray()
        coils.forEach { coil ->
            val coilObj = JSONObject()
            coilObj.put("id", coil.id)
            coilObj.put("inventory", coil.inventory)
            coilObj.put("status", coil.status)
            coilObj.put("updated_at", coil.updatedAt)
            coilsArray.put(coilObj)
        }
        request.put("coils", coilsArray)

        // Add transactions
        val transactionsArray = JSONArray()
        transactions.forEach { txn ->
            val txnObj = JSONObject()
            txnObj.put("id", txn.id)
            txnObj.put("coil_id", txn.coilId)
            txnObj.put("status", txn.status)
            txnObj.put("timestamp", txn.timestamp)
            transactionsArray.put(txnObj)
        }
        request.put("transactions", transactionsArray)

        return request
    }

    private fun sendSyncRequest(request: JSONObject): Boolean {
        var connection: HttpURLConnection? = null

        try {
            val url = URL(BACKEND_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            // Write request body
            connection.outputStream.use { output ->
                output.write(request.toString().toByteArray())
            }

            // Check response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Sync response: $response")
                return true
            } else {
                Log.w(TAG, "Sync failed with HTTP $responseCode")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during sync: ${e.message}", e)
            return false
        } finally {
            connection?.disconnect()
        }
    }
}
