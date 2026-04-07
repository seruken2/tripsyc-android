package com.tripsyc.app.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTripSheet(
    onDismiss: () -> Unit,
    onJoined: (Trip) -> Unit
) {
    val scope = rememberCoroutineScope()
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Chalk50
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Join a Trip",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Chalk900
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Enter the invite code shared by the trip organizer.",
                fontSize = 14.sp,
                color = Chalk500
            )

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase(); error = null },
                label = { Text("Invite Code") },
                placeholder = { Text("e.g. ABC123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = Chalk200,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (error != null) {
                Text(text = error!!, color = Danger, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (inviteCode.isBlank()) {
                        error = "Please enter an invite code"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            // First find trip by invite code
                            val trips = ApiClient.apiService.getTrips()
                            val matchingTrip = trips.firstOrNull {
                                it.inviteCode.equals(inviteCode.trim(), ignoreCase = true)
                            }
                            if (matchingTrip == null) {
                                // Try to join directly with any known tripId - not ideal but the join endpoint needs tripId
                                error = "Invalid invite code. Please check and try again."
                            } else {
                                val member = ApiClient.apiService.joinTrip(
                                    mapOf(
                                        "tripId" to matchingTrip.id,
                                        "inviteCode" to inviteCode.trim()
                                    )
                                )
                                val trip = ApiClient.apiService.getTrip(matchingTrip.id)
                                onJoined(trip)
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to join trip"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Join Trip", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
