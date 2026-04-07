package com.tripsyc.app.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
                            placeholder = { Text("Search trips") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = Chalk200
                            )
                        )
                    } else {
                        Text(
                            text = "Tripsyc",
                            fontWeight = FontWeight.Bold,
                            color = Chalk900
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Chalk500
                        )
                    }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState(
                        icon = "✈️",
                        title = "No trips yet",
                        message = "Create a trip and invite your crew.",
                        actionLabel = "Create Your First Trip",
                        onAction = { showCreateSheet = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showJoinSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                    ) {
                        Text("Join a Trip")
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
                    }

                    // Pending invites
                    if (state.pendingInvites.isNotEmpty() && state.searchQuery.isEmpty()) {
                        item {
                            Text(
                                text = "Pending Invitations",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Chalk900
                            )
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!invite.trip.approxMonth.isNullOrEmpty()) {
                            Text(
                                text = "📅 ${invite.trip.approxMonth}",
                                color = Chalk500,
                                fontSize = 12.sp
                            )
                        }
                        val count = invite.trip.count?.members ?: 0
                        if (count > 0) {
                            Text(
                                text = "👥 $count",
                                color = Chalk500,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Decline", color = Chalk500)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}
