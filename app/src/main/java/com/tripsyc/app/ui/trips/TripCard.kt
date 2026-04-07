package com.tripsyc.app.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.tripsyc.app.data.api.models.DecisionLock
import com.tripsyc.app.data.api.models.LockType
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.ui.theme.*

@Composable
fun TripCard(
    trip: Trip,
    modifier: Modifier = Modifier
) {
    val dateLock = trip.locks?.firstOrNull { it.lockType == LockType.DATE }
    val destLock = trip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
    val status = tripStatus(dateLock, destLock, trip)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        color = CardBackground,
        tonalElevation = 0.dp
    ) {
        Column {
            // ── Cover image with gradient overlay ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                // Cover image or placeholder gradient
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
                    // Coral gradient placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Coral, CoralLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Faint app icon placeholder
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Gradient overlay — clear → black 82% (matches iOS)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.4f to Color.Black.copy(alpha = 0.15f),
                                    1f to Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                )

                // Trip name + month (bottom-left)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, end = 80.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = trip.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (!trip.approxMonth.isNullOrEmpty()) {
                        Text(
                            text = trip.approxMonth,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status badge capsule (bottom-right, matches iOS .ultraThinMaterial)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.80f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Gathering status has a pulsing dot
                        if (status.label == "Gathering") {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(status.color)
                            )
                        }
                        Text(
                            text = status.label,
                            color = status.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Card body: member avatars + member count ──────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 11.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar row + member count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Member avatars (up to 4, overlapping)
                    val members = trip.members?.take(4) ?: emptyList()
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        members.forEach { member ->
                            val initial = member.name.firstOrNull()?.uppercase() ?: "?"
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.White) // white ring
                                    .padding(1.5.dp)
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
                                        text = initial,
                                        color = Coral,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Member count text
                    val memberCount = trip.count?.members ?: trip.members?.size ?: 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Chalk500,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "$memberCount ${if (memberCount == 1) "member" else "members"}",
                            color = Chalk500,
                            fontSize = 13.sp
                        )
                    }
                }

                // Lock indicators (date + destination)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LockBadge(
                        isLocked = dateLock?.locked == true,
                        label = "Dates"
                    )
                    LockBadge(
                        isLocked = destLock?.locked == true,
                        label = "Dest"
                    )
                }
            }

            // ── Progress strip: 4 segments ────────────────────────────────
            TripProgressStrip(
                currentStage = currentStage(dateLock, destLock, trip)
            )
        }
    }
}

@Composable
private fun LockBadge(isLocked: Boolean, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(if (isLocked) Success else Chalk200)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isLocked) Success else Chalk400
        )
    }
}

@Composable
fun TripProgressStrip(currentStage: Int) {
    val stageLabels = listOf("Gather", "Vote", "Lock", "Go!")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Bar segments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= currentStage) Coral else Chalk100)
                )
            }
        }

        // Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            stageLabels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = if (i <= currentStage) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (i <= currentStage) Coral else Chalk400,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ── Helper functions ──────────────────────────────────────────────────────────

private data class TripStatus(val label: String, val color: Color)

private fun tripStatus(dateLock: DecisionLock?, destLock: DecisionLock?, trip: Trip): TripStatus {
    val dl = dateLock?.locked == true
    val dest = destLock?.locked == true
    val memberCount = trip.count?.members ?: trip.members?.size ?: 0
    return when {
        dl && dest -> TripStatus("Confirmed", Success)
        dl || dest -> TripStatus("Locked", Dusk)
        memberCount > 1 -> TripStatus("Voting", Gold)
        else -> TripStatus("Gathering", Coral)
    }
}

private fun currentStage(dateLock: DecisionLock?, destLock: DecisionLock?, trip: Trip): Int {
    val dl = dateLock?.locked == true
    val dest = destLock?.locked == true
    val memberCount = trip.count?.members ?: trip.members?.size ?: 0
    return when {
        dl && dest -> 3
        dl || dest -> 2
        memberCount > 1 -> 1
        else -> 0
    }
}
