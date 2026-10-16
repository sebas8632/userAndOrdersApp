package com.example.userandordersapp.domain.models

/**
 * Representa un producto en el catálogo.
 */
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stock: Int
)
