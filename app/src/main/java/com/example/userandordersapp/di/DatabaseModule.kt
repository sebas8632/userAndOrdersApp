package com.example.userandordersapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.userandordersapp.data.database.AppDatabase
import com.example.userandordersapp.data.database.SeedData
import com.example.userandordersapp.data.datasources.local.dao.OrderDao
import com.example.userandordersapp.data.datasources.local.dao.ProductDao
import com.example.userandordersapp.data.datasources.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "user_and_orders.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    SeedData.statements.forEach { db.execSQL(it) }
                }
            })
            .build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()

    @Provides
    fun provideOrderDao(database: AppDatabase): OrderDao = database.orderDao()
}
