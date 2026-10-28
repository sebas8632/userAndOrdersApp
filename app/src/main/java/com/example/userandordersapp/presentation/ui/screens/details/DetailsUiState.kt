package com.example.userandordersapp.presentation.ui.screens.details

import com.example.userandordersapp.domain.models.Order
import com.example.userandordersapp.domain.models.User

data class DetailsUiState(
    val user: User? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
