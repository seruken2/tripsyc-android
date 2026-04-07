package com.tripsyc.app.ui.trip.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.Activity
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ActivityScreen(tripId: String) {
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        isLoading = true
        scope.launch {
            try {
                val response = ApiClient.apiService.getActivity(tripId, limit = 50)
                activities = response.activities
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Activity", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            Text("What's been happening in this trip", fontSize = 14.sp, color = Chalk500)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            item { LoadingView() }
        } else if (activities.isEmpty()) {
            item { EmptyState(icon = "🔔", title = "No activity yet", message = "Activity will appear here as the trip progresses.") }
        } else {
            items(activities) { activity ->
                Surface(shape = RoundedCornerShape(12.dp), color = CardBackground, shadowElevation = 1.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = activityEmoji(activity.type), fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activity.message, color = Chalk900, fontSize = 13.sp)
                            if (!activity.createdAt.isNullOrEmpty()) {
                                Text(formatRelativeTime(activity.createdAt), fontSize = 11.sp, color = Chalk400)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun activityEmoji(type: String): String = when (type.uppercase()) {
    "TRIP_CREATED" -> "🎉"
    "MEMBER_JOINED" -> "👋"
    "DATE_LOCKED" -> "📅"
    "DESTINATION_LOCKED" -> "📍"
    "EXPENSE_ADDED" -> "💸"
    "POLL_CREATED" -> "📊"
    "NOTE_ADDED" -> "📝"
    "PHOTO_UPLOADED" -> "📷"
    else -> "✨"
}

private fun formatRelativeTime(isoString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        val date = sdf.parse(isoString) ?: return ""
        val now = System.currentTimeMillis()
        val diff = now - date.time
        when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    } catch (_: Exception) { "" }
}
