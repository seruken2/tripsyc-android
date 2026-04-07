package com.tripsyc.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.calendar.CalendarScreen
import com.tripsyc.app.ui.profile.ProfileScreen
import com.tripsyc.app.ui.theme.Coral
import com.tripsyc.app.ui.theme.Chalk200
import com.tripsyc.app.ui.trips.TripsListScreen
import com.tripsyc.app.ui.trips.TripsViewModel

enum class MainTab { Trips, Calendar, Profile }

@Composable
fun MainScreen(
    initialUser: User?,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.Trips) }
    var currentUser by remember { mutableStateOf(initialUser) }
    val tripsViewModel: TripsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Trips,
                    onClick = { selectedTab = MainTab.Trips },
                    icon = {
                        Icon(Icons.Default.AirplanemodeActive, contentDescription = "Trips")
                    },
                    label = { Text("Trips") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Coral,
                        selectedTextColor = Coral,
                        indicatorColor = Coral.copy(alpha = 0.12f),
                        unselectedIconColor = Chalk200,
                        unselectedTextColor = Chalk200
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.Calendar,
                    onClick = { selectedTab = MainTab.Calendar },
                    icon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    },
                    label = { Text("Calendar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Coral,
                        selectedTextColor = Coral,
                        indicatorColor = Coral.copy(alpha = 0.12f),
                        unselectedIconColor = Chalk200,
                        unselectedTextColor = Chalk200
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.Profile,
                    onClick = { selectedTab = MainTab.Profile },
                    icon = {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Coral,
                        selectedTextColor = Coral,
                        indicatorColor = Coral.copy(alpha = 0.12f),
                        unselectedIconColor = Chalk200,
                        unselectedTextColor = Chalk200
                    )
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Trips -> TripsListScreen(
                viewModel = tripsViewModel,
                currentUser = currentUser,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Calendar -> CalendarScreen(
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Profile -> ProfileScreen(
                currentUser = currentUser,
                onUserUpdated = { updatedUser -> currentUser = updatedUser },
                onLogout = onLogout,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
