package com.example.userandordersapp.presentation.ui.screens.login

/**
 * Estado de la pantalla de login.
 *
 * [loggedInUserId] se llena cuando el login es exitoso; la vista navega y luego lo limpia.
 */
data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUserId: String? = null
)
