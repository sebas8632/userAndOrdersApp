package com.example.userandordersapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla local de usuarios.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatar: String?,
    val createdAt: Long
)
