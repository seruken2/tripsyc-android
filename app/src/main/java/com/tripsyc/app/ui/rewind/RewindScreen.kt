package com.tripsyc.app.ui.rewind

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
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
import com.tripsyc.app.data.api.models.RewindResponse
import com.tripsyc.app.data.api.models.RewindTotals
import com.tripsyc.app.ui.theme.*

/**
 * Year-in-travel "Rewind" recap. Pulls GET /api/rewind which returns
 * per-year totals, top buddies, persona, and a trip list. Mirrors the
 * iOS RewindView's primary deck — totals card, persona card, buddies
 * strip, destinations chips, trips grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewindScreen(onBack: () -> Unit) {
    var data by remember { mutableStateOf<RewindResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            data = ApiClient.apiService.getRewind()
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load your year"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tripsyc Rewind", fontWeight = FontWeight.Bold, color = Chalk900)
                        data?.let {
                            Text("${it.year} · ${it.displayName}", fontSize = 12.sp, color = Chalk500)
                        }
                    }
                },
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral)
                }
                error != null -> Text(
                    error!!,
                    color = Danger,
                    modifier = Modifier.padding(16.dp)
                )
                data != null -> RewindContent(data = data!!)
                else -> Text(
                    "No trips logged for this year yet — once you've taken a Tripsyc trip, your wrap will appear here.",
                    color = Chalk500,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RewindContent(data: RewindResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Totals card ──────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Coral, Dusk)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "${data.totals.tripCount} trip${if (data.totals.tripCount == 1) "" else "s"}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "${data.totals.cities} cities · ${data.totals.countries} countries",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TotalsGrid(data.totals)
                }
            }
        }

        // ── Persona card ─────────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Gold.copy(alpha = 0.12f),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(data.persona.emoji, fontSize = 32.sp)
                    Text(
                        "You're a ${data.persona.label}",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(data.persona.blurb, color = Chalk700, fontSize = 14.sp)
                }
            }
        }

        // ── Top buddies ──────────────────────────────────────────
        if (data.topBuddies.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Your travel crew",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            data.topBuddies.forEach { buddy ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Coral.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!buddy.avatarUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = buddy.avatarUrl,
                                                contentDescription = buddy.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                buddy.name.firstOrNull()?.uppercase() ?: "?",
                                                color = Coral,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        buddy.name.split(" ").firstOrNull() ?: buddy.name,
                                        fontSize = 11.sp,
                                        color = Chalk900,
                                        maxLines = 1
                                    )
                                    Text(
                                        "${buddy.trips} trip${if (buddy.trips == 1) "" else "s"}",
                                        fontSize = 10.sp,
                                        color = Chalk500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Destinations ────────────────────────────────────────
        if (data.destinations.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Where you went", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.destinations.forEach { dest ->
                                Surface(shape = RoundedCornerShape(20.dp), color = Dusk.copy(alpha = 0.12f)) {
                                    Text(
                                        dest,
                                        color = Dusk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Top month ───────────────────────────────────────────
        data.topMonth?.let { month ->
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Gold)
                        Column {
                            Text("Your busiest month", color = Chalk500, fontSize = 12.sp)
                            Text(month, color = Chalk900, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsGrid(totals: RewindTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TotalChip(Icons.Default.Bedtime, "${totals.nights} nights", Modifier.weight(1f))
            TotalChip(Icons.Default.Public, "${totals.countries} countries", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TotalChip(Icons.Default.Photo, "${totals.photos} photos", Modifier.weight(1f))
            TotalChip(Icons.Default.Chat, "${totals.chatMessages} messages", Modifier.weight(1f))
        }
        TotalChip(
            Icons.Default.Explore,
            "≈ $${totals.personalSpendUsd} spent",
            Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TotalChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.20f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
