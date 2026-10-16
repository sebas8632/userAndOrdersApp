package com.example.userandordersapp.domain.usecases.user

import com.example.userandordersapp.domain.models.User
import com.example.userandordersapp.domain.repositories.UserRepository
import javax.inject.Inject

/**
 * Crea un nuevo usuario con validaciones.
 *
 * @throws IllegalArgumentException si el nombre o el email están vacíos.
 */
class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): User {
        require(user.name.isNotBlank()) { "El nombre no puede estar vacío" }
        require(user.email.isNotBlank()) { "El email no puede estar vacío" }
        return userRepository.createUser(user)
    }
}
