package com.example.myapplication.database.models

/**
 * Data model representing a vending machine product with pricing
 *
 * Products can be physical (dispensed from coils) or digital (e.g., donations)
 */
data class Product(
    val id: String,                     // Product SKU (e.g., "PRD001")
    val name: String,                   // Product name (e.g., "ZYN CITRUS")
    val category: String,               // Category: "ZyNS", "Extras", "Donations"
    val price: Double,                  // Base price in USD (e.g., 3.50)
    val ageRestriction: Int = 0,        // Minimum age (0 = no restriction, 21 = tobacco)
    val imageUrl: String? = null,       // Product image path/URL
    val videoFilename: String? = null,  // Product video filename
    val isDigital: Boolean = false,     // Virtual product (no physical inventory)
    val active: Boolean = true,         // Can be sold (inactive products hidden)
    val createdAt: Long = System.currentTimeMillis() / 1000,
    val updatedAt: Long = System.currentTimeMillis() / 1000
) {
    companion object {
        // Product categories
        const val CATEGORY_ZYNS = "ZyNS"
        const val CATEGORY_EXTRAS = "Extras"
        const val CATEGORY_DONATIONS = "Donations"

        // Age restrictions
        const val AGE_UNRESTRICTED = 0
        const val AGE_TOBACCO = 21
    }

    /**
     * Check if product requires age verification
     */
    fun requiresAgeVerification(): Boolean = ageRestriction > 0

    /**
     * Check if product is physical (requires inventory)
     */
    fun isPhysical(): Boolean = !isDigital

    /**
     * Check if product can be purchased
     */
    fun isAvailable(): Boolean = active

    /**
     * Format price for display
     */
    fun getFormattedPrice(): String = String.format("$%.2f", price)
}

/**
 * Product with inventory information from assigned coils
 */
data class ProductWithInventory(
    val product: Product,
    val availableCoils: List<CoilInventoryInfo>,
    val totalInventory: Int
) {
    /**
     * Check if product is in stock
     */
    fun isInStock(): Boolean = totalInventory > 0 || product.isDigital

    /**
     * Get primary coil (highest priority with inventory)
     */
    fun getPrimaryCoil(): CoilInventoryInfo? = availableCoils
        .filter { it.inventory > 0 }
        .minByOrNull { it.priority }
}

/**
 * Coil inventory information for a specific product assignment
 */
data class CoilInventoryInfo(
    val coilId: String,      // Coil identifier (A1-J1)
    val inventory: Int,      // Current inventory count
    val status: String,      // Coil status (normal, jammed)
    val priority: Int        // Vend priority (0 = highest)
)

/**
 * Product-to-coil assignment mapping
 */
data class ProductCoilAssignment(
    val id: String,
    val productId: String,
    val coilId: String,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis() / 1000
)
