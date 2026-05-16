package com.tripsyc.app.ui.trip.dates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
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
import com.tripsyc.app.data.api.models.*
import com.tripsyc.app.ui.common.ErrorView
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DatesScreen(
    tripId: String,
    isOrganizer: Boolean,
    existingLock: DecisionLock?,
    tripName: String
) {
    var availabilityResponse by remember { mutableStateOf<AvailabilityResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var holidayCountry by remember { mutableStateOf<String?>(null) }
    var holidays by remember { mutableStateOf<List<com.tripsyc.app.data.api.models.Holiday>>(emptyList()) }
    var showCountryPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val isLocked = existingLock?.locked == true

    suspend fun loadHolidays(country: String, year: Int) {
        try {
            holidays = ApiClient.apiService.getHolidays(country, year).holidays
        } catch (_: Exception) {
            holidays = emptyList()
        }
    }

    LaunchedEffect(holidayCountry, currentMonth.year) {
        val country = holidayCountry
        if (country != null) loadHolidays(country, currentMonth.year)
        else holidays = emptyList()
    }

    LaunchedEffect(tripId) {
        isLoading = true
        try {
            availabilityResponse = ApiClient.apiService.getAvailability(tripId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load availability"
        }
        isLoading = false
    }

    fun toggleAvailability(date: LocalDate, status: AvailabilityStatus) {
        if (isLocked) return
        scope.launch {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                ApiClient.apiService.setAvailability(
                    mapOf(
                        "tripId" to tripId,
                        "dates" to listOf(mapOf("date" to dateStr, "status" to status.name))
                    )
                )
                availabilityResponse = ApiClient.apiService.getAvailability(tripId)
            } catch (e: Exception) {
                toastMessage = e.message ?: "Failed to set availability"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Availability", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                    Text("Mark your availability for the group", fontSize = 14.sp, color = Chalk500)
                }
                if (isLocked) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Gold)
                }
            }
        }

        // Holiday picker — pick a country to overlay public holidays
        // on the calendar (long-weekend planning). Server hosts the
        // ruleset so we don't ship 400KB of country data client-side.
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Gold.copy(alpha = 0.08f),
                onClick = { showCountryPicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Long-weekend planner", color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = holidayCountry?.let {
                                val count = holidays.count { h -> h.date.startsWith(currentMonth.toString()) }
                                "$it · $count holiday${if (count == 1) "" else "s"} this month"
                            } ?: "Pick a country to surface public holidays",
                            color = Chalk500,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Chalk400)
                }
            }
        }

        // Surface the holidays falling in the visible month.
        val visibleHolidays = holidays.filter { it.date.startsWith(currentMonth.toString()) }
        if (visibleHolidays.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = CardBackground, shadowElevation = 1.dp) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleHolidays.forEach { h ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    h.date.takeLast(5),
                                    color = Gold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(h.name, color = Chalk900, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        if (isLocked && existingLock?.lockedValue != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Success.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success)
                        Column {
                            Text("Dates Locked!", fontWeight = FontWeight.SemiBold, color = Chalk900)
                            Text(existingLock.lockedValue, color = Chalk500, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Calendar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
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
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Text("Next >", color = Coral)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                color = Chalk400,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Calendar grid
                    val aggregated = availabilityResponse?.aggregated ?: emptyMap()
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date != null) {
                                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        val counts = aggregated[dateStr]
                                        val isPast = date.isBefore(LocalDate.now())
                                        val bgColor = when {
                                            counts == null || counts.available + counts.flexible == 0 -> Color.Transparent
                                            counts.available >= (availabilityResponse?.threshold ?: 1) -> Success.copy(alpha = 0.2f)
                                            counts.flexible > 0 -> Gold.copy(alpha = 0.2f)
                                            else -> Danger.copy(alpha = 0.1f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .then(
                                                    if (!isPast && !isLocked)
                                                        Modifier.clickable {
                                                            val currentStatus = availabilityResponse?.raw
                                                                ?.firstOrNull { it.date.startsWith(dateStr) }
                                                                ?.status
                                                            val nextStatus = when (currentStatus) {
                                                                null -> AvailabilityStatus.AVAILABLE
                                                                AvailabilityStatus.AVAILABLE -> AvailabilityStatus.FLEXIBLE
                                                                AvailabilityStatus.FLEXIBLE -> AvailabilityStatus.UNAVAILABLE
                                                                AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.AVAILABLE
                                                            }
                                                            toggleAvailability(date, nextStatus)
                                                        }
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
                            // Pad remaining cells
                            repeat(7 - week.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendItem(color = Success.copy(alpha = 0.3f), label = "Available")
                        LegendItem(color = Gold.copy(alpha = 0.3f), label = "Flexible")
                        LegendItem(color = Danger.copy(alpha = 0.2f), label = "Unavailable")
                    }
                }
            }
        }

        // Best dates
        if ((availabilityResponse?.bestDates ?: emptyList()).isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Best Dates", fontWeight = FontWeight.SemiBold, color = Chalk900, fontSize = 16.sp)
                    availabilityResponse?.bestDates?.take(5)?.forEach { best ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Success.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(best.date, color = Chalk900, fontWeight = FontWeight.Medium)
                                Text(
                                    "${best.available} available · ${best.flexible} flexible",
                                    color = Chalk500,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item { LoadingView("Loading availability...") }
        }
        if (error != null) {
            item { ErrorView(message = error!!) }
        }

        if (toastMessage != null) {
            item {
                Text(
                    text = toastMessage!!,
                    color = Danger,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("Pick a country") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(SUPPORTED_HOLIDAY_COUNTRIES) { (code, name) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            onClick = {
                                holidayCountry = code
                                showCountryPicker = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (code == holidayCountry) "✓ $name" else name,
                                    color = if (code == holidayCountry) Coral else Chalk900,
                                    fontWeight = if (code == holidayCountry) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    holidayCountry = null
                    showCountryPicker = false
                }) {
                    Text("Clear", color = Chalk500)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCountryPicker = false }) { Text("Close") }
            }
        )
    }
}

// Short list of common holiday-supported countries. The server side
// handles ~202 countries but we hand-curate the top-tappable ones so
// the picker isn't a wall of obscure codes.
private val SUPPORTED_HOLIDAY_COUNTRIES = listOf(
    "US" to "United States",
    "GB" to "United Kingdom",
    "CA" to "Canada",
    "AU" to "Australia",
    "NZ" to "New Zealand",
    "DE" to "Germany",
    "FR" to "France",
    "ES" to "Spain",
    "IT" to "Italy",
    "PT" to "Portugal",
    "NL" to "Netherlands",
    "IE" to "Ireland",
    "JP" to "Japan",
    "KR" to "South Korea",
    "CN" to "China",
    "HK" to "Hong Kong",
    "TH" to "Thailand",
    "VN" to "Vietnam",
    "PH" to "Philippines",
    "SG" to "Singapore",
    "IN" to "India",
    "AE" to "UAE",
    "ZA" to "South Africa",
    "EG" to "Egypt",
    "BR" to "Brazil",
    "MX" to "Mexico",
    "AR" to "Argentina",
    "CO" to "Colombia"
)

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 11.sp, color = Chalk500)
    }
}
