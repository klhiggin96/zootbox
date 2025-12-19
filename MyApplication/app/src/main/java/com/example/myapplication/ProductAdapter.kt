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
    val videoFileName: String? = null,
    val category: String = "General"
)

class ProductAdapter(
    private val products: List<Product>,
    private val itemLayoutRes: Int = R.layout.item_product_card,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        // Inflate the new card layout
        val view = LayoutInflater.from(parent.context)
            .inflate(itemLayoutRes, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product, onProductClick)
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind to the new views in item_product_card.xml
        // Views might be null in simplified layout, use nullable types
        private val headerText: TextView? = itemView.findViewById(R.id.headerText)
        private val imgProduct: ImageView? = itemView.findViewById(R.id.productImage)
        private val tvDescription: TextView? = itemView.findViewById(R.id.productDescription)
        private val tvPrice: TextView? = itemView.findViewById(R.id.productPrice)

        fun bind(product: Product, onClick: (Product) -> Unit) {
            // Set static header or dynamic if needed (Design has "zb")
            headerText?.text = "zb"
            
            // Set background
            val bgContainer = itemView.findViewById<View>(R.id.backgroundContainer)
            bgContainer?.setBackgroundResource(product.backgroundRes)

            tvDescription?.text = product.name

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            tvPrice?.text = currencyFormat.format(product.price)

            imgProduct?.setImageResource(product.imageRes)
            imgProduct?.scaleX = product.scaleX
            imgProduct?.scaleY = product.scaleY
            
            itemView.setOnClickListener { onClick(product) }
        }
    }
}