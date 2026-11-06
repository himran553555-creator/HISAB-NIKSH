package com.example.ui.screens.sales

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
import com.example.model.Customer
import com.example.model.Product
import com.example.model.Sale
import com.example.ui.theme.EmeraldPrimary
import com.example.util.Formatters
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleDialog(
    products: List<Product>,
    customers: List<Customer>,
    initialBusinessId: String,
    onDismiss: () -> Unit,
    onSaveSale: (Sale) -> Unit
) {
    var businessId by remember { mutableStateOf(if (initialBusinessId == "all" || initialBusinessId == "personal") "eshop" else initialBusinessId) }
    var invoiceNumber by remember { mutableStateOf("INV-" + (System.currentTimeMillis() % 100000)) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf("") }

    val businessProducts = remember(products, businessId) {
        products.filter { it.businessId == businessId }
    }

    var selectedProduct by remember { mutableStateOf(businessProducts.firstOrNull()) }
    var quantityText by remember { mutableStateOf("1") }
    var unitPriceText by remember { mutableStateOf(selectedProduct?.sellingPrice?.toString() ?: "0") }
    var discountText by remember { mutableStateOf("0") }
    var deliveryChargeText by remember { mutableStateOf("60") }
    var packagingCostText by remember { mutableStateOf("20") }
    var amountReceivedText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash on Delivery") }
    var paymentStatus by remember { mutableStateOf("Paid") }
    var notes by remember { mutableStateOf("") }

    // When selected product changes, update price
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let {
            unitPriceText = it.sellingPrice.toString()
        }
    }

    // Calculations
    val qty = quantityText.toDoubleOrNull() ?: 1.0
    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val deliveryCharge = deliveryChargeText.toDoubleOrNull() ?: 0.0
    val packagingCost = packagingCostText.toDoubleOrNull() ?: 0.0

    val subtotal = qty * unitPrice
    val totalAmount = (subtotal - discount + deliveryCharge).coerceAtLeast(0.0)
    val amountReceived = amountReceivedText.toDoubleOrNull() ?: totalAmount
    val outstanding = (totalAmount - amountReceived).coerceAtLeast(0.0)

    val purchaseCost = (selectedProduct?.purchaseCost ?: 0.0) * qty
    val estimatedProfit = (subtotal - discount - purchaseCost - packagingCost).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("create_sale_dialog"),
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
                        text = "Create New Sale",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Business Profile Selection
                Text(
                    text = "Select Business Profile",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Customer Info
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_customer_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Customer Phone (01xxxxxxxxx)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select Product & Pricing",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Product Selector
                if (businessProducts.isEmpty()) {
                    Text(
                        text = "No products found for this profile. Please add products in Inventory first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (Stock: ${it.currentStock} ${it.unit})" } ?: "Select a Product",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            businessProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} - ৳${p.sellingPrice} (Stock: ${p.currentStock})") },
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
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("Unit Price (৳)") },
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
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = deliveryChargeText,
                        onValueChange = { deliveryChargeText = it },
                        label = { Text("Delivery (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var statusExpanded by remember { mutableStateOf(false) }
                    val statusOptions = listOf("Paid", "Partially Paid", "Unpaid")
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = paymentStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Status") },
                            trailingIcon = {
                                IconButton(onClick = { statusExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            statusOptions.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st) },
                                    onClick = {
                                        paymentStatus = st
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountReceivedText,
                        onValueChange = { amountReceivedText = it },
                        label = { Text("Received (৳)") },
                        placeholder = { Text(totalAmount.toString()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calculation Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.formatCurrency(subtotal), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Charge:", style = MaterialTheme.typography.bodySmall)
                            Text("+${Formatters.formatCurrency(deliveryCharge)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount:", style = MaterialTheme.typography.bodySmall)
                            Text("-${Formatters.formatCurrency(discount)}", style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Invoice:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(Formatters.formatCurrency(totalAmount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Profit:", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                            Text(Formatters.formatCurrency(estimatedProfit), style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        val finalCustomerName = customerName.ifBlank { "Guest Customer" }
                        val p = selectedProduct
                        val itemsList = if (p != null) {
                            listOf(
                                mapOf(
                                    "productId" to p.id,
                                    "productName" to p.name,
                                    "quantity" to qty,
                                    "unitPrice" to unitPrice,
                                    "purchaseCost" to p.purchaseCost,
                                    "totalPrice" to (qty * unitPrice)
                                )
                            )
                        } else emptyList()

                        val sale = Sale(
                            invoiceNumber = invoiceNumber,
                            businessId = businessId,
                            customerId = selectedCustomerId,
                            customerName = finalCustomerName,
                            customerPhone = customerPhone,
                            items = itemsList,
                            subtotal = subtotal,
                            discount = discount,
                            tax = 0.0,
                            deliveryCharge = deliveryCharge,
                            packagingCost = packagingCost,
                            totalAmount = totalAmount,
                            amountReceived = amountReceived,
                            outstandingAmount = outstanding,
                            estimatedProfit = estimatedProfit,
                            paymentMethod = paymentMethod,
                            paymentStatus = paymentStatus,
                            saleDate = Formatters.currentDateString(),
                            notes = notes
                        )
                        onSaveSale(sale)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_sale_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm & Generate Invoice", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
