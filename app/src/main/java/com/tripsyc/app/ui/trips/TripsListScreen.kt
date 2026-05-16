package com.tripsyc.app.ui.trips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.R
import com.tripsyc.app.data.api.models.PendingInvite
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.ErrorView
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*

enum class TripSortOption(val label: String) {
    Newest("Newest first"),
    Oldest("Oldest first"),
    Name("A–Z name"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsListScreen(
    viewModel: TripsViewModel,
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showJoinSheet by remember { mutableStateOf(false) }
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(TripSortOption.Newest) }
    var showSortMenu by remember { mutableStateOf(false) }
    // The just-created trip + first-trip flag drive the celebration
    // screen. Cleared once the user picks a next step or dismisses.
    var celebrationTrip by remember { mutableStateOf<Trip?>(null) }
    var celebrationIsFirst by remember { mutableStateOf(false) }

    if (celebrationTrip != null) {
        com.tripsyc.app.ui.onboarding.TripCreatedScreen(
            trip = celebrationTrip!!,
            isFirstTrip = celebrationIsFirst,
            onInviteBuddies = {
                val t = celebrationTrip!!
                celebrationTrip = null
                selectedTrip = t
                // TODO: deep-link straight to Invite tab — for now the
                // detail screen opens on Overview where Invite lives in
                // the More grid.
            },
            onAddActivity = {
                val t = celebrationTrip!!
                celebrationTrip = null
                selectedTrip = t
            },
            onDone = {
                val t = celebrationTrip!!
                celebrationTrip = null
                selectedTrip = t
            }
        )
        return
    }

    if (selectedTrip != null) {
        com.tripsyc.app.ui.trip.TripDetailScreen(
            trip = selectedTrip!!,
            currentUser = currentUser,
            onBack = { selectedTrip = null }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.updateSearch(it) },
                            placeholder = { Text("Search trips", color = Chalk400) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = Chalk200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    } else {
                        // Centered wordmark — real logo image
                        Image(
                            painter = painterResource(R.drawable.tripsyc_logo),
                            contentDescription = "Tripsyc",
                            modifier = Modifier.height(22.dp),
                            colorFilter = ColorFilter.tint(Coral),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                actions = {
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = Chalk500
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TripSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label,
                                            color = if (sortOption == option) Coral else Chalk900
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Coral circle + button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Coral)
                            .clickable { showCreateSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create trip",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        containerColor = Chalk50
    ) { padding ->
        when {
            state.isLoading -> LoadingView("Fetching your trips...")
            state.error != null && state.trips.isEmpty() -> ErrorView(
                message = state.error!!,
                onRetry = { viewModel.loadTrips() }
            )
            state.trips.isEmpty() && state.pendingInvites.isEmpty() -> {
                // Branded empty state matching iOS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        BrandedEmptyStateHero(onCreateTrip = { showCreateSheet = true })
                    }
                    item {
                        Button(
                            onClick = { showCreateSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Create Your First Trip",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    item {
                        FeatureHighlightRow()
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    item {
                        val firstName = currentUser?.name?.split(" ")?.firstOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (firstName != null) "Hey, $firstName!" else "My Trips",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Chalk900
                                )
                                Text(
                                    text = "Plan together. Go together.",
                                    fontSize = 13.sp,
                                    color = Chalk400
                                )
                            }
                            // Trip count pill
                            val count = state.filteredTrips.size
                            if (count > 0) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Coral.copy(alpha = 0.08f)),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Coral
                                    )
                                    Text(
                                        text = if (count == 1) "trip" else "trips",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Chalk400
                                    )
                                }
                            }
                        }
                    }

                    // Pending invites section
                    if (state.pendingInvites.isNotEmpty() && state.searchQuery.isEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pending Invitations",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Chalk900
                                )
                                // Count badge
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Coral)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${state.pendingInvites.size}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        items(state.pendingInvites) { invite ->
                            PendingInviteCard(
                                invite = invite,
                                onAccept = {
                                    viewModel.acceptInvite(invite.id) { trip ->
                                        trip?.let { selectedTrip = it }
                                    }
                                },
                                onDecline = { viewModel.declineInvite(invite.id) }
                            )
                        }
                    }

                    // Trip cards — wrapped in a swipe-to-toggle row so
                    // left-swipe pins/unpins (gold) and right-swipe
                    // archives/unarchives (gray), matching the iOS list.
                    items(state.filteredTrips, key = { it.id }) { trip ->
                        SwipeableTripRow(
                            trip = trip,
                            isPinned = trip.id in state.pinnedIds,
                            isArchived = trip.id in state.archivedIds,
                            onTogglePin = { viewModel.togglePinned(trip.id) },
                            onToggleArchive = { viewModel.toggleArchived(trip.id) },
                            onClick = { selectedTrip = trip }
                        )
                    }

                    // Archived peek — if any trips are archived and the
                    // toggle is off, surface a small footer so the user
                    // doesn't lose track of them.
                    if (!state.showArchived && state.archivedCount > 0) {
                        item {
                            TextButton(
                                onClick = { viewModel.setShowArchived(true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = Chalk500,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Show ${state.archivedCount} archived",
                                    color = Chalk500,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (state.showArchived && state.archivedCount > 0) {
                        item {
                            TextButton(
                                onClick = { viewModel.setShowArchived(false) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Chalk500,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Hide archived",
                                    color = Chalk500,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Join trip button
                    item {
                        OutlinedButton(
                            onClick = { showJoinSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Chalk200)
                        ) {
                            Text("Join a Trip with Invite Code")
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        // "First trip" is keyed on whether the list was empty *before*
        // we add the new row — checked against the current state count
        // so a follow-on create after the celebration doesn't re-fire it.
        val isFirst = state.trips.isEmpty()
        CreateTripSheet(
            onDismiss = { showCreateSheet = false },
            onCreated = { trip ->
                showCreateSheet = false
                viewModel.insertTrip(trip)
                celebrationIsFirst = isFirst
                celebrationTrip = trip
            }
        )
    }

    if (showJoinSheet) {
        JoinTripSheet(
            onDismiss = { showJoinSheet = false },
            onJoined = { trip ->
                showJoinSheet = false
                viewModel.insertTrip(trip)
                selectedTrip = trip
            }
        )
    }
}

// ── Branded Empty State Hero ─────────────────────────────────────────────────

@Composable
private fun BrandedEmptyStateHero(onCreateTrip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Coral, CoralLight)
                )
            )
            .padding(vertical = 52.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.tripsyc_icon),
                contentDescription = "Tripsyc",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(R.drawable.tripsyc_logo),
                contentDescription = "Tripsyc",
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(max = 160.dp),
                colorFilter = ColorFilter.tint(Color.White),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "Create a trip and invite your crew.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp
            )
        }
    }
}

// ── Feature Highlight Row ─────────────────────────────────────────────────────

@Composable
private fun FeatureHighlightRow() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Everything your group needs",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Chalk900
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureHighlightPill(icon = "📅", label = "Find dates", color = Dusk)
            FeatureHighlightPill(icon = "📍", label = "Vote destinations", color = Coral)
            FeatureHighlightPill(icon = "💰", label = "Track expenses", color = Sage)
            FeatureHighlightPill(icon = "💬", label = "Group chat", color = Gold)
            FeatureHighlightPill(icon = "✅", label = "Assign tasks", color = Dusk)
            FeatureHighlightPill(icon = "📸", label = "Share photos", color = CoralLight)
        }
    }
}

@Composable
private fun FeatureHighlightPill(icon: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 12.sp)
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Chalk500
        )
    }
}

// ── Pending Invite Card ───────────────────────────────────────────────────────

@Composable
private fun PendingInviteCard(
    invite: PendingInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = invite.trip.name,
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900,
                        fontSize = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!invite.trip.approxMonth.isNullOrEmpty()) {
                            Text(
                                text = "📅 ${invite.trip.approxMonth}",
                                color = Chalk500,
                                fontSize = 12.sp
                            )
                        }
                        val count = invite.trip.count?.members ?: 0
                        if (count > 0) {
                            Text(text = "·", color = Chalk400, fontSize = 12.sp)
                            Text(
                                text = "👥 $count",
                                color = Chalk500,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Chalk100,
                        contentColor = Chalk500
                    )
                ) {
                    Text("Decline", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * SwipeToDismissBox wrapper that fires a toggle on either end-of-swipe
 * and snaps back instead of dismissing the row. Backgrounds reveal the
 * action: gold pin pill on left swipe, gray archive pill on right swipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTripRow(
    trip: Trip,
    isPinned: Boolean,
    isArchived: Boolean,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onTogglePin()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onToggleArchive()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val (bg, icon, label, tint) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> SwipeBg(
                    bg = Gold.copy(alpha = 0.15f),
                    icon = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    label = if (isPinned) "Unpin" else "Pin",
                    tint = Gold
                )
                SwipeToDismissBoxValue.EndToStart -> SwipeBg(
                    bg = Chalk100,
                    icon = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    label = if (isArchived) "Unarchive" else "Archive",
                    tint = Chalk500
                )
                else -> SwipeBg(Color.Transparent, Icons.Default.Archive, "", Color.Transparent)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    Text(label, color = tint, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
            TripCard(
                trip = trip,
                modifier = Modifier
            )
            // Subtle pin badge anchored on top-end so pinned trips
            // remain identifiable after the swipe animation snaps back.
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.20f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(10.dp)
                        )
                        Text("Pinned", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class SwipeBg(
    val bg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val tint: Color
)
