package com.tripsyc.app.ui.trips

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
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        // Centered wordmark — bold coral "Tripsyc"
                        Text(
                            text = "Tripsyc",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Coral,
                            letterSpacing = (-0.5).sp
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

                    // Trip cards
                    items(state.filteredTrips) { trip ->
                        TripCard(
                            trip = trip,
                            modifier = Modifier.clickable { selectedTrip = trip }
                        )
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
        CreateTripSheet(
            onDismiss = { showCreateSheet = false },
            onCreated = { trip ->
                showCreateSheet = false
                viewModel.insertTrip(trip)
                selectedTrip = trip
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text("T", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Tripsyc",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
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
