package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.cart.CartManager
import java.text.NumberFormat
import java.util.Locale

/**
 * RecyclerView Adapter for Shopping Cart Items
 */
class CartAdapter(
    private val onQuantityChanged: (itemId: String, newQuantity: Int) -> Unit,
    private val onRemoveItem: (itemId: String) -> Unit
) : ListAdapter<CartManager.CartItem, CartAdapter.CartItemViewHolder>(CartItemDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartItemViewHolder(view, onQuantityChanged, onRemoveItem, currencyFormatter)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CartItemViewHolder(
        itemView: View,
        private val onQuantityChanged: (itemId: String, newQuantity: Int) -> Unit,
        private val onRemoveItem: (itemId: String) -> Unit,
        private val currencyFormatter: NumberFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val imageProduct: ImageView = itemView.findViewById(R.id.image_product)
        private val textProductName: TextView = itemView.findViewById(R.id.text_product_name)
        private val textProductPrice: TextView = itemView.findViewById(R.id.text_product_price)
        private val textQuantity: TextView = itemView.findViewById(R.id.text_quantity)
        private val textItemTotal: TextView = itemView.findViewById(R.id.text_item_total)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btn_quantity_minus)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btn_quantity_plus)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove)
        private val textCoilInfo: TextView = itemView.findViewById(R.id.text_coil_info)

        fun bind(item: CartManager.CartItem) {
            textProductName.text = item.product.name
            textProductPrice.text = "${currencyFormatter.format(item.unitPrice)} each"
            textQuantity.text = item.quantity.toString()
            textItemTotal.text = currencyFormatter.format(item.totalPrice)

            // Show coil assignment
            if (item.assignedCoil != null) {
                textCoilInfo.text = "Coil ${item.assignedCoil.id} • ${item.assignedCoil.inventory} in stock"
                textCoilInfo.visibility = View.VISIBLE
            } else {
                textCoilInfo.visibility = View.GONE
            }

            // Quantity controls
            btnMinus.setOnClickListener {
                if (item.quantity > 1) {
                    onQuantityChanged(item.id, item.quantity - 1)
                }
            }

            btnPlus.setOnClickListener {
                // Check inventory limit
                val maxQuantity = item.assignedCoil?.inventory ?: Int.MAX_VALUE
                if (item.quantity < maxQuantity) {
                    onQuantityChanged(item.id, item.quantity + 1)
                }
            }

            btnRemove.setOnClickListener {
                onRemoveItem(item.id)
            }

            // Disable minus button if quantity is 1
            btnMinus.isEnabled = item.quantity > 1
            btnMinus.alpha = if (item.quantity > 1) 1.0f else 0.5f

            // Disable plus button if at max inventory
            val atMaxInventory = item.assignedCoil?.let { item.quantity >= it.inventory } ?: false
            btnPlus.isEnabled = !atMaxInventory
            btnPlus.alpha = if (atMaxInventory) 0.5f else 1.0f
        }
    }

    class CartItemDiffCallback : DiffUtil.ItemCallback<CartManager.CartItem>() {
        override fun areItemsTheSame(
            oldItem: CartManager.CartItem,
            newItem: CartManager.CartItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: CartManager.CartItem,
            newItem: CartManager.CartItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
