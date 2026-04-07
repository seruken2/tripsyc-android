package com.tripsyc.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tripsyc.app.data.api.models.AvailabilityStatus
import com.tripsyc.app.data.api.models.GlobalAvailability
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    var entries by remember { mutableStateOf<List<GlobalAvailability>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try {
                val response = ApiClient.apiService.getGlobalAvailability()
                entries = response.entries
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    fun toggle(date: LocalDate, currentStatus: AvailabilityStatus?) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        scope.launch {
            try {
                if (currentStatus == null) {
                    ApiClient.apiService.setGlobalAvailability(
                        mapOf("dates" to listOf(mapOf("date" to dateStr, "status" to "AVAILABLE")))
                    )
                } else {
                    val next = when (currentStatus) {
                        AvailabilityStatus.AVAILABLE -> AvailabilityStatus.FLEXIBLE
                        AvailabilityStatus.FLEXIBLE -> AvailabilityStatus.UNAVAILABLE
                        AvailabilityStatus.UNAVAILABLE -> null // clear
                    }
                    if (next == null) {
                        ApiClient.apiService.deleteGlobalAvailability(mapOf("dates" to listOf(dateStr)))
                    } else {
                        ApiClient.apiService.setGlobalAvailability(
                            mapOf("dates" to listOf(mapOf("date" to dateStr, "status" to next.name)))
                        )
                    }
                }
                load()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Global Calendar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                Text("Mark your general availability across all trips", fontSize = 14.sp, color = Chalk500)
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Text("< Prev", color = Coral)
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            fontWeight = FontWeight.SemiBold, color = Chalk900
                        )
                        TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Text("Next >", color = Coral)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), fontSize = 11.sp, color = Chalk400,
                                textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Calendar grid
                    val entryMap = entries.associateBy { it.date }
                    val firstDay = currentMonth.atDay(1)
                    val startPadding = firstDay.dayOfWeek.value % 7
                    val daysInMonth = currentMonth.lengthOfMonth()

                    val cells = mutableListOf<LocalDate?>()
                    repeat(startPadding) { cells.add(null) }
                    for (d in 1..daysInMonth) cells.add(currentMonth.atDay(d))

                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date != null) {
                                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        val entry = entryMap[dateStr]
                                        val isPast = date.isBefore(LocalDate.now())
                                        val bgColor = when (entry?.status) {
                                            AvailabilityStatus.AVAILABLE -> Success.copy(alpha = 0.25f)
                                            AvailabilityStatus.FLEXIBLE -> Gold.copy(alpha = 0.25f)
                                            AvailabilityStatus.UNAVAILABLE -> Danger.copy(alpha = 0.15f)
                                            null -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .then(
                                                    if (date == LocalDate.now())
                                                        Modifier.border(2.dp, Coral, CircleShape)
                                                    else Modifier
                                                )
                                                .then(
                                                    if (!isPast) Modifier.clickable { toggle(date, entry?.status) }
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                fontSize = 12.sp,
                                                color = if (isPast) Chalk200 else Chalk900,
                                                fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(color = Success.copy(alpha = 0.3f), label = "Available")
                        LegendDot(color = Gold.copy(alpha = 0.3f), label = "Flexible")
                        LegendDot(color = Danger.copy(alpha = 0.2f), label = "Busy")
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = Dusk.copy(alpha = 0.08f)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡", fontSize = 16.sp)
                    Text(
                        "Tap a date to cycle: Available → Flexible → Busy → Clear",
                        fontSize = 12.sp, color = Chalk500
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = Chalk500)
    }
}
