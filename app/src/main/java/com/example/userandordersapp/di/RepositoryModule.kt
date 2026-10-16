package com.example.userandordersapp.di

import com.example.userandordersapp.data.repositories.OrderRepositoryImpl
import com.example.userandordersapp.data.repositories.ProductRepositoryImpl
import com.example.userandordersapp.data.repositories.UserRepositoryImpl
import com.example.userandordersapp.domain.repositories.OrderRepository
import com.example.userandordersapp.domain.repositories.ProductRepository
import com.example.userandordersapp.domain.repositories.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository
}
