package com.example.myapplication.database.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data model representing a vend transaction
 *
 * Immutable log of all vend attempts (success, jam, or failure)
 */
data class Transaction(
    val id: String,                  // Unique transaction ID
    val coilId: String,              // Coil that was vended (A1-J1)
    val status: String,              // 'success', 'jam', or 'failed'
    val timestamp: Long,             // Unix timestamp (seconds)
    val synced: Boolean = false,     // Whether synced to backend

    // Payment fields (added for Nayax integration)
    val amount: Double? = null,                 // Transaction amount in USD
    val paymentMethod: String? = null,          // 'card', 'nfc', 'cash', 'free'
    val paymentStatus: String? = null,          // 'pending', 'approved', 'declined', 'refunded'
    val currency: String? = "USD",              // Currency code
    val nayaxTransactionId: String? = null,     // Nayax transaction ID from VPOS Touch
    val productId: String? = null               // Product SKU
) {
    companion object {
        const val STATUS_SUCCESS = "success"
        const val STATUS_JAM = "jam"
        const val STATUS_FAILED = "failed"

        // Payment methods
        const val PAYMENT_METHOD_CARD = "card"
        const val PAYMENT_METHOD_NFC = "nfc"
        const val PAYMENT_METHOD_CASH = "cash"
        const val PAYMENT_METHOD_FREE = "free"

        // Payment statuses
        const val PAYMENT_STATUS_PENDING = "pending"
        const val PAYMENT_STATUS_APPROVED = "approved"
        const val PAYMENT_STATUS_DECLINED = "declined"
        const val PAYMENT_STATUS_REFUNDED = "refunded"

        /**
         * Generate unique transaction ID
         */
        fun generateId(): String {
            return "txn_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        }

        /**
         * Create new transaction (legacy - free vending)
         */
        fun create(coilId: String, status: String): Transaction {
            return Transaction(
                id = generateId(),
                coilId = coilId,
                status = status,
                timestamp = System.currentTimeMillis() / 1000,
                synced = false
            )
        }

        /**
         * Create new transaction with payment information
         */
        fun createWithPayment(
            coilId: String,
            status: String,
            amount: Double,
            paymentMethod: String,
            paymentStatus: String,
            nayaxTransactionId: String? = null,
            productId: String? = null
        ): Transaction {
            return Transaction(
                id = generateId(),
                coilId = coilId,
                status = status,
                timestamp = System.currentTimeMillis() / 1000,
                synced = false,
                amount = amount,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                currency = "USD",
                nayaxTransactionId = nayaxTransactionId,
                productId = productId
            )
        }
    }

    /**
     * Check if transaction was successful
     */
    fun isSuccess(): Boolean = status == STATUS_SUCCESS

    /**
     * Check if transaction resulted in jam
     */
    fun isJam(): Boolean = status == STATUS_JAM

    /**
     * Check if transaction failed
     */
    fun isFailed(): Boolean = status == STATUS_FAILED

    /**
     * Format timestamp for display
     */
    fun getFormattedTime(): String {
        val date = Date(timestamp * 1000)
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)
        return format.format(date)
    }

    /**
     * Get relative time string (e.g., "2 hours ago")
     */
    fun getRelativeTime(): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - timestamp

        return when {
            diff < 60 -> "Just now"
            diff < 3600 -> "${diff / 60} minutes ago"
            diff < 86400 -> "${diff / 3600} hours ago"
            diff < 604800 -> "${diff / 86400} days ago"
            else -> getFormattedTime()
        }
    }

    /**
     * Get display color for status
     */
    fun getStatusColor(): Int = when (status) {
        STATUS_SUCCESS -> android.graphics.Color.parseColor("#10B981") // Green
        STATUS_JAM -> android.graphics.Color.parseColor("#EF4444")     // Red
        STATUS_FAILED -> android.graphics.Color.parseColor("#F59E0B")  // Orange
        else -> android.graphics.Color.GRAY
    }

    /**
     * Get display icon for status
     */
    fun getStatusIcon(): String = when (status) {
        STATUS_SUCCESS -> "✓"
        STATUS_JAM -> "⚠"
        STATUS_FAILED -> "✗"
        else -> "?"
    }
}
