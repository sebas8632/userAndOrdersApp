package com.example.userandordersapp.data.datasources.remote

import com.example.userandordersapp.data.datasources.remote.api.UserApiService
import com.example.userandordersapp.data.models.UserDto
import javax.inject.Inject

/**
 * API remota de usuarios.
 */
class UserRemoteDataSource @Inject constructor(
    private val userApiService: UserApiService
) {
    suspend fun getUsers(): List<UserDto> = userApiService.getUsers()

    suspend fun getUserById(id: String): UserDto = userApiService.getUserById(id)

    suspend fun createUser(user: UserDto): UserDto = userApiService.createUser(user)

    suspend fun updateUser(id: String, user: UserDto): UserDto = userApiService.updateUser(id, user)

    suspend fun deleteUser(id: String): Boolean = userApiService.deleteUser(id).isSuccessful
}
