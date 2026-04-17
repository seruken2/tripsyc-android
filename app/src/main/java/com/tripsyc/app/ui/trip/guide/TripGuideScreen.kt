package com.tripsyc.app.ui.trip.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.tripsyc.app.data.api.models.ItineraryCategory
import com.tripsyc.app.data.api.models.ItineraryItem
import com.tripsyc.app.data.api.models.Responsibility
import com.tripsyc.app.data.api.models.TripMember
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TripGuideScreen(tripId: String) {
    var items by remember { mutableStateOf<List<ItineraryItem>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<Responsibility>>(emptyList()) }
    var members by remember { mutableStateOf<List<TripMember>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val its = ApiClient.apiService.getItinerary(tripId).items
                val resp = ApiClient.apiService.getResponsibilities(tripId).items
                val mem = runCatching { ApiClient.apiService.getMembers(tripId) }.getOrDefault(emptyList())
                items = its
                tasks = resp
                members = mem
            } catch (e: Exception) {
                error = e.message ?: "Failed to load guide"
            }
            loading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    when {
        loading -> Box(Modifier.fillMaxSize()) { LoadingView("Loading guide...") }
        items.isEmpty() && tasks.isEmpty() -> Box(
            Modifier.fillMaxSize().background(Chalk50)
        ) {
            EmptyState(
                icon = "📖",
                title = "Guide is empty",
                message = "Add itinerary items and tasks to build your trip guide."
            )
        }
        else -> GuideContent(items = items, tasks = tasks, members = members)
    }
}

@Composable
private fun GuideContent(
    items: List<ItineraryItem>,
    tasks: List<Responsibility>,
    members: List<TripMember>
) {
    val emergency = items.filter { it.category == ItineraryCategory.EMERGENCY }
    val grouped = groupByDay(items.filter { it.category != ItineraryCategory.EMERGENCY })
    val pending = tasks.filter { !it.completed }
    val done = tasks.filter { it.completed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (emergency.isNotEmpty()) {
            Section(icon = Icons.Default.LocalHospital, title = "Emergency Contacts", accent = Coral) {
                emergency.forEach { GuideItemRow(it) }
            }
        }

        grouped.forEach { (label, dayItems) ->
            Section(icon = Icons.Default.CalendarMonth, title = label, accent = Dusk) {
                dayItems.forEach { GuideItemRow(it) }
            }
        }

        if (tasks.isNotEmpty()) {
            Section(icon = Icons.Default.CheckCircle, title = "Tasks", accent = Sage) {
                if (pending.isNotEmpty()) {
                    Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Chalk500)
                    pending.forEach { TaskRow(it, members, completed = false) }
                }
                if (done.isNotEmpty()) {
                    Text("Completed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Chalk500)
                    done.forEach { TaskRow(it, members, completed = true) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GuideItemRow(item: ItineraryItem) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = categoryIcon(item.category),
            contentDescription = null,
            tint = Dusk,
            modifier = Modifier.size(18.dp).padding(top = 1.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Chalk900)
            item.confirmationCode?.takeIf { it.isNotBlank() }?.let {
                Text("Conf: $it", fontSize = 11.sp, color = Chalk500)
            }
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = Chalk700)
            }
        }
    }
}

@Composable
private fun TaskRow(task: Responsibility, members: List<TripMember>, completed: Boolean) {
    val assignee = members.firstOrNull { it.userId == task.assignedTo }?.name
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) Sage else Chalk400,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (completed) Chalk500 else Chalk900
            )
            assignee?.let {
                Text("→ $it", fontSize = 11.sp, color = Chalk500)
            }
        }
    }
}

@Composable
private fun Section(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accent: Color,
    body: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            }
            body()
        }
    }
}

private fun categoryIcon(c: ItineraryCategory) = when (c) {
    ItineraryCategory.FLIGHT -> Icons.Default.Flight
    ItineraryCategory.HOTEL -> Icons.Default.Hotel
    ItineraryCategory.ACTIVITY -> Icons.Default.Hiking
    ItineraryCategory.RESTAURANT -> Icons.Default.Restaurant
    ItineraryCategory.TRANSPORT -> Icons.Default.DirectionsCar
    ItineraryCategory.EMERGENCY -> Icons.Default.LocalHospital
    ItineraryCategory.INFO -> Icons.Default.Info
    ItineraryCategory.OTHER -> Icons.Default.MoreHoriz
}

private fun groupByDay(items: List<ItineraryItem>): List<Pair<String, List<ItineraryItem>>> {
    val isoFormats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
    val display = SimpleDateFormat("EEE, MMM d", Locale.US)
    val byDay = sortedMapOf<String, MutableList<ItineraryItem>>()
    val undated = mutableListOf<ItineraryItem>()
    for (it in items) {
        val ds = it.date
        if (ds.isNullOrBlank()) {
            undated += it
            continue
        }
        var parsed = false
        for (fmt in isoFormats) {
            val df = SimpleDateFormat(fmt, Locale.US)
            try {
                val d = df.parse(ds) ?: continue
                val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
                val label = display.format(d)
                byDay.getOrPut(key) { mutableListOf() }.add(it)
                // re-store labelled list in result later
                parsed = true
                // tag label by mapping later
                break
            } catch (_: Exception) {}
            if (parsed) break
        }
        if (!parsed) undated += it
    }
    val labelled = byDay.entries.map { (key, list) ->
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val label = runCatching { display.format(df.parse(key)!!) }.getOrDefault(key)
        label to list.toList()
    }
    return if (undated.isEmpty()) labelled else labelled + ("Unscheduled" to undated.toList())
}
