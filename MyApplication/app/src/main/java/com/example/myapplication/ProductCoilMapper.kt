package com.example.myapplication

import android.util.Log
import com.example.myapplication.database.InventoryRepository
import com.example.myapplication.database.models.Coil

/**
 * Utility class for mapping products to coils
 * 
 * Provides centralized product-to-coil mapping logic.
 * Currently uses hardcoded mappings for specific products,
 * with fallback to hash-based mapping for others.
 * 
 * TODO: Replace with database-driven product-to-coil mapping table
 */
object ProductCoilMapper {
    private const val TAG = "ProductCoilMapper"

    /**
     * Get the assigned coil for a product by name
     * 
     * @param productName The name of the product (e.g., "ZYN CITRUS")
     * @param inventoryRepo The inventory repository instance
     * @return The assigned Coil, or null if not found
     */
    fun getCoilForProduct(productName: String, inventoryRepo: InventoryRepository): Coil? {
        val coilId = getCoilIdForProduct(productName)
        Log.d(TAG, "Product '$productName' mapped to coil $coilId")
        return inventoryRepo.getCoil(coilId)
    }

    /**
     * Get the coil ID for a product by name
     * 
     * @param productName The name of the product
     * @return The coil ID (e.g., "A1", "B1", etc.)
     */
    private fun getCoilIdForProduct(productName: String): String {
        // Hardcoded mappings for specific products
        when (productName) {
            "ZYN CITRUS" -> return "A1"
            // Add more hardcoded mappings here as needed
        }

        // Fallback to hash-based mapping for other products
        // TODO: Replace with database-driven product-to-coil mapping table
        val coilIds = listOf("A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1", "I1", "J1")
        val index = (productName.hashCode() and 0x7FFFFFFF) % coilIds.size
        return coilIds[index]
    }
}

