package com.tripsyc.app.ui.trip.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.RsvpStatus
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Sticky RSVP block on the Overview tab. Lets a viewer flip
 * Going / Maybe / Can't go without digging into Settings, and exposes
 * the note field (MAYBE / CANT_MAKE_IT) and attend-window picker (GOING)
 * inline so the expense-split math stays correct.
 *
 * Mirrors the iOS RsvpHeaderCard so members see the same affordances
 * on whichever device they pick up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsvpHeaderCard(
    initialRsvp: RsvpStatus?,
    initialRsvpNote: String?,
    initialAttendFrom: String?,
    initialAttendUntil: String?,
    lockedTripFrom: LocalDate?,
    lockedTripUntil: LocalDate?,
    onRsvpChanged: (RsvpStatus) -> Unit,
    onWindowChanged: () -> Unit
) {
    var rsvp by remember { mutableStateOf(initialRsvp) }
    var note by remember { mutableStateOf(initialRsvpNote ?: "") }
    var attendFrom by remember { mutableStateOf(initialAttendFrom) }
    var attendUntil by remember { mutableStateOf(initialAttendUntil) }
    var isUpdating by remember { mutableStateOf(false) }
    var noteSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAttendSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun postRsvp(next: RsvpStatus) {
        scope.launch {
            isUpdating = true
            error = null
            try {
                ApiClient.apiService.updateMember(mapOf("rsvp" to next.name))
                rsvp = next
                onRsvpChanged(next)
            } catch (e: Exception) {
                error = e.message ?: "Couldn't update RSVP"
            }
            isUpdating = false
        }
    }

    fun postNote() {
        scope.launch {
            noteSaving = true
            try {
                ApiClient.apiService.updateMember(
                    mapOf("rsvpNote" to note.trim().ifEmpty { null })
                )
            } catch (_: Exception) {
                // Note save is best-effort; the rsvp itself is the contract.
            }
            noteSaving = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Adaptive banner above the pills — only shown when the
            // viewer hasn't answered yet or is on the fence.
            when (rsvp) {
                null -> banner(
                    "Are you in?",
                    "Tap a pill below so the group knows where you land.",
                    Coral
                )
                RsvpStatus.MAYBE -> banner(
                    "Still deciding?",
                    "A quick note helps the group plan around your maybe.",
                    Gold
                )
                else -> {}
            }

            // Three RSVP pills, stretched evenly across the row.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RsvpPill(
                    label = "Going",
                    icon = Icons.Default.Check,
                    selected = rsvp == RsvpStatus.GOING,
                    color = Success,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                    onClick = { postRsvp(RsvpStatus.GOING) }
                )
                RsvpPill(
                    label = "Maybe",
                    icon = Icons.Default.HelpOutline,
                    selected = rsvp == RsvpStatus.MAYBE,
                    color = Gold,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                    onClick = { postRsvp(RsvpStatus.MAYBE) }
                )
                RsvpPill(
                    label = "Can't go",
                    icon = Icons.Default.Close,
                    selected = rsvp == RsvpStatus.CANT_MAKE_IT,
                    color = Danger,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                    onClick = { postRsvp(RsvpStatus.CANT_MAKE_IT) }
                )
            }

            // Note field — only relevant when the viewer hasn't fully
            // committed. Going members don't need to justify themselves.
            if (rsvp == RsvpStatus.MAYBE || rsvp == RsvpStatus.CANT_MAKE_IT) {
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        // Match the server cap so the user can't compose a
                        // message that'll bounce on save.
                        if (it.length <= 140) note = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (rsvp == RsvpStatus.MAYBE) "What would swing you?"
                            else "Anything you'd like the group to know?"
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${note.length}/140",
                        fontSize = 11.sp,
                        color = Chalk400
                    )
                    TextButton(
                        onClick = { postNote() },
                        enabled = !noteSaving && note != (initialRsvpNote ?: "")
                    ) {
                        Text(
                            if (noteSaving) "Saving…" else "Save note",
                            color = Coral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Attend-window row — only meaningful once dates are locked
            // and the viewer is actually GOING.
            if (rsvp == RsvpStatus.GOING && lockedTripFrom != null && lockedTripUntil != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Chalk100,
                    onClick = { showAttendSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Dusk,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your attend window",
                                fontSize = 12.sp,
                                color = Chalk500,
                                fontWeight = FontWeight.Medium
                            )
                            val display = if (attendFrom.isNullOrEmpty() && attendUntil.isNullOrEmpty()) {
                                "Whole trip"
                            } else {
                                "${attendFrom ?: lockedTripFrom} → ${attendUntil ?: lockedTripUntil}"
                            }
                            Text(
                                text = display,
                                fontSize = 14.sp,
                                color = Chalk900,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Chalk400
                        )
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = Danger, fontSize = 12.sp)
            }
        }
    }

    if (showAttendSheet) {
        AttendWindowSheet(
            initialFrom = attendFrom,
            initialUntil = attendUntil,
            lockedFrom = lockedTripFrom,
            lockedUntil = lockedTripUntil,
            onDismiss = { showAttendSheet = false },
            onSave = { fromValue, untilValue ->
                scope.launch {
                    try {
                        ApiClient.apiService.updateMember(
                            mapOf(
                                "attendFrom" to fromValue,
                                "attendUntil" to untilValue
                            )
                        )
                        attendFrom = fromValue
                        attendUntil = untilValue
                        onWindowChanged()
                    } catch (e: Exception) {
                        error = e.message ?: "Couldn't update attend window"
                    }
                    showAttendSheet = false
                }
            }
        )
    }
}

@Composable
private fun banner(title: String, body: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Chalk700, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RsvpPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) color else Color.White
    val fg = if (selected) Color.White else color
    val border = if (selected) color else color.copy(alpha = 0.35f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendWindowSheet(
    initialFrom: String?,
    initialUntil: String?,
    lockedFrom: LocalDate?,
    lockedUntil: LocalDate?,
    onDismiss: () -> Unit,
    onSave: (from: String?, until: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isoFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val tripStart = lockedFrom ?: LocalDate.now()
    val tripEnd = lockedUntil ?: tripStart.plusDays(7)
    var fromDate by remember {
        mutableStateOf(initialFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: tripStart)
    }
    var untilDate by remember {
        mutableStateOf(initialUntil?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: tripEnd)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Set attend window", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            Text(
                "Pick the subset of the trip you'll actually be there for. Expense splits skip days you're not around.",
                fontSize = 12.sp,
                color = Chalk500
            )

            DateStepperRow(
                label = "Arriving",
                value = fromDate,
                min = tripStart,
                max = untilDate,
                isoFormat = isoFormat,
                onChange = { fromDate = it }
            )
            DateStepperRow(
                label = "Leaving",
                value = untilDate,
                min = fromDate,
                max = tripEnd,
                isoFormat = isoFormat,
                onChange = { untilDate = it }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSave(null, null) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Whole trip", color = Chalk700, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onSave(fromDate.format(isoFormat), untilDate.format(isoFormat)) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("Save window", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DateStepperRow(
    label: String,
    value: LocalDate,
    min: LocalDate,
    max: LocalDate,
    isoFormat: DateTimeFormatter,
    onChange: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Chalk100)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Chalk500, fontWeight = FontWeight.Medium)
            Text(value.format(isoFormat), fontSize = 16.sp, color = Chalk900, fontWeight = FontWeight.SemiBold)
        }
        IconButton(
            onClick = { if (value > min) onChange(value.minusDays(1)) },
            enabled = value > min
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Day earlier", tint = Chalk700)
        }
        IconButton(
            onClick = { if (value < max) onChange(value.plusDays(1)) },
            enabled = value < max
        ) {
            Icon(Icons.Default.Add, contentDescription = "Day later", tint = Chalk700)
        }
    }
}
