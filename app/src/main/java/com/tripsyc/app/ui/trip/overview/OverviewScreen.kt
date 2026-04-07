package com.tripsyc.app.ui.trip.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.models.LockType
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.theme.*
import com.tripsyc.app.ui.trip.TripTab

@Composable
fun OverviewScreen(
    trip: Trip,
    currentUser: User?,
    onTabSelected: (TripTab) -> Unit
) {
    val dateLock = trip.locks?.firstOrNull { it.lockType == LockType.DATE }
    val destLock = trip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
    val isDateLocked = dateLock?.locked == true
    val isDestLocked = destLock?.locked == true
    val isConfirmed = isDateLocked && isDestLocked

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (!trip.coverImage.isNullOrEmpty() &&
                    (trip.coverImage.startsWith("https://") || trip.coverImage.startsWith("http://"))
                ) {
                    AsyncImage(
                        model = trip.coverImage,
                        contentDescription = trip.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Coral, CoralLight)))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.6f to Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = trip.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!trip.approxMonth.isNullOrEmpty()) {
                        Text(
                            text = "📅 ${trip.approxMonth}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Status + members section
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        val statusText = when {
                            isConfirmed -> "✅ Confirmed"
                            isDateLocked && !isDestLocked -> "📅 Date Locked"
                            !isDateLocked && isDestLocked -> "📍 Destination Locked"
                            else -> "🗓️ Planning"
                        }
                        Text(text = statusText, color = Chalk500, fontSize = 13.sp)
                    }

                    Divider(color = Chalk200)

                    // Members row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Members (${trip.members?.size ?: trip.count?.members ?: 0})",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        if (!trip.members.isNullOrEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(trip.members) { member ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Coral.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!member.avatarUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = member.avatarUrl,
                                                    contentDescription = member.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                                                    color = Coral,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = member.name.split(" ").firstOrNull() ?: member.name,
                                            fontSize = 10.sp,
                                            color = Chalk500,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Locked values if any
        if (isDateLocked || isDestLocked) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Locked Decisions",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        if (isDateLocked && dateLock?.lockedValue != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                                Text(text = "Date: ${dateLock.lockedValue}", color = Chalk900)
                            }
                        }
                        if (isDestLocked && destLock?.lockedValue != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
                                val destName = trip.destinations?.firstOrNull { it.id == destLock.lockedValue }
                                    ?.let { "${it.city}, ${it.country}" }
                                    ?: destLock.lockedValue
                                Text(text = "Destination: $destName", color = Chalk900)
                            }
                        }
                    }
                }
            }
        }

        // Quick action cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Chalk900
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard(
                        icon = Icons.Default.CalendarMonth,
                        label = "Set Dates",
                        color = Dusk,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Dates) }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Map,
                        label = "Destinations",
                        color = Coral,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Destinations) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard(
                        icon = Icons.Default.AttachMoney,
                        label = "Budget",
                        color = Sage,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Budget) }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Chat,
                        label = "Chat",
                        color = Gold,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Chat) }
                    )
                }
            }
        }

        // Trip progress
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Trip Progress",
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900
                    )
                    listOf(
                        Triple("Dates voted", isDateLocked, Dusk),
                        Triple("Destination voted", isDestLocked, Coral),
                        Triple("Fully confirmed", isConfirmed, Success)
                    ).forEach { (label, done, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, color = Chalk900, fontSize = 14.sp)
                            Icon(
                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) color else Chalk200,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
