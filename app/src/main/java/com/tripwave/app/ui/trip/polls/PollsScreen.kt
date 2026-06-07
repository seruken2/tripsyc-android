package com.tripwave.app.ui.trip.polls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripwave.app.data.api.ApiClient
import com.tripwave.app.data.api.models.PollOptionWithVotes
import com.tripwave.app.data.api.models.PollWithVotes
import com.tripwave.app.data.api.models.User
import com.tripwave.app.ui.common.EmptyState
import com.tripwave.app.ui.common.LoadingView
import com.tripwave.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(tripId: String, currentUser: User?) {
    var polls by remember { mutableStateOf<List<PollWithVotes>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try { polls = ApiClient.apiService.getPolls(tripId) }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Polls", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Coral, contentColor = Color.White, modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isLoading) { item { LoadingView() } }
        else if (polls.isEmpty()) {
            item { EmptyState(icon = "📊", title = "No polls yet", message = "Create a poll to get the group's opinion.",
                actionLabel = "Create Poll", onAction = { showAddSheet = true }) }
        } else {
            items(polls) { poll ->
                PollCard(
                    poll = poll,
                    currentUserId = currentUser?.id,
                    onVote = { optionId ->
                        scope.launch {
                            try {
                                ApiClient.apiService.votePoll(mapOf("pollId" to poll.id, "optionId" to optionId))
                                load()
                            } catch (_: Exception) {}
                        }
                    },
                    onRankVote = { optionId, rank ->
                        scope.launch {
                            try {
                                ApiClient.apiService.votePoll(
                                    mapOf(
                                        "pollId" to poll.id,
                                        "optionId" to optionId,
                                        "rank" to rank
                                    )
                                )
                                load()
                            } catch (_: Exception) {}
                        }
                    },
                    onClose = {
                        scope.launch {
                            try {
                                ApiClient.apiService.closePoll(mapOf("pollId" to poll.id))
                                load()
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        CreatePollSheet(tripId = tripId, onDismiss = { showAddSheet = false }, onCreated = { load() })
    }
}

@Composable
private fun PollCard(
    poll: PollWithVotes,
    currentUserId: String?,
    onVote: (String) -> Unit,
    onRankVote: (String, Int) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val isClosed = poll.closedAt != null
    val isRanked = poll.kind == "RANKED"
    val totalVotes = poll.options.sumOf { it.voteCount }

    Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(poll.question, fontWeight = FontWeight.SemiBold, color = Chalk900)
                    if (isRanked) {
                        Text(
                            "Ranked-choice · tap numbers to order your picks",
                            color = Dusk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (isClosed) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Chalk200) {
                        Text("Closed", color = Chalk500, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }

            poll.options.forEach { option ->
                val pct = if (totalVotes > 0) option.voteCount.toFloat() / totalVotes else 0f
                val isWinner = poll.winnerOptionId == option.id
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isRanked && !isClosed) {
                                // Numbered rank chips 1..N; tap a rank to
                                // assign this option that position.
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val maxRank = poll.options.size
                                    for (n in 1..maxRank) {
                                        val isSelected = option.myRank == n
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Dusk else Chalk100,
                                            onClick = { onRankVote(option.id, n) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    n.toString(),
                                                    color = if (isSelected) Color.White else Chalk700,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (!isClosed) {
                                RadioButton(
                                    selected = option.votedByMe,
                                    onClick = { onVote(option.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Coral)
                                )
                            } else {
                                if (option.votedByMe || isWinner) {
                                    Text("✓", color = Coral, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            Text(option.text, color = Chalk900, fontSize = 14.sp)
                            if (isWinner) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Gold.copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        "Winner",
                                        color = Gold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        // For ranked polls we surface raw vote counts of
                        // "first-place" votes; the IRV winner is the gold
                        // tag, so the count column is informational.
                        Text("${option.voteCount}", color = Chalk500, fontSize = 13.sp)
                    }
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (isWinner) Gold else Coral,
                        trackColor = Chalk200
                    )
                }
            }

            if (!isClosed && poll.createdBy == currentUserId) {
                TextButton(onClick = onClose, contentPadding = PaddingValues(0.dp)) {
                    Text("Close poll", color = Chalk500, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePollSheet(tripId: String, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rankedMode by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Create Poll", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Question *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

            Text("Options (min 2)", color = Chalk500, fontSize = 13.sp)
            options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { v -> options = options.toMutableList().also { it[index] = v } },
                        label = { Text("Option ${index + 1}") },
                        modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                    if (options.size > 2) {
                        IconButton(onClick = { options = options.toMutableList().also { it.removeAt(index) } }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Chalk400)
                        }
                    }
                }
            }
            if (options.size < 6) {
                TextButton(onClick = { options = options + "" }) { Text("+ Add option", color = Coral) }
            }

            // Ranked-choice toggle. Server interprets kind=RANKED as
            // an IRV-style ballot where each voter orders the options;
            // SIMPLE just counts votes.
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Dusk.copy(alpha = 0.06f),
                onClick = { rankedMode = !rankedMode }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = rankedMode,
                        onCheckedChange = { rankedMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Dusk)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ranked-choice", color = Chalk900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (rankedMode) "Voters order all options; winner = instant runoff."
                            else "Voters pick one option; winner = most votes.",
                            color = Chalk500,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)
            Button(
                onClick = {
                    val validOptions = options.filter { it.isNotBlank() }
                    if (question.isBlank() || validOptions.size < 2) { error = "Question and at least 2 options required"; return@Button }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.createPoll(
                                mapOf(
                                    "tripId" to tripId,
                                    "question" to question.trim(),
                                    "options" to validOptions,
                                    "multiSelect" to false,
                                    "kind" to if (rankedMode) "RANKED" else "SIMPLE"
                                )
                            )
                            onCreated(); onDismiss()
                        } catch (e: Exception) { error = e.message }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !isLoading,
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Create Poll", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
