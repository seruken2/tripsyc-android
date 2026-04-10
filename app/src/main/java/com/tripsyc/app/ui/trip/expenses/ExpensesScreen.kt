package com.tripsyc.app.ui.trip.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.*
import com.tripsyc.app.ui.common.EmptyState
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    tripId: String,
    currentUser: User?,
    tripCurrency: String = "USD"
) {
    var response by remember { mutableStateOf<ExpensesResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                response = ApiClient.apiService.getExpenses(tripId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load expenses"
            }
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
                Column {
                    Text("Expenses", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)
                    Text("Track spending and settle up after the trip.", fontSize = 14.sp, color = Chalk500)
                }
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Coral,
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add expense", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isLoading) {
            item { LoadingView("Loading expenses...") }
            return@LazyColumn
        }

        if (error != null) {
            item { Text(text = error!!, color = Danger) }
        }

        // Balance summary
        val balances = response?.balances ?: emptyList()
        if (balances.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Balances", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        balances.forEach { balance ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${balance.fromName} → ${balance.toName}",
                                        color = Chalk900,
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${response?.tripCurrency ?: tripCurrency} ${String.format("%.2f", balance.amount)}",
                                        color = if (balance.from == currentUser?.id) Danger else Success,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    // Show Settle all button if current user is involved in this balance
                                    if (balance.from == currentUser?.id || balance.to == currentUser?.id) {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        ApiClient.apiService.settleAll(
                                                            mapOf(
                                                                "tripId" to tripId,
                                                                "fromUserId" to balance.from,
                                                                "toUserId" to balance.to
                                                            )
                                                        )
                                                        load()
                                                    } catch (_: Exception) {}
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Settle all", color = Success, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val expenses = response?.expenses ?: emptyList()
        if (expenses.isEmpty()) {
            item {
                EmptyState(
                    icon = "💸",
                    title = "No expenses yet",
                    message = "Add an expense to split costs with your group.",
                    actionLabel = "Add Expense",
                    onAction = { showAddSheet = true }
                )
            }
        } else {
            items(expenses) { expense ->
                ExpenseCard(
                    expense = expense,
                    currentUserId = currentUser?.id,
                    currency = response?.tripCurrency ?: tripCurrency,
                    onDelete = {
                        scope.launch {
                            try {
                                ApiClient.apiService.deleteExpense(mapOf("expenseId" to expense.id))
                                load()
                            } catch (_: Exception) {}
                        }
                    },
                    onSettle = { splitId, settled ->
                        scope.launch {
                            try {
                                ApiClient.apiService.settleSplit(
                                    mapOf("splitId" to splitId, "settled" to settled)
                                )
                                load()
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddExpenseSheet(
            tripId = tripId,
            currency = tripCurrency,
            onDismiss = { showAddSheet = false },
            onAdded = { load() }
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseWithUser,
    currentUserId: String?,
    currency: String,
    onDelete: () -> Unit,
    onSettle: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, fontWeight = FontWeight.SemiBold, color = Chalk900)
                    Text(
                        text = "Paid by ${expense.paidByUser.name ?: "Someone"}",
                        fontSize = 12.sp, color = Chalk500
                    )
                    Text(
                        text = "${expense.category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 11.sp, color = Chalk400
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currency ${String.format("%.2f", expense.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = Chalk900,
                        fontSize = 16.sp
                    )
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (expanded) "Hide splits ▲" else "Show splits ▼", color = Coral, fontSize = 11.sp)
                    }
                }
            }

            if (expanded) {
                Divider(color = Chalk200)
                expense.splits.forEach { split ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = split.user.name ?: "Someone",
                            fontSize = 13.sp,
                            color = Chalk900
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$currency ${String.format("%.2f", split.amount)}",
                                fontSize = 13.sp,
                                color = if (split.settled) Chalk400 else Chalk900
                            )
                            if (split.userId == currentUserId || expense.paidBy == currentUserId) {
                                TextButton(
                                    onClick = { onSettle(split.id, !split.settled) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (split.settled) "Unsettle" else "Settle",
                                        color = if (split.settled) Chalk400 else Success,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (expense.paidBy == currentUserId) {
                    Divider(color = Chalk200)
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Delete expense", color = Danger, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseSheet(
    tripId: String,
    currency: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chalk50) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Expense", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Chalk900)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount ($currency) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Category chips
            Text("Category", color = Chalk500, fontSize = 13.sp)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Coral.copy(alpha = 0.15f),
                            selectedLabelColor = Coral
                        )
                    )
                }
            }

            if (error != null) Text(text = error!!, color = Danger, fontSize = 13.sp)

            Button(
                onClick = {
                    if (title.isBlank() || amount.isBlank()) {
                        error = "Please fill in title and amount"
                        return@Button
                    }
                    val amt = amount.toDoubleOrNull() ?: run {
                        error = "Invalid amount"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            ApiClient.apiService.addExpense(
                                mapOf(
                                    "tripId" to tripId,
                                    "title" to title.trim(),
                                    "amount" to amt,
                                    "currency" to currency,
                                    "category" to selectedCategory.name,
                                    "splitType" to "EQUAL"
                                )
                            )
                            onAdded()
                            onDismiss()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to add expense"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Add Expense", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
