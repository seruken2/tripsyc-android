package com.tripsyc.app.ui.trip.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsScreen(trip: Trip, currentUser: User?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tripName by remember { mutableStateOf(trip.name) }
    var approxMonth by remember { mutableStateOf(trip.approxMonth ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var isLeaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    val isOrganizer = trip.members?.firstOrNull { it.userId == currentUser?.id }
        ?.role?.let { it.name == "CREATOR" || it.name == "CO_ORGANIZER" } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Trip Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)

        if (isOrganizer) {
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Trip Details", fontWeight = FontWeight.SemiBold, color = Chalk900)

                    OutlinedTextField(
                        value = tripName,
                        onValueChange = { tripName = it },
                        label = { Text("Trip Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    OutlinedTextField(
                        value = approxMonth,
                        onValueChange = { approxMonth = it },
                        label = { Text("Approx. Month") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Coral,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    if (error != null) Text(error!!, color = Danger, fontSize = 13.sp)

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                error = null
                                try {
                                    ApiClient.apiService.updateTrip(
                                        trip.id,
                                        mapOf("name" to tripName.trim(), "approxMonth" to approxMonth.trim().ifEmpty { null })
                                    )
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to save"
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

        // Trip info
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Trip Info", fontWeight = FontWeight.SemiBold, color = Chalk900)
                InfoRow("Currency", trip.currency)
                InfoRow("Invite Code", trip.inviteCode)
                InfoRow("Members", "${trip.members?.size ?: trip.count?.members ?: 0}")
            }
        }

        // Leave trip
        if (trip.members?.firstOrNull { it.userId == currentUser?.id }?.role?.name != "CREATOR") {
            Surface(shape = RoundedCornerShape(16.dp), color = Danger.copy(alpha = 0.05f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Danger Zone", fontWeight = FontWeight.SemiBold, color = Danger)
                    OutlinedButton(
                        onClick = { showLeaveConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                        enabled = !isLeaving
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leave Trip")
                    }
                }
            }
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Trip?") },
            text = { Text("Are you sure you want to leave \"${trip.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        scope.launch {
                            isLeaving = true
                            try {
                                ApiClient.apiService.leaveTrip(mapOf("tripId" to trip.id))
                                onBack()
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to leave trip"
                            }
                            isLeaving = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Leave") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Chalk500, fontSize = 14.sp)
        Text(value, color = Chalk900, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
