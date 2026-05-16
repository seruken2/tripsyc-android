package com.tripsyc.app.ui.trip.snaps

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.TripSnapAuthor
import com.tripsyc.app.data.api.models.TripSnapItem
import com.tripsyc.app.data.api.models.TripSnapsResponse
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val QUICK_EMOJIS = listOf("❤️", "😂", "😮", "😢", "👍", "🔥", "🎉", "💯", "👀", "✈️")

/**
 * Trip Snaps — Stories-style ephemeral posts. Author row at the top
 * (Instagram-ring style with unseen counter), grid view of recent
 * snaps below, full-screen player when an author is tapped. Mirrors
 * the iOS SnapsView; uploads go through multipart POST to /api/trips/
 * :id/snaps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapsScreen(tripId: String, currentUserId: String?) {
    val context = LocalContext.current
    var data by remember { mutableStateOf<TripSnapsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var playingAuthor by remember { mutableStateOf<TripSnapAuthor?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            data = ApiClient.apiService.getSnaps(tripId)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load snaps"
        }
        isLoading = false
    }

    LaunchedEffect(tripId) { reload() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                try {
                    uploadSnap(context, uri, tripId)
                    reload()
                } catch (e: Exception) {
                    error = e.message ?: "Snap upload failed"
                }
                isUploading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Snaps", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                Text(
                    "${data?.totalSnaps ?: 0} snap${if ((data?.totalSnaps ?: 0) == 1) "" else "s"} · auto-expire after the trip",
                    fontSize = 12.sp, color = Chalk500
                )
            }
            FloatingActionButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = Coral,
                contentColor = Color.White,
                modifier = Modifier.size(44.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "New snap", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) {
            Text(error!!, color = Danger, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Coral)
            }
            return
        }

        val authors = data?.authors ?: emptyList()
        if (authors.isEmpty()) {
            EmptyState(
                icon = "📸",
                title = "No snaps yet",
                message = "Tap the camera to drop a quick photo for the group. They auto-expire after the trip."
            )
            return
        }

        // ── Author row (Stories ring) ────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(authors, key = { it.authorId }) { author ->
                AuthorRing(author = author, onClick = { playingAuthor = author })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Recent snaps timeline ───────────────────────────────
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            authors.forEach { author ->
                items(author.snaps, key = { it.id }) { snap ->
                    SnapPreviewRow(
                        author = author,
                        snap = snap,
                        onTap = { playingAuthor = author }
                    )
                }
            }
        }
    }

    playingAuthor?.let { author ->
        Dialog(
            onDismissRequest = { playingAuthor = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SnapPlayer(
                author = author,
                currentUserId = currentUserId,
                onDismiss = {
                    playingAuthor = null
                    scope.launch { reload() }
                }
            )
        }
    }
}

@Composable
private fun AuthorRing(author: TripSnapAuthor, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(
                    width = if (author.unseenCount > 0) 3.dp else 1.dp,
                    color = if (author.unseenCount > 0) Coral else Chalk200,
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Coral.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (!author.authorAvatar.isNullOrEmpty()) {
                AsyncImage(
                    model = author.authorAvatar,
                    contentDescription = author.authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    author.authorName.firstOrNull()?.uppercase() ?: "?",
                    color = Coral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
        Text(
            if (author.isMe) "You" else (author.authorName.split(" ").firstOrNull() ?: author.authorName),
            fontSize = 11.sp,
            color = Chalk900,
            maxLines = 1
        )
        if (author.unseenCount > 0) {
            Text(
                "${author.unseenCount} new",
                fontSize = 9.sp,
                color = Coral,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SnapPreviewRow(
    author: TripSnapAuthor,
    snap: TripSnapItem,
    onTap: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        shadowElevation = 2.dp,
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Chalk100)
            ) {
                AsyncImage(
                    model = snap.mediaUrl,
                    contentDescription = snap.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (author.isMe) "You" else author.authorName,
                    color = Chalk900,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                if (!snap.caption.isNullOrBlank()) {
                    Text(snap.caption, color = Chalk500, fontSize = 12.sp, maxLines = 1)
                }
                if (snap.reactions.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        snap.reactions.take(3).forEach { r ->
                            Text("${r.emoji} ${r.count}", fontSize = 11.sp, color = Chalk500)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapPlayer(
    author: TripSnapAuthor,
    currentUserId: String?,
    onDismiss: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val snap = author.snaps.getOrNull(index) ?: run {
        onDismiss()
        return
    }

    // Mark viewed on landing on each snap (idempotent server-side).
    LaunchedEffect(snap.id) {
        if (!author.isMe) {
            try { ApiClient.apiService.markSnapViewed(snap.id) } catch (_: Exception) {}
        }
    }

    // Reactions are local-mutable so taps flip immediately; server is
    // the source of truth on reload.
    var localReactions by remember(snap.id) { mutableStateOf(snap.reactions) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = snap.mediaUrl,
            contentDescription = snap.caption,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Tap left / right to step
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable {
                        if (index > 0) index--
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable {
                        if (index < author.snaps.lastIndex) index++ else onDismiss()
                    }
            )
        }

        // Top bar with progress + close
        Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 12.dp, end = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                author.snaps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (i <= index) Color.White else Color.White.copy(alpha = 0.4f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!author.authorAvatar.isNullOrEmpty()) {
                        AsyncImage(
                            model = author.authorAvatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            author.authorName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (author.isMe) "You" else author.authorName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        // Caption + reactions at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!snap.caption.isNullOrBlank()) {
                Text(
                    snap.caption,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.45f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QUICK_EMOJIS.forEach { emoji ->
                    val mine = localReactions.firstOrNull { it.emoji == emoji }?.mine == true
                    Surface(
                        shape = CircleShape,
                        color = if (mine) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.4f),
                        onClick = {
                            scope.launch {
                                try {
                                    val res = ApiClient.apiService.toggleSnapReaction(
                                        snap.id,
                                        mapOf("emoji" to emoji)
                                    )
                                    // Optimistic flip
                                    val updated = localReactions.toMutableList()
                                    val idx = updated.indexOfFirst { it.emoji == emoji }
                                    if (idx >= 0) {
                                        val r = updated[idx]
                                        updated[idx] = r.copy(
                                            mine = res.active,
                                            count = if (res.active) r.count + (if (r.mine) 0 else 1)
                                            else (r.count - 1).coerceAtLeast(0)
                                        )
                                    } else if (res.active) {
                                        updated.add(com.tripsyc.app.data.api.models.TripSnapReaction(emoji, 1, true))
                                    }
                                    localReactions = updated
                                } catch (_: Exception) {}
                            }
                        }
                    ) {
                        Text(
                            emoji,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun uploadSnap(context: Context, uri: Uri, tripId: String) =
    withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Could not read snap")
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = mime.substringAfter("/", "jpeg")
        val filePart = MultipartBody.Part.createFormData(
            "file",
            "snap.$ext",
            bytes.toRequestBody(mime.toMediaType())
        )
        ApiClient.apiService.uploadSnap(tripId, filePart, null)
    }
