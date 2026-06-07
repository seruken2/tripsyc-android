package com.tripwave.app.ui.trip.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripwave.app.data.api.ApiClient
import com.tripwave.app.data.api.models.ChatMessageWithUser
import com.tripwave.app.data.api.models.ChatUser
import com.tripwave.app.data.api.models.ReplyInfo
import com.tripwave.app.data.api.models.ReplyUser
import com.tripwave.app.data.api.models.User
import com.tripwave.app.data.realtime.TypingChannel
import com.tripwave.app.ui.common.EmptyState
import com.tripwave.app.ui.common.LoadingView
import com.tripwave.app.ui.theme.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.UUID

private val QUICK_EMOJIS = listOf("❤️", "😂", "😮", "😢", "👍", "🔥", "🎉", "💯", "👀", "✈️")

/// Cluster consecutive same-author messages sent within this window.
/// First in a cluster shows the avatar + name; last shows the timestamp + tick.
private const val GROUP_WINDOW_MS = 5L * 60 * 1000

/// Optimistic-send state for messages still in flight or that failed to POST.
/// Kept in a side Map keyed by client-side id so the ChatMessageWithUser API
/// model stays pure.
enum class ChatSendState { SENDING, FAILED }

/// Server-Sent Events feed for `/api/chat/stream`. Emits one Unit per
/// non-heartbeat event so callers can trigger a refetch. Reuses the
/// app's shared OkHttp client so the cookie jar + mobile-client header
/// ride along automatically. Throws on disconnect → caller backs off
/// and reopens.
private fun chatStreamFlow(tripId: String): Flow<Unit> = callbackFlow {
    val request = Request.Builder()
        .url("${ApiClient.BASE_URL}api/chat/stream?tripId=$tripId")
        .header("Accept", "text/event-stream")
        .header("Cache-Control", "no-cache")
        .build()
    val listener = object : EventSourceListener() {
        override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
            // Skip server-side keepalive frames so we don't refetch every 30s for nothing.
            if (!data.contains("\"type\":\"heartbeat\"")) {
                trySend(Unit)
            }
        }
        override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
            close(t)
        }
        override fun onClosed(es: EventSource) {
            close()
        }
    }
    val factory = EventSources.createFactory(ApiClient.okHttpClient)
    val source = factory.newEventSource(request, listener)
    awaitClose { source.cancel() }
}

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
    var replyingTo by remember { mutableStateOf<ChatMessageWithUser?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessageWithUser?>(null) }
    var reportTarget by remember { mutableStateOf<ChatMessageWithUser?>(null) }
    var lastReportToast by remember { mutableStateOf<String?>(null) }
    // Read receipts — refetched on each new message arrival so the
    // "Read by …" row stays accurate after the user sends.
    var readReceipts by remember { mutableStateOf<List<com.tripwave.app.data.api.models.ReadReceipt>>(emptyList()) }
    // Client-side search. Filters the loaded messages list by case-
    // insensitive substring; no server round-trip.
    var searchQuery by remember { mutableStateOf("") }
    // Pending image attachment for the message currently being typed.
    // We compress on the IO thread and stash the resulting data URI; on
    // send it rides along in the POST body as `imageUrl`.
    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImageDataUri by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val chatContext = androidx.compose.ui.platform.LocalContext.current
    var typingNames by remember { mutableStateOf<List<String>>(emptyList()) }
    // Client-side delivery state for optimistic-sent bubbles. Keyed by the
    // synthetic client id we generate in the send handler; cleared on success,
    // flipped to FAILED on error so the user sees + can dismiss.
    var sendStates by remember { mutableStateOf<Map<String, ChatSendState>>(emptyMap()) }
    // Set of user ids currently viewing this trip — drives the avatar
    // presence dot. Polled from /api/trips/:id/presence at 30s, matching
    // LivePresenceRow's existing pattern (no Supabase dep needed).
    var onlineUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val chatImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingImageUri = uri
            pendingImageDataUri = null
            // Compress off-main, mirror iOS's 1024px / ~150KB cap so
            // the server stores compact data URIs.
            scope.launch {
                pendingImageDataUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { encodeChatImage(chatContext, uri) }.getOrNull()
                }
            }
        }
    }

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
            // Refresh the "Read by …" row alongside the message list.
            // Failure is silent — the row just stays empty.
            try {
                readReceipts = ApiClient.apiService.getReadReceipts(tripId).receipts
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

    // Supabase Realtime typing channel. Lives alongside the REST poll: when
    // SUPABASE_URL / SUPABASE_ANON_KEY are configured, the channel takes over
    // and emits names via its own StateFlow; when unset, isAvailable returns
    // false and we keep polling /api/chat/typing every 3s as a graceful fallback.
    val typingChannel = remember(tripId) { TypingChannel(tripId, scope) }
    val typingChannelNames by typingChannel.typingNames.collectAsState()

    // Stream new-message events from /api/chat/stream (SSE). Falls back to a
    // 5s poll cycle between disconnects with exponential backoff so the user
    // still sees updates if SSE is blocked (corporate proxy, flaky network).
    LaunchedEffect(tripId) {
        loadMessages()
        try { ApiClient.apiService.markRead(mapOf("tripId" to tripId)) } catch (_: Exception) {}

        var backoffMs = 2_000L
        while (true) {
            try {
                chatStreamFlow(tripId).collect {
                    backoffMs = 2_000L
                    loadMessages()
                }
            } catch (_: Exception) {
                // SSE dropped — fall through to backoff + poll fallback.
            }
            // While the stream is down, poll at 5s so we stay roughly fresh.
            val pollUntil = System.currentTimeMillis() + backoffMs
            while (System.currentTimeMillis() < pollUntil) {
                delay(5_000)
                loadMessages()
            }
            backoffMs = (backoffMs * 2).coerceAtMost(32_000L)
        }
    }

    // Presence: who else is in this trip right now. Same endpoint
    // LivePresenceRow polls, but cached here so MessageBubble can render
    // a green dot on each online member's avatar.
    LaunchedEffect(tripId) {
        while (true) {
            try {
                val resp = ApiClient.apiService.getPresence(tripId)
                onlineUserIds = resp.viewers.map { it.userId }.toSet()
            } catch (_: Exception) { /* non-critical */ }
            delay(15_000)
        }
    }

    // Typing indicators — either Supabase Realtime (when configured) or the
    // legacy 3s REST poll as a fallback. The channel's StateFlow updates
    // typingChannelNames directly via collectAsState above.
    LaunchedEffect(tripId, currentUser?.id) {
        if (typingChannel.isAvailable && currentUser != null) {
            typingChannel.start(currentUser.id, currentUser.name)
        } else {
            // REST fallback only when Supabase isn't configured.
            while (true) {
                fetchTyping()
                delay(3000)
            }
        }
    }
    DisposableEffect(tripId) {
        onDispose { typingChannel.stop() }
    }

    // Mirror typing names from whichever source is live so the existing
    // render block doesn't need to know about the source.
    LaunchedEffect(typingChannelNames) {
        if (typingChannel.isAvailable) typingNames = typingChannelNames
    }

    // Notify peers when the user is typing. Channel takes the realtime path;
    // when not available we POST the legacy /api/chat/typing endpoint with
    // the same 2s throttle.
    var lastTypingPingMs by remember { mutableStateOf(0L) }
    LaunchedEffect(messageText) {
        if (messageText.isEmpty()) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastTypingPingMs < 2_000L) return@LaunchedEffect
        lastTypingPingMs = now
        if (typingChannel.isAvailable) {
            typingChannel.broadcast()
        } else {
            try { ApiClient.apiService.sendTyping(mapOf("tripId" to tripId)) } catch (_: Exception) {}
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val filteredMessages = if (searchQuery.isBlank()) messages
    else messages.filter { it.text.contains(searchQuery.trim(), ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
    ) {
        // ── Search bar ───────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Chalk200)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = null,
                    tint = Chalk400,
                    modifier = Modifier.size(14.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Chalk900,
                        fontSize = 14.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Coral),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search messages...",
                                    color = Chalk400,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Chalk400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // ── Messages list ──────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> LoadingView("Loading messages...")
                messages.isEmpty() -> EmptyState(
                    icon = "💬",
                    title = "No messages yet",
                    message = "Be the first to say something!"
                )
                searchQuery.isNotBlank() && filteredMessages.isEmpty() -> EmptyState(
                    icon = "🔍",
                    title = "No matches",
                    message = "No messages match your search"
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

                        itemsIndexed(filteredMessages, key = { _, m -> m.id }) { index, message ->
                            val isMe = message.userId == currentUser?.id
                            val prev = filteredMessages.getOrNull(index - 1)
                            val next = filteredMessages.getOrNull(index + 1)
                            // Time-based grouping: same-author messages within
                            // a 5-min window cluster. First in a cluster shows
                            // the avatar + name; last in a cluster shows the
                            // timestamp + tick. A bigger gap starts a new cluster.
                            val msgMs = runCatching { Instant.parse(message.createdAt ?: "").toEpochMilli() }
                                .getOrDefault(System.currentTimeMillis())
                            val gapBefore = prev?.let {
                                val prevMs = runCatching { Instant.parse(it.createdAt ?: "").toEpochMilli() }
                                    .getOrDefault(Long.MIN_VALUE)
                                msgMs - prevMs > GROUP_WINDOW_MS
                            } ?: true
                            val gapAfter = next?.let {
                                val nextMs = runCatching { Instant.parse(it.createdAt ?: "").toEpochMilli() }
                                    .getOrDefault(Long.MAX_VALUE)
                                nextMs - msgMs > GROUP_WINDOW_MS
                            } ?: true
                            val showAvatar = prev == null || prev.userId != message.userId || gapBefore
                            val isLastInGroup = next == null || next.userId != message.userId || gapAfter
                            val sendState = sendStates[message.id]
                            val isOnline = !isMe && onlineUserIds.contains(message.userId)
                            val isReadByOthers = isMe && sendState == null && readReceipts.any { r ->
                                r.userId != currentUser?.id &&
                                    runCatching { Instant.parse(r.readAt).toEpochMilli() >= msgMs }.getOrDefault(false)
                            }
                            MessageBubble(
                                message = message,
                                isCurrentUser = isMe,
                                currentUserId = currentUser?.id,
                                showAvatar = showAvatar,
                                isLastInGroup = isLastInGroup,
                                sendState = sendState,
                                isOnline = isOnline,
                                isReadByOthers = isReadByOthers,
                                onDismissFailed = {
                                    messages = messages.filterNot { it.id == message.id }
                                    sendStates = sendStates - message.id
                                },
                                onLongPress = {
                                    // Optimistic-pending or failed bubbles can't
                                    // be acted on — their id isn't a real server row.
                                    if (sendState == null) actionTargetMessage = message
                                },
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = if (showAvatar) 4.dp else 1.dp
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

        // ── Read receipts row ─────────────────────────────────────────
        // "Read" + mini avatars of who's seen the latest message. Mirrors
        // iOS — global per-chat row (not per-bubble) so the bottom of
        // the conversation always shows the freshest read state.
        val latestMine = messages.lastOrNull { it.userId == currentUser?.id }
        val latestCreatedAtMs = latestMine?.createdAt
            ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: Long.MAX_VALUE
        val readers = readReceipts.filter { r ->
            r.userId != currentUser?.id &&
                latestMine != null &&
                runCatching { java.time.Instant.parse(r.readAt).toEpochMilli() >= latestCreatedAtMs }.getOrDefault(false)
        }
        if (readers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Read",
                    color = Chalk400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(-4.dp)) {
                    readers.take(4).forEach { r ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Coral.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!r.user.avatarUrl.isNullOrEmpty()) {
                                coil.compose.AsyncImage(
                                    model = r.user.avatarUrl,
                                    contentDescription = r.user.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    r.user.name?.firstOrNull()?.uppercase() ?: "?",
                                    color = Coral,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Compose bar ────────────────────────────────────────────────
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Reply preview — shown above the input when the user has
                // picked a message to reply to via long-press. Tap the X
                // to clear without sending.
                replyingTo?.let { target ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Chalk100
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(Coral, RoundedCornerShape(2.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${target.user.name ?: "someone"}",
                                    fontSize = 11.sp,
                                    color = Coral,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = target.text.take(80),
                                    fontSize = 12.sp,
                                    color = Chalk500,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { replyingTo = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Chalk500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Pending image preview — coil renders the URI before
                // the compressed data URI is ready, so the user sees
                // their attachment instantly.
                pendingImageUri?.let { uri ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Chalk100
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Image attached", color = Chalk900, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    if (pendingImageDataUri == null) "Compressing…" else "Ready to send",
                                    color = Chalk500,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    pendingImageUri = null
                                    pendingImageDataUri = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Chalk500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Attachment button (matches iOS plus.circle.fill)
                IconButton(
                    onClick = {
                        chatImagePicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
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

                // Send button — coral when can send, chalk200 otherwise.
                // Sendable when either text is present OR an image
                // attachment finished compressing. If the user picked
                // an image and tapped send before compression finishes,
                // the image would get dropped — so we block the button
                // while compression is in flight (pendingImageUri set
                // but pendingImageDataUri still null).
                val imageStillCompressing = pendingImageUri != null && pendingImageDataUri == null
                val canSend = (messageText.isNotBlank() || pendingImageDataUri != null) &&
                    !isSending && !imageStillCompressing
                IconButton(
                    onClick = {
                        if (!canSend) return@IconButton
                        val text = messageText.trim()
                        val replyRef = replyingTo
                        val replyId = replyRef?.id
                        val imageData = pendingImageDataUri
                        messageText = ""
                        replyingTo = null
                        pendingImageUri = null
                        pendingImageDataUri = null

                        // Optimistic bubble: synthesize a ChatMessageWithUser
                        // with a client-side id and append immediately. SSE +
                        // refetch on success will swap in the real row; on
                        // failure we flip its sendState to FAILED so the user
                        // can tap to dismiss.
                        val clientId = "optimistic-${UUID.randomUUID()}"
                        val optimisticReply = replyRef?.let {
                            ReplyInfo(
                                id = it.id,
                                text = it.text,
                                imageUrl = it.imageUrl,
                                userId = it.userId,
                                user = ReplyUser(id = it.user.id, name = it.user.name)
                            )
                        }
                        val nowIso = Instant.now().toString()
                        val optimistic = ChatMessageWithUser(
                            id = clientId,
                            tripId = tripId,
                            userId = currentUser?.id ?: "",
                            text = text,
                            imageUrl = imageData,
                            replyToId = replyId,
                            replyTo = optimisticReply,
                            isPinned = false,
                            editedAt = null,
                            createdAt = nowIso,
                            user = ChatUser(
                                id = currentUser?.id ?: "",
                                name = currentUser?.name,
                                avatarUrl = currentUser?.avatarUrl
                            ),
                            reactions = null
                        )
                        messages = messages + optimistic
                        sendStates = sendStates + (clientId to ChatSendState.SENDING)

                        scope.launch {
                            isSending = true
                            try {
                                val body = buildMap<String, Any?> {
                                    put("tripId", tripId)
                                    put("text", text)
                                    if (replyId != null) put("replyToId", replyId)
                                    if (imageData != null) put("imageUrl", imageData)
                                }
                                ApiClient.apiService.sendMessage(body)
                                // Drop the optimistic; loadMessages refetches
                                // the real row so reactions/replies stay consistent.
                                messages = messages.filterNot { it.id == clientId }
                                sendStates = sendStates - clientId
                                loadMessages()
                            } catch (_: Exception) {
                                sendStates = sendStates + (clientId to ChatSendState.FAILED)
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
                    icon = Icons.Default.Reply,
                    label = "Reply",
                    onClick = {
                        replyingTo = targetMsg
                        actionTargetMessage = null
                    }
                )

                // Edit — only author within 30-min window, no image.
                // Server enforces the same constraint; the UI just
                // hides the affordance once it can't take.
                val ageMs = targetMsg.createdAt
                    ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
                    ?.let { System.currentTimeMillis() - it }
                    ?: Long.MAX_VALUE
                val canEdit = isAuthor &&
                    targetMsg.imageUrl.isNullOrEmpty() &&
                    ageMs < 30L * 60 * 1000
                if (canEdit) {
                    ActionRow(
                        icon = Icons.Default.Edit,
                        label = "Edit",
                        onClick = {
                            editingMessage = targetMsg
                            actionTargetMessage = null
                        }
                    )
                }

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

                // Report — only on other people's messages. Opens the
                // full Report-or-Block dialog with six categories.
                if (!isAuthor) {
                    ActionRow(
                        icon = Icons.Default.Flag,
                        label = "Report…",
                        tint = Danger,
                        onClick = {
                            reportTarget = targetMsg
                            actionTargetMessage = null
                        }
                    )
                }
            }
        }
    }

    // Report / block dialog — six categories + Block this user, exact
    // labels and copy from iOS so moderators see consistent reasons
    // across platforms.
    reportTarget?.let { target ->
        val authorName = target.user.name ?: "user"
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report $authorName?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Our moderation team reviews every report. You can also block this user so you won't see their messages in any trip.",
                        color = Chalk500,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "Harassment or bullying" to "HARASSMENT",
                        "Hate speech" to "HATE",
                        "Spam or scam" to "SPAM",
                        "Sexual content" to "SEXUAL",
                        "Threats or violence" to "THREATS",
                        "Something else" to "OTHER"
                    ).forEach { (label, kind) ->
                        TextButton(
                            onClick = {
                                val mid = target.id
                                reportTarget = null
                                scope.launch {
                                    try {
                                        ApiClient.apiService.reportContent(
                                            mapOf(
                                                "targetType" to "chat_message",
                                                "targetId" to mid,
                                                "kind" to kind,
                                                "note" to null
                                            )
                                        )
                                        lastReportToast = "Thanks, our team will review."
                                    } catch (_: Exception) {
                                        lastReportToast = "Couldn't submit report — try again."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                color = Chalk900,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                    }
                    HorizontalDivider(color = Chalk100, modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(
                        onClick = {
                            val uid = target.userId
                            reportTarget = null
                            scope.launch {
                                try {
                                    ApiClient.apiService.blockUser(
                                        mapOf("blockedId" to uid, "reason" to null)
                                    )
                                    messages = messages.filter { it.userId != uid }
                                    lastReportToast = "Blocked $authorName."
                                } catch (_: Exception) {
                                    lastReportToast = "Couldn't block — try again."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(
                            "Block $authorName",
                            color = Danger,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Simple bottom toast for report / block results — matches the
    // ToastData pattern used elsewhere in the app.
    lastReportToast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            lastReportToast = null
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 80.dp),
                shape = RoundedCornerShape(12.dp),
                color = Chalk900,
                shadowElevation = 6.dp
            ) {
                Text(
                    msg,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }

    // Edit message dialog — mirrors the iOS EditMessageSheet: same
    // title, character count, Cancel / Save labels, 2000-char cap.
    editingMessage?.let { target ->
        EditMessageDialog(
            initialText = target.text,
            onCancel = { editingMessage = null },
            onSave = { newText ->
                val original = target
                editingMessage = null
                scope.launch {
                    try {
                        ApiClient.apiService.patchMessage(
                            mapOf("messageId" to original.id, "text" to newText)
                        )
                        // Optimistic local update so the bubble reflows
                        // before the next refetch.
                        messages = messages.map {
                            if (it.id == original.id) it.copy(
                                text = newText,
                                editedAt = java.time.Instant.now().toString()
                            ) else it
                        }
                    } catch (_: Exception) {}
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMessageDialog(
    initialText: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val trimmed = text.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != initialText.trim()

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Edit message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 2000) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Text(
                    "${text.length}/2000",
                    color = Chalk500,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(trimmed) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    )
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
    showAvatar: Boolean = true,
    isLastInGroup: Boolean = true,
    sendState: ChatSendState? = null,
    isOnline: Boolean = false,
    isReadByOthers: Boolean = false,
    onDismissFailed: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val senderName = message.user.name ?: "Someone"

    Column(
        modifier = modifier.fillMaxWidth().combinedClickable(
            // Tap a failed bubble to dismiss it. Otherwise tap is a no-op and
            // long-press opens the action sheet (suppressed upstream when pending).
            onClick = { if (sendState == ChatSendState.FAILED) onDismissFailed() },
            onLongClick = onLongPress
        ),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender name — only on the first bubble of a cluster (showAvatar true).
        if (!isCurrentUser && showAvatar) {
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
            // Avatar for other users (left side). Only on first bubble of a
            // cluster; subsequent bubbles in the same cluster reserve the
            // same horizontal slot with a Spacer so the bubbles line up.
            if (!isCurrentUser) {
                if (showAvatar) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                        // Sage-green presence dot, bottom-right of the avatar.
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Sage)
                                    .padding(1.5.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
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
                    // Pending / failed get a subtle visual treatment so the
                    // user knows the message hasn't reached the server yet.
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = if (!isCurrentUser && message.replyTo == null) 4.dp else 12.dp,
                            topEnd = if (isCurrentUser && message.replyTo == null) 4.dp else 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        color = if (isCurrentUser) Coral else Color.White,
                        shadowElevation = if (isCurrentUser) 0.dp else 1.dp,
                        tonalElevation = 0.dp,
                        border = if (sendState == ChatSendState.FAILED)
                            androidx.compose.foundation.BorderStroke(1.5.dp, Danger)
                        else null,
                        modifier = Modifier.alpha(if (sendState == ChatSendState.SENDING) 0.7f else 1f)
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

                    // Timestamp + delivery state. Only shown on the last bubble
                    // of a cluster (so a burst of messages doesn't get cluttered)
                    // or whenever the bubble is mid-flight / failed so the user
                    // always sees in-flight status.
                    if (message.createdAt != null && (isLastInGroup || sendState != null)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatTime(message.createdAt),
                                fontSize = 10.sp,
                                color = Chalk400
                            )
                            if (isCurrentUser) {
                                when (sendState) {
                                    ChatSendState.SENDING -> Text("·", fontSize = 10.sp, color = Chalk400)
                                    ChatSendState.FAILED -> Text(
                                        "Couldn't send — tap to dismiss",
                                        fontSize = 10.sp,
                                        color = Danger,
                                        fontWeight = FontWeight.Medium
                                    )
                                    null -> Text(
                                        if (isReadByOthers) "✓✓" else "✓",
                                        fontSize = 10.sp,
                                        color = if (isReadByOthers) Dusk else Chalk400,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (sendState == ChatSendState.SENDING) {
                                    Text(
                                        "Sending…",
                                        fontSize = 10.sp,
                                        color = Chalk400,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
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

/// Compresses a picked image to a base64 JPEG data URI under ~150 KB
/// at most 1024×1024. Mirrors the iOS ChatViewModel.setImage pipeline
/// so server-side storage stays identical across platforms.
private fun encodeChatImage(context: android.content.Context, uri: android.net.Uri): String {
    val source = context.contentResolver.openInputStream(uri).use {
        android.graphics.BitmapFactory.decodeStream(it)
            ?: throw Exception("Couldn't decode image")
    }
    val maxDim = 1024
    val scale = minOf(
        maxDim.toFloat() / source.width,
        maxDim.toFloat() / source.height,
        1f
    )
    val target = if (scale < 1f) {
        android.graphics.Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else source

    var quality = 70
    while (true) {
        val out = java.io.ByteArrayOutputStream()
        target.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
        if (out.size() <= 150_000 || quality <= 10) {
            val encoded = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
            return "data:image/jpeg;base64,$encoded"
        }
        quality -= 15
    }
}
