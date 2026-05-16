package com.tripsyc.app.ui.trip.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.AIItineraryDraft
import com.tripsyc.app.data.api.models.AIItineraryDraftItem
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

private val TONES = listOf("balanced", "foodie", "outdoorsy", "cultural", "nightlife", "chill")

/**
 * AI Smart Itinerary screen. Renders existing drafts (server caps live
 * drafts at 2 for compare-mode), lets organizers regenerate with a tone,
 * exposes per-item up/down voting for the whole group, and a one-tap
 * "Accept majority" button that promotes UP-leading items into the
 * manual itinerary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartItineraryScreen(
    tripId: String,
    currentUser: User?,
    isOrganizer: Boolean,
    onBack: () -> Unit
) {
    var drafts by remember { mutableStateOf<List<AIItineraryDraft>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTone by remember { mutableStateOf("balanced") }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            drafts = ApiClient.apiService.getSmartItineraryDrafts(tripId).drafts
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load itinerary drafts"
        }
        isLoading = false
    }

    LaunchedEffect(tripId) {
        reload()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Itinerary", fontWeight = FontWeight.Bold, color = Chalk900) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Chalk700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        containerColor = Chalk50
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Coral)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tone picker + generate button — only organizers can mint
            // a fresh draft (and burn server tokens) since drafts are
            // expensive and visible to everyone.
            if (isOrganizer) {
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                                Text("Generate a plan", fontWeight = FontWeight.SemiBold, color = Chalk900)
                            }
                            Text(
                                "Pick a tone and the AI drafts a day-by-day plan based on your trip's dates, destination, and members.",
                                fontSize = 12.sp, color = Chalk500
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(TONES) { tone ->
                                    val isSelected = tone == selectedTone
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Coral else Chalk100,
                                        onClick = { selectedTone = tone }
                                    ) {
                                        Text(
                                            tone.replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) Color.White else Chalk700,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        isGenerating = true
                                        try {
                                            ApiClient.apiService.generateSmartItinerary(
                                                mapOf(
                                                    "tripId" to tripId,
                                                    "tone" to selectedTone,
                                                    "mode" to "REPLACE"
                                                )
                                            )
                                            reload()
                                        } catch (e: Exception) {
                                            error = e.message ?: "Generation failed"
                                        }
                                        isGenerating = false
                                    }
                                },
                                enabled = !isGenerating,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        color = Color.White, strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        if (drafts.isEmpty()) "Generate plan" else "Regenerate",
                                        color = Color.White, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = Danger.copy(alpha = 0.10f)) {
                        Text(
                            error!!,
                            color = Danger,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (drafts.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = Chalk100) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Chalk400, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isOrganizer) "No draft yet — pick a tone above to generate."
                                else "No draft yet — the organizer can generate one.",
                                color = Chalk500, fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            drafts.forEachIndexed { index, draft ->
                item {
                    DraftHeader(
                        title = if (drafts.size > 1) "Plan ${'A' + index}" else "Plan",
                        tone = draft.tone,
                        itemCount = draft.items.size,
                        isOrganizer = isOrganizer,
                        onAcceptMajority = {
                            scope.launch {
                                try {
                                    ApiClient.apiService.acceptMajoritySmartItems(
                                        mapOf("tripId" to tripId, "draftId" to draft.id)
                                    )
                                    reload()
                                } catch (e: Exception) {
                                    error = e.message ?: "Couldn't accept majority"
                                }
                            }
                        }
                    )
                }
                items(draft.items, key = { it.id }) { item ->
                    DraftItemCard(
                        item = item,
                        myUserId = currentUser?.id,
                        isOrganizer = isOrganizer,
                        onVote = { value ->
                            scope.launch {
                                try {
                                    ApiClient.apiService.voteOnSmartItem(
                                        mapOf("itemId" to item.id, "value" to value)
                                    )
                                    reload()
                                } catch (_: Exception) {}
                            }
                        },
                        onAccept = {
                            scope.launch {
                                try {
                                    ApiClient.apiService.acceptSmartItem(item.id)
                                    reload()
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftHeader(
    title: String,
    tone: String?,
    itemCount: Int,
    isOrganizer: Boolean,
    onAcceptMajority: () -> Unit
) {
    Surface(shape = RoundedCornerShape(14.dp), color = CardBackground, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Chalk900, fontSize = 16.sp)
                Text(
                    listOfNotNull(
                        tone?.let { "Tone: $it" },
                        "$itemCount item${if (itemCount == 1) "" else "s"}"
                    ).joinToString(" · "),
                    color = Chalk500,
                    fontSize = 12.sp
                )
            }
            if (isOrganizer && itemCount > 0) {
                Button(
                    onClick = onAcceptMajority,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept majority", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DraftItemCard(
    item: AIItineraryDraftItem,
    myUserId: String?,
    isOrganizer: Boolean,
    onVote: (String) -> Unit,
    onAccept: () -> Unit
) {
    val accent = when (item.category) {
        com.tripsyc.app.data.api.models.AIItineraryCategory.RESTAURANT -> Coral
        com.tripsyc.app.data.api.models.AIItineraryCategory.ACTIVITY -> Dusk
        com.tripsyc.app.data.api.models.AIItineraryCategory.TRANSPORT -> Sage
        com.tripsyc.app.data.api.models.AIItineraryCategory.INFO -> Gold
    }
    val myVote = item.votes.mine

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.15f),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            item.category.name.first().toString(),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    item.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (item.isAccepted) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Success.copy(alpha = 0.18f)) {
                        Text(
                            "Accepted",
                            color = Success,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    "Day ${item.dayOffset + 1}",
                    color = Chalk500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(item.title, color = Chalk900, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            if (item.description.isNotBlank()) {
                Text(item.description, color = Chalk700, fontSize = 13.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!item.location.isNullOrEmpty()) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Chalk400, modifier = Modifier.size(12.dp))
                    Text(item.location, color = Chalk500, fontSize = 11.sp)
                }
                item.startTime?.let { st ->
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Chalk400, modifier = Modifier.size(12.dp))
                    Text(
                        listOfNotNull(st, item.endTime).joinToString(" – "),
                        color = Chalk500,
                        fontSize = 11.sp
                    )
                }
                item.costUsd?.let { cost ->
                    Text("≈ \$$cost", color = Sage, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Vote / accept controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoteButton(
                    isSelected = myVote == "UP",
                    count = item.votes.up.size,
                    iconOn = Icons.Default.ThumbUp,
                    iconOff = Icons.Outlined.ThumbUp,
                    accent = Success,
                    onClick = { if (item.isLive) onVote("UP") }
                )
                VoteButton(
                    isSelected = myVote == "DOWN",
                    count = item.votes.down.size,
                    iconOn = Icons.Default.ThumbDown,
                    iconOff = Icons.Outlined.ThumbDown,
                    accent = Danger,
                    onClick = { if (item.isLive) onVote("DOWN") }
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isOrganizer && item.isLive && !item.isAccepted) {
                    TextButton(
                        onClick = onAccept,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Coral, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoteButton(
    isSelected: Boolean,
    count: Int,
    iconOn: androidx.compose.ui.graphics.vector.ImageVector,
    iconOff: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accent.copy(alpha = 0.15f) else Chalk100,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isSelected) iconOn else iconOff,
                contentDescription = null,
                tint = if (isSelected) accent else Chalk500,
                modifier = Modifier.size(14.dp)
            )
            Text(
                count.toString(),
                color = if (isSelected) accent else Chalk700,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
