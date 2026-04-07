package com.tripsyc.app.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun CreateTripSheet(
    onDismiss: () -> Unit,
    onCreated: (Trip) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var approxMonth by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Create a Trip",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Chalk900
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Trip Name *") },
                placeholder = { Text("e.g. Paris Summer 2026") },
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

            OutlinedTextField(
                value = approxMonth,
                onValueChange = { approxMonth = it },
                label = { Text("Approx. Month (optional)") },
                placeholder = { Text("e.g. June 2026") },
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

            // Currency selector
            var currencyExpanded by remember { mutableStateOf(false) }
            val currencies = listOf("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF", "INR")

            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it }
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = Chalk200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    currencies.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                currency = c
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            if (error != null) {
                Text(text = error!!, color = Danger, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Please enter a trip name"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val body = mutableMapOf<String, String?>(
                                "name" to name.trim(),
                                "currency" to currency
                            )
                            if (approxMonth.isNotBlank()) body["approxMonth"] = approxMonth.trim()
                            val trip = ApiClient.apiService.createTrip(body)
                            onCreated(trip)
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to create trip"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading && name.isNotBlank(),
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
                Text("Create Trip", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
