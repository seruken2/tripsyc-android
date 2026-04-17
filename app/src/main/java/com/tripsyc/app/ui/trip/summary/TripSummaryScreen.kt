package com.tripsyc.app.ui.trip.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.tripsyc.app.data.api.models.*
import com.tripsyc.app.ui.common.WeatherWidget
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TripSummaryScreen(trip: Trip) {
    var freshTrip by remember { mutableStateOf(trip) }
    var weather by remember { mutableStateOf<WeatherResponse?>(null) }
    val scope = rememberCoroutineScope()

    val dateLock = freshTrip.locks?.firstOrNull { it.lockType == LockType.DATE }
    val destLock = freshTrip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
    val lockedDest = freshTrip.destinations?.firstOrNull { it.id == destLock?.lockedValue }
    val memberCount = freshTrip.count?.members ?: freshTrip.members?.size ?: 0

    LaunchedEffect(trip.id) {
        scope.launch {
            try { freshTrip = ApiClient.apiService.getTrip(trip.id) } catch (_: Exception) {}
        }
        scope.launch {
            val dest = freshTrip.destinations?.firstOrNull { it.id == destLock?.lockedValue }
                ?: return@launch
            try {
                weather = ApiClient.apiService.getWeather(
                    city = dest.city.trim(),
                    country = dest.country.trim().ifEmpty { null }
                )
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("✅", fontSize = 40.sp)
            Text(
                text = freshTrip.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Chalk900
            )
            Text(
                text = "All decisions are locked",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Chalk500
            )
        }

        if (dateLock?.locked == true && !dateLock.lockedValue.isNullOrEmpty()) {
            SummaryCard(
                accent = Sage,
                icon = Icons.Default.EventAvailable,
                label = "DATE",
                primary = dateLock.lockedValue
            )
        }

        if (lockedDest != null) {
            SummaryCard(
                accent = Coral,
                icon = Icons.Default.LocationOn,
                label = "DESTINATION",
                primary = "${lockedDest.city}, ${lockedDest.country}",
                trailing = {
                    val min = lockedDest.estimatedCostMin
                    if (min != null) {
                        Spacer(Modifier.height(8.dp))
                        Divider(color = Chalk100)
                        Spacer(Modifier.height(8.dp))
                        Column {
                            Text(
                                "Estimated cost",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Chalk500
                            )
                            val priceText = lockedDest.estimatedCostMax?.let {
                                "${freshTrip.currency} $min – $it"
                            } ?: "${freshTrip.currency} $min"
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    priceText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Chalk900
                                )
                                Text(
                                    "per person",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Chalk500
                                )
                            }
                        }
                    }
                }
            )
        }

        weather?.let { w ->
            WeatherWidget(weather = w, city = lockedDest?.city ?: "", daysUntil = null)
        }

        // Group card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "GROUP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Chalk500
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Chalk100
                    ) {
                        Text(
                            text = "$memberCount traveler${if (memberCount == 1) "" else "s"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk500,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                freshTrip.members?.let { members ->
                    if (members.isNotEmpty()) {
                        Divider(color = Chalk100)
                        members.forEachIndexed { idx, member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Coral.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.firstOrNull()?.uppercase() ?: "?",
                                        color = Coral,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        member.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Chalk900
                                    )
                                }
                                if (member.role == MemberRole.CREATOR) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Coral.copy(alpha = 0.10f)
                                    ) {
                                        Text(
                                            "Creator",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Coral,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < members.lastIndex) Divider(color = Chalk100)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard(
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Chalk500
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Chalk900
                    )
                }
                Icon(Icons.Default.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            trailing?.invoke()
        }
    }
}
