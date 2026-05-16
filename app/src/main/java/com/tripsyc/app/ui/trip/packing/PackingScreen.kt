package com.tripsyc.app.ui.trip.packing

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var addedSuggestions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSuggesting by remember { mutableStateOf(false) }
    var suggestError by remember { mutableStateOf<String?>(null) }
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
        Spacer(modifier = Modifier.height(12.dp))

        // ── AI suggestions row ───────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Gold.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(15.dp))
                    Text("AI suggestions", color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSuggesting = true
                                suggestError = null
                                try {
                                    suggestions = ApiClient.apiService
                                        .generatePackingSuggestions(mapOf("tripId" to tripId))
                                        .items
                                } catch (e: Exception) {
                                    suggestError = e.message ?: "Couldn't suggest"
                                }
                                isSuggesting = false
                            }
                        },
                        enabled = !isSuggesting
                    ) {
                        Text(
                            if (suggestions.isEmpty()) "Generate" else "Refresh",
                            color = Gold,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isSuggesting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Gold
                    )
                }
                if (suggestError != null) {
                    Text(suggestError!!, color = Danger, fontSize = 12.sp)
                }
                if (suggestions.isNotEmpty()) {
                    // Existing item names so a chip-tap doesn't create a dup.
                    val existingLower = items.map { it.text.lowercase() }.toSet()
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestions.forEach { s ->
                            val isAdded = s.lowercase() in existingLower || s in addedSuggestions
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAdded) Sage.copy(alpha = 0.18f) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAdded) Sage else Gold.copy(alpha = 0.4f)
                                ),
                                onClick = {
                                    if (!isAdded) {
                                        addedSuggestions = addedSuggestions + s
                                        scope.launch {
                                            try {
                                                ApiClient.apiService.addPackingItem(
                                                    mapOf("tripId" to tripId, "text" to s)
                                                )
                                                load()
                                            } catch (_: Exception) {
                                                addedSuggestions = addedSuggestions - s
                                            }
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (isAdded) Icons.Default.Add else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isAdded) Sage else Gold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        s,
                                        color = if (isAdded) Sage else Chalk900,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
