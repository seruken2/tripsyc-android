package com.tripsyc.app.ui.trip.unlock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.UnlockStatus
import com.tripsyc.app.data.api.models.UnlockVote
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun UnlockVoteScreen(
    tripId: String,
    lockType: String,  // "DATE" or "DESTINATION"
    currentUser: User?,
    isOrganizer: Boolean
) {
    var vote by remember { mutableStateOf<UnlockVote?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isActing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.apiService.getUnlockVote(tripId)
                vote = response.body()
            } catch (e: Exception) {
                error = e.message ?: "Failed to load unlock vote"
            }
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    if (isLoading) {
        LoadingView("Loading...")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Unlock ${lockType.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Chalk900
            )
            Text(
                text = "Vote to unlock the ${lockType.lowercase()} decision for this trip.",
                fontSize = 14.sp,
                color = Chalk500
            )
        }

        // Error/Success
        error?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Danger.copy(alpha = 0.1f)
            ) {
                Text(
                    text = msg,
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        successMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Success.copy(alpha = 0.1f)
            ) {
                Text(
                    text = msg,
                    color = Success,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        val currentVote = vote
        if (currentVote == null || currentVote.status != UnlockStatus.PENDING) {
            // No pending vote — show request form for organizers
            NoPendingVoteSection(
                hasClosedVote = currentVote != null,
                closedVote = currentVote,
                isOrganizer = isOrganizer,
                reason = reason,
                onReasonChange = { reason = it },
                isActing = isActing,
                onRequest = {
                    scope.launch {
                        isActing = true
                        error = null
                        try {
                            val newVote = ApiClient.apiService.requestUnlock(
                                mapOf(
                                    "tripId" to tripId,
                                    "lockType" to lockType,
                                    "reason" to reason.trim().ifEmpty { null }
                                )
                            )
                            vote = newVote
                            reason = ""
                            successMessage = "Unlock request submitted."
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to request unlock"
                        }
                        isActing = false
                    }
                }
            )
        } else {
            // Pending vote exists — show status and voting UI
            PendingVoteSection(
                vote = currentVote,
                currentUserId = currentUser?.id,
                isActing = isActing,
                onVote = { approve ->
                    scope.launch {
                        isActing = true
                        error = null
                        try {
                            val updated = ApiClient.apiService.castUnlockBallot(
                                mapOf(
                                    "unlockVoteId" to currentVote.id,
                                    "approve" to approve
                                )
                            )
                            vote = updated
                            successMessage = if (approve) "You approved the unlock." else "You rejected the unlock."
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to cast vote"
                        }
                        isActing = false
                    }
                }
            )
        }
    }
}

@Composable
private fun NoPendingVoteSection(
    hasClosedVote: Boolean,
    closedVote: UnlockVote?,
    isOrganizer: Boolean,
    reason: String,
    onReasonChange: (String) -> Unit,
    isActing: Boolean,
    onRequest: () -> Unit
) {
    if (hasClosedVote && closedVote != null) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardBackground,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (closedVote.status == UnlockStatus.APPROVED)
                            Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (closedVote.status == UnlockStatus.APPROVED) Success else Danger,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Previous vote: ${closedVote.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900
                    )
                }
                closedVote.reason?.let { r ->
                    Text("Reason: $r", fontSize = 13.sp, color = Chalk500)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Approved: ${closedVote.approveCount ?: 0}",
                        fontSize = 13.sp,
                        color = Success
                    )
                    Text(
                        "Rejected: ${closedVote.rejectCount ?: 0}",
                        fontSize = 13.sp,
                        color = Danger
                    )
                }
            }
        }
    }

    if (isOrganizer) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardBackground,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Request Unlock",
                    fontWeight = FontWeight.SemiBold,
                    color = Chalk900,
                    fontSize = 16.sp
                )
                Text(
                    text = "As an organizer, you can request members to vote on unlocking this decision.",
                    fontSize = 13.sp,
                    color = Chalk500
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("Why should this be unlocked?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Button(
                    onClick = onRequest,
                    enabled = !isActing,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    if (isActing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Request Unlock", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Chalk100
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Chalk400,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Only organizers can request an unlock vote.",
                    color = Chalk500,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PendingVoteSection(
    vote: UnlockVote,
    currentUserId: String?,
    isActing: Boolean,
    onVote: (Boolean) -> Unit
) {
    // Current vote status card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active Unlock Vote", fontWeight = FontWeight.SemiBold, color = Chalk900)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Gold.copy(alpha = 0.1f)
                ) {
                    Text(
                        "Pending",
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            vote.reason?.let { r ->
                Text(
                    text = "Reason: $r",
                    fontSize = 13.sp,
                    color = Chalk500
                )
            }

            // Vote counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${vote.approveCount ?: 0} approve",
                        fontSize = 13.sp,
                        color = Success,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbDown,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${vote.rejectCount ?: 0} reject",
                        fontSize = 13.sp,
                        color = Danger,
                        fontWeight = FontWeight.Medium
                    )
                }
                vote.memberCount?.let { mc ->
                    Text(
                        "of $mc members",
                        fontSize = 13.sp,
                        color = Chalk400
                    )
                }
            }

            // Progress bar
            val total = (vote.approveCount ?: 0) + (vote.rejectCount ?: 0)
            val approveRatio = if (total > 0) (vote.approveCount ?: 0).toFloat() / total else 0f
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { approveRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Success,
                    trackColor = Danger.copy(alpha = 0.3f)
                )
            }
        }
    }

    // Check if current user has already voted
    val myBallot = vote.ballots?.firstOrNull { it.userId == currentUserId }

    if (myBallot != null) {
        // Already voted
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (myBallot.approve) Success.copy(alpha = 0.08f) else Danger.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (myBallot.approve) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                    contentDescription = null,
                    tint = if (myBallot.approve) Success else Danger,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "You voted: ${if (myBallot.approve) "Approve" else "Reject"}",
                    color = if (myBallot.approve) Success else Danger,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    } else if (currentUserId != null) {
        // Not yet voted — show approve/reject buttons
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Cast your vote",
                fontWeight = FontWeight.SemiBold,
                color = Chalk900,
                fontSize = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onVote(true) },
                    enabled = !isActing,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    if (isActing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Approve", fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedButton(
                    onClick = { onVote(false) },
                    enabled = !isActing,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = Chalk900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Chalk300)
                ) {
                    Icon(
                        Icons.Default.ThumbDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
