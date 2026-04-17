package com.tripsyc.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.models.WeatherDay
import com.tripsyc.app.data.api.models.WeatherResponse
import com.tripsyc.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

private fun weatherEmoji(icon: String): String = when (icon.lowercase()) {
    "sunny", "clear" -> "☀️"
    "cloudy" -> "⛅"
    "rainy", "drizzle" -> "🌧️"
    "snowy", "snow" -> "❄️"
    "stormy", "thunder" -> "⛈️"
    "fog", "mist" -> "🌫️"
    else -> "🌤️"
}

private fun shortDay(day: String): String {
    val parsers = listOf("yyyy-MM-dd", "EEEE", "EEE")
    for (fmt in parsers) {
        try {
            val df = SimpleDateFormat(fmt, Locale.US)
            df.isLenient = false
            val parsed = df.parse(day) ?: continue
            return SimpleDateFormat("EEE", Locale.US).format(parsed)
        } catch (_: Exception) {
            // try next
        }
    }
    return day.take(3)
}

@Composable
fun WeatherWidget(
    weather: WeatherResponse,
    city: String,
    daysUntil: Int? = null,
    modifier: Modifier = Modifier
) {
    val headline = when {
        daysUntil == null -> "Weather in $city"
        daysUntil == 0 -> "Weather in $city · departing today"
        daysUntil == 1 -> "Weather in $city · 1 day to go"
        else -> "Weather in $city · $daysUntil days to go"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = weatherEmoji(weather.current.icon), fontSize = 18.sp)
                Text(
                    text = headline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Chalk900,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = "${weather.current.temp}°",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Chalk900,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = weather.current.description,
                fontSize = 12.sp,
                color = Chalk500
            )

            weather.matched?.let { matched ->
                if (matched.countryMismatch) {
                    Text(
                        text = "Showing ${matched.city}, ${matched.country} — couldn't find exact match",
                        fontSize = 10.sp,
                        color = Coral
                    )
                }
            }

            if (weather.forecast.isNotEmpty()) {
                Divider(color = Chalk100)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    weather.forecast.take(5).forEach { day ->
                        ForecastDay(day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastDay(day: WeatherDay) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = shortDay(day.day),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Chalk400
        )
        Text(text = weatherEmoji(day.icon), fontSize = 16.sp)
        Text(
            text = "${day.temp}°",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Chalk900,
            fontFamily = FontFamily.Monospace
        )
        day.low?.let {
            Text(
                text = "$it°",
                fontSize = 11.sp,
                color = Chalk400,
                fontFamily = FontFamily.Monospace
            )
        }
        day.precip?.let { p ->
            if (p > 0) {
                Text(
                    text = "💧${p.toInt()}%",
                    fontSize = 9.sp,
                    color = Dusk
                )
            }
        }
    }
}
