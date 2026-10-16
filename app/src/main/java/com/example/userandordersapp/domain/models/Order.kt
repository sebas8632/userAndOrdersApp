package com.example.userandordersapp.domain.models

/**
 * Representa una orden de compra.
 */
data class Order(
    val id: String,
    val userId: String,
    val products: List<Product>,
    val totalAmount: Double,
    val status: OrderStatus,
    val createdAt: Long
)
