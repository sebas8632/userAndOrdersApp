package com.example.userandordersapp.domain.usecases.user

import com.example.userandordersapp.domain.models.User
import com.example.userandordersapp.domain.repositories.UserRepository
import javax.inject.Inject

/**
 * Actualiza datos de un usuario.
 */
class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(id: String, user: User): User = userRepository.updateUser(id, user)
}
