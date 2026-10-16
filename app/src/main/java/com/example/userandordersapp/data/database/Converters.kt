package com.example.userandordersapp.data.database

import androidx.room.TypeConverter
import com.example.userandordersapp.data.database.entities.ProductEntity
import kotlinx.serialization.json.Json

/**
 * Convierte la lista de productos de una orden a JSON para guardarla en Room.
 */
class Converters {

    @TypeConverter
    fun fromProducts(products: List<ProductEntity>): String = Json.encodeToString(products)

    @TypeConverter
    fun toProducts(value: String): List<ProductEntity> = Json.decodeFromString(value)
}
