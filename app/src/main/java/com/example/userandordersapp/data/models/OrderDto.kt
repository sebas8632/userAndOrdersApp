package com.example.userandordersapp.data.models

import com.example.userandordersapp.domain.models.OrderStatus
import kotlinx.serialization.Serializable

/**
 * Representación JSON de una orden en la API remota.
 */
@Serializable
data class OrderDto(
    val id: String,
    val userId: String,
    val products: List<ProductDto>,
    val totalAmount: Double,
    val status: OrderStatus,
    val createdAt: Long
)
