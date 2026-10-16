package com.example.userandordersapp.domain.usecases.product

import com.example.userandordersapp.domain.models.Product
import com.example.userandordersapp.domain.repositories.ProductRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Busca productos por término, con caché en memoria por consulta.
 *
 * Es `@Singleton` para que la caché sobreviva entre usos del caso de uso.
 */
@Singleton
class SearchProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    private val cache = ConcurrentHashMap<String, List<Product>>()

    suspend operator fun invoke(query: String): List<Product> {
        val key = query.trim().lowercase()
        cache[key]?.let { return it }
        val results = productRepository.searchProducts(query)
        cache[key] = results
        return results
    }
}
