package com.example.userandordersapp.data.repositories

import com.example.userandordersapp.data.datasources.local.ProductLocalDataSource
import com.example.userandordersapp.data.datasources.remote.ProductRemoteDataSource
import com.example.userandordersapp.data.mappers.toDomain
import com.example.userandordersapp.data.mappers.toEntity
import com.example.userandordersapp.domain.models.Product
import com.example.userandordersapp.domain.repositories.ProductRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Lee de la API remota y guarda en local (caché). Si la red falla, usa la caché local.
 */
class ProductRepositoryImpl @Inject constructor(
    private val remote: ProductRemoteDataSource,
    private val local: ProductLocalDataSource
) : ProductRepository {

    override suspend fun getProducts(): List<Product> =
        try {
            remote.getProducts().also { local.insertAll(it.map { dto -> dto.toEntity() }) }.map { it.toDomain() }
        } catch (e: IOException) {
            local.getAll().takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: throw e
        }

    override suspend fun getProductById(id: String): Product =
        try {
            remote.getProductById(id).also { local.insertAll(listOf(it.toEntity())) }.toDomain()
        } catch (e: IOException) {
            local.getById(id)?.toDomain() ?: throw e
        }

    override suspend fun searchProducts(query: String): List<Product> =
        try {
            remote.searchProducts(query).also { local.insertAll(it.map { dto -> dto.toEntity() }) }.map { it.toDomain() }
        } catch (e: IOException) {
            local.search(query).takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: throw e
        }
}
