package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.BusinessProfileFilterRow
import com.example.ui.components.FinancialStatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun MainDashboardScreen(
    activeProfile: BusinessProfileType,
    onProfileSelected: (BusinessProfileType) -> Unit,
    products: List<Product>,
    sales: List<Sale>,
    purchases: List<Purchase>,
    expenses: List<Expense>,
    incomes: List<Income>,
    parcels: List<Parcel>,
    suppliers: List<Supplier>,
    customers: List<Customer>,
    accounts: List<Account>,
    onNavigateToSales: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToCourier: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDateFilter by remember { mutableStateOf("This Month") }
    val dateFilters = listOf("Today", "This Week", "This Month", "All Time")

    // Filter by profile
    val filteredSales = remember(sales, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> sales
            BusinessProfileType.PERSONAL -> emptyList()
            else -> sales.filter { it.businessId == activeProfile.id }
        }
    }

    val filteredPurchases = remember(purchases, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> purchases
            BusinessProfileType.PERSONAL -> emptyList()
            else -> purchases.filter { it.businessId == activeProfile.id }
        }
    }

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

    val filteredProducts = remember(products, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> products
            BusinessProfileType.PERSONAL -> emptyList()
            else -> products.filter { it.businessId == activeProfile.id }
        }
    }

    val filteredParcels = remember(parcels, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> parcels
            BusinessProfileType.PERSONAL -> emptyList()
            else -> parcels.filter { it.businessId == activeProfile.id }
        }
    }

    // Calculations
    val totalRevenue = if (activeProfile == BusinessProfileType.PERSONAL) {
        filteredIncomes.sumOf { it.amount }
    } else {
        filteredSales.sumOf { it.totalAmount } + filteredIncomes.sumOf { it.amount }
    }

    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val totalPurchases = filteredPurchases.sumOf { it.totalAmount }
    val grossProfit = if (activeProfile == BusinessProfileType.PERSONAL) {
        totalRevenue - totalExpenses
    } else {
        filteredSales.sumOf { it.estimatedProfit }
    }
    val netProfit = grossProfit - totalExpenses

    val outstandingReceivables = customers.sumOf { it.outstandingBalance }
    val outstandingPayables = suppliers.sumOf { it.outstandingPayable }

    val cashInHand = accounts.filter { it.accountType == "Cash" }.sumOf { it.balance }
    val bankBalance = accounts.filter { it.accountType == "Bank" || it.accountType == "bKash" || it.accountType == "Nagad" }.sumOf { it.balance }
    val inventoryValuation = filteredProducts.sumOf { it.currentStock * it.purchaseCost }

    val totalOrders = filteredSales.size.toLong()
    val pendingOrders = filteredSales.count { it.paymentStatus == "Unpaid" || it.paymentStatus == "Partially Paid" }.toLong()
    val deliveredOrders = filteredSales.count { it.paymentStatus == "Paid" }.toLong()

    val pendingCourierParcels = filteredParcels.count { it.deliveryStatus != "Delivered" && it.deliveryStatus != "Returned" }.toLong()
    val pendingCodAmount = filteredParcels.filter { it.deliveryStatus != "Returned" && it.settlementStatus != "Settled" }.sumOf { it.codAmount }
    val lowStockCount = filteredProducts.count { it.currentStock <= it.minStock }.toLong()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_dashboard"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Business Profile Filter
        item {
            BusinessProfileFilterRow(
                selectedProfile = activeProfile,
                onProfileSelected = onProfileSelected
            )
        }

        // Date Filter Chips & AI Banner
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Executive Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dateFilters) { filter ->
                            val isSelected = selectedDateFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDateFilter = filter },
                                label = { Text(filter, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gemini AI Prompt Shortcut Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAi() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini Business Assistant",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Ask questions about profits, low stock & pending COD in natural language",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        label = "New Sale",
                        icon = Icons.Default.PointOfSale,
                        color = EmeraldPrimary,
                        onClick = onNavigateToSales
                    )
                    QuickActionButton(
                        label = "Purchase",
                        icon = Icons.Default.ShoppingBag,
                        color = Color(0xFF0284C7),
                        onClick = onNavigateToPurchases
                    )
                    QuickActionButton(
                        label = "Courier",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFFD97706),
                        onClick = onNavigateToCourier
                    )
                    QuickActionButton(
                        label = "Expense",
                        icon = Icons.Default.ReceiptLong,
                        color = Color(0xFFDC2626),
                        onClick = onNavigateToExpenses
                    )
                }
            }
        }

        // Financial Cards Grid (2 columns)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Financial Position",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialStatCard(
                        title = if (activeProfile == BusinessProfileType.PERSONAL) "Personal Income" else "Total Revenue",
                        value = Formatters.formatCurrency(totalRevenue),
                        icon = Icons.Default.TrendingUp,
                        iconTint = EmeraldPrimary,
                        iconBg = EmeraldContainer,
                        subtitle = "$totalOrders sales completed",
                        modifier = Modifier.weight(1f),
                        testTag = "stat_revenue"
                    )
                    FinancialStatCard(
                        title = if (activeProfile == BusinessProfileType.PERSONAL) "Personal Expenses" else "Business Expenses",
                        value = Formatters.formatCurrency(totalExpenses),
                        icon = Icons.Default.TrendingDown,
                        iconTint = RedDanger,
                        iconBg = RedContainer,
                        subtitle = "${filteredExpenses.size} records",
                        modifier = Modifier.weight(1f),
                        testTag = "stat_expenses"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialStatCard(
                        title = "Gross Profit",
                        value = Formatters.formatCurrency(grossProfit),
                        icon = Icons.Default.ShowChart,
                        iconTint = Color(0xFF059669),
                        iconBg = Color(0xFFD1FAE5),
                        subtitle = "Profit margin ${if (totalRevenue > 0) String.format(java.util.Locale.US, "%.1f%%", (grossProfit / totalRevenue) * 100) else "0%"}",
                        modifier = Modifier.weight(1f)
                    )
                    FinancialStatCard(
                        title = "Net Profit",
                        value = Formatters.formatCurrency(netProfit),
                        icon = Icons.Default.MonetizationOn,
                        iconTint = if (netProfit >= 0) EmeraldPrimary else RedDanger,
                        iconBg = if (netProfit >= 0) EmeraldContainer else RedContainer,
                        subtitle = "After all expenses",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialStatCard(
                        title = "Cash in Hand",
                        value = Formatters.formatCurrency(cashInHand),
                        icon = Icons.Default.Payments,
                        iconTint = Color(0xFF0D9488),
                        iconBg = Color(0xFFCCFBF1),
                        modifier = Modifier.weight(1f)
                    )
                    FinancialStatCard(
                        title = "Bank & Wallets",
                        value = Formatters.formatCurrency(bankBalance),
                        icon = Icons.Default.AccountBalance,
                        iconTint = Color(0xFF2563EB),
                        iconBg = Color(0xFFDBEAFE),
                        subtitle = "bKash / Nagad / Bank",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (activeProfile != BusinessProfileType.PERSONAL) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinancialStatCard(
                            title = "Customer Receivables",
                            value = Formatters.formatCurrency(outstandingReceivables),
                            icon = Icons.Default.CallReceived,
                            iconTint = AmberWarning,
                            iconBg = AmberContainer,
                            subtitle = "Dues to collect",
                            modifier = Modifier.weight(1f)
                        )
                        FinancialStatCard(
                            title = "Supplier Payables",
                            value = Formatters.formatCurrency(outstandingPayables),
                            icon = Icons.Default.CallMade,
                            iconTint = Color(0xFFE11D48),
                            iconBg = Color(0xFFFFE4E6),
                            subtitle = "Dues to pay",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinancialStatCard(
                            title = "Total Purchases",
                            value = Formatters.formatCurrency(totalPurchases),
                            icon = Icons.Default.ShoppingBag,
                            iconTint = NavySecondary,
                            iconBg = NavyContainer,
                            subtitle = "${filteredPurchases.size} supplier orders",
                            modifier = Modifier.weight(1f)
                        )
                        FinancialStatCard(
                            title = "Inventory Valuation",
                            value = Formatters.formatCurrency(inventoryValuation),
                            icon = Icons.Default.Inventory2,
                            iconTint = Color(0xFF7C3AED),
                            iconBg = Color(0xFFEDE9FE),
                            subtitle = "${filteredProducts.size} SKUs in stock",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Operational Status Cards
        if (activeProfile != BusinessProfileType.PERSONAL) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Operations & Courier Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Courier",
                                        tint = AmberWarning
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pending Courier COD",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(pendingCodAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$pendingCourierParcels parcels currently with Steadfast, Pathao, RedX",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Low Stock",
                                        tint = if (lowStockCount > 0) RedDanger else EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Low Stock Alerts",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                StatusBadge(status = if (lowStockCount > 0) "$lowStockCount Low Stock" else "Stock Healthy")
                            }
                        }
                    }
                }
            }
        }

        // Recent Sales Section
        if (filteredSales.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Sales Invoices",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToSales) {
                            Text("View All")
                        }
                    }
                }
            }

            items(filteredSales.take(3)) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        Column {
                            Text(
                                text = "#${sale.invoiceNumber}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${sale.customerName} • ${sale.saleDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatCurrency(sale.totalAmount),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(status = sale.paymentStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
