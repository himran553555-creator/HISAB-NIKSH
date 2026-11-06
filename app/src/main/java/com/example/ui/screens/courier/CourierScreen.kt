package com.example.ui.screens.courier

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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CodSettlement
import com.example.model.Parcel
import com.example.ui.components.EmptyView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedDanger
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierScreen(
    parcels: List<Parcel>,
    codSettlements: List<CodSettlement>,
    activeBusinessId: String,
    onAddParcel: (Parcel) -> Unit,
    onUpdateParcelStatus: (String, String, String) -> Unit,
    onAddSettlement: (CodSettlement) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourierFilter by remember { mutableStateOf("All") }
    var showCreateParcelDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var updatingParcel by remember { mutableStateOf<Parcel?>(null) }

    val filteredParcels = remember(parcels, searchQuery, selectedCourierFilter, activeBusinessId) {
        parcels.filter { p ->
            val matchesBiz = if (activeBusinessId == "all" || activeBusinessId == "personal") true else p.businessId == activeBusinessId
            val matchesCourier = selectedCourierFilter == "All" || p.courierCompany.equals(selectedCourierFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    p.trackingNumber.contains(searchQuery, ignoreCase = true) ||
                    p.customerName.contains(searchQuery, ignoreCase = true) ||
                    p.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    p.parcelId.contains(searchQuery, ignoreCase = true)
            matchesBiz && matchesCourier && matchesQuery
        }
    }

    // Analytics
    val totalParcelsCount = filteredParcels.size
    val deliveredCount = filteredParcels.count { it.deliveryStatus == "Delivered" }
    val returnedCount = filteredParcels.count { it.deliveryStatus == "Returned" }
    val pendingCod = filteredParcels.filter { it.deliveryStatus != "Returned" && it.settlementStatus != "Settled" }.sumOf { it.codAmount }
    val successRate = if (totalParcelsCount > 0) ((deliveredCount.toDouble() / totalParcelsCount) * 100) else 0.0
    val returnRate = if (totalParcelsCount > 0) ((returnedCount.toDouble() / totalParcelsCount) * 100) else 0.0

    Scaffold(
        modifier = modifier.testTag("courier_screen"),
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateParcelDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Create Parcel") },
                    text = { Text("Book Parcel") },
                    modifier = Modifier.testTag("create_parcel_fab")
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showSettlementDialog = true },
                    containerColor = AmberWarning,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Settle COD") },
                    text = { Text("Record Settlement") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Courier KPIs Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pending COD with Couriers",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatCurrency(pendingCod),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$totalParcelsCount Parcels",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Delivery Success", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", successRate)}% ($deliveredCount)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                        Column {
                            Text(text = "Return Rate", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", returnRate)}% ($returnedCount)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (returnRate > 10.0) RedDanger else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Column {
                            Text(text = "In Transit", style = MaterialTheme.typography.labelSmall)
                            val inTransitCount = filteredParcels.count { it.deliveryStatus == "In Transit" || it.deliveryStatus == "Picked Up" }
                            Text(
                                text = "$inTransitCount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Tabs: Parcels vs COD Settlements
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Parcels (${filteredParcels.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("COD Settlements (${codSettlements.size})") }
                )
            }

            if (selectedTab == 0) {
                // Search and Courier Filter
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Tracking #, Customer, Phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val couriers = listOf("All", "Steadfast", "Pathao", "RedX", "Paperfly")
                        couriers.forEach { cr ->
                            val isSelected = selectedCourierFilter == cr
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCourierFilter = cr },
                                label = { Text(cr, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (filteredParcels.isEmpty()) {
                    EmptyView(
                        message = "No parcels found",
                        icon = Icons.Default.LocalShipping,
                        actionLabel = "Create New Parcel",
                        onAction = { showCreateParcelDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredParcels) { parcel ->
                            ParcelCard(
                                parcel = parcel,
                                onClick = { updatingParcel = parcel }
                            )
                        }
                    }
                }
            } else {
                // COD Settlements List
                if (codSettlements.isEmpty()) {
                    EmptyView(
                        message = "No COD settlements recorded yet",
                        icon = Icons.Default.Payments,
                        actionLabel = "Record First Settlement",
                        onAction = { showSettlementDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(codSettlements) { st ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "#${st.settlementId}", fontWeight = FontWeight.Bold)
                                        StatusBadge(status = st.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${st.courierCompany} • ${st.settlementDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total COD Collected:", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatCurrency(st.totalCodCollected), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Courier Charges Deducted:", style = MaterialTheme.typography.bodySmall)
                                        Text("-${Formatters.formatCurrency(st.courierCharges)}", style = MaterialTheme.typography.bodySmall, color = RedDanger)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Received Amount:", fontWeight = FontWeight.Bold)
                                        Text(Formatters.formatCurrency(st.receivedAmount), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateParcelDialog) {
        CreateParcelDialog(
            initialBusinessId = activeBusinessId,
            onDismiss = { showCreateParcelDialog = false },
            onSave = { parcel ->
                onAddParcel(parcel)
                showCreateParcelDialog = false
            }
        )
    }

    updatingParcel?.let { parcel ->
        UpdateParcelStatusDialog(
            parcel = parcel,
            onDismiss = { updatingParcel = null },
            onConfirm = { dStatus, sStatus ->
                onUpdateParcelStatus(parcel.id, dStatus, sStatus)
                updatingParcel = null
            }
        )
    }

    if (showSettlementDialog) {
        RecordSettlementDialog(
            parcels = filteredParcels,
            onDismiss = { showSettlementDialog = false },
            onConfirm = { settlement ->
                onAddSettlement(settlement)
                showSettlementDialog = false
            }
        )
    }
}

@Composable
fun ParcelCard(
    parcel: Parcel,
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
                        text = parcel.courierCompany,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = parcel.trackingNumber.ifBlank { "NO-TRACK" },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                StatusBadge(status = parcel.deliveryStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parcel.customerName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${parcel.customerPhone} • ${parcel.customerAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Item: ${parcel.productDetails}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "COD: ${Formatters.formatCurrency(parcel.codAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = parcel.settlementStatus)
                }
            }
        }
    }
}

@Composable
fun CreateParcelDialog(
    initialBusinessId: String,
    onDismiss: () -> Unit,
    onSave: (Parcel) -> Unit
) {
    var businessId by remember { mutableStateOf(if (initialBusinessId == "all" || initialBusinessId == "personal") "eshop" else initialBusinessId) }
    var courierCompany by remember { mutableStateOf("Steadfast") }
    var trackingNumber by remember { mutableStateOf("ST-" + (System.currentTimeMillis() % 100000)) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var productDetails by remember { mutableStateOf("") }
    var codAmountText by remember { mutableStateOf("") }
    var deliveryChargeText by remember { mutableStateOf("70") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Book Courier Parcel",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Select Courier
                Text("Courier Partner", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Steadfast", "Pathao", "RedX", "Paperfly").forEach { cr ->
                        FilterChip(
                            selected = courierCompany == cr,
                            onClick = {
                                courierCompany = cr
                                val prefix = when (cr) {
                                    "Steadfast" -> "ST-"
                                    "Pathao" -> "PTH-"
                                    "RedX" -> "RDX-"
                                    else -> "PAP-"
                                }
                                trackingNumber = prefix + (System.currentTimeMillis() % 100000)
                            },
                            label = { Text(cr, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { trackingNumber = it },
                    label = { Text("Tracking Number / Consignment ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Recipient Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Recipient Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { customerAddress = it },
                    label = { Text("Delivery Address (City, Area, Road)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = productDetails,
                    onValueChange = { productDetails = it },
                    label = { Text("Product Description & Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = codAmountText,
                        onValueChange = { codAmountText = it },
                        label = { Text("COD Collection (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = deliveryChargeText,
                        onValueChange = { deliveryChargeText = it },
                        label = { Text("Courier Fee (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cod = codAmountText.toDoubleOrNull() ?: 0.0
                        val fee = deliveryChargeText.toDoubleOrNull() ?: 70.0
                        onSave(
                            Parcel(
                                parcelId = "PRC-" + (System.currentTimeMillis() % 100000),
                                orderId = "ORD-" + (System.currentTimeMillis() % 10000),
                                businessId = businessId,
                                customerName = customerName.ifBlank { "Customer" },
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                productDetails = productDetails,
                                codAmount = cod,
                                courierCompany = courierCompany,
                                trackingNumber = trackingNumber,
                                deliveryCharge = fee,
                                packagingCharge = 20.0,
                                parcelDate = Formatters.currentDateString(),
                                deliveryStatus = "Pending",
                                settlementStatus = "Pending"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm Parcel Booking")
                }
            }
        }
    }
}

@Composable
fun UpdateParcelStatusDialog(
    parcel: Parcel,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var deliveryStatus by remember { mutableStateOf(parcel.deliveryStatus) }
    var settlementStatus by remember { mutableStateOf(parcel.settlementStatus) }

    val deliveryOptions = listOf("Pending", "Ready to Ship", "Picked Up", "In Transit", "Delivered", "Returned", "Cancelled", "Delivery Failed")
    val settlementOptions = listOf("Pending", "Partially Settled", "Settled")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Update Parcel Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${parcel.courierCompany} • #${parcel.trackingNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Delivery Status", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                var dExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { dExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(deliveryStatus)
                    }
                    DropdownMenu(expanded = dExpanded, onDismissRequest = { dExpanded = false }) {
                        deliveryOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                deliveryStatus = opt
                                dExpanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Settlement Status", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                var sExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { sExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(settlementStatus)
                    }
                    DropdownMenu(expanded = sExpanded, onDismissRequest = { sExpanded = false }) {
                        settlementOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                settlementStatus = opt
                                sExpanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onConfirm(deliveryStatus, settlementStatus) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Status Update")
                }
            }
        }
    }
}

@Composable
fun RecordSettlementDialog(
    parcels: List<Parcel>,
    onDismiss: () -> Unit,
    onConfirm: (CodSettlement) -> Unit
) {
    var courierCompany by remember { mutableStateOf("Steadfast") }
    var totalCodText by remember { mutableStateOf("") }
    var courierChargesText by remember { mutableStateOf("") }
    var receivedAmountText by remember { mutableStateOf("") }

    val pendingParcelsForCourier = remember(parcels, courierCompany) {
        parcels.filter { it.courierCompany == courierCompany && it.settlementStatus != "Settled" }
    }

    LaunchedEffect(pendingParcelsForCourier) {
        val total = pendingParcelsForCourier.sumOf { it.codAmount }
        val charges = pendingParcelsForCourier.sumOf { it.deliveryCharge }
        totalCodText = total.toString()
        courierChargesText = charges.toString()
        receivedAmountText = (total - charges).coerceAtLeast(0.0).toString()
    }

    val codTotal = totalCodText.toDoubleOrNull() ?: 0.0
    val charges = courierChargesText.toDoubleOrNull() ?: 0.0
    val received = receivedAmountText.toDoubleOrNull() ?: (codTotal - charges)
    val net = codTotal - charges

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
                    text = "Record COD Settlement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Steadfast", "Pathao", "RedX", "Paperfly").forEach { cr ->
                        FilterChip(
                            selected = courierCompany == cr,
                            onClick = { courierCompany = cr },
                            label = { Text(cr) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = totalCodText,
                    onValueChange = { totalCodText = it },
                    label = { Text("Total COD Collected (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = courierChargesText,
                    onValueChange = { courierChargesText = it },
                    label = { Text("Courier Delivery Charges Deducted (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = receivedAmountText,
                    onValueChange = { receivedAmountText = it },
                    label = { Text("Net Amount Credited to Bank/Wallet (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${pendingParcelsForCourier.size} parcels will be marked as Settled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val settlement = CodSettlement(
                            settlementId = "SETTL-" + (System.currentTimeMillis() % 100000),
                            courierCompany = courierCompany,
                            businessId = "eshop",
                            totalCodCollected = codTotal,
                            courierCharges = charges,
                            additionalFees = 0.0,
                            netSettlement = net,
                            receivedAmount = received,
                            outstandingAmount = (net - received).coerceAtLeast(0.0),
                            settlementDate = Formatters.currentDateString(),
                            parcelIds = pendingParcelsForCourier.map { it.id },
                            status = "Settled"
                        )
                        onConfirm(settlement)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm Settlement Reconciliation")
                }
            }
        }
    }
}
