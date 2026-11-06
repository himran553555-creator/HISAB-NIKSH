package com.example.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.BusinessProfileType
import com.example.model.Expense
import com.example.model.Income
import com.example.ui.components.EmptyView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedDanger
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseIncomeScreen(
    expenses: List<Expense>,
    incomes: List<Income>,
    activeProfile: BusinessProfileType,
    onAddExpense: (Expense) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onAddIncome: (Income) -> Unit,
    onDeleteIncome: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Expenses, 1: Income
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredExpenses = remember(expenses, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> expenses.filter { it.profileId != "personal" }
            BusinessProfileType.PERSONAL -> expenses.filter { it.profileId == "personal" }
            else -> expenses.filter { it.profileId == activeProfile.id }
        }
    }

    val filteredIncomes = remember(incomes, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> incomes.filter { it.profileId != "personal" }
            BusinessProfileType.PERSONAL -> incomes.filter { it.profileId == "personal" }
            else -> incomes.filter { it.profileId == activeProfile.id }
        }
    }

    val totalExpenseSum = filteredExpenses.sumOf { it.amount }
    val totalIncomeSum = filteredIncomes.sumOf { it.amount }

    Scaffold(
        modifier = modifier.testTag("expense_income_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = if (selectedTab == 0) RedDanger else EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Entry") },
                text = { Text(if (selectedTab == 0) "Add Expense" else "Add Income") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedTab == 0) {
                                if (activeProfile == BusinessProfileType.PERSONAL) "Total Personal Expenses" else "Total Business Expenses"
                            } else {
                                if (activeProfile == BusinessProfileType.PERSONAL) "Total Personal Income" else "Total Other Business Income"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = Formatters.formatCurrency(if (selectedTab == 0) totalExpenseSum else totalIncomeSum),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = activeProfile.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Expenses (${filteredExpenses.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Income (${filteredIncomes.size})") }
                )
            }

            if (selectedTab == 0) {
                if (filteredExpenses.isEmpty()) {
                    EmptyView(
                        message = "No expenses logged for this profile",
                        icon = Icons.Default.ReceiptLong,
                        actionLabel = "Record First Expense",
                        onAction = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredExpenses) { expense ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = expense.category,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${expense.description} • ${expense.date}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Method: ${expense.paymentMethod}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatCurrency(expense.amount),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = RedDanger
                                            )
                                        )
                                        IconButton(onClick = { onDeleteExpense(expense.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (filteredIncomes.isEmpty()) {
                    EmptyView(
                        message = "No income records found for this profile",
                        icon = Icons.Default.AttachMoney,
                        actionLabel = "Record First Income",
                        onAction = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredIncomes) { income ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = income.category,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${income.description} • ${income.date}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatCurrency(income.amount),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        )
                                        IconButton(onClick = { onDeleteIncome(income.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        if (selectedTab == 0) {
            AddExpenseDialog(
                initialProfileId = activeProfile.id,
                onDismiss = { showAddDialog = false },
                onSave = { exp ->
                    onAddExpense(exp)
                    showAddDialog = false
                }
            )
        } else {
            AddIncomeDialog(
                initialProfileId = activeProfile.id,
                onDismiss = { showAddDialog = false },
                onSave = { inc ->
                    onAddIncome(inc)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddExpenseDialog(
    initialProfileId: String,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var profileId by remember { mutableStateOf(if (initialProfileId == "all") "eshop" else initialProfileId) }
    val isPersonal = profileId == "personal"

    val categories = if (isPersonal) {
        listOf("Food & Groceries", "Transport", "Shopping", "Mobile Bills", "Entertainment", "Household", "Other Personal")
    } else {
        listOf("Courier Charges", "Packaging", "Transport", "Marketing & Ads", "Office Rent", "Internet", "Staff Salary", "Utilities", "Other Business")
    }

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(profileId) {
        selectedCategory = if (profileId == "personal") "Food & Groceries" else "Courier Charges"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Expense Entry",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Expense Account / Profile", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = profileId == "eshop",
                        onClick = { profileId = "eshop" },
                        label = { Text("E-SHOP") }
                    )
                    FilterChip(
                        selected = profileId == "freshoo",
                        onClick = { profileId = "freshoo" },
                        label = { Text("FRESHOO") }
                    )
                    FilterChip(
                        selected = profileId == "personal",
                        onClick = { profileId = "personal" },
                        label = { Text("PERSONAL") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Expense Category", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                var catExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory)
                    }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = {
                                selectedCategory = c
                                catExpanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Note *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        onSave(
                            Expense(
                                profileId = profileId,
                                category = selectedCategory,
                                amount = amt,
                                date = Formatters.currentDateString(),
                                paymentMethod = paymentMethod,
                                description = description.ifBlank { selectedCategory }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Save Expense")
                }
            }
        }
    }
}

@Composable
fun AddIncomeDialog(
    initialProfileId: String,
    onDismiss: () -> Unit,
    onSave: (Income) -> Unit
) {
    var profileId by remember { mutableStateOf(if (initialProfileId == "all") "eshop" else initialProfileId) }
    val categories = if (profileId == "personal") {
        listOf("Salary", "Freelancing", "Investment", "Gift", "Other Income")
    } else {
        listOf("Product Sales", "Service Income", "Affiliate", "Delivery Fee Collected", "Other Business Income")
    }

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Record Income",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = profileId == "eshop",
                        onClick = { profileId = "eshop" },
                        label = { Text("E-SHOP") }
                    )
                    FilterChip(
                        selected = profileId == "freshoo",
                        onClick = { profileId = "freshoo" },
                        label = { Text("FRESHOO") }
                    )
                    FilterChip(
                        selected = profileId == "personal",
                        onClick = { profileId = "personal" },
                        label = { Text("PERSONAL") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Source *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        onSave(
                            Income(
                                profileId = profileId,
                                category = selectedCategory,
                                amount = amt,
                                date = Formatters.currentDateString(),
                                paymentMethod = "Cash/Bank",
                                description = description.ifBlank { selectedCategory }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Income")
                }
            }
        }
    }
}
