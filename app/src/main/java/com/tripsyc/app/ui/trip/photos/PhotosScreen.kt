package com.tripsyc.app.ui.trip.photos

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.tripsyc.app.data.api.models.PhotoComment
import com.tripsyc.app.data.api.models.ReactionSummary
import com.tripsyc.app.data.api.models.TripPhoto
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(tripId: String, currentUser: User? = null) {
    var photos by remember { mutableStateOf<List<TripPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var selectedPhoto by remember { mutableStateOf<TripPhoto?>(null) }
    var captionInput by remember { mutableStateOf("") }
    var showCaptionSheet by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun load() {
        scope.launch {
            isLoading = true
            try { photos = ApiClient.apiService.getPhotos(tripId).photos }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingUri = uri
            captionInput = ""
            showCaptionSheet = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Photos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                Text("${photos.size} photo${if (photos.size != 1) "s" else ""}", fontSize = 13.sp, color = Chalk500)
            }
            FloatingActionButton(
                onClick = { imagePicker.launch("image/*") },
                containerColor = Coral, contentColor = Color.White,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add photo", modifier = Modifier.size(20.dp))
            }
        }

        if (uploadError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Danger.copy(alpha = 0.1f)
            ) {
                Text(uploadError!!, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            LoadingView()
        } else if (photos.isEmpty()) {
            EmptyState(
                icon = "📷",
                title = "No photos yet",
                message = "Share photos from your trip with the group.",
                actionLabel = "Add Photo",
                onAction = { imagePicker.launch("image/*") }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(3.dp)
            ) {
                // Upload progress tile
                if (isUploading) {
                    item {
                        Box(
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp))
                                .background(Chalk100),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Coral, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                items(photos, key = { it.id }) { photo ->
                    AsyncImage(
                        model = photo.url,
                        contentDescription = photo.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedPhoto = photo }
                    )
                }
            }
        }
    }

    // Caption + upload confirmation sheet
    if (showCaptionSheet && pendingUri != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showCaptionSheet = false
                pendingUri = null
                captionInput = ""
            },
            containerColor = Chalk50
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Photo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)

                // Preview
                AsyncImage(
                    model = pendingUri,
                    contentDescription = "Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                )

                OutlinedTextField(
                    value = captionInput,
                    onValueChange = { captionInput = it },
                    label = { Text("Caption (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        val uri = pendingUri ?: return@Button
                        showCaptionSheet = false
                        pendingUri = null
                        isUploading = true
                        uploadError = null
                        scope.launch {
                            try {
                                val photoUrl = uploadPhoto(context, uri, tripId)
                                val photo = ApiClient.apiService.savePhoto(
                                    buildMap {
                                        put("tripId", tripId)
                                        put("url", photoUrl)
                                        captionInput.trim().ifEmpty { null }?.let { put("caption", it) }
                                    }
                                )
                                photos = listOf(photo) + photos
                                captionInput = ""
                            } catch (e: Exception) {
                                uploadError = e.message ?: "Upload failed"
                            }
                            isUploading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("Upload Photo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Fullscreen photo viewer
    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            currentUser = currentUser,
            onDismiss = { selectedPhoto = null },
            onDeleted = {
                photos = photos.filter { it.id != photo.id }
                selectedPhoto = null
            },
            onPhotoUpdated = { updated ->
                photos = photos.map { if (it.id == updated.id) updated else it }
                selectedPhoto = updated
            }
        )
    }
}

// ── Upload helper ──────────────────────────────────────────────────────────────

private suspend fun uploadPhoto(context: Context, uri: Uri, tripId: String): String =
    withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Could not read image")

        // Try Azure SAS upload
        return@withContext try {
            val uploadResponse = ApiClient.apiService.getUploadUrl(tripId)
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(uploadResponse.uploadUrl)
                .put(bytes.toRequestBody("image/jpeg".toMediaType()))
                .addHeader("x-ms-blob-type", "BlockBlob")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Azure upload failed: ${response.code}")
            uploadResponse.blobUrl
        } catch (_: Exception) {
            // Fallback: base64 data URI
            "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        }
    }

// ── Fullscreen photo dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoDetailDialog(
    photo: TripPhoto,
    currentUser: User?,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    onPhotoUpdated: (TripPhoto) -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canDelete = photo.userId == currentUser?.id

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            AsyncImage(
                model = photo.url,
                contentDescription = photo.caption,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top bar
            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
                    .statusBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (canDelete) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.85f))
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(0.85f))
                }
            }

            // Right action rail (highlight, react, comment)
            Column(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ActionPill(
                    icon = if (photo.isHighlight == true) Icons.Default.Star else Icons.Default.StarBorder,
                    label = (photo.highlightVotes ?: 0).toString(),
                    tint = if (photo.isHighlight == true) Gold else Color.White,
                    onClick = {
                        scope.launch {
                            try {
                                val r = ApiClient.apiService.togglePhotoHighlight(mapOf("photoId" to photo.id))
                                onPhotoUpdated(
                                    photo.copy(
                                        highlightVotes = r.highlightVotes ?: photo.highlightVotes,
                                        isHighlight = r.isHighlight ?: photo.isHighlight
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                )
                ActionPill(
                    icon = Icons.Default.AddReaction,
                    label = "${photo.reactions?.sumOf { it.count } ?: 0}",
                    tint = Color.White,
                    onClick = { showReactionPicker = true }
                )
                ActionPill(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "${photo.commentCount ?: 0}",
                    tint = Color.White,
                    onClick = { showComments = true }
                )
            }

            // Bottom info: caption, reactions strip, author
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.7f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photo.reactions?.takeIf { it.isNotEmpty() }?.let { rxs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rxs.forEach { rx ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (rx.mine) Coral.copy(0.6f) else Color.Black.copy(0.4f)
                            ) {
                                Text(
                                    "${rx.emoji} ${rx.count}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                if (!photo.caption.isNullOrEmpty()) {
                    Text(photo.caption, color = Color.White, fontSize = 15.sp)
                }
                val authorName = photo.user?.name ?: "Unknown"
                Text(authorName, color = Color.White.copy(0.7f), fontSize = 12.sp)
            }

            if (isDeleting) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    if (showReactionPicker) {
        ReactionPickerSheet(
            current = photo.reactions ?: emptyList(),
            onPick = { emoji ->
                showReactionPicker = false
                scope.launch {
                    try {
                        val res = ApiClient.apiService.togglePhotoReaction(
                            mapOf("photoId" to photo.id, "emoji" to emoji)
                        )
                        val current = (photo.reactions ?: emptyList()).toMutableList()
                        val idx = current.indexOfFirst { it.emoji == emoji }
                        when (res.toggled) {
                            "added" -> {
                                if (idx >= 0) {
                                    current[idx] = current[idx].copy(count = current[idx].count + 1, mine = true)
                                } else {
                                    current += ReactionSummary(emoji, 1, true)
                                }
                            }
                            "removed" -> {
                                if (idx >= 0) {
                                    val updated = current[idx].copy(count = (current[idx].count - 1).coerceAtLeast(0), mine = false)
                                    if (updated.count == 0) current.removeAt(idx) else current[idx] = updated
                                }
                            }
                        }
                        onPhotoUpdated(photo.copy(reactions = current.toList()))
                    } catch (_: Exception) {}
                }
            },
            onDismiss = { showReactionPicker = false }
        )
    }

    if (showComments) {
        CommentsSheet(
            photoId = photo.id,
            currentUser = currentUser,
            onDismiss = { showComments = false },
            onCommentChange = { newCount ->
                onPhotoUpdated(photo.copy(commentCount = newCount))
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Photo?") },
            text = { Text("This will permanently remove the photo for everyone in the trip.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            isDeleting = true
                            try {
                                ApiClient.apiService.deletePhoto(mapOf("photoId" to photo.id))
                                onDeleted()
                            } catch (_: Exception) {}
                            isDeleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Action pill (fullscreen overlay) ───────────────────────────────────────────

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Reaction picker sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionPickerSheet(
    current: List<ReactionSummary>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🔥", "🎉", "✨")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("React", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                emojis.forEach { emoji ->
                    val mine = current.any { it.emoji == emoji && it.mine }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (mine) Coral.copy(alpha = 0.15f) else Chalk100)
                            .clickable { onPick(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

// ── Comments sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(
    photoId: String,
    currentUser: User?,
    onDismiss: () -> Unit,
    onCommentChange: (Int) -> Unit
) {
    var comments by remember { mutableStateOf<List<PhotoComment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(photoId) {
        scope.launch {
            loading = true
            try { comments = ApiClient.apiService.getPhotoComments(photoId).comments }
            catch (_: Exception) {}
            loading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 560.dp)
                .padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Comments", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Chalk900)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> LoadingView()
                    comments.isEmpty() -> Text(
                        "No comments yet. Be the first.",
                        color = Chalk500,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(comments, key = { it.id }) { c ->
                            CommentRow(
                                comment = c,
                                canDelete = c.userId == currentUser?.id,
                                onDelete = {
                                    scope.launch {
                                        try {
                                            ApiClient.apiService.deletePhotoComment(mapOf("commentId" to c.id))
                                            comments = comments.filter { it.id != c.id }
                                            onCommentChange(comments.size)
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Add a comment…") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                val canSend = input.trim().isNotEmpty() && !sending
                FilledIconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty()) return@FilledIconButton
                        sending = true
                        scope.launch {
                            try {
                                val created = ApiClient.apiService.addPhotoComment(
                                    mapOf("photoId" to photoId, "text" to text)
                                )
                                comments = comments + created
                                input = ""
                                onCommentChange(comments.size)
                            } catch (_: Exception) {}
                            sending = false
                        }
                    },
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Coral,
                        disabledContainerColor = Chalk100
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: PhotoComment,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Coral.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                comment.user?.name?.firstOrNull()?.uppercase() ?: "?",
                color = Coral,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                comment.user?.name ?: "Unknown",
                color = Chalk900,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(comment.text, color = Chalk700, fontSize = 13.sp)
        }
        if (canDelete) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Chalk400, modifier = Modifier.size(16.dp))
            }
        }
    }
}
