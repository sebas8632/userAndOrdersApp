package com.example.userandordersapp.data.datasources.local

import com.example.userandordersapp.data.database.entities.UserEntity
import com.example.userandordersapp.data.datasources.local.dao.UserDao
import javax.inject.Inject

/**
 * Almacenamiento local de usuarios.
 */
class UserLocalDataSource @Inject constructor(
    private val userDao: UserDao
) {
    suspend fun getAll(): List<UserEntity> = userDao.getAll()

    suspend fun getById(id: String): UserEntity? = userDao.getById(id)

    suspend fun insert(user: UserEntity) = userDao.insert(user)

    suspend fun update(user: UserEntity): Boolean = userDao.update(user) > 0

    suspend fun delete(id: String): Boolean = userDao.delete(id) > 0
}
