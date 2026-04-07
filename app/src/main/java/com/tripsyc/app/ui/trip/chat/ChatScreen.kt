package com.tripsyc.app.ui.trip.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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

@Composable
fun ChatScreen(
    tripId: String,
    currentUser: User?
) {
    var messages by remember { mutableStateOf<List<ChatMessageWithUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
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

    // Poll for new messages every 5 seconds
    LaunchedEffect(tripId) {
        loadMessages()
        while (true) {
            delay(5000)
            loadMessages()
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages list
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                LoadingView("Loading messages...")
            } else if (messages.isEmpty()) {
                EmptyState(
                    icon = "💬",
                    title = "No messages yet",
                    message = "Be the first to say something!"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (nextCursor != null) {
                        item {
                            TextButton(
                                onClick = { loadMessages(nextCursor) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Load earlier messages", color = Dusk)
                            }
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        val isMe = message.userId == currentUser?.id
                        MessageBubble(
                            message = message,
                            isCurrentUser = isMe
                        )
                    }
                }
            }
        }

        // Compose bar
        Surface(
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Chalk400,
                        unfocusedBorderColor = Chalk200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

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
                                listState.animateScrollToItem(messages.size - 1)
                            } catch (_: Exception) {
                                messageText = text // restore on failure
                            }
                            isSending = false
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (canSend) Coral else Chalk200)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else Chalk400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageWithUser,
    isCurrentUser: Boolean
) {
    val senderName = message.user.name
        ?: message.user.email.substringBefore("@")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
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

            // Reply indicator
            val replyColumn = @Composable {
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
                                val rName = message.replyTo.user.name
                                    ?: message.replyTo.user.email.substringBefore("@")
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
            }

            Box(
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start) {
                    if (message.replyTo != null) {
                        replyColumn()
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = if (!isCurrentUser && message.replyTo == null) 4.dp else 12.dp,
                            topEnd = if (isCurrentUser && message.replyTo == null) 4.dp else 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        color = if (isCurrentUser) Coral else Color.White,
                        shadowElevation = if (isCurrentUser) 0.dp else 1.dp
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

                    if (message.createdAt != null) {
                        Text(
                            text = formatTime(message.createdAt),
                            fontSize = 10.sp,
                            color = Chalk400,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
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
