package com.tripsyc.app.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.PresenceResponse
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Live presence strip that surfaces "Sarah is here right now" or
 * "Jordan was here 3m ago". Polls /api/trips/:id/presence every 30s
 * while mounted, fires a heartbeat at the same cadence so the viewer
 * shows up in everyone else's strip. Self-suppresses when nobody else
 * is around so the slot is invisible in the common case.
 *
 * REST polling matches the web client; iOS uses Supabase Realtime for
 * the same surface but the REST endpoint is the canonical fallback
 * the server already maintains (no extra deps needed on Android).
 */
@Composable
fun LivePresenceRow(tripId: String) {
    var presence by remember(tripId) { mutableStateOf<PresenceResponse?>(null) }

    LaunchedEffect(tripId) {
        while (true) {
            try {
                // Fire heartbeat first so we show up in our own
                // friends' fetches that happen the next tick.
                runCatching { ApiClient.apiService.heartbeatPresence(tripId) }
                presence = ApiClient.apiService.getPresence(tripId)
            } catch (_: Exception) {
                // Best-effort. Endpoint returns empty viewers when
                // Redis isn't configured; we just skip the row.
            }
            delay(30_000)
        }
    }

    val p = presence ?: return
    if (p.viewers.isEmpty() && p.recentlySeen.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = Sage.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (p.viewers.isNotEmpty()) Sage else Chalk400)
            )

            // Avatars
            Row(horizontalArrangement = Arrangement.spacedBy(-6.dp)) {
                val avatarPool = (p.viewers.map { it.userId to (it.name to it.avatarUrl) } +
                    p.recentlySeen.map { it.userId to (it.name to it.avatarUrl) })
                    .distinctBy { it.first }
                    .take(4)
                avatarPool.forEach { (_, pair) ->
                    val (name, avatar) = pair
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Coral.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                color = Coral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = presenceLabel(p),
                color = Chalk700,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun presenceLabel(p: PresenceResponse): String {
    val viewers = p.viewers
    val recent = p.recentlySeen
    return when {
        viewers.size == 1 && recent.isEmpty() -> "${viewers[0].name} is here"
        viewers.size > 1 && recent.isEmpty() ->
            "${viewers[0].name} and ${viewers.size - 1} other${if (viewers.size - 1 == 1) "" else "s"} are here"
        viewers.isEmpty() && recent.size == 1 -> "${recent[0].name} was here ${formatAgo(recent[0].secondsAgo)}"
        viewers.isEmpty() && recent.size > 1 ->
            "${recent[0].name} + ${recent.size - 1} more here recently"
        else -> "${viewers.size} here · ${recent.size} recent"
    }
}

private fun formatAgo(seconds: Int): String = when {
    seconds < 60 -> "just now"
    seconds < 60 * 60 -> "${seconds / 60}m ago"
    else -> "${seconds / 3600}h ago"
}
