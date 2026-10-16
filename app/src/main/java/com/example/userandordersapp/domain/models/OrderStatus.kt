package com.example.userandordersapp.domain.models

/**
 * Estado de una orden de compra.
 */
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
