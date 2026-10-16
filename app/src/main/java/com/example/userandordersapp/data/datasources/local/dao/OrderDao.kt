package com.example.userandordersapp.data.datasources.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.userandordersapp.data.database.entities.OrderEntity

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders WHERE userId = :userId")
    suspend fun getAllByUser(userId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)
}
