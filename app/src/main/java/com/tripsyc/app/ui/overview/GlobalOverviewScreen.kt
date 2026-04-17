package com.tripsyc.app.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.*
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.common.WeatherWidget
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private fun parseTripDateRange(locked: String): Pair<Date, Date>? {
    val parts = locked.split(" to ")
    if (parts.size != 2) return null
    val fmts = listOf("MMM d, yyyy", "yyyy-MM-dd", "MMMM d, yyyy", "d MMM yyyy")
    var s: Date? = null; var e: Date? = null
    for (fmt in fmts) {
        val df = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
        if (s == null) { s = runCatching { df.parse(parts[0].trim()) }.getOrNull() }
        if (e == null) { e = runCatching { df.parse(parts[1].trim()) }.getOrNull() }
    }
    val start = s ?: return null
    val end = e ?: return null
    return start to end
}

@Composable
fun GlobalOverviewScreen(
    modifier: Modifier = Modifier
) {
    var data by remember { mutableStateOf<OverviewData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var weather by remember { mutableStateOf<WeatherResponse?>(null) }
    var weatherCity by remember { mutableStateOf<String?>(null) }
    var weatherDaysUntil by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadWeather(overview: OverviewData) {
        val now = Date()
        data class Cand(val start: Date, val city: String, val country: String)
        val candidates = overview.tripCosts.mapNotNull { trip ->
            val locked = trip.lockedDestination ?: return@mapNotNull null
            val dates = trip.lockedDates ?: return@mapNotNull null
            val range = parseTripDateRange(dates) ?: return@mapNotNull null
            if (range.second.before(now)) return@mapNotNull null
            val parts = locked.split(", ")
            if (parts.size < 2) return@mapNotNull null
            val country = parts.last().trim()
            val city = parts.dropLast(1).joinToString(", ").trim()
            if (city.isEmpty() || country.isEmpty()) return@mapNotNull null
            Cand(range.first, city, country)
        }.sortedBy { it.start }

        val next = candidates.firstOrNull()
        if (next == null) {
            weather = null; weatherCity = null; weatherDaysUntil = null
            return
        }
        val days = ((next.start.time - now.time) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        try {
            weather = ApiClient.apiService.getWeather(city = next.city, country = next.country)
            weatherCity = next.city
            weatherDaysUntil = days
        } catch (_: Exception) {
            // weather is non-critical, fail silently
        }
    }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val overview = ApiClient.apiService.getOverview()
                data = overview
                loadWeather(overview)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load overview"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    when {
        isLoading && data == null -> Box(modifier = modifier.fillMaxSize()) { LoadingView("Loading overview...") }
        error != null && data == null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(error ?: "Error", color = Danger, textAlign = TextAlign.Center)
                    Button(
                        onClick = { load() },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        else -> {
            val overviewData = data
            if (overviewData == null) {
                Box(modifier = modifier.fillMaxSize()) {
                    EmptyState(
                        icon = "✈️",
                        title = "No trips yet",
                        message = "Create or join a trip to see your overview."
                    )
                }
            } else {
                OverviewContent(
                    data = overviewData,
                    weather = weather,
                    weatherCity = weatherCity,
                    weatherDaysUntil = weatherDaysUntil,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun OverviewContent(
    data: OverviewData,
    weather: WeatherResponse? = null,
    weatherCity: String? = null,
    weatherDaysUntil: Int? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Overview",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Chalk900
                )
                Text(
                    text = "Your travel finances at a glance",
                    fontSize = 14.sp,
                    color = Chalk400
                )
            }
        }

        // Hero stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Total Spent",
                    value = "${data.userCurrency} ${String.format("%.0f", data.totalExpenses)}",
                    icon = Icons.Default.AccountBalance,
                    color = Coral,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "You Owe",
                    value = "${data.userCurrency} ${String.format("%.0f", data.totalOwed)}",
                    icon = Icons.Default.ArrowUpward,
                    color = Danger,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Owed to You",
                    value = "${data.userCurrency} ${String.format("%.0f", data.totalOwedToYou)}",
                    icon = Icons.Default.ArrowDownward,
                    color = Success,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weather for next confirmed trip
        if (weather != null && weatherCity != null) {
            item {
                WeatherWidget(weather = weather, city = weatherCity, daysUntil = weatherDaysUntil)
            }
        }

        // Alerts
        val alertCount = data.packingAlerts.size + data.deadlineAlerts.size + data.availabilityAlerts.size
        if (alertCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Gold.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$alertCount alert${if (alertCount != 1) "s" else ""}",
                                fontWeight = FontWeight.SemiBold,
                                color = Chalk900,
                                fontSize = 14.sp
                            )
                        }
                        data.deadlineAlerts.forEach { alert ->
                            Text(
                                text = "• ${alert.tripName}: ${alert.type} deadline in ${alert.daysLeft} day${if (alert.daysLeft != 1) "s" else ""}",
                                fontSize = 13.sp,
                                color = Chalk600
                            )
                        }
                        data.packingAlerts.forEach { alert ->
                            Text(
                                text = "• ${alert.tripName}: ${alert.unpacked}/${alert.total} items unpacked",
                                fontSize = 13.sp,
                                color = Chalk600
                            )
                        }
                        data.availabilityAlerts.forEach { alert ->
                            Text(
                                text = "• ${alert.tripName}: availability not set",
                                fontSize = 13.sp,
                                color = Chalk600
                            )
                        }
                    }
                }
            }
        }

        // Settlements
        if (data.settlements.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Settlements", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        data.settlements.forEach { settlement ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val directionText = if (settlement.direction == "owe")
                                        "You owe ${settlement.name}" else "${settlement.name} owes you"
                                    Text(directionText, color = Chalk900, fontSize = 13.sp)
                                    if (settlement.tripNames.isNotEmpty()) {
                                        Text(
                                            settlement.tripNames.joinToString(", "),
                                            fontSize = 11.sp,
                                            color = Chalk400
                                        )
                                    }
                                }
                                Text(
                                    text = "${settlement.displayCurrency ?: data.userCurrency} ${
                                        String.format("%.2f", settlement.convertedTotal ?: settlement.amount)
                                    }",
                                    color = if (settlement.direction == "owe") Danger else Success,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Trips breakdown
        if (data.tripCosts.isNotEmpty()) {
            item {
                Text(
                    text = "Trips",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Chalk900
                )
            }
            items(data.tripCosts) { trip ->
                TripCostCard(trip = trip, userCurrency = data.userCurrency)
            }
        }

        // Category breakdown
        if (data.categoryBreakdown.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Spending by Category", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        val total = data.categoryBreakdown.values.sum().let { if (it == 0.0) 1.0 else it }
                        data.categoryBreakdown.entries
                            .sortedByDescending { it.value }
                            .forEach { (cat, amount) ->
                                val pct = (amount / total * 100).toInt()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            cat.lowercase().replaceFirstChar { it.uppercase() },
                                            fontSize = 13.sp,
                                            color = Chalk900
                                        )
                                        Text(
                                            "${data.userCurrency} ${String.format("%.0f", amount)} ($pct%)",
                                            fontSize = 13.sp,
                                            color = Chalk500
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (amount / total).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Coral,
                                        trackColor = Chalk100
                                    )
                                }
                            }
                    }
                }
            }
        }

        // Travel buddies
        if (data.travelBuddies.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Travel Buddies", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        data.travelBuddies.forEach { buddy ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Dusk.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = buddy.name.firstOrNull()?.uppercase() ?: "?",
                                            color = Dusk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(buddy.name, color = Chalk900, fontSize = 13.sp)
                                }
                                Text(
                                    "${buddy.count} trip${if (buddy.count != 1) "s" else ""}",
                                    color = Chalk500,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent activity
        if (data.recentActivity.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Recent Activity", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        data.recentActivity.take(10).forEach { activity ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Coral)
                                        .align(Alignment.Top)
                                        .padding(top = 5.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        activity.message,
                                        fontSize = 13.sp,
                                        color = Chalk900
                                    )
                                    Text(
                                        activity.tripName,
                                        fontSize = 11.sp,
                                        color = Chalk400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty state if no trips
        if (data.tripCosts.isEmpty() && data.settlements.isEmpty() && data.recentActivity.isEmpty()) {
            item {
                EmptyState(
                    icon = "✈️",
                    title = "No trips yet",
                    message = "Create or join a trip to see your spending overview here."
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = Chalk900,
                fontSize = 13.sp
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Chalk500
            )
        }
    }
}

@Composable
private fun TripCostCard(trip: TripCost, userCurrency: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        trip.tripName,
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900,
                        fontSize = 15.sp
                    )
                    if (!trip.approxMonth.isNullOrEmpty()) {
                        Text(trip.approxMonth, fontSize = 12.sp, color = Chalk400)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = when (trip.status) {
                        "Confirmed" -> Success.copy(alpha = 0.1f)
                        "Locked" -> Dusk.copy(alpha = 0.1f)
                        else -> Coral.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = trip.status,
                        fontSize = 11.sp,
                        color = when (trip.status) {
                            "Confirmed" -> Success
                            "Locked" -> Dusk
                            else -> Coral
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Total", fontSize = 10.sp, color = Chalk400)
                    Text(
                        "${trip.currency} ${String.format("%.0f", trip.totalExpenses)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Chalk900
                    )
                }
                Column {
                    Text("My share", fontSize = 10.sp, color = Chalk400)
                    Text(
                        "${trip.currency} ${String.format("%.0f", trip.myShare)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Chalk900
                    )
                }
                Column {
                    Text("I paid", fontSize = 10.sp, color = Chalk400)
                    Text(
                        "${trip.currency} ${String.format("%.0f", trip.iPaid)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Chalk900
                    )
                }
            }

            if (!trip.lockedDates.isNullOrEmpty() || !trip.lockedDestination.isNullOrEmpty()) {
                HorizontalDivider(color = Chalk100)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!trip.lockedDates.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(trip.lockedDates, fontSize = 11.sp, color = Chalk500)
                        }
                    }
                    if (!trip.lockedDestination.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Coral,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(trip.lockedDestination, fontSize = 11.sp, color = Chalk500)
                        }
                    }
                }
            }
        }
    }
}
