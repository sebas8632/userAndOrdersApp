package com.example.userandordersapp.domain.usecases.order

import com.example.userandordersapp.domain.models.Order
import com.example.userandordersapp.domain.repositories.OrderRepository
import javax.inject.Inject

/**
 * Obtiene las órdenes de un usuario.
 */
class GetUserOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(userId: String): List<Order> = orderRepository.getOrders(userId)
}
