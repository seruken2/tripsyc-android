package com.tripsyc.app.ui.trip.photos

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
fun PhotosScreen(tripId: String) {
    var photos by remember { mutableStateOf<List<TripPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try { photos = ApiClient.apiService.getPhotos(tripId).photos }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Photos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
        Text("${photos.size} photos", fontSize = 13.sp, color = Chalk500)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            LoadingView()
        } else if (photos.isEmpty()) {
            EmptyState(icon = "📷", title = "No photos yet", message = "Share photos from your trip.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(photos) { photo ->
                    AsyncImage(
                        model = photo.url,
                        contentDescription = photo.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
