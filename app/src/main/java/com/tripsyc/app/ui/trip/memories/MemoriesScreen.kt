package com.tripsyc.app.ui.trip.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
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
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.api.models.TripPhoto
import com.tripsyc.app.data.api.models.TripSummaryResponse
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MemoriesScreen(tripId: String, tripName: String? = null) {
    var photos by remember { mutableStateOf<List<TripPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var summary by remember { mutableStateOf<TripSummaryResponse?>(null) }
    var isGeneratingSummary by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf<String?>(null) }

    var showCloneDialog by remember { mutableStateOf(false) }
    var cloneName by remember { mutableStateOf("") }
    var cloneReinvite by remember { mutableStateOf(true) }
    var isCloning by remember { mutableStateOf(false) }
    var cloneResult by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        isLoading = true
        try { photos = ApiClient.apiService.getPhotos(tripId).photos }
        catch (_: Exception) {}
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Memories", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            Text("Relive the best moments and re-run the trip.", fontSize = 14.sp, color = Chalk500)
        }

        // ── AI summary card ──────────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                        Text("Trip recap", fontWeight = FontWeight.SemiBold, color = Chalk900)
                    }

                    val s = summary
                    if (s != null) {
                        Text(s.headline, fontWeight = FontWeight.Bold, color = Chalk900, fontSize = 18.sp)
                        Text(s.narrative, color = Chalk700, fontSize = 14.sp)
                        if (s.highlights.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                s.highlights.forEach { h ->
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("•", color = Coral, fontWeight = FontWeight.Bold)
                                        Text(h, color = Chalk700, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Generate a narrative recap from the trip's locked dates, destination, expenses, and members.",
                            fontSize = 12.sp,
                            color = Chalk500
                        )
                    }

                    if (summaryError != null) {
                        Text(summaryError!!, color = Danger, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isGeneratingSummary = true
                                summaryError = null
                                try {
                                    summary = ApiClient.apiService.generateTripSummary(
                                        mapOf("tripId" to tripId)
                                    )
                                } catch (e: Exception) {
                                    summaryError = e.message ?: "Couldn't generate"
                                }
                                isGeneratingSummary = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        enabled = !isGeneratingSummary,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        if (isGeneratingSummary) {
                            CircularProgressIndicator(
                                color = Color.White, strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                if (summary == null) "Generate recap" else "Regenerate",
                                color = Color.White, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── Clone trip card ─────────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
                        Text("Plan it again", fontWeight = FontWeight.SemiBold, color = Chalk900)
                    }
                    Text(
                        "Copy this trip's destinations, itinerary, and (optionally) the member list into a fresh new trip.",
                        fontSize = 12.sp,
                        color = Chalk500
                    )
                    if (cloneResult != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Success.copy(alpha = 0.10f)) {
                            Text(cloneResult!!, color = Success, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            cloneName = tripName?.let { "$it (next time)" } ?: "New trip"
                            showCloneDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                    ) {
                        Text("Clone this trip", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Text("Photos from the trip", fontWeight = FontWeight.SemiBold, color = Chalk900, fontSize = 15.sp)
        }

        if (isLoading) {
            item { LoadingView() }
        } else if (photos.isEmpty()) {
            item { EmptyState(icon = "🎞️", title = "No memories yet", message = "Photos shared during the trip will appear here.") }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 4000.dp),
                    userScrollEnabled = false
                ) {
                    items(photos) { photo ->
                        Column {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = photo.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (!photo.caption.isNullOrEmpty()) {
                                Text(photo.caption, fontSize = 11.sp, color = Chalk500, modifier = Modifier.padding(top = 4.dp))
                            }
                            val uploaderName = photo.user?.name ?: ""
                            if (uploaderName.isNotEmpty()) {
                                Text("by $uploaderName", fontSize = 10.sp, color = Chalk400)
                            }
                        }
                    }
                }
            }
        }
    }

    // Clone confirmation dialog — the rename is required (server rejects
    // empty) and re-invite default mirrors iOS (organizer usually wants
    // their crew along again for "let's do this annually" trips).
    if (showCloneDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCloning) showCloneDialog = false },
            title = { Text("Clone trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cloneName,
                        onValueChange = { if (it.length <= 60) cloneName = it },
                        label = { Text("New trip name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = cloneReinvite,
                            onCheckedChange = { cloneReinvite = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Coral)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Re-invite the same members", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCloning = true
                            try {
                                val res = ApiClient.apiService.cloneTrip(
                                    tripId,
                                    mapOf(
                                        "name" to cloneName.trim(),
                                        "reinviteMembers" to cloneReinvite
                                    )
                                )
                                showCloneDialog = false
                                cloneResult = "Created “${res.trip.name}”" +
                                    if (cloneReinvite) " · ${res.reInvited} re-invited" else ""
                            } catch (e: Exception) {
                                cloneResult = e.message ?: "Couldn't clone"
                            }
                            isCloning = false
                        }
                    },
                    enabled = !isCloning && cloneName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) { Text("Clone") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCloneDialog = false },
                    enabled = !isCloning
                ) { Text("Cancel") }
            }
        )
    }
}
