package com.example.userandordersapp.data.datasources.remote.api

import com.example.userandordersapp.data.models.OrderDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OrderApiService {

    @GET("users/{userId}/orders")
    suspend fun getOrders(@Path("userId") userId: String): List<OrderDto>

    @POST("orders")
    suspend fun createOrder(@Body order: OrderDto): OrderDto

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): OrderDto
}
