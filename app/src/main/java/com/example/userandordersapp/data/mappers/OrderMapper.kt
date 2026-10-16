package com.example.userandordersapp.data.mappers

import com.example.userandordersapp.data.database.entities.OrderEntity
import com.example.userandordersapp.data.models.OrderDto
import com.example.userandordersapp.domain.models.Order

fun OrderDto.toDomain(): Order = Order(
    id = id,
    userId = userId,
    products = products.map { it.toDomain() },
    totalAmount = totalAmount,
    status = status,
    createdAt = createdAt
)

fun Order.toDto(): OrderDto = OrderDto(
    id = id,
    userId = userId,
    products = products.map { it.toDto() },
    totalAmount = totalAmount,
    status = status,
    createdAt = createdAt
)

fun OrderDto.toEntity(): OrderEntity = OrderEntity(
    id = id,
    userId = userId,
    products = products.map { it.toEntity() },
    totalAmount = totalAmount,
    status = status,
    createdAt = createdAt
)

fun OrderEntity.toDomain(): Order = Order(
    id = id,
    userId = userId,
    products = products.map { it.toDomain() },
    totalAmount = totalAmount,
    status = status,
    createdAt = createdAt
)
