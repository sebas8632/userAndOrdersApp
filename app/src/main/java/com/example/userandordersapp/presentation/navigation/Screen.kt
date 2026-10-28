package com.example.userandordersapp.presentation.navigation

/**
 * Rutas de la app. [Details] recibe el ID del usuario como argumento.
 */
sealed class Screen(val route: String) {

    data object Login : Screen("login")

    data object Home : Screen("home")

    data object Details : Screen("details/{userId}") {
        fun createRoute(userId: String) = "details/$userId"
    }
}
