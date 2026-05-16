package com.tripsyc.app.ui.crews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.Crew
import com.tripsyc.app.data.api.models.CrewMember
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Saved co-traveler groups — recurring crews users can pre-build and
 * invite as a bundle when they spin up a new trip. CRUD against
 * /api/crews; mirrors iOS CrewsView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewsScreen() {
    var crews by remember { mutableStateOf<List<Crew>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingCrew by remember { mutableStateOf<Crew?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var crewToDelete by remember { mutableStateOf<Crew?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try { crews = ApiClient.apiService.getCrews() }
        catch (e: Exception) { error = e.message ?: "Couldn't load crews" }
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Crews", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                Text(
                    "Saved travel groups you can bulk-invite to new trips.",
                    fontSize = 13.sp, color = Chalk500
                )
            }
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Coral,
                contentColor = Color.White,
                modifier = Modifier.size(44.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "New crew", modifier = Modifier.size(20.dp)) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            LoadingView()
        } else if (crews.isEmpty()) {
            EmptyState(
                icon = "👥",
                title = "No crews yet",
                message = "Group your usual travelers so you can pull them into new trips with one tap."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items = crews, key = { it.id }) { crew ->
                    CrewCard(
                        crew = crew,
                        onRename = { editingCrew = crew },
                        onDelete = { crewToDelete = crew }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CrewNameDialog(
            initial = "",
            title = "New crew",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                scope.launch {
                    try {
                        ApiClient.apiService.createCrew(mapOf("name" to name))
                        reload()
                    } catch (_: Exception) {}
                }
            }
        )
    }

    editingCrew?.let { crew ->
        CrewNameDialog(
            initial = crew.name,
            title = "Rename crew",
            confirmLabel = "Save",
            onDismiss = { editingCrew = null },
            onConfirm = { name ->
                editingCrew = null
                scope.launch {
                    try {
                        ApiClient.apiService.renameCrew(crew.id, mapOf("name" to name))
                        reload()
                    } catch (_: Exception) {}
                }
            }
        )
    }

    crewToDelete?.let { crew ->
        AlertDialog(
            onDismissRequest = { crewToDelete = null },
            title = { Text("Delete crew?") },
            text = { Text("\"${crew.name}\" will be removed from your saved groups. This doesn't affect any trips you already invited them to.") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = crew
                        crewToDelete = null
                        scope.launch {
                            try {
                                ApiClient.apiService.deleteCrew(target.id)
                                reload()
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { crewToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CrewCard(crew: Crew, onRename: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = CardBackground, shadowElevation = 2.dp) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(crew.name, fontWeight = FontWeight.SemiBold, color = Chalk900, fontSize = 16.sp)
                    Text(
                        listOfNotNull(
                            "${crew.members?.size ?: 0} member${if ((crew.members?.size ?: 0) == 1) "" else "s"}",
                            crew.count?.trips?.let { "$it trip${if (it == 1) "" else "s"}" }
                        ).joinToString(" · "),
                        color = Chalk500, fontSize = 12.sp
                    )
                }
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Chalk500, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Danger.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            if (!crew.members.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    crew.members.forEach { m -> CrewMemberRow(member = m) }
                }
            }
        }
    }
}

@Composable
private fun CrewMemberRow(member: CrewMember) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Coral.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                member.name.firstOrNull()?.uppercase() ?: "?",
                color = Coral,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, color = Chalk900, fontSize = 13.sp)
            (member.email ?: member.phone)?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Chalk500, fontSize = 11.sp)
            }
        }
        if (member.userId == null) {
            Surface(shape = RoundedCornerShape(6.dp), color = Chalk100) {
                Text(
                    "Pending",
                    color = Chalk500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CrewNameDialog(
    initial: String,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 60) name = it },
                label = { Text("Crew name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
