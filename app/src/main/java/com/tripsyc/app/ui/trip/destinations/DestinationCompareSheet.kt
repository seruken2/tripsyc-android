package com.tripsyc.app.ui.trip.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.models.Destination
import com.tripsyc.app.data.api.models.VoteValue
import com.tripsyc.app.ui.theme.*

/**
 * Side-by-side destination comparison sheet. Each column is one
 * destination; each row pulls a focused signal (vote tally, cost,
 * distance, dealbreaker count) so the group can run a quick
 * shoot-out without bouncing between detail views.
 *
 * Slimmer than the iOS DestinationCompareView — we don't expose
 * group-fit / been-here / bucket-list on Android yet, so those rows
 * are skipped. Horizontally scrolls so 4+ destinations still fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationCompareSheet(
    destinations: List<Destination>,
    onDismiss: () -> Unit
) {
    val columnWidth = 132.dp
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Compare (${destinations.size})",
                        fontWeight = FontWeight.Bold,
                        color = Chalk900
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Chalk700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        containerColor = Chalk50
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // Header row — destination thumbnails + city/country.
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Spacer(modifier = Modifier.width(96.dp))
                destinations.forEach { dest ->
                    Column(
                        modifier = Modifier.width(columnWidth).padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DestinationThumb(dest, size = 48.dp)
                        Text(
                            dest.city,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            dest.country,
                            fontSize = 10.sp,
                            color = Chalk500,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = Chalk100)

            // Rows
            CompareRow(
                icon = Icons.Default.SwapVert,
                accent = Coral,
                label = "Vote score",
                columnWidth = columnWidth,
                destinations = destinations
            ) { dest ->
                val up = dest.votes?.count { it.value == VoteValue.UP } ?: 0
                val down = dest.votes?.count { it.value == VoteValue.DOWN } ?: 0
                val score = up - down
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (score > 0) "+$score" else "$score",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            score > 0 -> Coral
                            score < 0 -> Chalk400
                            else -> Chalk400.copy(alpha = 0.7f)
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("👍 $up", fontSize = 11.sp, color = Chalk500)
                        Text("👎 $down", fontSize = 11.sp, color = Chalk500)
                    }
                }
            }

            CompareRow(
                icon = Icons.Default.AttachMoney,
                accent = Sage,
                label = "Cost",
                columnWidth = columnWidth,
                destinations = destinations
            ) { dest ->
                val min = dest.estimatedCostMin
                val max = dest.estimatedCostMax
                val text = when {
                    min != null && max != null -> "\$$min–\$$max"
                    min != null -> "from \$$min"
                    max != null -> "up to \$$max"
                    else -> "—"
                }
                Text(
                    text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (text == "—") Chalk400 else Chalk900,
                    textAlign = TextAlign.Center
                )
            }

            CompareRow(
                icon = Icons.Default.Flight,
                accent = Dusk,
                label = "Distance",
                columnWidth = columnWidth,
                destinations = destinations
            ) { dest ->
                val info = dest.distance
                if (info == null) {
                    Text("—", fontSize = 13.sp, color = Chalk400)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${info.km.toInt()} km",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        info.flightTime?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontSize = 11.sp, color = Chalk500)
                        }
                    }
                }
            }

            CompareRow(
                icon = Icons.Default.Warning,
                accent = Danger,
                label = "Dealbreakers",
                columnWidth = columnWidth,
                destinations = destinations
            ) { dest ->
                val count = dest.dealbreakers?.size ?: 0
                if (count == 0) {
                    Text(
                        "None flagged",
                        fontSize = 12.sp,
                        color = Sage,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Danger.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "$count flagged",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Danger,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            CompareRow(
                icon = Icons.Default.Star,
                accent = Gold,
                label = "Shortlist",
                columnWidth = columnWidth,
                destinations = destinations
            ) { dest ->
                if (dest.shortlisted) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Shortlisted",
                        tint = Gold,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("—", color = Chalk400, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CompareRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    label: String,
    columnWidth: androidx.compose.ui.unit.Dp,
    destinations: List<Destination>,
    cell: @Composable (Destination) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(96.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Chalk500
            )
        }
        destinations.forEach { dest ->
            Box(
                modifier = Modifier.width(columnWidth).padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                cell(dest)
            }
        }
    }
    HorizontalDivider(color = Chalk100)
}

@Composable
private fun DestinationThumb(destination: Destination, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Coral.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (!destination.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = destination.imageUrl,
                contentDescription = destination.city,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                destination.city.firstOrNull()?.uppercase() ?: "?",
                color = Coral,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
