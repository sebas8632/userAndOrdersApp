package com.example.userandordersapp.data.models

import kotlinx.serialization.Serializable

/**
 * Representación JSON de un usuario en la API remota.
 */
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val createdAt: Long
)
