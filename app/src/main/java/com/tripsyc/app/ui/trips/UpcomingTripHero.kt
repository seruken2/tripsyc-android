package com.tripsyc.app.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.tripsyc.app.data.api.models.LockType
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Animated countdown hero. Surfaces the soonest upcoming trip whose
 * dates are already locked at the top of the list — replaces the
 * generic empty-state space with a "Bali · 12 days" tile that
 * recomputes every minute so the user sees the countdown tick.
 *
 * Returns null when there's no upcoming locked trip; the caller
 * should hide the slot in that case.
 */
@Composable
fun UpcomingTripHero(
    trips: List<Trip>,
    onTap: (Trip) -> Unit
): Boolean {
    // Pick the trip with the soonest locked start date that's in the
    // future. Skips trips with no DATE lock or with start dates in
    // the past (those are "completed" or "ongoing" — handled by the
    // regular list rows).
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        // 60s tick keeps the days-remaining label honest without
        // hammering recomposition.
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }

    val today = now.toLocalDate()
    val nextTrip = trips
        .mapNotNull { trip ->
            val start = trip.lockedStartDate() ?: return@mapNotNull null
            if (start.isBefore(today)) return@mapNotNull null
            trip to start
        }
        .minByOrNull { it.second }

    if (nextTrip == null) {
        // Fall through to a memory flashback when no upcoming trip
        // exists — anniversary today / last year / N years ago lands
        // a "Remember when…" card instead of an empty slot.
        val anchor = computeMemoryAnchor(trips, today)
        if (anchor != null) {
            MemoryFlashbackCard(anchor = anchor, onTap = { onTap(anchor.trip) })
            return true
        }
        return false
    }

    val (trip, start) = nextTrip
    val daysLeft = ChronoUnit.DAYS.between(today, start).coerceAtLeast(0)
    val palette = paletteForDays(daysLeft)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(palette.gradient))
            .clickable { onTap(trip) }
    ) {
        if (!trip.coverImage.isNullOrEmpty() &&
            (trip.coverImage.startsWith("https://") || trip.coverImage.startsWith("http://"))) {
            AsyncImage(
                model = trip.coverImage,
                contentDescription = trip.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Readability scrim — bottom darkening so light covers still
        // show the trip name.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                palette.eyebrow,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                trip.name,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = when (daysLeft) {
                        0L -> "Today"
                        1L -> "Tomorrow"
                        else -> "$daysLeft days to go"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    return true
}

private data class HeroPalette(val gradient: List<Color>, val eyebrow: String)

private fun paletteForDays(days: Long): HeroPalette = when {
    days <= 1 -> HeroPalette(listOf(Coral, Color(0xFFE85C90)), "ALMOST THERE")
    days <= 7 -> HeroPalette(listOf(Coral, Gold), "NEXT WEEK")
    days <= 30 -> HeroPalette(listOf(Dusk, Coral), "COMING UP")
    days <= 90 -> HeroPalette(listOf(Dusk, Sage), "ON THE HORIZON")
    else -> HeroPalette(listOf(Sage, Dusk), "SOMEDAY")
}

private fun Trip.lockedStartDate(): LocalDate? {
    val lock = locks?.firstOrNull { it.lockType == LockType.DATE && it.locked } ?: return null
    val value = lock.lockedValue ?: return null
    val first = value.split(" to ").firstOrNull()?.trim() ?: return null
    return runCatching { LocalDate.parse(first) }.getOrNull()
}

private data class MemoryAnchor(
    val trip: Trip,
    val yearsAgo: Int
)

/// Finds a past trip whose date range covered today's month/day in
/// some prior year (up to 9 back). Picks the most recent anniversary
/// so we don't rotate through the same ancient trip every day.
private fun computeMemoryAnchor(trips: List<Trip>, today: LocalDate): MemoryAnchor? {
    var best: MemoryAnchor? = null
    for (trip in trips) {
        val lock = trip.locks?.firstOrNull { it.lockType == LockType.DATE && it.locked } ?: continue
        val value = lock.lockedValue ?: continue
        val parts = value.split(" to ")
        if (parts.size != 2) continue
        val start = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: continue
        val end = runCatching { LocalDate.parse(parts[1].trim()) }.getOrNull() ?: continue
        if (!end.isBefore(today)) continue   // only past trips
        for (y in 1..9) {
            val anniversary = today.minusYears(y.toLong())
            if (!anniversary.isBefore(start) && !anniversary.isAfter(end)) {
                if (best == null || y < best.yearsAgo) {
                    best = MemoryAnchor(trip, y)
                }
                break
            }
        }
    }
    return best
}

@Composable
private fun MemoryFlashbackCard(anchor: MemoryAnchor, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Dusk, Coral)))
            .clickable { onTap() }
    ) {
        if (!anchor.trip.coverImage.isNullOrEmpty() &&
            (anchor.trip.coverImage.startsWith("https://") || anchor.trip.coverImage.startsWith("http://"))
        ) {
            AsyncImage(
                model = anchor.trip.coverImage,
                contentDescription = anchor.trip.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                "${anchor.yearsAgo} YEAR${if (anchor.yearsAgo == 1) "" else "S"} AGO TODAY",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                anchor.trip.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap to revisit",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
