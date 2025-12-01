package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

data class Product(
    val id: String,
    val name: String,
    val row: String,
    val col: String,
    val imageRes: Int,
    val backgroundRes: Int = R.drawable.bg_card_gradient,
    val isDigital: Boolean = false,
    val price: Double = 0.0,
    val ageRestriction: Int? = null,
    val scaleX: Float = 1.71f,
    val scaleY: Float = 1.76f,
    val videoFileName: String? = null
)

class ProductAdapter(
    private val products: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        // Inflate the new card layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product, onProductClick)
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind to the new views in item_product_card.xml
        private val headerText: TextView = itemView.findViewById(R.id.headerText)
        private val imgProduct: ImageView = itemView.findViewById(R.id.productImage)
        private val tvDescription: TextView = itemView.findViewById(R.id.productDescription)
        private val tvPrice: TextView = itemView.findViewById(R.id.productPrice)

        fun bind(product: Product, onClick: (Product) -> Unit) {
            // Set static header or dynamic if needed (Design has "zb")
            headerText.text = "zb"

            // Set background
            // The background is on the first child ConstraintLayout inside the CardView
            // Let's access it. The layout hierarchy is MaterialCardView -> ConstraintLayout -> ...
            // We can find the ConstraintLayout by finding its children or by ID if we added one.
            // In item_product_card.xml, the first child constraint layout doesn't have an ID. 
            // But we can set it on the parent if we moved the background there, or traverse.
            // Better yet, let's assume the background is set on a specific view.
            // In the updated layout, line 16 of item_product_card.xml:
            // <androidx.constraintlayout.widget.ConstraintLayout ... android:background="@drawable/bg_card_gradient">
            // We need to set the background of *that* view.
            // Let's assume we will add an ID to it.
            val bgContainer = itemView.findViewById<View>(R.id.backgroundContainer)
            bgContainer?.setBackgroundResource(product.backgroundRes)

            // Set product name as description
            tvDescription.text = product.name

            // Format and set price
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            tvPrice.text = currencyFormat.format(product.price)

            // Load product image
            imgProduct.setImageResource(product.imageRes)
            imgProduct.scaleX = product.scaleX
            imgProduct.scaleY = product.scaleY
            
            itemView.setOnClickListener { onClick(product) }
        }
    }
}
