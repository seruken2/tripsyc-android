package com.tripsyc.app.ui.trip.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.*
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailScreen(
    destination: Destination,
    tripId: String,
    currentUser: User?,
    isLocked: Boolean,
    onBack: () -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(destination.comments ?: emptyList()) }
    var isLoadingComments by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isDealbreaker by remember { mutableStateOf(false) }
    var isDealbreakerLoading by remember { mutableStateOf(false) }
    var myVote by remember { mutableStateOf<VoteValue?>(null) }
    var upVotes by remember { mutableIntStateOf(destination.votes?.count { it.value == VoteValue.UP } ?: 0) }
    var downVotes by remember { mutableIntStateOf(destination.votes?.count { it.value == VoteValue.DOWN } ?: 0) }
    val scope = rememberCoroutineScope()

    fun loadComments() {
        scope.launch {
            isLoadingComments = true
            try {
                val resp = ApiClient.apiService.getDestinations(tripId)
                val dest = resp.destinations.firstOrNull { it.id == destination.id }
                comments = dest?.comments ?: emptyList()
                upVotes = dest?.votes?.count { it.value == VoteValue.UP } ?: upVotes
                downVotes = dest?.votes?.count { it.value == VoteValue.DOWN } ?: downVotes
                myVote = dest?.votes?.firstOrNull { it.userId == currentUser?.id }?.value
            } catch (_: Exception) {}
            isLoadingComments = false
        }
    }

    LaunchedEffect(destination.id) {
        scope.launch {
            try {
                val resp = ApiClient.apiService.getDealbreakers(tripId)
                isDealbreaker = resp.destinationIds.contains(destination.id)
            } catch (_: Exception) {}
        }
        myVote = destination.votes?.firstOrNull { it.userId == currentUser?.id }?.value
        loadComments()
    }

    fun deleteComment(commentId: String) {
        scope.launch {
            try {
                ApiClient.apiService.deleteComment(mapOf("commentId" to commentId))
                loadComments()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${destination.city}, ${destination.country}",
                        fontWeight = FontWeight.Bold,
                        color = Chalk900,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Chalk900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        containerColor = Chalk50
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero image
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    if (!destination.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = destination.imageUrl,
                            contentDescription = destination.city,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.linearGradient(listOf(Dusk, Coral.copy(alpha = 0.7f))))
                        ) {
                            Text(
                                text = destination.city.first().uppercase(),
                                color = Color.White.copy(alpha = 0.2f),
                                fontSize = 120.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    // Gradient overlay
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.45f)))
                            )
                    )
                    // Badges bottom-left
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (destination.shortlisted) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Gold.copy(0.92f)) {
                                Text(
                                    "★ Shortlisted", color = Color.White, fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (!destination.affordability.isNullOrEmpty()) {
                            val (afColor, afLabel) = when (destination.affordability) {
                                "budget" -> Pair(Success, "Budget Friendly")
                                "mid-range" -> Pair(Gold, "Mid-Range")
                                "expensive" -> Pair(Danger, "Pricey")
                                else -> Pair(Chalk400, destination.affordability)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = afColor.copy(0.9f)) {
                                Text(
                                    afLabel, color = Color.White, fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Voting + Dealbreaker row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLocked) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        ApiClient.apiService.castVote(
                                            mapOf("tripId" to tripId, "destinationId" to destination.id, "value" to "UP")
                                        )
                                        if (myVote == VoteValue.UP) { upVotes--; myVote = null }
                                        else { if (myVote == VoteValue.DOWN) downVotes--; upVotes++; myVote = VoteValue.UP }
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (myVote == VoteValue.UP) Success.copy(0.12f) else Color.Transparent,
                                contentColor = Success
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(if (myVote == VoteValue.UP) Success else Chalk200)
                            )
                        ) { Text("👍 $upVotes") }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        ApiClient.apiService.castVote(
                                            mapOf("tripId" to tripId, "destinationId" to destination.id, "value" to "DOWN")
                                        )
                                        if (myVote == VoteValue.DOWN) { downVotes--; myVote = null }
                                        else { if (myVote == VoteValue.UP) upVotes--; downVotes++; myVote = VoteValue.DOWN }
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (myVote == VoteValue.DOWN) Danger.copy(0.12f) else Color.Transparent,
                                contentColor = Danger
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(if (myVote == VoteValue.DOWN) Danger else Chalk200)
                            )
                        ) { Text("👎 $downVotes") }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (!isLocked) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isDealbreakerLoading = true
                                    try {
                                        val result = ApiClient.apiService.toggleDealbreaker(
                                            mapOf("destinationId" to destination.id, "tripId" to tripId)
                                        )
                                        isDealbreaker = result.dealbreaker
                                    } catch (_: Exception) {}
                                    isDealbreakerLoading = false
                                }
                            },
                            enabled = !isDealbreakerLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDealbreaker) Danger.copy(0.1f) else Color.Transparent,
                                contentColor = Danger
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(if (isDealbreaker) Danger else Chalk200)
                            )
                        ) { Text(if (isDealbreaker) "🚫 Dealbreaker" else "Not For Me", fontSize = 12.sp) }
                    }
                }
            }

            // Cost + distance chips
            if (destination.estimatedCostMin != null || destination.distance != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (destination.estimatedCostMin != null) {
                            DestInfoChip(
                                icon = "💰",
                                text = "$${destination.estimatedCostMin}–${destination.estimatedCostMax ?: "?"}/person"
                            )
                        }
                        destination.distance?.flightTime?.let { ft ->
                            DestInfoChip(icon = "✈️", text = ft)
                        } ?: destination.distance?.let { dist ->
                            DestInfoChip(icon = "📍", text = "${dist.km.toInt()} km away")
                        }
                        destination.distance?.driveTime?.let { dt ->
                            DestInfoChip(icon = "🚗", text = dt)
                        }
                    }
                }
            }

            // Comments header
            item {
                HorizontalDivider(color = Chalk100, modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Comments (${comments.size})",
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900,
                        fontSize = 16.sp
                    )
                }
            }

            // Add comment
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (replyingTo != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Chalk100) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Replying to ${replyingTo?.user?.name ?: "comment"}",
                                    fontSize = 12.sp, color = Chalk500
                                )
                                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Chalk400)
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a comment…", color = Chalk400, fontSize = 14.sp) },
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = Chalk200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        val canPost = commentText.isNotBlank() && !isSubmitting
                        IconButton(
                            onClick = {
                                if (!canPost) return@IconButton
                                val text = commentText.trim()
                                commentText = ""
                                scope.launch {
                                    isSubmitting = true
                                    try {
                                        ApiClient.apiService.addComment(
                                            buildMap {
                                                put("destinationId", destination.id)
                                                put("text", text)
                                                replyingTo?.id?.let { put("parentId", it) }
                                            }
                                        )
                                        replyingTo = null
                                        loadComments()
                                    } catch (_: Exception) { commentText = text }
                                    isSubmitting = false
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (canPost) Coral else Chalk200)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Post",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Comments
            if (isLoadingComments) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Coral, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (comments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        Text("No comments yet. Be the first!", color = Chalk400, fontSize = 14.sp)
                    }
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    CommentRow(
                        comment = comment,
                        currentUserId = currentUser?.id,
                        onReply = { replyingTo = it },
                        onDelete = { deleteComment(it) },
                        isReply = false
                    )
                }
            }
        }
    }
}

@Composable
private fun DestInfoChip(icon: String, text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Chalk100) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 13.sp)
            Text(text, fontSize = 12.sp, color = Chalk600, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    currentUserId: String?,
    onReply: (Comment) -> Unit,
    onDelete: (commentId: String) -> Unit,
    isReply: Boolean
) {
    val authorName = comment.user?.name ?: comment.userName ?: "Someone"

    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = if (isReply) 44.dp else 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(if (isReply) Dusk.copy(0.2f) else Coral.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = authorName.firstOrNull()?.uppercase() ?: "?",
                    color = if (isReply) Dusk else Coral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(authorName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Chalk700)
                Text(comment.text, fontSize = 14.sp, color = Chalk900, lineHeight = 20.sp)
            }

            // Actions
            if (!isReply) {
                IconButton(onClick = { onReply(comment) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Reply, contentDescription = "Reply", tint = Chalk400, modifier = Modifier.size(14.dp))
                }
            }
            if (comment.userId == currentUserId) {
                IconButton(onClick = { onDelete(comment.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Danger.copy(0.7f), modifier = Modifier.size(14.dp))
                }
            }
        }

        // Threaded replies
        comment.replies?.forEach { reply ->
            CommentRow(
                comment = reply,
                currentUserId = currentUserId,
                onReply = onReply,
                onDelete = onDelete,
                isReply = true
            )
        }
    }
}
