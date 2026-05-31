package com.tripwave.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripwave.app.data.prefs.TripPrefsStore
import com.tripwave.app.ui.theme.*

/**
 * Bottom sheet replicating the iOS HelpSheet — replay welcome,
 * sample trip preview, contact support, FAQ link. Mount from anywhere
 * via a `?` icon. Replay-welcome clears the seen flag and routes the
 * user back through the carousel on next launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(onDismiss: () -> Unit, onReplayWelcome: () -> Unit, onShowSampleTrip: () -> Unit) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Chalk50
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "How can we help?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Chalk900,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            HelpRow(
                icon = Icons.Default.Refresh,
                accent = Coral,
                title = "Replay tutorial",
                subtitle = "See the welcome screens again.",
                onClick = {
                    // Clear the seen flag and bubble up so the caller
                    // can route to the welcome flow.
                    val prefs = context.applicationContext.getSharedPreferences(
                        "tripwave.trip_prefs", android.content.Context.MODE_PRIVATE
                    )
                    prefs.edit().putBoolean("welcomeSeen", false).apply()
                    onReplayWelcome()
                }
            )
            HelpRow(
                icon = Icons.Default.Flight,
                accent = Dusk,
                title = "See an example trip",
                subtitle = "What a populated trip looks like.",
                onClick = onShowSampleTrip
            )
            HelpRow(
                icon = Icons.Default.MailOutline,
                accent = Sage,
                title = "Contact support",
                subtitle = "We usually reply within a day.",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hi@tripwave.co?subject=Help")
                    }
                    runCatching { context.startActivity(intent) }
                }
            )
            HelpRow(
                icon = Icons.Default.HelpOutline,
                accent = Gold,
                title = "FAQs",
                subtitle = "Common questions and answers.",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tripwave.co/help"))
                    runCatching { context.startActivity(intent) }
                }
            )
        }
    }
}

@Composable
private fun HelpRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Chalk900, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Chalk500, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Chalk400)
        }
    }
}
