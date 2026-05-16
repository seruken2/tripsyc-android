package com.tripsyc.app.ui.trip.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.*
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.ErrorView
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationsScreen(
    tripId: String,
    isOrganizer: Boolean,
    existingLock: DecisionLock?,
    destinationPhase: DestinationPhase?,
    currentUser: com.tripsyc.app.data.api.models.User? = null
) {
    var destinations by remember { mutableStateOf<List<Destination>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    var showCompare by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isLocked = existingLock?.locked == true

    fun loadDestinations() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.apiService.getDestinations(tripId)
                destinations = response.destinations
            } catch (e: Exception) {
                error = e.message ?: "Failed to load destinations"
            }
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { loadDestinations() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Destinations", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                    Text(
                        text = "Phase: ${destinationPhase?.name ?: "SUGGEST"}",
                        fontSize = 13.sp, color = Chalk500
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (destinations.size >= 2) {
                        OutlinedButton(
                            onClick = { showCompare = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compare", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (!isLocked) {
                        IconButton(onClick = { showAddSheet = true }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Coral),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (isLocked && existingLock?.lockedValue != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Success.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Success)
                        Column {
                            Text("Destination Locked!", fontWeight = FontWeight.SemiBold, color = Chalk900)
                            val lockedDest = destinations.firstOrNull { it.id == existingLock.lockedValue }
                            val name = lockedDest?.let { "${it.city}, ${it.country}" } ?: existingLock.lockedValue
                            Text(text = name ?: "", color = Chalk500, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item { LoadingView("Loading destinations...") }
        } else if (error != null) {
            item { ErrorView(message = error!!, onRetry = { loadDestinations() }) }
        } else if (destinations.isEmpty()) {
            item {
                EmptyState(
                    icon = "🗺️",
                    title = "No destinations yet",
                    message = "Suggest a destination for the group to vote on.",
                    actionLabel = if (!isLocked) "Add Destination" else null,
                    onAction = if (!isLocked) ({ showAddSheet = true }) else null
                )
            }
        } else {
            items(destinations) { dest ->
                DestinationCard(
                    destination = dest,
                    isLocked = isLocked,
                    onVote = { value ->
                        scope.launch {
                            try {
                                ApiClient.apiService.castVote(
                                    mapOf("tripId" to tripId, "destinationId" to dest.id, "value" to value.name)
                                )
                                loadDestinations()
                            } catch (_: Exception) {}
                        }
                    },
                    onClick = { selectedDestination = dest }
                )
            }
        }
    }

    if (showAddSheet) {
        AddDestinationSheet(
            tripId = tripId,
            onDismiss = { showAddSheet = false },
            onAdded = { loadDestinations() }
        )
    }

    if (showCompare) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCompare = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DestinationCompareSheet(
                destinations = destinations,
                onDismiss = { showCompare = false }
            )
        }
    }

    // Navigate to destination detail
    selectedDestination?.let { dest ->
        DestinationDetailScreen(
            destination = dest,
            tripId = tripId,
            currentUser = currentUser,
            isLocked = isLocked,
            onBack = {
                selectedDestination = null
                loadDestinations()
            }
        )
    }
}

@Composable
private fun DestinationCard(
    destination: Destination,
    isLocked: Boolean,
    onVote: (VoteValue) -> Unit,
    onClick: () -> Unit = {}
) {
    val upVotes = destination.votes?.count { it.value == VoteValue.UP } ?: 0
    val downVotes = destination.votes?.count { it.value == VoteValue.DOWN } ?: 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 3.dp,
        onClick = onClick
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (!destination.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = destination.imageUrl,
                        contentDescription = destination.city,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Dusk, Dusk.copy(alpha = 0.6f))))
                    )
                }
                if (destination.shortlisted) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Gold.copy(alpha = 0.9f)
                    ) {
                        Text(
                            "Shortlisted",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = destination.city,
                            fontWeight = FontWeight.Bold,
                            color = Chalk900,
                            fontSize = 16.sp
                        )
                        Text(
                            text = destination.country,
                            color = Chalk500,
                            fontSize = 13.sp
                        )
                    }
                    if (destination.estimatedCostMin != null) {
                        Text(
                            text = "$${destination.estimatedCostMin}-${destination.estimatedCostMax ?: "?"}",
                            color = Chalk500,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!isLocked) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onVote(VoteValue.UP) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Success),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("👍 $upVotes", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { onVote(VoteValue.DOWN) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("👎 $downVotes", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDestinationSheet(
    tripId: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Destination", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)

            Button(
                onClick = {
                    if (city.isBlank() || country.isBlank()) {
                        error = "Please fill in city and country"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.addDestination(
                                mapOf("tripId" to tripId, "city" to city.trim(), "country" to country.trim())
                            )
                            onAdded()
                            onDismiss()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to add destination"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Add Destination", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
