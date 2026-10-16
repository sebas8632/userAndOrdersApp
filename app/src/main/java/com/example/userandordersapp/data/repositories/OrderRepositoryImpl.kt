package com.example.userandordersapp.data.repositories

import com.example.userandordersapp.data.datasources.local.OrderLocalDataSource
import com.example.userandordersapp.data.datasources.remote.OrderRemoteDataSource
import com.example.userandordersapp.data.mappers.toDomain
import com.example.userandordersapp.data.mappers.toDto
import com.example.userandordersapp.data.mappers.toEntity
import com.example.userandordersapp.domain.models.Order
import com.example.userandordersapp.domain.repositories.OrderRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Lee de la API remota y guarda en local. Si la red falla, usa la caché local.
 * Crear una orden no usa caché: si falla, el error se propaga.
 */
class OrderRepositoryImpl @Inject constructor(
    private val remote: OrderRemoteDataSource,
    private val local: OrderLocalDataSource
) : OrderRepository {

    override suspend fun getOrders(userId: String): List<Order> =
        try {
            remote.getOrders(userId).onEach { local.insert(it.toEntity()) }.map { it.toDomain() }
        } catch (e: IOException) {
            local.getAll(userId).takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: throw e
        }

    override suspend fun createOrder(order: Order): Order =
        remote.createOrder(order.toDto()).also { local.insert(it.toEntity()) }.toDomain()

    override suspend fun getOrderById(id: String): Order =
        try {
            remote.getOrderById(id).also { local.insert(it.toEntity()) }.toDomain()
        } catch (e: IOException) {
            local.getById(id)?.toDomain() ?: throw e
        }
}
