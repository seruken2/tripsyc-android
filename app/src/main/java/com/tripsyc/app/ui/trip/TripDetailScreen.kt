package com.tripsyc.app.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.tripsyc.app.ui.trip.unlock.UnlockVoteScreen
import com.tripsyc.app.ui.trip.summary.TripSummaryScreen
import com.tripsyc.app.ui.trip.group.GroupProfileScreen
import com.tripsyc.app.ui.trip.guide.TripGuideScreen
import kotlinx.coroutines.launch

// Exactly 6 tabs matching iOS: Overview, Dates, Destinations, Budget, Chat, More
enum class TripTab(val label: String, val icon: ImageVector) {
    Overview("Overview", Icons.Default.Home),
    Dates("Dates", Icons.Default.CalendarMonth),
    Destinations("Destinations", Icons.Default.Map),
    Budget("Budget", Icons.Default.AccountBalance),
    Chat("Chat", Icons.Default.ChatBubble),
    More("More", Icons.Default.MoreHoriz)
}

enum class MoreTab {
    Expenses, Notes, Packing, Photos, Itinerary, Polls,
    Responsibilities, Activity, Memories, Invite, Settings, Unlock,
    Summary, GroupProfile, Guide, SmartItinerary, Snaps, GroupRewind
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

    // Cross-screen settings broadcast. Settings save emits the new trip
    // shape here; this screen swaps it into freshTrip so the nav title,
    // hero, and lock cards re-render without a manual reload.
    LaunchedEffect(trip.id) {
        com.tripsyc.app.data.TripEventBus.tripUpdates.collect { updated ->
            if (updated.id == trip.id) freshTrip = updated
        }
    }

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

    LaunchedEffect(trip.id) { reloadTrip() }

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
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Chalk900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        bottomBar = {
            // White card bottom nav with top shadow — matches iOS TripTabBar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                val tabBadges by com.tripsyc.app.push.TabBadgeStore.badges.collectAsState()
                val pendingTabs = tabBadges[freshTrip.id] ?: emptySet()
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    TripTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                com.tripsyc.app.push.TabBadgeStore.clear(freshTrip.id, tab.name.lowercase())
                                reloadTrip()
                            },
                            icon = {
                                // Coral dot overlay when this tab has unseen
                                // FCM activity. Tap clears via TabBadgeStore.
                                Box {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (tab.name.lowercase() in pendingTabs) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-2).dp)
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Coral)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
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
            }
        },
        containerColor = Chalk50
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LivePresenceRow(tripId = freshTrip.id)
            when (selectedTab) {
                TripTab.Overview -> OverviewScreen(
                    trip = freshTrip,
                    currentUser = currentUser,
                    onTabSelected = { selectedTab = it },
                    isOrganizer = isOrganizer,
                    onTripUpdated = { freshTrip = it },
                    onMemberChanged = { reloadTrip() }
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
                    destinationPhase = freshTrip.destinationPhase,
                    currentUser = currentUser
                )
                TripTab.Budget -> BudgetScreen(tripId = freshTrip.id)
                TripTab.Chat -> ChatScreen(
                    tripId = freshTrip.id,
                    currentUser = currentUser,
                    isOrganizer = isOrganizer
                )
                TripTab.More -> MoreMenuScreen(
                    trip = freshTrip,
                    onSelect = { selectedMoreTab = it }
                )
            }
        }
    }
}

/// Feature meta for the More grid. Matches the iOS TripMoreFeature enum:
/// per-feature title, subtitle, icon, tint color, and category.
private data class MoreFeatureMeta(
    val tab: MoreTab,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val tint: Color,
    val category: MoreCategory
)

private enum class MoreCategory(val label: String, val accent: Color) {
    PLAN("Plan Together", Coral),
    TRACK("Track & Share", Dusk),
    GROUP("Group", Sage)
}

@Composable
fun MoreMenuScreen(
    trip: Trip,
    onSelect: (MoreTab) -> Unit
) {
    val dateLock = trip.locks?.firstOrNull { it.lockType == LockType.DATE }
    val destLock = trip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
    val isAnyLocked = (dateLock?.locked == true) || (destLock?.locked == true)
    val isFullyLocked = (dateLock?.locked == true) && (destLock?.locked == true)

    // iOS subtitles / tint colors / categories — mirrored verbatim so
    // users see identical descriptive copy and color coding across
    // platforms. Order within each category matches the iOS enum.
    val planFeatures = listOf(
        MoreFeatureMeta(MoreTab.Polls, Icons.Default.Poll, "Polls", "Vote on dates & destinations", Dusk, MoreCategory.PLAN),
        MoreFeatureMeta(MoreTab.Notes, Icons.Default.Note, "Notes", "Shared trip notes", Gold, MoreCategory.PLAN),
        MoreFeatureMeta(MoreTab.Packing, Icons.Default.Luggage, "Packing List", "Never forget a thing", Sage, MoreCategory.PLAN),
        MoreFeatureMeta(MoreTab.Itinerary, Icons.Default.ListAlt, "Itinerary", "Day-by-day plans", Dusk, MoreCategory.PLAN),
        MoreFeatureMeta(MoreTab.SmartItinerary, Icons.Default.AutoAwesome, "Smart Plan", "Group-voted day-by-day plan", Coral, MoreCategory.PLAN),
        MoreFeatureMeta(MoreTab.Guide, Icons.Default.Book, "Trip Guide", "All plans in one place", Sage, MoreCategory.PLAN)
    )

    val trackFeatures = buildList {
        add(MoreFeatureMeta(MoreTab.Expenses, Icons.Default.AccountBalance, "Expenses", "Track spending & settle up", Coral, MoreCategory.TRACK))
        add(MoreFeatureMeta(MoreTab.Photos, Icons.Default.PhotoLibrary, "Photos", "Capture the memories", CoralLight, MoreCategory.TRACK))
        add(MoreFeatureMeta(MoreTab.Snaps, Icons.Default.PhotoCamera, "Snaps", "Live moments that vanish", Coral, MoreCategory.TRACK))
        add(MoreFeatureMeta(MoreTab.Responsibilities, Icons.Default.CheckCircle, "Tasks", "Assign & track to-dos", Sage, MoreCategory.TRACK))
        add(MoreFeatureMeta(MoreTab.Memories, Icons.Default.Favorite, "Memories", "Trip recap & stats", Coral, MoreCategory.TRACK))
        add(MoreFeatureMeta(MoreTab.GroupRewind, Icons.Default.EmojiEvents, "Group Rewind", "The crew's collective recap", Coral, MoreCategory.TRACK))
        if (isFullyLocked) {
            add(MoreFeatureMeta(MoreTab.Summary, Icons.Default.Verified, "Trip Summary", "Locked decisions & group", Sage, MoreCategory.TRACK))
        }
    }

    val groupFeatures = buildList {
        add(MoreFeatureMeta(MoreTab.GroupProfile, Icons.Default.Groups, "Group Profile", "Compatibility & preferences", Dusk, MoreCategory.GROUP))
        add(MoreFeatureMeta(MoreTab.Activity, Icons.Default.Notifications, "Activity", "See what's happening", Gold, MoreCategory.GROUP))
        add(MoreFeatureMeta(MoreTab.Invite, Icons.Default.PersonAdd, "Invite", "Grow your group", Coral, MoreCategory.GROUP))
        add(MoreFeatureMeta(MoreTab.Settings, Icons.Default.Settings, "Settings", "Manage trip details", Chalk500, MoreCategory.GROUP))
        if (isAnyLocked) {
            add(MoreFeatureMeta(MoreTab.Unlock, Icons.Default.LockOpen, "Unlock", "Petition to reopen a locked decision", Coral, MoreCategory.GROUP))
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        listOf(planFeatures, trackFeatures, groupFeatures).forEach { features ->
            if (features.isEmpty()) return@forEach
            item {
                MoreCategorySection(features = features, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun MoreCategorySection(
    features: List<MoreFeatureMeta>,
    onSelect: (MoreTab) -> Unit
) {
    val category = features.first().category
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header: 3dp accent bar + uppercase tracking 1.0
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(category.accent)
            )
            Text(
                category.label.uppercase(),
                color = Chalk400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        // 2-col grid of feature cards
        features.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { f ->
                    MoreFeatureCard(meta = f, modifier = Modifier.weight(1f), onClick = { onSelect(f.tab) })
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoreFeatureCard(
    meta: MoreFeatureMeta,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Per-feature gradient icon circle — coral 0.20 → 0.08
                // is the iOS treatment; we approximate with two layered
                // ovals since Compose's Brush.radialGradient on a Circle
                // looks identical to the LinearGradient iOS uses here.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(meta.tint.copy(alpha = 0.20f), meta.tint.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = null,
                        tint = meta.tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Chalk200,
                    modifier = Modifier.size(14.dp).padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                meta.title,
                color = Chalk900,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                meta.subtitle,
                color = Chalk400,
                fontSize = 11.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
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
                            MoreTab.Responsibilities -> "Tasks"
                            MoreTab.Activity -> "Activity"
                            MoreTab.Memories -> "Memories"
                            MoreTab.Invite -> "Invite"
                            MoreTab.Settings -> "Trip Settings"
                            MoreTab.Unlock -> "Unlock Vote"
                            MoreTab.Summary -> "Trip Summary"
                            MoreTab.GroupProfile -> "Group Profile"
                            MoreTab.Guide -> "Trip Guide"
                            MoreTab.SmartItinerary -> "Smart Plan"
                            MoreTab.Snaps -> "Snaps"
                            MoreTab.GroupRewind -> "Group Rewind"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Chalk900
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Chalk900
                        )
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
                MoreTab.Photos -> PhotosScreen(tripId = trip.id, currentUser = currentUser)
                MoreTab.Itinerary -> ItineraryScreen(tripId = trip.id, currentUser = currentUser)
                MoreTab.Polls -> PollsScreen(tripId = trip.id, currentUser = currentUser)
                MoreTab.Responsibilities -> ResponsibilitiesScreen(
                    tripId = trip.id,
                    members = trip.members ?: emptyList()
                )
                MoreTab.Activity -> ActivityScreen(tripId = trip.id)
                MoreTab.Memories -> MemoriesScreen(tripId = trip.id, tripName = trip.name)
                MoreTab.Invite -> InviteScreen(trip = trip)
                MoreTab.Settings -> TripSettingsScreen(
                    trip = trip,
                    currentUser = currentUser,
                    onBack = onBack
                )
                MoreTab.Summary -> TripSummaryScreen(trip = trip)
                MoreTab.GroupProfile -> GroupProfileScreen(tripId = trip.id)
                MoreTab.Guide -> TripGuideScreen(tripId = trip.id)
                MoreTab.Snaps -> com.tripsyc.app.ui.trip.snaps.SnapsScreen(
                    tripId = trip.id,
                    currentUserId = currentUser?.id
                )
                MoreTab.GroupRewind -> com.tripsyc.app.ui.trip.rewind.GroupRewindScreen(
                    tripId = trip.id
                )
                MoreTab.SmartItinerary -> {
                    val isOrganizer = trip.members?.firstOrNull { it.userId == currentUser?.id }
                        ?.role?.let { it.name == "CREATOR" || it.name == "CO_ORGANIZER" } == true
                    com.tripsyc.app.ui.trip.smart.SmartItineraryScreen(
                        tripId = trip.id,
                        currentUser = currentUser,
                        isOrganizer = isOrganizer,
                        onBack = onBack
                    )
                }
                MoreTab.Unlock -> {
                    val dateLock = trip.locks?.firstOrNull { it.lockType == LockType.DATE }
                    val destLock = trip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
                    val lockType = when {
                        dateLock?.locked == true -> "DATE"
                        destLock?.locked == true -> "DESTINATION"
                        else -> "DATE"
                    }
                    val isOrganizer = trip.members?.firstOrNull { it.userId == currentUser?.id }
                        ?.role?.let { it.name == "CREATOR" || it.name == "CO_ORGANIZER" } == true
                    UnlockVoteScreen(
                        tripId = trip.id,
                        lockType = lockType,
                        currentUser = currentUser,
                        isOrganizer = isOrganizer
                    )
                }
            }
        }
    }
}
