package com.tripsyc.app.ui.trip.settings

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
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsScreen(trip: Trip, currentUser: User?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tripName by remember { mutableStateOf(trip.name) }
    var approxMonth by remember { mutableStateOf(trip.approxMonth ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var isLeaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<TripMember>>(trip.members ?: emptyList()) }
    var memberToKick by remember { mutableStateOf<TripMember?>(null) }
    var notifMode by remember { mutableStateOf<NotificationMode?>(null) }
    var isLoadingNotif by remember { mutableStateOf(false) }
    var isSavingNotif by remember { mutableStateOf(false) }

    val myMember = members.firstOrNull { it.userId == currentUser?.id }
    val isOrganizer = myMember?.role?.let { it == MemberRole.CREATOR || it == MemberRole.CO_ORGANIZER } == true

    // Load fresh members + notification prefs
    LaunchedEffect(trip.id) {
        scope.launch {
            try { members = ApiClient.apiService.getMembers(trip.id) } catch (_: Exception) {}
        }
        scope.launch {
            isLoadingNotif = true
            try {
                val pref = ApiClient.apiService.getNotificationPrefs(trip.id)
                notifMode = pref.mode
            } catch (_: Exception) { notifMode = NotificationMode.ALL }
            isLoadingNotif = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Trip Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)

        // ── Trip Details (organizer only) ─────────────────────────────────────
        if (isOrganizer) {
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Trip Details", fontWeight = FontWeight.SemiBold, color = Chalk900)
                    OutlinedTextField(
                        value = tripName, onValueChange = { tripName = it },
                        label = { Text("Trip Name") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                    OutlinedTextField(
                        value = approxMonth, onValueChange = { approxMonth = it },
                        label = { Text("Approx. Month") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                    if (error != null) Text(error!!, color = Danger, fontSize = 13.sp)
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true; error = null
                                try {
                                    ApiClient.apiService.updateTrip(
                                        trip.id,
                                        mapOf("name" to tripName.trim(), "approxMonth" to approxMonth.trim().ifEmpty { null })
                                    )
                                } catch (e: Exception) { error = e.message ?: "Failed to save" }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save Changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Notification Preferences ──────────────────────────────────────────
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Notifications", fontWeight = FontWeight.SemiBold, color = Chalk900)
                if (isLoadingNotif) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Coral)
                } else {
                    val modes = listOf(
                        NotificationMode.ALL to "All Activity",
                        NotificationMode.DIGEST_DAILY to "Daily Digest",
                        NotificationMode.DIGEST_WEEKLY to "Weekly Digest",
                        NotificationMode.LOCKS_ONLY to "Locks Only",
                        NotificationMode.MUTED to "Muted"
                    )
                    modes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = Chalk900, fontSize = 14.sp)
                            RadioButton(
                                selected = notifMode == mode,
                                onClick = {
                                    notifMode = mode
                                    scope.launch {
                                        isSavingNotif = true
                                        try {
                                            ApiClient.apiService.updateNotificationPrefs(
                                                mapOf("tripId" to trip.id, "mode" to mode.name)
                                            )
                                        } catch (_: Exception) {}
                                        isSavingNotif = false
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Coral)
                            )
                        }
                    }
                }
            }
        }

        // ── My Trip Info (plus-one, arrival, chat handle) ─────────────────────
        if (myMember != null) {
            MyTripInfoCard(
                member = myMember,
                onSaved = {
                    scope.launch {
                        try { members = ApiClient.apiService.getMembers(trip.id) }
                        catch (_: Exception) {}
                    }
                }
            )
        }

        // ── Members ───────────────────────────────────────────────────────────
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Members (${members.size})", fontWeight = FontWeight.SemiBold, color = Chalk900)
                members.forEach { member ->
                    MemberRow(
                        member = member,
                        isCurrentUser = member.userId == currentUser?.id,
                        isOrganizer = isOrganizer,
                        myRole = myMember?.role ?: MemberRole.MEMBER,
                        onRoleChange = { newRole ->
                            scope.launch {
                                try {
                                    ApiClient.apiService.updateMember(
                                        mapOf("tripId" to trip.id, "userId" to member.userId, "role" to newRole.name)
                                    )
                                    members = ApiClient.apiService.getMembers(trip.id)
                                } catch (_: Exception) {}
                            }
                        },
                        onKick = { memberToKick = member }
                    )
                }
            }
        }

        // ── Trip Info ─────────────────────────────────────────────────────────
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Trip Info", fontWeight = FontWeight.SemiBold, color = Chalk900)
                SettingsInfoRow("Currency", trip.currency)
                SettingsInfoRow("Invite Code", trip.inviteCode)
                SettingsInfoRow("Members", "${members.size}")
            }
        }

        // ── Danger Zone ───────────────────────────────────────────────────────
        if (myMember?.role != MemberRole.CREATOR) {
            Surface(shape = RoundedCornerShape(16.dp), color = Danger.copy(alpha = 0.05f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Danger Zone", fontWeight = FontWeight.SemiBold, color = Danger)
                    OutlinedButton(
                        onClick = { showLeaveConfirm = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), enabled = !isLeaving
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leave Trip")
                    }
                }
            }
        }
    }

    // Leave confirm dialog
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Trip?") },
            text = { Text("Are you sure you want to leave \"${trip.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        scope.launch {
                            isLeaving = true
                            try { ApiClient.apiService.leaveTrip(mapOf("tripId" to trip.id)); onBack() }
                            catch (e: Exception) { error = e.message ?: "Failed to leave trip" }
                            isLeaving = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Leave") }
            },
            dismissButton = { OutlinedButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") } }
        )
    }

    // Kick confirm dialog
    memberToKick?.let { kickTarget ->
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = { Text("Remove Member?") },
            text = { Text("Remove ${kickTarget.name} from \"${trip.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        memberToKick = null
                        scope.launch {
                            try {
                                ApiClient.apiService.removeMember(
                                    trip.id,
                                    mapOf("userId" to kickTarget.userId)
                                )
                                members = ApiClient.apiService.getMembers(trip.id)
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Remove") }
            },
            dismissButton = { OutlinedButton(onClick = { memberToKick = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberRow(
    member: TripMember,
    isCurrentUser: Boolean,
    isOrganizer: Boolean,
    myRole: MemberRole,
    onRoleChange: (MemberRole) -> Unit,
    onKick: () -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }
    var showTransferConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(Coral.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                member.name.firstOrNull()?.uppercase() ?: "?",
                color = Coral, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                member.name + if (isCurrentUser) " (You)" else "",
                fontWeight = FontWeight.Medium, color = Chalk900, fontSize = 14.sp
            )
            val roleLabel = when (member.role) {
                MemberRole.CREATOR -> "Creator"
                MemberRole.CO_ORGANIZER -> "Co-Organizer"
                MemberRole.MEMBER -> "Member"
            }
            Text(roleLabel, fontSize = 12.sp, color = Chalk400)
        }

        // Role change (organizer only, not for self or creator)
        if (isOrganizer && !isCurrentUser && member.role != MemberRole.CREATOR) {
            Box {
                IconButton(onClick = { showRoleMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = "Change role", tint = Chalk500, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                    if (member.role != MemberRole.CO_ORGANIZER) {
                        DropdownMenuItem(
                            text = { Text("Make Co-Organizer") },
                            onClick = { showRoleMenu = false; onRoleChange(MemberRole.CO_ORGANIZER) }
                        )
                    }
                    if (member.role != MemberRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text("Change to Member") },
                            onClick = { showRoleMenu = false; onRoleChange(MemberRole.MEMBER) }
                        )
                    }
                    // Only the CREATOR can hand off the role — gates a
                    // permanent transfer behind a confirm dialog.
                    if (myRole == MemberRole.CREATOR) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Transfer creator role…",
                                    color = Danger,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                showRoleMenu = false
                                showTransferConfirm = true
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onKick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = Danger.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showTransferConfirm) {
        AlertDialog(
            onDismissRequest = { showTransferConfirm = false },
            title = { Text("Transfer creator role?") },
            text = {
                Text(
                    "Hand the trip over to ${member.name}? You'll become a " +
                    "co-organizer. This can't be undone without their cooperation."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTransferConfirm = false
                        onRoleChange(MemberRole.CREATOR)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Transfer") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTransferConfirm = false }) { Text("Cancel") }
            }
        )
    }

    HorizontalDivider(color = Chalk100, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Chalk500, fontSize = 14.sp)
        Text(value, color = Chalk900, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
