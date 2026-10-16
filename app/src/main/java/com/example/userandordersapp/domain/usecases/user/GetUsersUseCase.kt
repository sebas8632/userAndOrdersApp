package com.example.userandordersapp.domain.usecases.user

import com.example.userandordersapp.domain.models.User
import com.example.userandordersapp.domain.repositories.UserRepository
import javax.inject.Inject

/**
 * Obtiene lista de todos los usuarios.
 */
class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<User> = userRepository.getUsers()
}
