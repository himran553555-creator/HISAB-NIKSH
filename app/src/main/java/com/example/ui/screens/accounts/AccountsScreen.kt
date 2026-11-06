package com.example.ui.screens.accounts

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
import com.example.model.Account
import com.example.model.Transfer
import com.example.ui.components.EmptyView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySecondary
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<Account>,
    transfers: List<Transfer>,
    onAddAccount: (Account) -> Unit,
    onExecuteTransfer: (Transfer) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    val totalBalance = accounts.sumOf { it.balance }

    Scaffold(
        modifier = modifier.testTag("accounts_screen"),
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showTransferDialog = true },
                    containerColor = NavySecondary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.CompareArrows, contentDescription = "Transfer") },
                    text = { Text("Transfer") }
                )
                ExtendedFloatingActionButton(
                    onClick = { showAddAccountDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Account") },
                    text = { Text("New Account") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Liquid Capital Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Liquid Funds (Cash + Bank + Wallets)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = Formatters.formatCurrency(totalBalance),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${accounts.size} active financial channels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = "Accounts & Wallets",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (accounts.isEmpty()) {
                EmptyView(
                    message = "No accounts registered",
                    icon = Icons.Default.AccountBalance,
                    actionLabel = "Add First Account",
                    onAction = { showAddAccountDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts) { account ->
                        AccountCard(account = account)
                    }

                    if (transfers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Recent Internal Transfers (Non-Income)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        items(transfers.take(5)) { transfer ->
                            TransferHistoryCard(transfer = transfer)
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { acc ->
                onAddAccount(acc)
                showAddAccountDialog = false
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { tr ->
                onExecuteTransfer(tr)
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun AccountCard(
    account: Account,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
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
                    text = account.accountName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${account.accountType} • ${account.accountNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account.isPersonal) {
                    Surface(
                        color = Color(0xFFF3E8FF),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Personal",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7E22CE),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = Formatters.formatCurrency(account.balance),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            )
        }
    }
}

@Composable
fun TransferHistoryCard(transfer: Transfer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${transfer.fromAccountName} → ${transfer.toAccountName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${transfer.date} • ${transfer.note.ifBlank { "Internal transfer" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = Formatters.formatCurrency(transfer.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Bank") }
    var accountNumber by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("0") }
    var isPersonal by remember { mutableStateOf(false) }

    val types = listOf("Cash", "Bank", "bKash", "Nagad", "Rocket", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Add Financial Account", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Label (e.g. City Bank)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))

                Text("Channel Type", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.take(4).forEach { t ->
                        FilterChip(selected = accountType == t, onClick = { accountType = t }, label = { Text(t) })
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account/Wallet Number") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = balanceText, onValueChange = { balanceText = it }, label = { Text("Opening Balance (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPersonal, onCheckedChange = { isPersonal = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("This is a Personal Account (not business)")
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val bal = balanceText.toDoubleOrNull() ?: 0.0
                        onSave(Account(accountName = accountName.ifBlank { accountType }, accountType = accountType, accountNumber = accountNumber, balance = bal, isPersonal = isPersonal))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Account")
                }
            }
        }
    }
}

@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Transfer) -> Unit
) {
    if (accounts.size < 2) {
        Dialog(onDismissRequest = onDismiss) {
            Card(modifier = Modifier.padding(20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Need at least 2 accounts to execute transfers.")
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDismiss) { Text("OK") }
                }
            }
        }
        return
    }

    var fromAccount by remember { mutableStateOf(accounts[0]) }
    var toAccount by remember { mutableStateOf(accounts[1]) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Internal Transfer", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))

                Text("From Account", fontWeight = FontWeight.SemiBold)
                var fromExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { fromExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${fromAccount.accountName} (৳${fromAccount.balance})")
                    }
                    DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(text = { Text("${acc.accountName} (৳${acc.balance})") }, onClick = {
                                fromAccount = acc
                                fromExpanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("To Account", fontWeight = FontWeight.SemiBold)
                var toExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { toExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${toAccount.accountName} (৳${toAccount.balance})")
                    }
                    DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(text = { Text("${acc.accountName} (৳${acc.balance})") }, onClick = {
                                toAccount = acc
                                toExpanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Transfer Amount (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Transfer Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && fromAccount.id != toAccount.id) {
                            onConfirm(
                                Transfer(
                                    fromAccountId = fromAccount.id,
                                    fromAccountName = fromAccount.accountName,
                                    toAccountId = toAccount.id,
                                    toAccountName = toAccount.accountName,
                                    amount = amt,
                                    date = Formatters.currentDateString(),
                                    note = note
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Execute Transfer")
                }
            }
        }
    }
}
