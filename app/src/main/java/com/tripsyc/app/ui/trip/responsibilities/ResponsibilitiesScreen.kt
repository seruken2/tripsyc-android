package com.tripsyc.app.ui.trip.responsibilities

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
import com.tripsyc.app.data.api.models.Responsibility
import com.tripsyc.app.data.api.models.TripMember
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsibilitiesScreen(tripId: String, members: List<TripMember>) {
    var items by remember { mutableStateOf<List<Responsibility>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try { items = ApiClient.apiService.getResponsibilities(tripId).items }
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
                Column {
                    Text("Responsibilities", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                    Text("${items.count { it.completed }} / ${items.size} done", fontSize = 13.sp, color = Chalk500)
                }
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Coral, contentColor = Color.White, modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isLoading) { item { LoadingView() } }
        else if (items.isEmpty()) {
            item { EmptyState(icon = "✅", title = "No tasks yet", message = "Assign responsibilities to keep things organized.",
                actionLabel = "Add Task", onAction = { showAddSheet = true }) }
        } else {
            items(items) { item ->
                Surface(shape = RoundedCornerShape(12.dp), color = CardBackground, shadowElevation = 1.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = item.completed,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    try {
                                        ApiClient.apiService.updateResponsibility(mapOf("id" to item.id, "completed" to checked))
                                        load()
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Success)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = if (item.completed) Chalk400 else Chalk900,
                                textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                fontWeight = FontWeight.Medium
                            )
                            if (!item.description.isNullOrEmpty()) {
                                Text(item.description, fontSize = 12.sp, color = Chalk500)
                            }
                            if (!item.assignedTo.isNullOrEmpty()) {
                                val memberName = members.firstOrNull { it.userId == item.assignedTo }?.name ?: item.assignedTo
                                Text("Assigned to $memberName", fontSize = 11.sp, color = Dusk)
                            }
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        ApiClient.apiService.deleteResponsibility(mapOf("id" to item.id))
                                        load()
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Chalk200, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddResponsibilitySheet(
            tripId = tripId,
            members = members,
            onDismiss = { showAddSheet = false },
            onAdded = { load() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddResponsibilitySheet(
    tripId: String,
    members: List<TripMember>,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Task", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task title *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(), maxLines = 3, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

            // Assignee dropdown
            ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                OutlinedTextField(
                    value = members.firstOrNull { it.userId == assignedTo }?.name ?: "Unassigned",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign to") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )
                ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    DropdownMenuItem(text = { Text("Unassigned") }, onClick = { assignedTo = null; dropdownExpanded = false })
                    members.forEach { m ->
                        DropdownMenuItem(text = { Text(m.name) }, onClick = { assignedTo = m.userId; dropdownExpanded = false })
                    }
                }
            }

            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)
            Button(
                onClick = {
                    if (title.isBlank()) { error = "Title is required"; return@Button }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.addResponsibility(mapOf(
                                "tripId" to tripId, "title" to title.trim(),
                                "description" to description.trim().ifEmpty { null },
                                "assignedTo" to assignedTo
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
                else Text("Add Task", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
