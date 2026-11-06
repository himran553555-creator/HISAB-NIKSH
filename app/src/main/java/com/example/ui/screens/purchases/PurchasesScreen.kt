package com.example.ui.screens.purchases

import androidx.compose.foundation.clickable
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
import com.example.model.Product
import com.example.model.Purchase
import com.example.model.Supplier
import com.example.ui.components.EmptyView
import com.example.ui.theme.EmeraldPrimary
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    purchases: List<Purchase>,
    products: List<Product>,
    suppliers: List<Supplier>,
    activeBusinessId: String,
    onSavePurchase: (Purchase) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredPurchases = remember(purchases, searchQuery, activeBusinessId) {
        purchases.filter { p ->
            val matchesBiz = if (activeBusinessId == "all" || activeBusinessId == "personal") true else p.businessId == activeBusinessId
            val matchesQuery = searchQuery.isBlank() ||
                    p.purchaseId.contains(searchQuery, ignoreCase = true) ||
                    p.supplierName.contains(searchQuery, ignoreCase = true)
            matchesBiz && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.testTag("purchases_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Purchase") },
                text = { Text("New Purchase") },
                modifier = Modifier.testTag("create_purchase_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Purchase ID or Supplier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredPurchases.isEmpty()) {
                EmptyView(
                    message = "No purchase orders recorded",
                    icon = Icons.Default.ShoppingBag,
                    actionLabel = "Record First Purchase",
                    onAction = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPurchases) { purchase ->
                        PurchaseCard(purchase = purchase)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePurchaseDialog(
            products = products,
            suppliers = suppliers,
            initialBusinessId = activeBusinessId,
            onDismiss = { showCreateDialog = false },
            onSavePurchase = { newPurchase ->
                onSavePurchase(newPurchase)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun PurchaseCard(
    purchase: Purchase,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${purchase.purchaseId}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = purchase.businessId.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = purchase.supplierName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = purchase.purchaseDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(purchase.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Paid: ${Formatters.formatCurrency(purchase.amountPaid)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary
                    )
                    if (purchase.outstandingPayable > 0) {
                        Text(
                            text = "Due: ${Formatters.formatCurrency(purchase.outstandingPayable)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseDialog(
    products: List<Product>,
    suppliers: List<Supplier>,
    initialBusinessId: String,
    onDismiss: () -> Unit,
    onSavePurchase: (Purchase) -> Unit
) {
    var businessId by remember { mutableStateOf(if (initialBusinessId == "all" || initialBusinessId == "personal") "eshop" else initialBusinessId) }
    var purchaseId by remember { mutableStateOf("PO-" + (System.currentTimeMillis() % 100000)) }
    var supplierName by remember { mutableStateOf("") }
    var selectedSupplierId by remember { mutableStateOf("") }

    val businessProducts = remember(products, businessId) {
        products.filter { it.businessId == businessId }
    }

    var selectedProduct by remember { mutableStateOf(businessProducts.firstOrNull()) }
    var quantityText by remember { mutableStateOf("10") }
    var unitCostText by remember { mutableStateOf(selectedProduct?.purchaseCost?.toString() ?: "100") }
    var additionalCostsText by remember { mutableStateOf("0") }
    var amountPaidText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Bank Transfer") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(selectedProduct) {
        selectedProduct?.let {
            unitCostText = it.purchaseCost.toString()
        }
    }

    val qty = quantityText.toDoubleOrNull() ?: 1.0
    val cost = unitCostText.toDoubleOrNull() ?: 0.0
    val additional = additionalCostsText.toDoubleOrNull() ?: 0.0
    val totalAmount = (qty * cost) + additional
    val paid = amountPaidText.toDoubleOrNull() ?: totalAmount
    val due = (totalAmount - paid).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("create_purchase_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Record Supplier Purchase",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Business Profile Selection
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = businessId == "eshop",
                        onClick = {
                            businessId = "eshop"
                            selectedProduct = products.firstOrNull { it.businessId == "eshop" }
                        },
                        label = { Text("E-SHOP") }
                    )
                    FilterChip(
                        selected = businessId == "freshoo",
                        onClick = {
                            businessId = "freshoo"
                            selectedProduct = products.firstOrNull { it.businessId == "freshoo" }
                        },
                        label = { Text("FRESHOO FOOD") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = purchaseId,
                    onValueChange = { purchaseId = it },
                    label = { Text("Purchase / PO Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Select Product to Restock", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                if (businessProducts.isEmpty()) {
                    Text(
                        text = "No products found in this profile. Add a product in Inventory first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (Current: ${it.currentStock} ${it.unit})" } ?: "Select Product",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            businessProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} - Cost: ৳${p.purchaseCost}") },
                                    onClick = {
                                        selectedProduct = p
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitCostText,
                        onValueChange = { unitCostText = it },
                        label = { Text("Unit Cost (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = additionalCostsText,
                        onValueChange = { additionalCostsText = it },
                        label = { Text("Shipping/Costs (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = { amountPaidText = it },
                        label = { Text("Amount Paid (৳)") },
                        placeholder = { Text(totalAmount.toString()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Purchase Amount:", fontWeight = FontWeight.Bold)
                            Text(Formatters.formatCurrency(totalAmount), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Outstanding Payable (Due):", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.formatCurrency(due), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "ℹ️ Stock will automatically increase by $qty upon saving.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val finalSuppName = supplierName.ifBlank { "General Supplier" }
                        val p = selectedProduct
                        val itemsList = if (p != null) {
                            listOf(
                                mapOf(
                                    "productId" to p.id,
                                    "productName" to p.name,
                                    "quantity" to qty,
                                    "unitCost" to cost,
                                    "totalCost" to (qty * cost)
                                )
                            )
                        } else emptyList()

                        val purchase = Purchase(
                            purchaseId = purchaseId,
                            supplierId = selectedSupplierId,
                            supplierName = finalSuppName,
                            businessId = businessId,
                            items = itemsList,
                            totalAmount = totalAmount,
                            amountPaid = paid,
                            outstandingPayable = due,
                            additionalCosts = additional,
                            purchaseDate = Formatters.currentDateString(),
                            paymentMethod = paymentMethod,
                            notes = notes
                        )
                        onSavePurchase(purchase)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_purchase_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm Purchase & Add Stock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
