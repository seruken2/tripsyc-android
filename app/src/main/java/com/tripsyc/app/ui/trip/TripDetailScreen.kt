package com.tripsyc.app.ui.trip

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.LockType
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.theme.*
import com.tripsyc.app.ui.trip.activity.ActivityScreen
import com.tripsyc.app.ui.trip.budget.BudgetScreen
import com.tripsyc.app.ui.trip.chat.ChatScreen
import com.tripsyc.app.ui.trip.dates.DatesScreen
import com.tripsyc.app.ui.trip.destinations.DestinationsScreen
import com.tripsyc.app.ui.trip.expenses.ExpensesScreen
import com.tripsyc.app.ui.trip.invite.InviteScreen
import com.tripsyc.app.ui.trip.itinerary.ItineraryScreen
import com.tripsyc.app.ui.trip.memories.MemoriesScreen
import com.tripsyc.app.ui.trip.notes.NotesScreen
import com.tripsyc.app.ui.trip.overview.OverviewScreen
import com.tripsyc.app.ui.trip.packing.PackingScreen
import com.tripsyc.app.ui.trip.photos.PhotosScreen
import com.tripsyc.app.ui.trip.polls.PollsScreen
import com.tripsyc.app.ui.trip.responsibilities.ResponsibilitiesScreen
import com.tripsyc.app.ui.trip.settings.TripSettingsScreen
import kotlinx.coroutines.launch

enum class TripTab(val label: String, val icon: ImageVector) {
    Overview("Overview", Icons.Default.Home),
    Dates("Dates", Icons.Default.CalendarMonth),
    Destinations("Destinations", Icons.Default.Map),
    Budget("Budget", Icons.Default.AttachMoney),
    Chat("Chat", Icons.Default.Chat),
    More("More", Icons.Default.MoreHoriz)
}

enum class MoreTab {
    Expenses, Notes, Packing, Photos, Itinerary, Polls,
    Responsibilities, Activity, Memories, Invite, Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip,
    currentUser: User?,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(TripTab.Overview) }
    var freshTrip by remember { mutableStateOf(trip) }
    var selectedMoreTab by remember { mutableStateOf<MoreTab?>(null) }
    val scope = rememberCoroutineScope()

    val dateLock = freshTrip.locks?.firstOrNull { it.lockType == LockType.DATE }
    val destLock = freshTrip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
    val isOrganizer = freshTrip.members?.firstOrNull { it.userId == currentUser?.id }
        ?.role?.let { it.name == "CREATOR" || it.name == "CO_ORGANIZER" } == true

    val tripStatus = when {
        (dateLock?.locked == true) && (destLock?.locked == true) -> "Confirmed"
        (dateLock?.locked == true) || (destLock?.locked == true) -> "Locked"
        else -> "Planning"
    }

    fun reloadTrip() {
        scope.launch {
            try {
                freshTrip = ApiClient.apiService.getTrip(trip.id)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(trip.id) {
        reloadTrip()
    }

    // If a More tab is selected, show that screen
    if (selectedMoreTab != null) {
        MoreTabScreen(
            tab = selectedMoreTab!!,
            trip = freshTrip,
            currentUser = currentUser,
            onBack = { selectedMoreTab = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = freshTrip.name,
                            fontWeight = FontWeight.Bold,
                            color = Chalk900,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Text(
                            text = tripStatus,
                            color = Chalk500,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                TripTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            reloadTrip()
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(20.dp)) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Coral,
                            selectedTextColor = Coral,
                            indicatorColor = Coral.copy(alpha = 0.12f),
                            unselectedIconColor = Chalk400,
                            unselectedTextColor = Chalk400
                        )
                    )
                }
            }
        },
        containerColor = Chalk50
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                TripTab.Overview -> OverviewScreen(
                    trip = freshTrip,
                    currentUser = currentUser,
                    onTabSelected = { selectedTab = it }
                )
                TripTab.Dates -> DatesScreen(
                    tripId = freshTrip.id,
                    isOrganizer = isOrganizer,
                    existingLock = dateLock,
                    tripName = freshTrip.name
                )
                TripTab.Destinations -> DestinationsScreen(
                    tripId = freshTrip.id,
                    isOrganizer = isOrganizer,
                    existingLock = destLock,
                    destinationPhase = freshTrip.destinationPhase
                )
                TripTab.Budget -> BudgetScreen(tripId = freshTrip.id)
                TripTab.Chat -> ChatScreen(
                    tripId = freshTrip.id,
                    currentUser = currentUser
                )
                TripTab.More -> MoreMenuScreen(
                    trip = freshTrip,
                    onSelect = { selectedMoreTab = it }
                )
            }
        }
    }
}

@Composable
fun MoreMenuScreen(
    trip: Trip,
    onSelect: (MoreTab) -> Unit
) {
    val items = listOf(
        Triple(MoreTab.Expenses, Icons.Default.AccountBalance, "Expenses"),
        Triple(MoreTab.Notes, Icons.Default.Note, "Notes"),
        Triple(MoreTab.Packing, Icons.Default.Luggage, "Packing"),
        Triple(MoreTab.Photos, Icons.Default.PhotoLibrary, "Photos"),
        Triple(MoreTab.Itinerary, Icons.Default.ListAlt, "Itinerary"),
        Triple(MoreTab.Polls, Icons.Default.Poll, "Polls"),
        Triple(MoreTab.Responsibilities, Icons.Default.CheckCircle, "Responsibilities"),
        Triple(MoreTab.Activity, Icons.Default.Notifications, "Activity"),
        Triple(MoreTab.Memories, Icons.Default.Favorite, "Memories"),
        Triple(MoreTab.Invite, Icons.Default.PersonAdd, "Invite"),
        Triple(MoreTab.Settings, Icons.Default.Settings, "Settings"),
    )

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "More Options",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Chalk900,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items.chunked(2).forEach { rowItems ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (tab, icon, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = CardBackground,
                            shadowElevation = 2.dp,
                            onClick = { onSelect(tab) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = Coral,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Medium,
                                    color = Chalk900,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    // Fill empty slot if odd
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTabScreen(
    tab: MoreTab,
    trip: Trip,
    currentUser: User?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (tab) {
                            MoreTab.Expenses -> "Expenses"
                            MoreTab.Notes -> "Notes"
                            MoreTab.Packing -> "Packing List"
                            MoreTab.Photos -> "Photos"
                            MoreTab.Itinerary -> "Itinerary"
                            MoreTab.Polls -> "Polls"
                            MoreTab.Responsibilities -> "Responsibilities"
                            MoreTab.Activity -> "Activity"
                            MoreTab.Memories -> "Memories"
                            MoreTab.Invite -> "Invite People"
                            MoreTab.Settings -> "Trip Settings"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Chalk900
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        containerColor = Chalk50
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                MoreTab.Expenses -> ExpensesScreen(
                    tripId = trip.id,
                    currentUser = currentUser,
                    tripCurrency = trip.currency
                )
                MoreTab.Notes -> NotesScreen(tripId = trip.id, currentUser = currentUser)
                MoreTab.Packing -> PackingScreen(tripId = trip.id)
                MoreTab.Photos -> PhotosScreen(tripId = trip.id)
                MoreTab.Itinerary -> ItineraryScreen(tripId = trip.id)
                MoreTab.Polls -> PollsScreen(tripId = trip.id, currentUser = currentUser)
                MoreTab.Responsibilities -> ResponsibilitiesScreen(
                    tripId = trip.id,
                    members = trip.members ?: emptyList()
                )
                MoreTab.Activity -> ActivityScreen(tripId = trip.id)
                MoreTab.Memories -> MemoriesScreen(tripId = trip.id)
                MoreTab.Invite -> InviteScreen(trip = trip)
                MoreTab.Settings -> TripSettingsScreen(
                    trip = trip,
                    currentUser = currentUser,
                    onBack = onBack
                )
            }
        }
    }
}
