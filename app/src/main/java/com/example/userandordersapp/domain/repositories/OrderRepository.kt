package com.example.userandordersapp.domain.repositories

import com.example.userandordersapp.domain.models.Order

interface OrderRepository {

    /** Obtiene órdenes de un usuario. */
    suspend fun getOrders(userId: String): List<Order>

    /** Crea una nueva orden. */
    suspend fun createOrder(order: Order): Order

    /** Obtiene orden por ID. */
    suspend fun getOrderById(id: String): Order
}
