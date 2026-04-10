package com.tripsyc.app.ui.trip.memories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.TripPhoto
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MemoriesScreen(tripId: String) {
    var photos by remember { mutableStateOf<List<TripPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        scope.launch {
            isLoading = true
            try { photos = ApiClient.apiService.getPhotos(tripId).photos }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Memories", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
        Text("Relive the best moments", fontSize = 14.sp, color = Chalk500)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            LoadingView()
        } else if (photos.isEmpty()) {
            EmptyState(icon = "🎞️", title = "No memories yet", message = "Photos shared during the trip will appear here.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    Column {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        if (!photo.caption.isNullOrEmpty()) {
                            Text(photo.caption, fontSize = 11.sp, color = Chalk500, modifier = Modifier.padding(top = 4.dp))
                        }
                        val uploaderName = photo.user?.name ?: ""
                        if (uploaderName.isNotEmpty()) {
                            Text("by $uploaderName", fontSize = 10.sp, color = Chalk400)
                        }
                    }
                }
            }
        }
    }
}
