package com.tripsyc.app.ui.trip.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.BudgetType
import com.tripsyc.app.data.api.models.MyBudgetResponse
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(tripId: String) {
    var budgetData by remember { mutableStateOf<com.tripsyc.app.data.api.models.BudgetAPIResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var budgetInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BudgetType.SOFT) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadBudget() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.apiService.getBudget(tripId)
                budgetData = response
                if (response.myBudget != null) {
                    budgetInput = response.myBudget.budgetMax.toString()
                    selectedType = response.myBudget.budgetType
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load budget"
            }
            isLoading = false
        }
    }

    LaunchedEffect(tripId) { loadBudget() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Budget Comfort Zone", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                Text("Anonymous group budget overview. No individual amounts shown.", fontSize = 14.sp, color = Chalk500)
            }
        }

        // Privacy notice
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Dusk.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Dusk)
                    Column {
                        Text("Privacy Protected", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        Text(
                            "Individual budget amounts are never shown. The group only sees anonymous bands.",
                            fontSize = 12.sp, color = Chalk500
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item { LoadingView("Loading budget...") }
            return@LazyColumn
        }

        // My budget form
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Your Budget",
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Max budget (${budgetData?.currency ?: "USD"})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Coral,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Budget type
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == BudgetType.SOFT,
                            onClick = { selectedType = BudgetType.SOFT },
                            label = { Text("Soft Limit") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Coral.copy(alpha = 0.15f),
                                selectedLabelColor = Coral
                            )
                        )
                        FilterChip(
                            selected = selectedType == BudgetType.HARD,
                            onClick = { selectedType = BudgetType.HARD },
                            label = { Text("Hard Limit") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Coral.copy(alpha = 0.15f),
                                selectedLabelColor = Coral
                            )
                        )
                    }

                    if (error != null) {
                        Text(text = error!!, color = Danger, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val amount = budgetInput.toIntOrNull() ?: return@Button
                            scope.launch {
                                isSaving = true
                                try {
                                    ApiClient.apiService.updateBudget(
                                        mapOf(
                                            "tripId" to tripId,
                                            "budgetMax" to amount,
                                            "budgetType" to selectedType.name
                                        )
                                    )
                                    loadBudget()
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to save budget"
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isSaving && budgetInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save Budget", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Group budget bands
        val data = budgetData
        if (data != null && data.showBands) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Group Budget Overview", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        Text(
                            "${data.memberCount} of ${data.totalMembers} members have set budgets",
                            fontSize = 13.sp, color = Chalk500
                        )
                        if (data.greenBand != null) {
                            BandRow(
                                label = "Green Zone",
                                range = "$${data.greenBand.min}-$${data.greenBand.max}",
                                color = Success
                            )
                        }
                        if (data.yellowBand != null) {
                            BandRow(
                                label = "Stretch Zone",
                                range = "$${data.yellowBand.min}-$${data.yellowBand.max}",
                                color = Gold
                            )
                        }
                        if (data.hasHardCaps && data.lowestHardCap != null) {
                            BandRow(
                                label = "Hard Cap",
                                range = "Max $${data.lowestHardCap}",
                                color = Danger
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BandRow(label: String, range: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = RoundedCornerShape(50))
            )
            Text(text = label, color = Chalk900, fontSize = 14.sp)
        }
        Text(text = range, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
