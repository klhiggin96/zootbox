package com.example.myapplication

data class Category(
    val id: String,
    val name: String,  // "ZYNS", "CIGARETTES", "VAPES", "ZB LEGENDARY LOOT"
    val imageRes: Int,
    val backgroundRes: Int = R.drawable.bg_card_gradient
)
