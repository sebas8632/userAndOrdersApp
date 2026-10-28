package com.example.userandordersapp.presentation.ui.screens.home

import com.example.userandordersapp.domain.models.User

data class HomeUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
