package com.example.userandordersapp.data.mappers

import com.example.userandordersapp.data.database.entities.ProductEntity
import com.example.userandordersapp.data.models.ProductDto
import com.example.userandordersapp.domain.models.Product

fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    price = price,
    imageUrl = imageUrl,
    stock = stock
)

fun Product.toDto(): ProductDto = ProductDto(
    id = id,
    title = title,
    description = description,
    price = price,
    imageUrl = imageUrl,
    stock = stock
)

fun ProductDto.toEntity(): ProductEntity = ProductEntity(
    id = id,
    title = title,
    description = description,
    price = price,
    imageUrl = imageUrl,
    stock = stock
)

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    price = price,
    imageUrl = imageUrl,
    stock = stock
)
