package com.example.userandordersapp.data.datasources.remote

import com.example.userandordersapp.data.datasources.remote.api.OrderApiService
import com.example.userandordersapp.data.models.OrderDto
import javax.inject.Inject

/**
 * API remota de órdenes.
 */
class OrderRemoteDataSource @Inject constructor(
    private val orderApiService: OrderApiService
) {
    suspend fun getOrders(userId: String): List<OrderDto> = orderApiService.getOrders(userId)

    suspend fun createOrder(order: OrderDto): OrderDto = orderApiService.createOrder(order)

    suspend fun getOrderById(id: String): OrderDto = orderApiService.getOrderById(id)
}
