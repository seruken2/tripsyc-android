package com.tripsyc.app.ui.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.ui.common.ConfettiBurst
import com.tripsyc.app.ui.theme.*

/**
 * Celebration screen shown right after a trip is created. Mirrors the
 * iOS TripCreatedView: a checkmark animation, "You're set" headline,
 * and two prominent next-step rows (Invite buddies, Add activity).
 * Confetti fires on first composition so first-trip creation feels
 * like a moment, not just a navigation.
 */
@Composable
fun TripCreatedScreen(
    trip: Trip,
    isFirstTrip: Boolean,
    onInviteBuddies: () -> Unit,
    onAddActivity: () -> Unit,
    onDone: () -> Unit
) {
    var checkAppeared by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (checkAppeared) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check-scale"
    )

    LaunchedEffect(Unit) {
        checkAppeared = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier
                    .size(96.dp)
                    .scale(checkScale)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "You're set.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Chalk900
                )
                Text(
                    text = if (isFirstTrip)
                        "\"${trip.name}\" is live. Get your crew on board and pencil in something to do."
                    else
                        "\"${trip.name}\" is live. Pick a next step or jump straight in.",
                    fontSize = 15.sp,
                    color = Chalk500,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NextStepRow(
                    number = 1,
                    icon = Icons.Default.PersonAdd,
                    title = "Invite your travel buddies",
                    subtitle = "Get the crew on board so dates can lock in.",
                    accent = Coral,
                    onClick = onInviteBuddies
                )
                NextStepRow(
                    number = 2,
                    icon = Icons.Default.ListAlt,
                    title = "Drop in a first activity",
                    subtitle = "Even a placeholder helps the group rally.",
                    accent = Dusk,
                    onClick = onAddActivity
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I'll do this later", color = Chalk500, fontWeight = FontWeight.SemiBold)
            }
        }

        // Confetti only fires on first-trip creation so repeat users
        // don't get a parade every time they spin up a new trip.
        if (isFirstTrip) {
            ConfettiBurst(trigger = true)
        }
    }
}

@Composable
private fun NextStepRow(
    number: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Step $number",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        title,
                        color = Chalk900,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(subtitle, color = Chalk500, fontSize = 12.sp)
            }
        }
    }
}
