package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var productRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        productRecycler = findViewById(R.id.product_recycler)
        
        // Start hardware service
        try {
            val serviceIntent = Intent(this, com.example.myapplication.hardware.HardwareService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                @Suppress("DEPRECATION")
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            statusText.text = "Service Error: ${e.message}"
        }

        // Setup ZootBox Product Grid (2 Columns)
        productRecycler.layoutManager = GridLayoutManager(this, 2)
        
        Toast.makeText(this, "USB Connection Active", Toast.LENGTH_LONG).show()

        // Create fixed list of 10 slots mapping to Hardware Row 1, Cols 1-10
        // Plus one Digital Donation item
        // Note: videoFileName assumes files are in /storage/emulated/0/Movies/ZootBox/
        val products = listOf(
            Product("01", "ZOOT VAPE X", "1", "1", R.drawable.zyn, price = 29.99, ageRestriction = 21, videoFileName = "zoot_vape_x.mp4"),
            Product("02", "NIGHT OWL CAM", "1", "2", R.drawable.zyn, price = 15.99, videoFileName = "night_owl_cam.mp4"),
            Product("03", "ZYN CITRUS", "1", "3", R.drawable.img_zyn_citrus, backgroundRes = R.drawable.bg_zyn_citrus_gradient, price = 8.99, ageRestriction = 21, scaleX = 0.95f, scaleY = 0.95f, videoFileName = "zyn_citrus.mp4"),
            Product("04", "RED BULL 12OZ", "1", "4", R.drawable.zyn, price = 4.99, videoFileName = "red_bull.mp4"),
            Product("05", "LIGHTER GOLD", "1", "5", R.drawable.zyn, price = 2.99, ageRestriction = 18, videoFileName = "lighter_gold.mp4"),
            Product("06", "ROLLING PAPERS", "1", "6", R.drawable.zyn, price = 3.99, ageRestriction = 18, videoFileName = "rolling_papers.mp4"),
            Product("07", "ENERGY SHOT", "1", "7", R.drawable.zyn, price = 3.49, videoFileName = "energy_shot.mp4"),
            Product("08", "GUM MINT", "1", "8", R.drawable.zyn, price = 1.99, videoFileName = "gum_mint.mp4"),
            Product("09", "CONDOM PACK", "1", "9", R.drawable.zyn, price = 5.99, videoFileName = "condom_pack.mp4"),
            Product("10", "WATER 500ML", "1", "10", R.drawable.zyn, price = 2.49, videoFileName = "water.mp4"),
            // Digital Item
            Product("11", "Donate to the Autism Fund", "0", "0", R.drawable.img_donate, isDigital = true, price = 5.00, videoFileName = "donate.mp4")
        )

        val adapter = ProductAdapter(products) { product ->
            // Launch Product Detail Activity
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("name", product.name)
            intent.putExtra("row", product.row)
            intent.putExtra("col", product.col)
            intent.putExtra("imageRes", product.imageRes)
            intent.putExtra("backgroundRes", product.backgroundRes)
            intent.putExtra("isDigital", product.isDigital)
            intent.putExtra("price", product.price)
            intent.putExtra("ageRestriction", product.ageRestriction ?: -1)
            intent.putExtra("desc", if(product.isDigital) "Support the cause. All proceeds go directly to the Autism Fund." else "Premium nightlife selection. High quality.")
            intent.putExtra("videoFileName", product.videoFileName)
            startActivity(intent)
        }
        productRecycler.adapter = adapter
    }
}