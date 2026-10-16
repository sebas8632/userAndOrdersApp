package com.example.userandordersapp.data.repositories

import com.example.userandordersapp.data.datasources.local.UserLocalDataSource
import com.example.userandordersapp.data.datasources.remote.UserRemoteDataSource
import com.example.userandordersapp.data.mappers.toDomain
import com.example.userandordersapp.data.mappers.toDto
import com.example.userandordersapp.data.mappers.toEntity
import com.example.userandordersapp.domain.models.User
import com.example.userandordersapp.domain.repositories.UserRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Lee de la API remota y guarda en local. Si la red falla, usa la caché local.
 * Las escrituras no usan caché: si fallan, el error se propaga.
 */
class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val local: UserLocalDataSource
) : UserRepository {

    override suspend fun getUsers(): List<User> =
        try {
            remote.getUsers().onEach { local.insert(it.toEntity()) }.map { it.toDomain() }
        } catch (e: IOException) {
            local.getAll().takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: throw e
        }

    override suspend fun getUserById(id: String): User =
        try {
            remote.getUserById(id).also { local.insert(it.toEntity()) }.toDomain()
        } catch (e: IOException) {
            local.getById(id)?.toDomain() ?: throw e
        }

    override suspend fun createUser(user: User): User =
        remote.createUser(user.toDto()).also { local.insert(it.toEntity()) }.toDomain()

    override suspend fun updateUser(id: String, user: User): User =
        remote.updateUser(id, user.toDto()).also { local.insert(it.toEntity()) }.toDomain()

    override suspend fun deleteUser(id: String): Boolean {
        val deleted = remote.deleteUser(id)
        if (deleted) local.delete(id)
        return deleted
    }
}
