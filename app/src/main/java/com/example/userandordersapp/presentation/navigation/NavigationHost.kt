package com.example.userandordersapp.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.userandordersapp.presentation.ui.screens.details.DetailsScreen
import com.example.userandordersapp.presentation.ui.screens.home.HomeScreen
import com.example.userandordersapp.presentation.ui.screens.login.LoginScreen

@Composable
fun NavigationHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onUserClick = { userId ->
                    navController.navigate(Screen.Details.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            DetailsScreen(onBack = { navController.popBackStack() })
        }
    }
}
