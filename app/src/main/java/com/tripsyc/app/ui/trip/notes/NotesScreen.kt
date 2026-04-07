package com.tripsyc.app.ui.trip.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.Note
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(tripId: String, currentUser: User?) {
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try { notes = ApiClient.apiService.getNotes(tripId).notes }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notes & Links", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Coral, contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add note", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isLoading) { item { LoadingView() } }
        else if (notes.isEmpty()) {
            item {
                EmptyState(icon = "📝", title = "No notes yet", message = "Add links, notes, or inspiration for the trip.",
                    actionLabel = "Add Note", onAction = { showAddSheet = true })
            }
        } else {
            items(notes) { note ->
                NoteCard(
                    note = note,
                    isOwner = note.userId == currentUser?.id,
                    onDelete = {
                        scope.launch {
                            try {
                                ApiClient.apiService.deleteNote(mapOf("noteId" to note.id))
                                load()
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddNoteSheet(tripId = tripId, onDismiss = { showAddSheet = false }, onAdded = { load() })
    }
}

@Composable
private fun NoteCard(note: Note, isOwner: Boolean, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = CardBackground, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(note.title, fontWeight = FontWeight.SemiBold, color = Chalk900, modifier = Modifier.weight(1f))
                if (isOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Danger, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (!note.body.isNullOrEmpty()) {
                Text(note.body, color = Chalk500, fontSize = 13.sp)
            }
            if (!note.url.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Dusk, modifier = Modifier.size(14.dp))
                    Text(note.url, color = Dusk, fontSize = 12.sp, maxLines = 1)
                }
            }
            val author = note.user?.name ?: note.user?.email?.substringBefore("@") ?: ""
            if (author.isNotEmpty()) {
                Text("by $author", fontSize = 11.sp, color = Chalk400)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteSheet(tripId: String, onDismiss: () -> Unit, onAdded: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Note", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), maxLines = 4, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Link (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)
            Button(
                onClick = {
                    if (title.isBlank()) { error = "Title is required"; return@Button }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.createNote(mapOf(
                                "tripId" to tripId, "title" to title.trim(),
                                "body" to body.trim().ifEmpty { null },
                                "url" to url.trim().ifEmpty { null }
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
                else Text("Save Note", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
