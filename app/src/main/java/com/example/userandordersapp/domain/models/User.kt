package com.example.userandordersapp.domain.models

/**
 * Representa un usuario del sistema.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String?,
    val createdAt: Long
)
