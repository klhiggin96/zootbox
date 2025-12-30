package com.example.myapplication

import android.app.Application
import com.example.myapplication.cart.CartManager
import com.example.myapplication.database.InventoryRepository

/**
 * Application class for ZootBox
 *
 * Manages singleton instances:
 * - CartManager (shopping cart state)
 * - InventoryRepository (database access)
 */
class CartApplication : Application() {

    lateinit var cartManager: CartManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize cart manager
        val inventoryRepo = InventoryRepository.getInstance(this)
        cartManager = CartManager(inventoryRepo)
    }

    companion object {
        private lateinit var instance: CartApplication

        fun getInstance(): CartApplication = instance
    }
}
