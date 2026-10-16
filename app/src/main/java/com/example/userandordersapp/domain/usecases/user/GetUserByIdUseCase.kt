package com.example.userandordersapp.domain.usecases.user

import com.example.userandordersapp.domain.models.User
import com.example.userandordersapp.domain.repositories.UserRepository
import javax.inject.Inject

/**
 * Obtiene un usuario específico por ID.
 */
class GetUserByIdUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): User = userRepository.getUserById(userId)
}
