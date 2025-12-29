package com.example.myapplication.database.models

/**
 * Data model representing a single coil position in the vending machine
 *
 * Each coil can hold 0-10 units of product and has a status (available/jammed)
 */
data class Coil(
    val id: String,                  // Coil ID (A1-J1)
    val inventory: Int,              // Current inventory count (0-10)
    val status: String,              // 'available' or 'jammed'
    val updatedAt: Long              // Unix timestamp (seconds)
) {
    companion object {
        const val MAX_INVENTORY = 10
        const val LOW_STOCK_THRESHOLD = 2

        const val STATUS_AVAILABLE = "available"
        const val STATUS_JAMMED = "jammed"
    }

    /**
     * Check if coil is empty
     */
    fun isEmpty(): Boolean = inventory == 0

    /**
     * Check if coil is low on stock
     */
    fun isLowStock(): Boolean = inventory in 1..LOW_STOCK_THRESHOLD

    /**
     * Check if coil is full
     */
    fun isFull(): Boolean = inventory == MAX_INVENTORY

    /**
     * Check if coil is jammed
     */
    fun isJammed(): Boolean = status == STATUS_JAMMED

    /**
     * Check if coil is available for vending
     */
    fun isAvailable(): Boolean = status == STATUS_AVAILABLE && inventory > 0

    /**
     * Get stock level percentage (0-100)
     */
    fun getStockPercentage(): Int = (inventory * 100) / MAX_INVENTORY

    /**
     * Get display status
     */
    fun getDisplayStatus(): String = when {
        isJammed() -> "JAMMED"
        isEmpty() -> "EMPTY"
        isLowStock() -> "LOW STOCK"
        else -> "ACTIVE"
    }

    /**
     * Get row number (1-10)
     */
    fun getRowNumber(): Int {
        return when (id[0]) {
            'A' -> 1
            'B' -> 2
            'C' -> 3
            'D' -> 4
            'E' -> 5
            'F' -> 6
            'G' -> 7
            'H' -> 8
            'I' -> 9
            'J' -> 10
            else -> 0
        }
    }
}
