package com.example.userandordersapp.domain.usecases.order

import com.example.userandordersapp.domain.models.Order
import com.example.userandordersapp.domain.repositories.OrderRepository
import javax.inject.Inject

/**
 * Crea una nueva orden con validación.
 *
 * @throws IllegalArgumentException si la orden no tiene productos o su total no es mayor que 0.
 */
class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(order: Order): Order {
        require(order.products.isNotEmpty()) { "La orden debe tener al menos un producto" }
        require(order.totalAmount > 0) { "El total de la orden debe ser mayor que 0" }
        return orderRepository.createOrder(order)
    }
}
