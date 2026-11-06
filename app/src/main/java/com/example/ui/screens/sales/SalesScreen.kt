package com.example.ui.screens.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.Customer
import com.example.model.Product
import com.example.model.Sale
import com.example.ui.components.EmptyView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    sales: List<Sale>,
    products: List<Product>,
    customers: List<Customer>,
    activeBusinessId: String,
    onSaveSale: (Sale) -> Unit,
    onUpdateSaleStatus: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var viewingSale by remember { mutableStateOf<Sale?>(null) }

    val filteredSales = remember(sales, searchQuery, selectedStatusFilter, activeBusinessId) {
        sales.filter { sale ->
            val matchesBusiness = if (activeBusinessId == "all" || activeBusinessId == "personal") true else sale.businessId == activeBusinessId
            val matchesQuery = searchQuery.isBlank() ||
                    sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    sale.customerName.contains(searchQuery, ignoreCase = true) ||
                    sale.customerPhone.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == "All" || sale.paymentStatus.equals(selectedStatusFilter, ignoreCase = true)
            matchesBusiness && matchesQuery && matchesStatus
        }
    }

    Scaffold(
        modifier = modifier.testTag("sales_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Sale") },
                text = { Text("New Sale") },
                modifier = Modifier.testTag("create_sale_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Filters
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Invoice #, Customer or Phone...") },
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statuses = listOf("All", "Paid", "Partially Paid", "Unpaid")
                    statuses.forEach { st ->
                        val isSelected = selectedStatusFilter == st
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatusFilter = st },
                            label = { Text(st) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Sales Invoices List
            if (filteredSales.isEmpty()) {
                EmptyView(
                    message = "No sales records found",
                    icon = Icons.Default.Receipt,
                    actionLabel = "Create First Sale",
                    onAction = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales) { sale ->
                        SaleInvoiceCard(
                            sale = sale,
                            onClick = { viewingSale = sale }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSaleDialog(
            products = products,
            customers = customers,
            initialBusinessId = activeBusinessId,
            onDismiss = { showCreateDialog = false },
            onSaveSale = { newSale ->
                onSaveSale(newSale)
                showCreateDialog = false
            }
        )
    }

    viewingSale?.let { sale ->
        ViewInvoiceDialog(
            sale = sale,
            onDismiss = { viewingSale = null },
            onMarkPaid = {
                onUpdateSaleStatus(sale.id, "Paid")
                viewingSale = null
            }
        )
    }
}

@Composable
fun SaleInvoiceCard(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${sale.invoiceNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = sale.businessId.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                StatusBadge(status = sale.paymentStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = sale.customerName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (sale.customerPhone.isNotBlank()) {
                        Text(
                            text = sale.customerPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = sale.saleDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(sale.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                    if (sale.outstandingAmount > 0) {
                        Text(
                            text = "Due: ${Formatters.formatCurrency(sale.outstandingAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Est. Profit: ${Formatters.formatCurrency(sale.estimatedProfit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF047857)
                    )
                }
            }
        }
    }
}

@Composable
fun ViewInvoiceDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sales Invoice",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "IMRAN BUSINESS MANAGER",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                        Text(text = "Invoice #${sale.invoiceNumber}", fontWeight = FontWeight.Bold)
                        Text(text = "Date: ${sale.saleDate}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Business Profile: ${sale.businessId.uppercase()}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Bill To: ${sale.customerName}", fontWeight = FontWeight.SemiBold)
                        if (sale.customerPhone.isNotBlank()) {
                            Text(text = "Phone: ${sale.customerPhone}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(sale.items) { item ->
                        val name = item["productName"] as? String ?: "Product"
                        val qty = (item["quantity"] as? Number)?.toDouble() ?: 1.0
                        val price = (item["unitPrice"] as? Number)?.toDouble() ?: 0.0
                        val total = (item["totalPrice"] as? Number)?.toDouble() ?: (qty * price)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(
                                    text = "$qty x ${Formatters.formatCurrency(price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = Formatters.formatCurrency(total),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                        Text(Formatters.formatCurrency(sale.subtotal), style = MaterialTheme.typography.bodySmall)
                    }
                    if (sale.deliveryCharge > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Charge", style = MaterialTheme.typography.bodySmall)
                            Text("+${Formatters.formatCurrency(sale.deliveryCharge)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (sale.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount", style = MaterialTheme.typography.bodySmall)
                            Text("-${Formatters.formatCurrency(sale.discount)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatCurrency(sale.totalAmount), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Received", style = MaterialTheme.typography.bodySmall)
                        Text(Formatters.formatCurrency(sale.amountReceived), style = MaterialTheme.typography.bodySmall)
                    }
                    if (sale.outstandingAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Outstanding Due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text(Formatters.formatCurrency(sale.outstandingAmount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sale.paymentStatus != "Paid") {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Mark Invoice as Paid")
                    }
                }
            }
        }
    }
}
