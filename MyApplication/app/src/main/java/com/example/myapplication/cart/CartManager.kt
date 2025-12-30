package com.example.myapplication.cart

import android.util.Log
import com.example.myapplication.database.InventoryRepository
import com.example.myapplication.database.models.Coil
import com.example.myapplication.database.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shopping Cart Manager
 *
 * Manages multi-item cart for single payment transaction using Nayax Multi-Session mode.
 *
 * Features:
 * - Add/remove products with quantities
 * - Coil reservation to prevent overselling
 * - Total amount calculation
 * - Backend cart session integration
 */
class CartManager(
    private val inventoryRepo: InventoryRepository
) {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount.asStateFlow()

    companion object {
        private const val TAG = "CartManager"
    }

    /**
     * Cart item with product, quantity, and reserved coil
     */
    data class CartItem(
        val id: String,                  // Unique cart item ID
        val product: Product,            // Product details
        val quantity: Int,               // Quantity in cart
        val assignedCoil: Coil?,         // Assigned coil for this product
        val unitPrice: Double            // Price per unit (frozen at add time)
    ) {
        val totalPrice: Double
            get() = unitPrice * quantity

        companion object {
            fun create(product: Product, quantity: Int, assignedCoil: Coil?): CartItem {
                return CartItem(
                    id = "ITEM_${System.currentTimeMillis()}_${(0..9999).random()}",
                    product = product,
                    quantity = quantity,
                    assignedCoil = assignedCoil,
                    unitPrice = product.price
                )
            }
        }
    }

    /**
     * Checkout result
     */
    data class CheckoutResult(
        val success: Boolean,
        val cartSessionId: String? = null,
        val totalAmount: Double = 0.0,
        val itemCount: Int = 0,
        val error: String? = null
    )

    /**
     * Add product to cart
     *
     * @param product Product to add
     * @param quantity Quantity to add (default: 1)
     * @return Result with success status
     */
    fun addItem(product: Product, quantity: Int = 1): Result<CartItem> {
        return try {
            Log.d(TAG, "Adding to cart: ${product.name} x$quantity")

            // Validate quantity
            if (quantity <= 0) {
                return Result.failure(IllegalArgumentException("Quantity must be greater than 0"))
            }

            // Find assigned coil for product
            val assignedCoil = findCoilForProduct(product)

            // Verify inventory availability
            if (assignedCoil != null && !product.isDigital) {
                val availableInventory = assignedCoil.inventory

                // Check total quantity in cart for this coil
                val existingQuantity = _cartItems.value
                    .filter { it.assignedCoil?.id == assignedCoil.id }
                    .sumOf { it.quantity }

                val totalRequested = existingQuantity + quantity

                if (totalRequested > availableInventory) {
                    return Result.failure(
                        IllegalStateException(
                            "Insufficient inventory: ${availableInventory - existingQuantity} available"
                        )
                    )
                }
            }

            // Check if product already in cart
            val existingItem = _cartItems.value.find { it.product.id == product.id }

            val updatedItems = if (existingItem != null) {
                // Update quantity of existing item
                _cartItems.value.map { item ->
                    if (item.id == existingItem.id) {
                        item.copy(quantity = item.quantity + quantity)
                    } else {
                        item
                    }
                }
            } else {
                // Add new item
                val newItem = CartItem.create(product, quantity, assignedCoil)
                _cartItems.value + newItem
            }

            _cartItems.value = updatedItems
            updateTotals()

            Log.i(TAG, "Cart updated: ${_cartItems.value.size} items, total: $${_totalAmount.value}")
            Result.success(updatedItems.last())
        } catch (e: Exception) {
            Log.e(TAG, "Error adding item to cart", e)
            Result.failure(e)
        }
    }

    /**
     * Remove item from cart by item ID
     *
     * @param itemId Cart item ID
     * @return Result with success status
     */
    fun removeItem(itemId: String): Result<Unit> {
        return try {
            val updatedItems = _cartItems.value.filter { it.id != itemId }
            _cartItems.value = updatedItems
            updateTotals()

            Log.d(TAG, "Removed item $itemId from cart")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing item from cart", e)
            Result.failure(e)
        }
    }

    /**
     * Update quantity of cart item
     *
     * @param itemId Cart item ID
     * @param newQuantity New quantity (will remove if 0)
     * @return Result with success status
     */
    fun updateQuantity(itemId: String, newQuantity: Int): Result<Unit> {
        return try {
            if (newQuantity < 0) {
                return Result.failure(IllegalArgumentException("Quantity cannot be negative"))
            }

            if (newQuantity == 0) {
                return removeItem(itemId)
            }

            val item = _cartItems.value.find { it.id == itemId }
                ?: return Result.failure(IllegalArgumentException("Item not found in cart"))

            // Verify inventory availability
            if (item.assignedCoil != null && !item.product.isDigital) {
                val availableInventory = item.assignedCoil.inventory

                // Check total quantity in cart for this coil (excluding current item)
                val otherItemsQuantity = _cartItems.value
                    .filter { it.assignedCoil?.id == item.assignedCoil.id && it.id != itemId }
                    .sumOf { it.quantity }

                val totalRequested = otherItemsQuantity + newQuantity

                if (totalRequested > availableInventory) {
                    return Result.failure(
                        IllegalStateException(
                            "Insufficient inventory: ${availableInventory - otherItemsQuantity} available"
                        )
                    )
                }
            }

            val updatedItems = _cartItems.value.map { cartItem ->
                if (cartItem.id == itemId) {
                    cartItem.copy(quantity = newQuantity)
                } else {
                    cartItem
                }
            }

            _cartItems.value = updatedItems
            updateTotals()

            Log.d(TAG, "Updated item $itemId quantity to $newQuantity")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item quantity", e)
            Result.failure(e)
        }
    }

    /**
     * Clear entire cart
     */
    fun clear() {
        _cartItems.value = emptyList()
        updateTotals()
        Log.d(TAG, "Cart cleared")
    }

    /**
     * Check if cart is empty
     */
    fun isEmpty(): Boolean = _cartItems.value.isEmpty()

    /**
     * Get cart summary for display
     */
    fun getSummary(): String {
        return if (isEmpty()) {
            "Cart is empty"
        } else {
            "${_itemCount.value} items - $${String.format("%.2f", _totalAmount.value)}"
        }
    }

    /**
     * Validate cart before checkout
     *
     * Checks:
     * - Cart not empty
     * - All items have valid inventory
     * - No jammed coils
     *
     * @return Result with validation success or error message
     */
    fun validateCart(): Result<Unit> {
        return try {
            if (isEmpty()) {
                return Result.failure(IllegalStateException("Cart is empty"))
            }

            for (item in _cartItems.value) {
                val coil = item.assignedCoil

                // Skip validation for digital products
                if (item.product.isDigital) {
                    continue
                }

                if (coil == null) {
                    return Result.failure(
                        IllegalStateException("Product '${item.product.name}' has no assigned coil")
                    )
                }

                // Refresh coil data
                val refreshedCoil = inventoryRepo.getCoil(coil.id)
                    ?: return Result.failure(
                        IllegalStateException("Coil ${coil.id} not found")
                    )

                // Check if jammed
                if (refreshedCoil.isJammed()) {
                    return Result.failure(
                        IllegalStateException("Coil ${coil.id} is jammed - '${item.product.name}' unavailable")
                    )
                }

                // Check inventory
                val totalQuantityForCoil = _cartItems.value
                    .filter { it.assignedCoil?.id == coil.id }
                    .sumOf { it.quantity }

                if (refreshedCoil.inventory < totalQuantityForCoil) {
                    return Result.failure(
                        IllegalStateException("Insufficient inventory for '${item.product.name}': ${refreshedCoil.inventory} available")
                    )
                }
            }

            Log.d(TAG, "Cart validation passed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Cart validation error", e)
            Result.failure(e)
        }
    }

    /**
     * Find assigned coil for product
     *
     * TODO: Replace with actual product-coil mapping from database
     * Currently uses same hash-based mapping as ProductDetailActivity
     */
    private fun findCoilForProduct(product: Product): Coil? {
        val coilIds = listOf("A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1", "I1", "J1")
        val index = (product.name.hashCode() and 0x7FFFFFFF) % coilIds.size
        val coilId = coilIds[index]

        return inventoryRepo.getCoil(coilId)
    }

    /**
     * Update total amount and item count
     */
    private fun updateTotals() {
        _totalAmount.value = _cartItems.value.sumOf { it.totalPrice }
        _itemCount.value = _cartItems.value.sumOf { it.quantity }
    }

    /**
     * Get all items grouped by coil for dispensing
     *
     * Returns map of coilId to list of (product, quantity) for that coil
     */
    fun getItemsByCoil(): Map<String, List<Pair<Product, Int>>> {
        return _cartItems.value
            .filter { it.assignedCoil != null }
            .groupBy { it.assignedCoil!!.id }
            .mapValues { (_, items) ->
                items.map { it.product to it.quantity }
            }
    }

    /**
     * Get cart items as flat list for vending
     *
     * Returns list of (coilId, product, quantity) for sequential vending
     */
    fun getVendList(): List<Triple<String, Product, Int>> {
        return _cartItems.value
            .filter { it.assignedCoil != null }
            .flatMap { item ->
                // Repeat coilId for each quantity
                List(item.quantity) {
                    Triple(item.assignedCoil!!.id, item.product, 1)
                }
            }
    }
}
