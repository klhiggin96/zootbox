package com.example.myapplication.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.myapplication.database.models.Coil
import com.example.myapplication.database.models.Transaction

/**
 * Repository layer for inventory database operations
 *
 * Provides high-level methods for managing coils and transactions
 * All database operations are synchronous - call from background thread if needed
 */
class InventoryRepository private constructor(context: Context) {

    private val dbHelper = InventoryDatabase.getInstance(context)
    private val db: SQLiteDatabase
        get() = dbHelper.writableDatabase

    companion object {
        private const val TAG = "InventoryRepository"

        @Volatile
        private var INSTANCE: InventoryRepository? = null

        fun getInstance(context: Context): InventoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InventoryRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // ==================== COIL OPERATIONS ====================

    /**
     * Get all coils
     */
    fun getAllCoils(): List<Coil> {
        val coils = mutableListOf<Coil>()

        db.query(
            InventoryDatabase.TABLE_COILS,
            null,
            null,
            null,
            null,
            null,
            "${InventoryDatabase.COLUMN_COIL_ID} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                coils.add(cursorToCoil(cursor))
            }
        }

        Log.d(TAG, "Retrieved ${coils.size} coils")
        return coils
    }

    /**
     * Get single coil by ID
     */
    fun getCoil(coilId: String): Coil? {
        db.query(
            InventoryDatabase.TABLE_COILS,
            null,
            "${InventoryDatabase.COLUMN_COIL_ID} = ?",
            arrayOf(coilId),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursorToCoil(cursor)
            } else {
                Log.w(TAG, "Coil not found: $coilId")
                null
            }
        }
    }

    /**
     * Update inventory count for a specific coil
     */
    fun updateInventory(coilId: String, newCount: Int): Boolean {
        if (newCount < 0 || newCount > Coil.MAX_INVENTORY) {
            Log.e(TAG, "Invalid inventory count: $newCount (must be 0-10)")
            return false
        }

        val values = ContentValues().apply {
            put(InventoryDatabase.COLUMN_INVENTORY, newCount)
            put(InventoryDatabase.COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
        }

        val rowsAffected = db.update(
            InventoryDatabase.TABLE_COILS,
            values,
            "${InventoryDatabase.COLUMN_COIL_ID} = ?",
            arrayOf(coilId)
        )

        if (rowsAffected > 0) {
            Log.d(TAG, "Updated coil $coilId inventory to $newCount")
            return true
        }

        Log.w(TAG, "Failed to update coil $coilId")
        return false
    }

    /**
     * Decrement inventory by 1 (after successful vend)
     * Returns false if already at 0
     */
    fun decrementInventory(coilId: String): Boolean {
        val coil = getCoil(coilId) ?: return false

        if (coil.inventory <= 0) {
            Log.w(TAG, "Cannot decrement - coil $coilId already empty")
            return false
        }

        return updateInventory(coilId, coil.inventory - 1)
    }

    /**
     * Increment inventory by 1
     * Returns false if already at max
     */
    fun incrementInventory(coilId: String): Boolean {
        val coil = getCoil(coilId) ?: return false

        if (coil.inventory >= Coil.MAX_INVENTORY) {
            Log.w(TAG, "Cannot increment - coil $coilId already full")
            return false
        }

        return updateInventory(coilId, coil.inventory + 1)
    }

    /**
     * Reset all coils to specified count (default 10)
     */
    fun resetAllInventory(count: Int = Coil.MAX_INVENTORY): Int {
        if (count < 0 || count > Coil.MAX_INVENTORY) {
            Log.e(TAG, "Invalid reset count: $count")
            return 0
        }

        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(InventoryDatabase.COLUMN_INVENTORY, count)
                put(InventoryDatabase.COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }

            val rowsAffected = db.update(
                InventoryDatabase.TABLE_COILS,
                values,
                null,
                null
            )

            db.setTransactionSuccessful()
            Log.d(TAG, "Reset $rowsAffected coils to $count units")
            return rowsAffected
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting inventory", e)
            return 0
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Set coil status (available/jammed)
     */
    fun setCoilStatus(coilId: String, status: String): Boolean {
        if (status !in listOf(Coil.STATUS_AVAILABLE, Coil.STATUS_JAMMED)) {
            Log.e(TAG, "Invalid status: $status")
            return false
        }

        val values = ContentValues().apply {
            put(InventoryDatabase.COLUMN_STATUS, status)
            put(InventoryDatabase.COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
        }

        val rowsAffected = db.update(
            InventoryDatabase.TABLE_COILS,
            values,
            "${InventoryDatabase.COLUMN_COIL_ID} = ?",
            arrayOf(coilId)
        )

        if (rowsAffected > 0) {
            Log.d(TAG, "Updated coil $coilId status to $status")
            return true
        }

        return false
    }

    // ==================== STATISTICS ====================

    /**
     * Get total inventory across all coils
     */
    fun getTotalInventory(): Int {
        db.rawQuery(
            "SELECT SUM(${InventoryDatabase.COLUMN_INVENTORY}) FROM ${InventoryDatabase.TABLE_COILS}",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    /**
     * Get stock status breakdown
     * Returns map with keys: "empty", "low", "normal", "full"
     */
    fun getStockStatus(): Map<String, Int> {
        val status = mutableMapOf(
            "empty" to 0,
            "low" to 0,
            "normal" to 0,
            "full" to 0,
            "jammed" to 0
        )

        getAllCoils().forEach { coil ->
            when {
                coil.isJammed() -> status["jammed"] = status["jammed"]!! + 1
                coil.isEmpty() -> status["empty"] = status["empty"]!! + 1
                coil.isLowStock() -> status["low"] = status["low"]!! + 1
                coil.isFull() -> status["full"] = status["full"]!! + 1
                else -> status["normal"] = status["normal"]!! + 1
            }
        }

        return status
    }

    /**
     * Get list of low stock coils (inventory <= 2)
     */
    fun getLowStockCoils(): List<Coil> {
        return getAllCoils().filter { it.isLowStock() || it.isEmpty() }
    }

    /**
     * Get list of jammed coils
     */
    fun getJammedCoils(): List<Coil> {
        return getAllCoils().filter { it.isJammed() }
    }

    // ==================== TRANSACTION OPERATIONS ====================

    /**
     * Log a new transaction
     * Returns transaction ID
     */
    fun logTransaction(coilId: String, status: String): String {
        val transaction = Transaction.create(coilId, status)

        val values = ContentValues().apply {
            put(InventoryDatabase.COLUMN_TRANSACTION_ID, transaction.id)
            put(InventoryDatabase.COLUMN_TRANSACTION_COIL_ID, transaction.coilId)
            put(InventoryDatabase.COLUMN_TRANSACTION_STATUS, transaction.status)
            put(InventoryDatabase.COLUMN_TIMESTAMP, transaction.timestamp)
            put(InventoryDatabase.COLUMN_SYNCED, if (transaction.synced) 1 else 0)
        }

        val result = db.insert(
            InventoryDatabase.TABLE_TRANSACTIONS,
            null,
            values
        )

        if (result != -1L) {
            Log.d(TAG, "Logged transaction: ${transaction.id} for coil $coilId with status $status")
            return transaction.id
        } else {
            Log.e(TAG, "Failed to log transaction for coil $coilId")
            return ""
        }
    }

    /**
     * Get transaction history (most recent first)
     */
    fun getTransactionHistory(limit: Int = 100): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        db.query(
            InventoryDatabase.TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "${InventoryDatabase.COLUMN_TIMESTAMP} DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                transactions.add(cursorToTransaction(cursor))
            }
        }

        Log.d(TAG, "Retrieved ${transactions.size} transactions")
        return transactions
    }

    /**
     * Get transactions for specific coil
     */
    fun getCoilTransactions(coilId: String, limit: Int = 50): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        db.query(
            InventoryDatabase.TABLE_TRANSACTIONS,
            null,
            "${InventoryDatabase.COLUMN_TRANSACTION_COIL_ID} = ?",
            arrayOf(coilId),
            null,
            null,
            "${InventoryDatabase.COLUMN_TIMESTAMP} DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                transactions.add(cursorToTransaction(cursor))
            }
        }

        return transactions
    }

    /**
     * Get unsynced transactions (for backend sync)
     */
    fun getUnsyncedTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        db.query(
            InventoryDatabase.TABLE_TRANSACTIONS,
            null,
            "${InventoryDatabase.COLUMN_SYNCED} = ?",
            arrayOf("0"),
            null,
            null,
            "${InventoryDatabase.COLUMN_TIMESTAMP} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                transactions.add(cursorToTransaction(cursor))
            }
        }

        Log.d(TAG, "Found ${transactions.size} unsynced transactions")
        return transactions
    }

    /**
     * Mark transaction as synced to backend
     */
    fun markTransactionSynced(transactionId: String): Boolean {
        val values = ContentValues().apply {
            put(InventoryDatabase.COLUMN_SYNCED, 1)
        }

        val rowsAffected = db.update(
            InventoryDatabase.TABLE_TRANSACTIONS,
            values,
            "${InventoryDatabase.COLUMN_TRANSACTION_ID} = ?",
            arrayOf(transactionId)
        )

        if (rowsAffected > 0) {
            Log.d(TAG, "Marked transaction $transactionId as synced")
            return true
        }

        return false
    }

    /**
     * Delete old transactions (older than specified days)
     */
    fun clearOldTransactions(olderThanDays: Int = 90): Int {
        val cutoffTimestamp = (System.currentTimeMillis() / 1000) - (olderThanDays * 86400)

        val rowsDeleted = db.delete(
            InventoryDatabase.TABLE_TRANSACTIONS,
            "${InventoryDatabase.COLUMN_TIMESTAMP} < ?",
            arrayOf(cutoffTimestamp.toString())
        )

        Log.d(TAG, "Deleted $rowsDeleted transactions older than $olderThanDays days")
        return rowsDeleted
    }

    /**
     * Get transaction statistics for today
     */
    fun getTodayStats(): TransactionStats {
        val startOfDay = getTodayStartTimestamp()

        db.rawQuery(
            """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN ${InventoryDatabase.COLUMN_TRANSACTION_STATUS} = ? THEN 1 ELSE 0 END) as success,
                SUM(CASE WHEN ${InventoryDatabase.COLUMN_TRANSACTION_STATUS} = ? THEN 1 ELSE 0 END) as jams,
                SUM(CASE WHEN ${InventoryDatabase.COLUMN_TRANSACTION_STATUS} = ? THEN 1 ELSE 0 END) as failed
            FROM ${InventoryDatabase.TABLE_TRANSACTIONS}
            WHERE ${InventoryDatabase.COLUMN_TIMESTAMP} >= ?
            """.trimIndent(),
            arrayOf(
                Transaction.STATUS_SUCCESS,
                Transaction.STATUS_JAM,
                Transaction.STATUS_FAILED,
                startOfDay.toString()
            )
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                TransactionStats(
                    total = cursor.getInt(0),
                    success = cursor.getInt(1),
                    jams = cursor.getInt(2),
                    failed = cursor.getInt(3)
                )
            } else {
                TransactionStats()
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private fun cursorToCoil(cursor: Cursor): Coil {
        return Coil(
            id = cursor.getString(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_COIL_ID)),
            inventory = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_INVENTORY)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_STATUS)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_UPDATED_AT))
        )
    }

    private fun cursorToTransaction(cursor: Cursor): Transaction {
        return Transaction(
            id = cursor.getString(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_TRANSACTION_ID)),
            coilId = cursor.getString(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_TRANSACTION_COIL_ID)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_TRANSACTION_STATUS)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_TIMESTAMP)),
            synced = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryDatabase.COLUMN_SYNCED)) == 1
        )
    }

    private fun getTodayStartTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis / 1000
    }

    /**
     * Close database connection (call when done)
     */
    fun close() {
        dbHelper.close()
    }
}

/**
 * Data class for transaction statistics
 */
data class TransactionStats(
    val total: Int = 0,
    val success: Int = 0,
    val jams: Int = 0,
    val failed: Int = 0
) {
    val successRate: Float
        get() = if (total > 0) (success.toFloat() / total.toFloat()) * 100 else 0f
}
