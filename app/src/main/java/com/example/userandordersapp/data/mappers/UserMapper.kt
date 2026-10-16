package com.example.userandordersapp.data.mappers

import com.example.userandordersapp.data.database.entities.UserEntity
import com.example.userandordersapp.data.models.UserDto
import com.example.userandordersapp.domain.models.User

fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    avatar = avatar,
    createdAt = createdAt
)

fun User.toDto(): UserDto = UserDto(
    id = id,
    name = name,
    email = email,
    avatar = avatar,
    createdAt = createdAt
)

fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email,
    avatar = avatar,
    createdAt = createdAt
)

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    avatar = avatar,
    createdAt = createdAt
)
