package com.example.userandordersapp.data.datasources.local

import com.example.userandordersapp.data.database.entities.OrderEntity
import com.example.userandordersapp.data.datasources.local.dao.OrderDao
import javax.inject.Inject

/**
 * Almacenamiento local de órdenes.
 */
class OrderLocalDataSource @Inject constructor(
    private val orderDao: OrderDao
) {
    suspend fun getAll(userId: String): List<OrderEntity> = orderDao.getAllByUser(userId)

    suspend fun getById(id: String): OrderEntity? = orderDao.getById(id)

    suspend fun insert(order: OrderEntity) = orderDao.insert(order)
}
