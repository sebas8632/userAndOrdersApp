package com.example.userandordersapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Tabla local de productos (caché).
 *
 * `@Serializable` permite guardar la lista de productos de una orden como JSON.
 */
@Serializable
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stock: Int
)
