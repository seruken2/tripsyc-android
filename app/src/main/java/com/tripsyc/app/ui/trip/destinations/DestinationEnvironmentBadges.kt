package com.tripsyc.app.ui.trip.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.DestinationEnvironment
import com.tripsyc.app.ui.theme.*

/**
 * Horizontal strip of environment chips for a destination — local time,
 * air quality, pollen. Each chip self-suppresses when the upstream
 * Google API didn't have data, so the strip just collapses instead of
 * showing "N/A" placeholders.
 */
@Composable
fun DestinationEnvironmentBadges(destinationId: String) {
    var environment by remember(destinationId) { mutableStateOf<DestinationEnvironment?>(null) }

    LaunchedEffect(destinationId) {
        environment = try {
            ApiClient.apiService.getDestinationEnvironment(destinationId)
        } catch (_: Exception) {
            null
        }
    }

    val env = environment ?: return
    val hasAnyChip = env.localTime != null || env.airQuality != null || env.pollen != null
    if (!hasAnyChip) return

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        env.localTime?.let { time ->
            EnvChip(
                icon = Icons.Default.AccessTime,
                label = time,
                sublabel = env.timezone?.split("/")?.lastOrNull()?.replace("_", " "),
                accent = Dusk
            )
        }
        env.airQuality?.let { aq ->
            EnvChip(
                icon = Icons.Default.Air,
                label = "AQI ${aq.aqi}",
                sublabel = aq.category,
                accent = aqiAccent(aq.aqi)
            )
        }
        env.pollen?.let { pollen ->
            EnvChip(
                icon = Icons.Default.Grass,
                label = "Pollen",
                sublabel = pollenLabel(pollen.overall),
                accent = pollenAccent(pollen.overall)
            )
        }
    }
}

@Composable
private fun EnvChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String?,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = accent.copy(alpha = 0.18f),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                }
            }
            Column {
                Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (!sublabel.isNullOrEmpty()) {
                    Text(sublabel, color = Chalk500, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun aqiAccent(aqi: Int): Color = when {
    aqi <= 50 -> Success     // Good
    aqi <= 100 -> Gold        // Moderate
    aqi <= 150 -> Coral       // Unhealthy for sensitive
    else -> Danger            // Unhealthy and above
}

private fun pollenLabel(overall: String): String = when (overall) {
    "NONE" -> "None"
    "VERY_LOW" -> "Very low"
    "LOW" -> "Low"
    "MODERATE" -> "Moderate"
    "HIGH" -> "High"
    "VERY_HIGH" -> "Very high"
    else -> overall.lowercase().replaceFirstChar { it.uppercase() }
}

private fun pollenAccent(overall: String): Color = when (overall) {
    "NONE", "VERY_LOW", "LOW" -> Success
    "MODERATE" -> Gold
    "HIGH" -> Coral
    "VERY_HIGH" -> Danger
    else -> Chalk500
}
