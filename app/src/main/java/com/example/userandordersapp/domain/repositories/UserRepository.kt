package com.example.userandordersapp.domain.repositories

import com.example.userandordersapp.domain.models.User

interface UserRepository {

    /** Obtiene lista de usuarios. */
    suspend fun getUsers(): List<User>

    /** Obtiene un usuario por ID. */
    suspend fun getUserById(id: String): User

    /** Crea un nuevo usuario. */
    suspend fun createUser(user: User): User

    /** Actualiza un usuario existente. */
    suspend fun updateUser(id: String, user: User): User

    /** Elimina un usuario. */
    suspend fun deleteUser(id: String): Boolean
}
