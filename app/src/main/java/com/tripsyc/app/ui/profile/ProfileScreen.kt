package com.tripsyc.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.TravelStyle
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: User?,
    onUserUpdated: (User) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profile by remember { mutableStateOf(currentUser) }
    var isLoading by remember { mutableStateOf(currentUser == null) }
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (profile == null) {
            try {
                profile = ApiClient.apiService.getProfile()
                profile?.let { onUserUpdated(it) }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    // Edit form state
    var editName by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var editCity by remember(profile) { mutableStateOf(profile?.city ?: "") }
    var editBio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var editStyle by remember(profile) { mutableStateOf(profile?.travelStyle) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (isLoading) {
        LoadingView("Loading profile...")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)

        // Avatar + name card
        Surface(shape = RoundedCornerShape(20.dp), color = CardBackground, shadowElevation = 3.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile!!.avatarUrl,
                            contentDescription = profile?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Coral, modifier = Modifier.size(40.dp))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = profile?.name ?: profile?.email?.substringBefore("@") ?: "User",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Chalk900
                    )
                    Text(
                        text = profile?.email ?: "",
                        fontSize = 13.sp,
                        color = Chalk500
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!profile?.city.isNullOrEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Chalk100) {
                            Text("📍 ${profile!!.city}", color = Chalk500, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    profile?.travelStyle?.let { style ->
                        Surface(shape = RoundedCornerShape(8.dp), color = Coral.copy(alpha = 0.12f)) {
                            Text(style.name.lowercase().replaceFirstChar { it.uppercase() }, color = Coral, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                OutlinedButton(
                    onClick = { isEditing = !isEditing },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditing) "Cancel" else "Edit Profile")
                }
            }
        }

        // Edit form
        if (isEditing) {
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Edit Profile", fontWeight = FontWeight.SemiBold, color = Chalk900)

                    OutlinedTextField(value = editName, onValueChange = { editName = it },
                        label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

                    OutlinedTextField(value = editCity, onValueChange = { editCity = it },
                        label = { Text("Home City") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

                    OutlinedTextField(value = editBio, onValueChange = { editBio = it },
                        label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

                    // Travel style
                    Text("Travel Style", color = Chalk500, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TravelStyle.values().forEach { style ->
                            FilterChip(
                                selected = editStyle == style,
                                onClick = { editStyle = style },
                                label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Coral.copy(alpha = 0.15f),
                                    selectedLabelColor = Coral
                                )
                            )
                        }
                    }

                    if (error != null) Text(error!!, color = Danger, fontSize = 13.sp)

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                error = null
                                try {
                                    val updated = ApiClient.apiService.updateProfile(
                                        mapOf(
                                            "name" to editName.trim().ifEmpty { null },
                                            "city" to editCity.trim().ifEmpty { null },
                                            "bio" to editBio.trim().ifEmpty { null },
                                            "travelStyle" to editStyle?.name
                                        )
                                    )
                                    profile = updated
                                    onUserUpdated(updated)
                                    isEditing = false
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to save profile"
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save Changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Logout
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            try { ApiClient.apiService.logout() } catch (_: Exception) {}
                            ApiClient.clearSession()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) { Text("Sign Out") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}
