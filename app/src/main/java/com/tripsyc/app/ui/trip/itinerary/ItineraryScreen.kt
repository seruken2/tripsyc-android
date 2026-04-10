package com.tripsyc.app.ui.trip.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.ItineraryCategory
import com.tripsyc.app.data.api.models.ItineraryItem
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(tripId: String, currentUser: com.tripsyc.app.data.api.models.User? = null) {
    var items by remember { mutableStateOf<List<ItineraryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItineraryItem?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try { items = ApiClient.apiService.getItinerary(tripId).items }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Itinerary", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Coral, contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isLoading) { item { LoadingView() } }
        else if (items.isEmpty()) {
            item {
                EmptyState(icon = "📋", title = "No itinerary yet", message = "Add flights, hotels, and activities.",
                    actionLabel = "Add Item", onAction = { showAddSheet = true })
            }
        } else {
            items(items) { item ->
                ItineraryCard(
                    item = item,
                    onEdit = { editingItem = item },
                    onDelete = {
                        scope.launch {
                            try {
                                ApiClient.apiService.deleteItineraryItem(mapOf("itemId" to item.id))
                                load()
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddItinerarySheet(tripId = tripId, onDismiss = { showAddSheet = false }, onAdded = { load() })
    }

    editingItem?.let { item ->
        EditItinerarySheet(item = item, onDismiss = { editingItem = null }, onSaved = { editingItem = null; load() })
    }
}

@Composable
private fun ItineraryCard(item: ItineraryItem, onEdit: () -> Unit = {}, onDelete: () -> Unit) {
    val (icon, color) = categoryIconColor(item.category)
    Surface(shape = RoundedCornerShape(14.dp), color = CardBackground, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, color = Chalk900)
                if (!item.date.isNullOrEmpty()) {
                    Text("📅 ${item.date}${if (!item.time.isNullOrEmpty()) " ${item.time}" else ""}", fontSize = 12.sp, color = Chalk500)
                }
                if (!item.location.isNullOrEmpty()) {
                    Text("📍 ${item.location}", fontSize = 12.sp, color = Chalk500)
                }
                if (!item.confirmationCode.isNullOrEmpty()) {
                    Text("Ref: ${item.confirmationCode}", fontSize = 11.sp, color = Chalk400)
                }
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Chalk400, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Chalk200, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

private fun categoryIconColor(cat: ItineraryCategory): Pair<ImageVector, androidx.compose.ui.graphics.Color> =
    when (cat) {
        ItineraryCategory.FLIGHT -> Pair(Icons.Default.AirplanemodeActive, Dusk)
        ItineraryCategory.HOTEL -> Pair(Icons.Default.Hotel, Gold)
        ItineraryCategory.ACTIVITY -> Pair(Icons.Default.DirectionsRun, Coral)
        ItineraryCategory.RESTAURANT -> Pair(Icons.Default.Restaurant, Success)
        ItineraryCategory.TRANSPORT -> Pair(Icons.Default.DirectionsCar, Chalk500)
        ItineraryCategory.EMERGENCY -> Pair(Icons.Default.LocalHospital, Danger)
        ItineraryCategory.INFO -> Pair(Icons.Default.Info, Dusk)
        ItineraryCategory.OTHER -> Pair(Icons.Default.Circle, Chalk400)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItinerarySheet(tripId: String, onDismiss: () -> Unit, onAdded: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ItineraryCategory.ACTIVITY) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Itinerary Item", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)

            // Category
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ItineraryCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Coral.copy(alpha = 0.15f), selectedLabelColor = Coral)
                    )
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            }
            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)
            Button(
                onClick = {
                    if (title.isBlank()) { error = "Title required"; return@Button }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.addItineraryItem(mapOf(
                                "tripId" to tripId, "title" to title.trim(),
                                "category" to selectedCategory.name,
                                "location" to location.trim().ifEmpty { null },
                                "date" to date.trim().ifEmpty { null },
                                "time" to time.trim().ifEmpty { null }
                            ))
                            onAdded(); onDismiss()
                        } catch (e: Exception) { error = e.message }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !isLoading,
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Add Item", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItinerarySheet(item: ItineraryItem, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(item.title) }
    var location by remember { mutableStateOf(item.location ?: "") }
    var date by remember { mutableStateOf(item.date?.take(10) ?: "") }
    var time by remember { mutableStateOf(item.time ?: "") }
    var confirmationCode by remember { mutableStateOf(item.confirmationCode ?: "") }
    var description by remember { mutableStateOf(item.description ?: "") }
    var selectedCategory by remember { mutableStateOf(item.category) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Item", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ItineraryCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Coral.copy(alpha = 0.15f), selectedLabelColor = Coral)
                    )
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            }
            OutlinedTextField(value = confirmationCode, onValueChange = { confirmationCode = it }, label = { Text("Confirmation/Ref #") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(), maxLines = 3, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)
            Button(
                onClick = {
                    if (title.isBlank()) { error = "Title required"; return@Button }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.updateItineraryItem(mapOf(
                                "itemId" to item.id,
                                "title" to title.trim(),
                                "category" to selectedCategory.name,
                                "location" to location.trim().ifEmpty { null },
                                "date" to date.trim().ifEmpty { null },
                                "time" to time.trim().ifEmpty { null },
                                "confirmationCode" to confirmationCode.trim().ifEmpty { null },
                                "description" to description.trim().ifEmpty { null }
                            ))
                            onSaved()
                        } catch (e: Exception) { error = e.message }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !isLoading,
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Save Changes", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
