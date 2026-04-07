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
    val status = tripStatus(dateLock, destLock)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        color = CardBackground
    ) {
        Column {
            // Cover image with overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
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
                    // Placeholder gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Coral, CoralLight)
                                )
                            )
                    )
                }

                // Gradient overlay for readability
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

                // Bottom content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
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
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Status badge top-right
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor(status).copy(alpha = 0.9f)
                ) {
                    Text(
                        text = status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Card body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Member count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Chalk500,
                        modifier = Modifier.size(14.dp)
                    )
                    val memberCount = trip.count?.members ?: trip.members?.size ?: 0
                    Text(
                        text = "$memberCount ${if (memberCount == 1) "member" else "members"}",
                        color = Chalk500,
                        fontSize = 12.sp
                    )
                }

                // Member avatars (first 4)
                val members = trip.members?.take(4) ?: emptyList()
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    members.forEachIndexed { index, member ->
                        val initial = member.name.firstOrNull()?.uppercase() ?: "?"
                        Box(
                            modifier = Modifier
                                .size(26.dp)
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Progress strip (4 stages)
            TripProgressStrip(
                dateVoting = true, // Simplified: always show all stages
                dateLocked = dateLock?.locked == true,
                destVoting = true,
                destLocked = destLock?.locked == true
            )
        }
    }
}

@Composable
fun TripProgressStrip(
    dateVoting: Boolean,
    dateLocked: Boolean,
    destVoting: Boolean,
    destLocked: Boolean
) {
    val stages = listOf(
        Pair("Planning", true),
        Pair("Date", dateLocked),
        Pair("Destination", destLocked),
        Pair("Confirmed", dateLocked && destLocked)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        stages.forEachIndexed { index, (_, active) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (active) Coral else Chalk200
                    )
                    .then(
                        if (index == 0) Modifier.clip(
                            RoundedCornerShape(bottomStart = 18.dp)
                        ) else Modifier
                    )
                    .then(
                        if (index == stages.size - 1) Modifier.clip(
                            RoundedCornerShape(bottomEnd = 18.dp)
                        ) else Modifier
                    )
            )
            if (index < stages.size - 1) {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

private fun tripStatus(dateLock: DecisionLock?, destLock: DecisionLock?): String {
    val dl = dateLock?.locked == true
    val dest = destLock?.locked == true
    return when {
        dl && dest -> "Confirmed"
        dl || dest -> "Locked"
        else -> "Planning"
    }
}

private fun statusColor(status: String) = when (status) {
    "Confirmed" -> Success
    "Locked" -> Gold
    else -> Coral
}
