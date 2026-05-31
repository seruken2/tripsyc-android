package com.tripwave.app.navigation

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
import com.tripwave.app.data.api.ApiClient
import com.tripwave.app.data.api.models.User
import com.tripwave.app.data.prefs.TripPrefsStore
import com.tripwave.app.ui.auth.AuthViewModel
import com.tripwave.app.ui.auth.LoginScreen
import com.tripwave.app.ui.auth.OtpScreen
import com.tripwave.app.ui.main.MainScreen
import com.tripwave.app.ui.onboarding.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
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
fun AppNavigation(
    pendingTripId: String? = null,
    pendingDeepLink: String? = null
) {
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
            // First-run users land on the four-page Welcome carousel
            // before the login screen. Once they tap Get Started we
            // mark the flag so they go straight to Login next time.
            if (TripPrefsStore.welcomeSeen()) Screen.Login.route
            else Screen.Welcome.route
        }
    }

    if (startDestination == null) {
        // Show splash/loading while checking auth
        com.tripwave.app.ui.common.LoadingScreen()
        return
    }

    // After auth determined, handle pending deep link/trip navigation
    LaunchedEffect(startDestination) {
        if (startDestination == Screen.Main.route) {
            // Handle notification tripId — navigate to that trip
            if (pendingTripId != null) {
                navController.navigate(Screen.TripDetail.createRoute(pendingTripId))
            }
            // Handle deep link URIs
            if (pendingDeepLink != null) {
                val uri = Uri.parse(pendingDeepLink)
                when {
                    uri.scheme == "tripwave" && uri.host == "join" -> {
                        // tripwave://join/{tripId}?code=X
                        val tripId = uri.pathSegments.firstOrNull()
                        if (tripId != null) {
                            navController.navigate(Screen.TripDetail.createRoute(tripId))
                        }
                    }
                    // tripwave://verify handled by auth flow — no nav needed post-login
                    else -> { /* no-op */ }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    TripPrefsStore.markWelcomeSeen()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

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

        composable(
            route = Screen.TripDetail.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            // Load trip and show TripDetailScreen
            var trip by remember { mutableStateOf<com.tripwave.app.data.api.models.Trip?>(null) }
            var loadFailed by remember { mutableStateOf(false) }
            var reloadKey by remember { mutableStateOf(0) }
            LaunchedEffect(tripId, reloadKey) {
                loadFailed = false
                try {
                    trip = withContext(Dispatchers.IO) { ApiClient.apiService.getTrip(tripId) }
                } catch (_: Exception) {
                    // Surface the failure — the catch used to swallow it
                    // silently, stranding the user on an endless spinner.
                    loadFailed = true
                }
            }
            val loadedTrip = trip
            when {
                loadedTrip != null -> com.tripwave.app.ui.trip.TripDetailScreen(
                    trip = loadedTrip,
                    currentUser = currentUser,
                    onBack = { navController.popBackStack() }
                )
                loadFailed -> com.tripwave.app.ui.common.ErrorView(
                    message = "Couldn't load this trip. Check your connection and try again.",
                    onRetry = { reloadKey++ }
                )
                else -> com.tripwave.app.ui.common.LoadingScreen()
            }
        }
    }
}
