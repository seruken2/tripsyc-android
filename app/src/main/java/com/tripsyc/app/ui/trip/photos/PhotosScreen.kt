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

@Composable
private fun PhotoDetailDialog(
    photo: TripPhoto,
    currentUser: User?,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canDelete = photo.userId == currentUser?.id

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            // Photo
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

            // Caption + info at bottom
            if (!photo.caption.isNullOrEmpty() || photo.user != null) {
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!photo.caption.isNullOrEmpty()) {
                        Text(photo.caption, color = Color.White, fontSize = 15.sp)
                    }
                    val authorName = photo.user?.name ?: "Unknown"
                    Text(authorName, color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
            }

            // Deleting overlay
            if (isDeleting) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
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
