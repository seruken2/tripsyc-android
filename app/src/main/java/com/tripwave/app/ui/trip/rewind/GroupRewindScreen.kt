package com.tripwave.app.ui.trip.rewind

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Photo
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
import com.tripwave.app.data.api.ApiClient
import com.tripwave.app.data.api.models.GroupRewindResponse
import com.tripwave.app.ui.theme.*

/**
 * Per-trip group recap. Mirrors iOS GroupRewindResponse: trip header,
 * totals (members / nights / messages / photos / expenses / spend),
 * member roll, superlatives ("Most generous", "Most messages", etc.),
 * and the list of destinations that were on the table before lock-in.
 */
@Composable
fun GroupRewindScreen(tripId: String) {
    var data by remember(tripId) { mutableStateOf<GroupRewindResponse?>(null) }
    var isLoading by remember(tripId) { mutableStateOf(true) }
    var error by remember(tripId) { mutableStateOf<String?>(null) }

    LaunchedEffect(tripId) {
        try {
            data = ApiClient.apiService.getGroupRewind(tripId)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load group recap"
        }
        isLoading = false
    }

    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Coral)
        }
        error != null -> Text(error!!, color = Danger, modifier = Modifier.padding(16.dp))
        data != null -> Content(data!!)
    }
}

@Composable
private fun Content(data: GroupRewindResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Trip header card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Dusk, Coral)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "GROUP REWIND",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        data.trip.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        listOfNotNull(
                            data.trip.destination,
                            data.trip.country,
                            // Pretty range when both ends are present;
                            // otherwise the formatted single date so
                            // the header never shows raw ISO.
                            data.trip.startDate?.let { start ->
                                val end = data.trip.endDate
                                if (end != null) {
                                    com.tripwave.app.ui.common.formatLockedDateRange("$start to $end")
                                } else {
                                    com.tripwave.app.ui.common.formatIsoDate(start)
                                }
                            }
                        ).joinToString(" · "),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Totals grid
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("By the numbers", fontWeight = FontWeight.SemiBold, color = Chalk900)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TotalCell(Icons.Default.Group, "${data.totals.memberCount} travelers", Modifier.weight(1f))
                        TotalCell(Icons.Default.Bedtime, "${data.totals.nights} nights", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TotalCell(Icons.Default.Chat, "${data.totals.chatMessages} messages", Modifier.weight(1f))
                        TotalCell(Icons.Default.Photo, "${data.totals.photos} photos", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TotalCell(Icons.Default.Paid, "${data.totals.expenseCount} expenses", Modifier.weight(1f))
                        TotalCell(Icons.Default.Paid, "≈ $${data.totals.totalSpendUsd} total", Modifier.weight(1f))
                    }
                }
            }
        }

        // Members
        if (data.members.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("The crew", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            data.members.forEach { m ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Coral.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!m.avatarUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = m.avatarUrl,
                                                contentDescription = m.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                m.name.firstOrNull()?.uppercase() ?: "?",
                                                color = Coral,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        m.name.split(" ").firstOrNull() ?: m.name,
                                        fontSize = 11.sp,
                                        color = Chalk900,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Superlatives
        if (data.superlatives.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold)
                            Text("Superlatives", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        }
                        data.superlatives.forEach { s ->
                            Surface(shape = RoundedCornerShape(12.dp), color = Chalk100) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Coral.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!s.memberAvatar.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = s.memberAvatar,
                                                contentDescription = s.memberName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                s.memberName.firstOrNull()?.uppercase() ?: "?",
                                                color = Coral,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.title, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(s.label, color = Chalk900, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(
                                            "${s.memberName} · ${s.value}",
                                            color = Chalk500,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (data.destinationsConsidered.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Almost went to", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.destinationsConsidered.forEach { d ->
                                Surface(shape = RoundedCornerShape(20.dp), color = Dusk.copy(alpha = 0.12f)) {
                                    Text(
                                        d,
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
    }
}

@Composable
private fun TotalCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Chalk100)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Chalk500, modifier = Modifier.size(14.dp))
        Text(label, color = Chalk900, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
