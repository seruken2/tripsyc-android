package com.tripsyc.app.ui.trip.overview

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.LockType
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.ChalkDivider
import com.tripsyc.app.ui.theme.*
import com.tripsyc.app.ui.trip.TripTab
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Composable
fun OverviewScreen(
    trip: Trip,
    currentUser: User?,
    onTabSelected: (TripTab) -> Unit,
    isOrganizer: Boolean = false,
    onTripUpdated: (Trip) -> Unit = {},
    onMemberChanged: () -> Unit = {}
) {
    val viewerMember = trip.members?.firstOrNull { it.userId == currentUser?.id }
    val lockedDateValue = trip.locks
        ?.firstOrNull { it.lockType == LockType.DATE && it.locked }
        ?.lockedValue
    val (lockedTripFrom, lockedTripUntil) = parseLockedDateRange(lockedDateValue)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cover-photo edit state — kept local so the rest of the overview
    // (members, locks, progress) can re-render without flashing the hero.
    var pendingCoverUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingCover by remember { mutableStateOf(false) }
    var coverError by remember { mutableStateOf<String?>(null) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCoverUri = uri
            coverError = null
            scope.launch {
                isUploadingCover = true
                try {
                    val url = uploadCoverPhoto(context, uri, trip.id)
                    val updated = ApiClient.apiService.updateTrip(
                        trip.id,
                        mapOf("coverImage" to url)
                    )
                    onTripUpdated(updated)
                } catch (e: Exception) {
                    coverError = e.message ?: "Couldn't update cover"
                    pendingCoverUri = null
                }
                isUploadingCover = false
            }
        }
    }

    val dateLock = trip.locks?.firstOrNull { it.lockType == LockType.DATE }
    val destLock = trip.locks?.firstOrNull { it.lockType == LockType.DESTINATION }
    val isDateLocked = dateLock?.locked == true
    val isDestLocked = destLock?.locked == true
    val isConfirmed = isDateLocked && isDestLocked
    val memberCount = trip.count?.members ?: trip.members?.size ?: 0

    val currentStage = when {
        isConfirmed -> 3
        isDateLocked || isDestLocked -> 2
        memberCount > 1 -> 1
        else -> 0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Hero cover image ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Optimistic preview wins while an upload is in flight so
                // the user sees their new photo immediately rather than
                // staring at the old cover.
                when {
                    pendingCoverUri != null -> {
                        AsyncImage(
                            model = pendingCoverUri,
                            contentDescription = trip.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    !trip.coverImage.isNullOrEmpty() &&
                        (trip.coverImage.startsWith("https://") || trip.coverImage.startsWith("http://")) -> {
                        AsyncImage(
                            model = trip.coverImage,
                            contentDescription = trip.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Coral, CoralLight))),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("T", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.6f to Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = trip.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!trip.approxMonth.isNullOrEmpty()) {
                        Text(
                            text = trip.approxMonth,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }

                // Top-trailing "Change cover" pill — visible only to organizers.
                // The Settings entry-point is buried under More → Settings, so
                // a discoverable affordance on the hero matches what users
                // expect after seeing similar buttons on Instagram / Airbnb.
                if (isOrganizer) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.35f)
                        ),
                        onClick = {
                            if (!isUploadingCover) {
                                coverPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (!trip.coverImage.isNullOrEmpty()) "Change cover" else "Add cover",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Uploading overlay — dim + spinner over the hero so the user
                // always sees feedback for a tap they just made.
                if (isUploadingCover) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                            Text(
                                text = "Updating cover…",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            if (coverError != null) {
                Text(
                    text = coverError!!,
                    color = Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // ── RSVP header (only for actual trip members) ────────────────────
        if (viewerMember != null) {
            item {
                RsvpHeaderCard(
                    initialRsvp = viewerMember.rsvp,
                    initialRsvpNote = viewerMember.rsvpNote,
                    initialAttendFrom = viewerMember.attendFrom,
                    initialAttendUntil = viewerMember.attendUntil,
                    lockedTripFrom = lockedTripFrom,
                    lockedTripUntil = lockedTripUntil,
                    onRsvpChanged = { onMemberChanged() },
                    onWindowChanged = { onMemberChanged() }
                )
            }
        }

        // ── Progress strip ────────────────────────────────────────────────
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Trip Progress",
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900
                    )

                    // 4-segment progress bar
                    val stageLabels = listOf("Gather", "Vote", "Lock", "Go!")
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until 4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (i <= currentStage) Coral else Chalk100)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            stageLabels.forEachIndexed { i, label ->
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (i <= currentStage) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (i <= currentStage) Coral else Chalk400,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    ChalkDivider()

                    // Checklist items
                    listOf(
                        Triple("Dates voted", isDateLocked, Dusk),
                        Triple("Destination voted", isDestLocked, Coral),
                        Triple("Fully confirmed", isConfirmed, Success)
                    ).forEach { (label, done, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, color = Chalk900, fontSize = 14.sp)
                            Icon(
                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) color else Chalk200,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Status + members card ─────────────────────────────────────────
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        val (statusText, statusColor) = when {
                            isConfirmed -> "Confirmed" to Success
                            isDateLocked && !isDestLocked -> "Date Locked" to Dusk
                            !isDateLocked && isDestLocked -> "Dest Locked" to Coral
                            memberCount > 1 -> "Voting" to Gold
                            else -> "Gathering" to Coral
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    ChalkDivider()

                    // Members row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Members (${trip.members?.size ?: trip.count?.members ?: 0})",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        if (!trip.members.isNullOrEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(trip.members) { member ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Coral.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!member.avatarUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = member.avatarUrl,
                                                    contentDescription = member.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                                                    color = Coral,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = member.name.split(" ").firstOrNull() ?: member.name,
                                            fontSize = 10.sp,
                                            color = Chalk500,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Locked decisions ──────────────────────────────────────────────
        if (isDateLocked || isDestLocked) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Locked Decisions",
                            fontWeight = FontWeight.SemiBold,
                            color = Chalk900
                        )
                        if (isDateLocked && dateLock?.lockedValue != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                                Text(text = "Date: ${dateLock.lockedValue}", color = Chalk900)
                            }
                        }
                        if (isDestLocked && destLock?.lockedValue != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
                                val destName = trip.destinations?.firstOrNull { it.id == destLock.lockedValue }
                                    ?.let { "${it.city}, ${it.country}" }
                                    ?: destLock.lockedValue
                                Text(text = "Destination: $destName", color = Chalk900)
                            }
                        }
                    }
                }
            }
        }

        // ── Quick action cards ────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Chalk900
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard(
                        icon = Icons.Default.CalendarMonth,
                        label = "Dates",
                        color = Dusk,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Dates) }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Map,
                        label = "Destinations",
                        color = Coral,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Destinations) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard(
                        icon = Icons.Default.AccountBalance,
                        label = "Budget",
                        color = Sage,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Budget) }
                    )
                    QuickActionCard(
                        icon = Icons.Default.ChatBubble,
                        label = "Chat",
                        color = Gold,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(TripTab.Chat) }
                    )
                }
            }
        }
    }
}

/// Parses the lock value "YYYY-MM-DD to YYYY-MM-DD" into two LocalDates.
/// Returns (null, null) when the date isn't locked yet or the string is
/// malformed. Used to seed/clamp the attend-window picker on the RSVP card.
private fun parseLockedDateRange(value: String?): Pair<LocalDate?, LocalDate?> {
    if (value.isNullOrBlank()) return null to null
    val parts = value.split(" to ")
    if (parts.size != 2) return null to null
    val from = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull()
    val until = runCatching { LocalDate.parse(parts[1].trim()) }.getOrNull()
    return from to until
}

// Uploads the picked image to Azure Blob via SAS, falling back to a base64
// data URI if Azure is unreachable. Mirrors PhotosScreen.uploadPhoto so the
// two entry points produce identical server-side results.
private suspend fun uploadCoverPhoto(context: Context, uri: Uri, tripId: String): String =
    withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Could not read image")

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
            "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        }
    }

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        tonalElevation = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
