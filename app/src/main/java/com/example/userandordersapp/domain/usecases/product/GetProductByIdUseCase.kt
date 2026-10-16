package com.example.userandordersapp.domain.usecases.product

import com.example.userandordersapp.domain.models.Product
import com.example.userandordersapp.domain.repositories.ProductRepository
import javax.inject.Inject

/**
 * Obtiene detalles de un producto.
 */
class GetProductByIdUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Product = productRepository.getProductById(productId)
}
