package com.tripsyc.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.auth.AuthViewModel
import com.tripsyc.app.ui.auth.LoginScreen
import com.tripsyc.app.ui.auth.OtpScreen
import com.tripsyc.app.ui.main.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp/{email}") {
        fun createRoute(email: String) = "otp/$email"
    }
    object Main : Screen("main")
    object TripDetail : Screen("trip/{tripId}") {
        fun createRoute(tripId: String) = "trip/$tripId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    var startDestination by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        // Check if the user has an active session
        val hasSession = withContext(Dispatchers.IO) {
            ApiClient.hasSession()
        }
        startDestination = if (hasSession) {
            // Try to validate session
            try {
                val user = withContext(Dispatchers.IO) {
                    val response = ApiClient.apiService.getCurrentUser()
                    response.body()
                }
                if (user != null) {
                    currentUser = user
                    Screen.Main.route
                } else {
                    ApiClient.clearSession()
                    Screen.Login.route
                }
            } catch (e: Exception) {
                Screen.Login.route
            }
        } else {
            Screen.Login.route
        }
    }

    if (startDestination == null) {
        // Show splash/loading while checking auth
        com.tripsyc.app.ui.common.LoadingScreen()
        return
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onOtpSent = { email ->
                    navController.navigate(Screen.Otp.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OtpScreen(
                email = email,
                viewModel = authViewModel,
                onVerified = { user ->
                    currentUser = user
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                initialUser = currentUser,
                onLogout = {
                    ApiClient.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
