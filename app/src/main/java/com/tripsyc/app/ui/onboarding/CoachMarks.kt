package com.tripsyc.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.tripsyc.app.ui.theme.*

/**
 * First-run coach marks. Sequential tip cards over a dimmed scrim
 * that teach the user where the critical surfaces live: trip
 * creation, the six tabs, long-press chat actions, and the More
 * grid. Less surgical than iOS's anchored spotlights, but covers
 * the same educational beats and dismisses with a single Got-it.
 */
@Composable
fun CoachMarksOverlay(onDismiss: () -> Unit) {
    val tips = listOf(
        CoachTip(
            emoji = "+",
            title = "Create a trip",
            body = "Tap the coral + at the top of your Trips list to spin up a new trip. Invite your crew with the invite code or quick-pick from past co-travelers."
        ),
        CoachTip(
            emoji = "🧭",
            title = "Six tabs per trip",
            body = "Each trip has Overview, Dates, Destinations, Budget, Chat, and More. The little coral dots on a tab mean there's unseen activity over there."
        ),
        CoachTip(
            emoji = "💬",
            title = "Long-press a message",
            body = "Long-press any chat message for reactions, reply, copy, edit (within 30 min), or report. Tap + on the composer to drop in a photo."
        ),
        CoachTip(
            emoji = "✨",
            title = "More tab is the bench",
            body = "Polls, Notes, Packing, Itinerary, Smart Plan, Photos, Snaps, Memories, Group Rewind — all live in the More grid so the main tabs stay focused."
        )
    )
    var index by remember { mutableStateOf(0) }
    val tip = tips[index]
    val isLast = index == tips.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Chalk50
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(tip.emoji, fontSize = 40.sp)
                Text(
                    tip.title,
                    color = Chalk900,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    tip.body,
                    color = Chalk500,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    tips.indices.forEach { i ->
                        val selected = i == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (selected) 22.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) Coral else Chalk200)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isLast) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Skip", color = Chalk500, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = {
                            if (isLast) onDismiss() else index += 1
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        Text(
                            if (isLast) "Got it" else "Next",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private data class CoachTip(val emoji: String, val title: String, val body: String)
