package com.example.userandordersapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.userandordersapp.data.database.entities.OrderEntity
import com.example.userandordersapp.data.database.entities.ProductEntity
import com.example.userandordersapp.data.database.entities.UserEntity
import com.example.userandordersapp.data.datasources.local.dao.OrderDao
import com.example.userandordersapp.data.datasources.local.dao.ProductDao
import com.example.userandordersapp.data.datasources.local.dao.UserDao

@Database(
    entities = [UserEntity::class, ProductEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun productDao(): ProductDao

    abstract fun orderDao(): OrderDao
}
