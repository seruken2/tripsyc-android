package com.tripsyc.app.ui.trip.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.ChatMessageWithUser
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val QUICK_EMOJIS = listOf("❤️", "😂", "😮", "😢", "👍", "🔥", "🎉", "💯", "👀", "✈️")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    tripId: String,
    currentUser: User?,
    isOrganizer: Boolean = false
) {
    var messages by remember { mutableStateOf<List<ChatMessageWithUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var actionTargetMessage by remember { mutableStateOf<ChatMessageWithUser?>(null) }
    val clipboard = LocalClipboardManager.current
    var typingNames by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun loadMessages(cursor: String? = null) {
        scope.launch {
            try {
                val response = ApiClient.apiService.getMessages(
                    tripId = tripId,
                    cursor = cursor,
                    limit = 50
                )
                messages = if (cursor == null) response.messages
                else response.messages + messages
                nextCursor = response.nextCursor
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    fun fetchTyping() {
        scope.launch {
            try {
                val response = ApiClient.apiService.getTyping(tripId)
                if (response.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    val typingMap = response.body() as? Map<String, Any> ?: emptyMap()
                    // Response shape: { typingUsers: [ { name: "..." }, ... ] }
                    @Suppress("UNCHECKED_CAST")
                    val users = typingMap["typingUsers"] as? List<Map<String, Any>> ?: emptyList()
                    typingNames = users.mapNotNull { it["name"] as? String }
                        .filter { it.isNotEmpty() }
                }
            } catch (_: Exception) {}
        }
    }

    // Poll for new messages every 5 seconds
    LaunchedEffect(tripId) {
        loadMessages()
        // Mark chat as read
        try { ApiClient.apiService.markRead(mapOf("tripId" to tripId)) } catch (_: Exception) {}
        while (true) {
            delay(5000)
            loadMessages()
        }
    }

    // Poll for typing indicators every 3 seconds
    LaunchedEffect(tripId) {
        while (true) {
            fetchTyping()
            delay(3000)
        }
    }

    // Send typing signal when text changes (debounced by just firing on change)
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            try {
                ApiClient.apiService.sendTyping(mapOf("tripId" to tripId))
            } catch (_: Exception) {}
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
    ) {
        // ── Messages list ──────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> LoadingView("Loading messages...")
                messages.isEmpty() -> EmptyState(
                    icon = "💬",
                    title = "No messages yet",
                    message = "Be the first to say something!"
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Load earlier button
                        if (nextCursor != null) {
                            item {
                                TextButton(
                                    onClick = { loadMessages(nextCursor) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Load earlier messages",
                                        color = Dusk,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        items(messages, key = { it.id }) { message ->
                            val isMe = message.userId == currentUser?.id
                            MessageBubble(
                                message = message,
                                isCurrentUser = isMe,
                                currentUserId = currentUser?.id,
                                onLongPress = { actionTargetMessage = message },
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Typing indicator ──────────────────────────────────────────────
        if (typingNames.isNotEmpty()) {
            val typingText = when (typingNames.size) {
                1 -> "${typingNames[0]} is typing…"
                2 -> "${typingNames[0]} and ${typingNames[1]} are typing…"
                else -> "${typingNames.size} people are typing…"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = typingText,
                    fontSize = 12.sp,
                    color = Chalk400,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // ── Compose bar ────────────────────────────────────────────────
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Attachment button (matches iOS plus.circle.fill)
                IconButton(
                    onClick = { /* photo picker */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Attachment",
                        tint = Chalk400,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text input field (pill style matching iOS)
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Message…", color = Chalk400, fontSize = 15.sp)
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(22.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = Chalk900),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Chalk400,
                        unfocusedBorderColor = Chalk200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Chalk900,
                        unfocusedTextColor = Chalk900
                    )
                )

                // Send button — coral when can send, chalk200 otherwise
                val canSend = messageText.isNotBlank() && !isSending
                IconButton(
                    onClick = {
                        if (!canSend) return@IconButton
                        val text = messageText.trim()
                        messageText = ""
                        scope.launch {
                            isSending = true
                            try {
                                val sent = ApiClient.apiService.sendMessage(
                                    mapOf("tripId" to tripId, "text" to text)
                                )
                                messages = messages + sent
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            } catch (_: Exception) {
                                messageText = text
                            }
                            isSending = false
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (canSend) Coral else Chalk200)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else Chalk400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Long-press action sheet — react row + per-message actions (pin,
    // delete, copy). Author and organizer get different action sets.
    actionTargetMessage?.let { targetMsg ->
        val isAuthor = targetMsg.userId == currentUser?.id
        val canDelete = isAuthor || isOrganizer
        ModalBottomSheet(
            onDismissRequest = { actionTargetMessage = null },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("React", fontWeight = FontWeight.SemiBold, color = Chalk900, fontSize = 16.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QUICK_EMOJIS) { emoji ->
                        val myReaction = targetMsg.reactions?.firstOrNull { r ->
                            r.emoji == emoji && r.userIds.contains(currentUser?.id)
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (myReaction != null) Coral.copy(alpha = 0.15f) else Chalk100,
                            modifier = Modifier.size(48.dp),
                            onClick = {
                                actionTargetMessage = null
                                scope.launch {
                                    try {
                                        if (myReaction != null) {
                                            ApiClient.apiService.removeReaction(
                                                mapOf("messageId" to targetMsg.id, "emoji" to emoji)
                                            )
                                        } else {
                                            ApiClient.apiService.addReaction(
                                                mapOf("messageId" to targetMsg.id, "emoji" to emoji, "tripId" to tripId)
                                            )
                                        }
                                        loadMessages()
                                    } catch (_: Exception) {}
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                // Per-message action rows below the reactions.
                ActionRow(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy text",
                    onClick = {
                        clipboard.setText(AnnotatedString(targetMsg.text))
                        actionTargetMessage = null
                    }
                )

                if (isOrganizer) {
                    ActionRow(
                        icon = if (targetMsg.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        label = if (targetMsg.isPinned) "Unpin message" else "Pin message",
                        tint = Gold,
                        onClick = {
                            val target = targetMsg
                            actionTargetMessage = null
                            scope.launch {
                                try {
                                    ApiClient.apiService.patchMessage(
                                        mapOf(
                                            "messageId" to target.id,
                                            "isPinned" to !target.isPinned
                                        )
                                    )
                                    // Optimistic local flip so the pin icon
                                    // updates without waiting for a refetch.
                                    messages = messages.map {
                                        if (it.id == target.id) it.copy(isPinned = !target.isPinned)
                                        else it
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }

                if (canDelete) {
                    ActionRow(
                        icon = Icons.Default.Delete,
                        label = "Delete message",
                        tint = Danger,
                        onClick = {
                            val target = targetMsg
                            actionTargetMessage = null
                            scope.launch {
                                try {
                                    ApiClient.apiService.deleteMessage(mapOf("messageId" to target.id))
                                    messages = messages.filter { it.id != target.id }
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Chalk700,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Chalk100,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Message Bubble ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessageWithUser,
    isCurrentUser: Boolean,
    currentUserId: String? = null,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val senderName = message.user.name ?: "Someone"

    Column(
        modifier = modifier.fillMaxWidth().combinedClickable(
            onClick = {},
            onLongClick = onLongPress
        ),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender name (shown above other user messages)
        if (!isCurrentUser) {
            Text(
                text = senderName,
                fontSize = 11.sp,
                color = Chalk400,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 42.dp, bottom = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar for other users (left side)
            if (!isCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!message.user.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = message.user.avatarUrl,
                            contentDescription = senderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = senderName.firstOrNull()?.uppercase() ?: "?",
                            color = Coral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(modifier = Modifier.widthIn(max = 280.dp)) {
                Column(
                    horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
                ) {
                    // Pin badge — only on messages an organizer pinned. Shown
                    // above the bubble so the pin context survives a reply
                    // chain without crowding the message body.
                    if (message.isPinned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Gold,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Pinned",
                                color = Gold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Reply context
                    if (message.replyTo != null) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            color = if (isCurrentUser) Coral.copy(alpha = 0.15f) else Chalk100
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(Coral, RoundedCornerShape(2.dp))
                                )
                                Column {
                                    val rName = message.replyTo.user.name ?: "Someone"
                                    Text(text = rName, fontSize = 10.sp, color = Coral, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = message.replyTo.text.take(80),
                                        fontSize = 11.sp,
                                        color = Chalk500,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Main bubble
                    // Own messages: coral bg, white text
                    // Other messages: white card, chalk900 text
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = if (!isCurrentUser && message.replyTo == null) 4.dp else 12.dp,
                            topEnd = if (isCurrentUser && message.replyTo == null) 4.dp else 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        color = if (isCurrentUser) Coral else Color.White,
                        shadowElevation = if (isCurrentUser) 0.dp else 1.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (!message.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = message.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (message.text.isNotEmpty()) {
                                Text(
                                    text = message.text,
                                    color = if (isCurrentUser) Color.White else Chalk900,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Timestamp
                    if (message.createdAt != null) {
                        Text(
                            text = formatTime(message.createdAt),
                            fontSize = 10.sp,
                            color = Chalk400,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Reactions
                    val reactions = message.reactions?.filter { it.count > 0 }
                    if (!reactions.isNullOrEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            items(reactions) { reaction ->
                                val iMine = reaction.userIds.contains(currentUserId)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (iMine) Coral.copy(alpha = 0.15f) else Chalk100,
                                    border = if (iMine) androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = 0.4f)) else null
                                ) {
                                    Text(
                                        text = "${reaction.emoji} ${reaction.count}",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isCurrentUser) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

private fun formatTime(isoString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = sdf.parse(isoString) ?: return ""
        SimpleDateFormat("HH:mm", Locale.US).format(date)
    } catch (_: Exception) { "" }
}
