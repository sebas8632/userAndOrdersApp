package com.example.userandordersapp.domain.repositories

import com.example.userandordersapp.domain.models.Product

interface ProductRepository {

    /** Obtiene lista de productos. */
    suspend fun getProducts(): List<Product>

    /** Obtiene un producto por ID. */
    suspend fun getProductById(id: String): Product

    /** Busca productos por término. */
    suspend fun searchProducts(query: String): List<Product>
}
