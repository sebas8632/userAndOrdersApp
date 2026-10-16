package com.example.userandordersapp.data.datasources.local

import com.example.userandordersapp.data.database.entities.ProductEntity
import com.example.userandordersapp.data.datasources.local.dao.ProductDao
import javax.inject.Inject

/**
 * Almacenamiento local de productos (caché).
 */
class ProductLocalDataSource @Inject constructor(
    private val productDao: ProductDao
) {
    suspend fun getAll(): List<ProductEntity> = productDao.getAll()

    suspend fun getById(id: String): ProductEntity? = productDao.getById(id)

    suspend fun search(query: String): List<ProductEntity> = productDao.search(query)

    suspend fun insertAll(products: List<ProductEntity>) = productDao.insertAll(products)
}
