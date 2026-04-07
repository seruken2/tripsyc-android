package com.tripsyc.app.ui.trip.packing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.PackingItem
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PackingScreen(tripId: String) {
    var items by remember { mutableStateOf<List<PackingItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var newItemText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try { items = ApiClient.apiService.getPackingItems(tripId).items }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Packing List", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${items.count { it.packed }} / ${items.size} packed",
            fontSize = 13.sp, color = Chalk500
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Add item row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add item...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Button(
                onClick = {
                    val text = newItemText.trim()
                    if (text.isEmpty()) return@Button
                    newItemText = ""
                    scope.launch {
                        try {
                            ApiClient.apiService.addPackingItem(mapOf("tripId" to tripId, "text" to text))
                            load()
                        } catch (_: Exception) {}
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            LoadingView()
        } else if (items.isEmpty()) {
            EmptyState(icon = "🧳", title = "Nothing packed yet", message = "Add items to your packing list.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardBackground,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = item.packed,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            try {
                                                ApiClient.apiService.togglePacked(mapOf("itemId" to item.id, "packed" to checked))
                                                load()
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Coral)
                                )
                                Text(
                                    text = item.text,
                                    color = if (item.packed) Chalk400 else Chalk900,
                                    textDecoration = if (item.packed) TextDecoration.LineThrough else null,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            ApiClient.apiService.deletePackingItem(mapOf("itemId" to item.id))
                                            load()
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Chalk200, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
