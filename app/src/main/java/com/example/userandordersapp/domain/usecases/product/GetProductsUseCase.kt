package com.example.userandordersapp.domain.usecases.product

import com.example.userandordersapp.domain.models.Product
import com.example.userandordersapp.domain.repositories.ProductRepository
import javax.inject.Inject

/**
 * Obtiene lista de productos disponibles.
 */
class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(): List<Product> = productRepository.getProducts()
}
