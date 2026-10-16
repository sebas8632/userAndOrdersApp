package com.example.userandordersapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.userandordersapp.data.database.Converters
import com.example.userandordersapp.domain.models.OrderStatus

/**
 * Tabla local de órdenes. La lista de productos se guarda como JSON.
 */
@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val products: List<ProductEntity>,
    val totalAmount: Double,
    val status: OrderStatus,
    val createdAt: Long
)
