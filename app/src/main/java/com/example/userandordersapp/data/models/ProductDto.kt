package com.example.userandordersapp.data.models

import kotlinx.serialization.Serializable

/**
 * Representación JSON de un producto en la API remota.
 */
@Serializable
data class ProductDto(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stock: Int
)
