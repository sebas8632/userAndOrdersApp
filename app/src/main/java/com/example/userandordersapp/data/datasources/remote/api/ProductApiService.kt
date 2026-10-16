package com.example.userandordersapp.data.datasources.remote.api

import com.example.userandordersapp.data.models.ProductDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {

    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: String): ProductDto

    @GET("products/search")
    suspend fun searchProducts(@Query("q") query: String): List<ProductDto>
}
