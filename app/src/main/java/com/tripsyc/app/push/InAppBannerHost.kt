package com.tripsyc.app.push

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global in-app banner shown when a push lands while the app is in
 * foreground — replaces the system notification so the user doesn't
 * get a banner that drags them out of whatever they're doing. Tap to
 * route to the trip; auto-dismisses after 4 seconds.
 *
 * Source-of-truth foreground flag flips from MainActivity's onResume
 * / onPause, so TripFcmService can ask "should I suppress the system
 * notification?" before posting one.
 */
object InAppBanner {
    data class Event(
        val title: String,
        val body: String,
        val tripId: String? = null,
        val createdAtMs: Long = System.currentTimeMillis()
    )

    private val foreground = AtomicBoolean(false)
    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    fun setForeground(value: Boolean) { foreground.set(value) }
    fun isForeground(): Boolean = foreground.get()

    fun post(event: Event) { _events.value = event }
    fun dismiss() { _events.value = null }
}

@Composable
fun InAppBannerHost(onTapTrip: (String) -> Unit) {
    val event by InAppBanner.events.collectAsState()

    // 4-second auto-dismiss timer keyed on createdAtMs so re-posting
    // before timeout cancels the prior dismiss.
    LaunchedEffect(event?.createdAtMs) {
        val current = event ?: return@LaunchedEffect
        delay(4000)
        // Only dismiss if it's still the same banner.
        if (InAppBanner.events.value?.createdAtMs == current.createdAtMs) {
            InAppBanner.dismiss()
        }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        val current = event ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    InAppBanner.dismiss()
                    current.tripId?.let { onTapTrip(it) }
                },
            color = Color(0xFF1F1B17),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        current.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Text(
                        current.body,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }
                IconButton(
                    onClick = { InAppBanner.dismiss() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
