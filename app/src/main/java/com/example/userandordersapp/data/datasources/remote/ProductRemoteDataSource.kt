package com.example.userandordersapp.data.datasources.remote

import com.example.userandordersapp.data.datasources.remote.api.ProductApiService
import com.example.userandordersapp.data.models.ProductDto
import javax.inject.Inject

/**
 * API remota de productos.
 */
class ProductRemoteDataSource @Inject constructor(
    private val productApiService: ProductApiService
) {
    suspend fun getProducts(): List<ProductDto> = productApiService.getProducts()

    suspend fun getProductById(id: String): ProductDto = productApiService.getProductById(id)

    suspend fun searchProducts(query: String): List<ProductDto> = productApiService.searchProducts(query)
}
