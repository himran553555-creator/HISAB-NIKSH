package com.example.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.RedDanger
import com.example.util.Formatters

@Composable
fun ReportsScreen(
    sales: List<Sale>,
    purchases: List<Purchase>,
    expenses: List<Expense>,
    incomes: List<Income>,
    parcels: List<Parcel>,
    accounts: List<Account>,
    activeProfile: BusinessProfileType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedReportType by remember { mutableStateOf("Profit & Loss") }

    val filteredSales = remember(sales, activeProfile) {
        if (activeProfile == BusinessProfileType.ALL || activeProfile == BusinessProfileType.PERSONAL) sales else sales.filter { it.businessId == activeProfile.id }
    }
    val filteredExpenses = remember(expenses, activeProfile) {
        when (activeProfile) {
            BusinessProfileType.ALL -> expenses.filter { it.profileId != "personal" }
            BusinessProfileType.PERSONAL -> expenses.filter { it.profileId == "personal" }
            else -> expenses.filter { it.profileId == activeProfile.id }
        }
    }

    val totalRevenue = filteredSales.sumOf { it.totalAmount }
    val totalEstimatedGrossProfit = filteredSales.sumOf { it.estimatedProfit }
    val totalBizExpenses = filteredExpenses.sumOf { it.amount }
    val netProfit = totalEstimatedGrossProfit - totalBizExpenses

    fun shareReport() {
        val reportContent = buildString {
            appendLine("==========================================")
            appendLine("       IMRAN BUSINESS MANAGER REPORT      ")
            appendLine("           (হিসাব নিকাশ)                  ")
            appendLine("==========================================")
            appendLine("Profile: ${activeProfile.displayName}")
            appendLine("Date: ${Formatters.currentDateString()}")
            appendLine("Currency: BDT (৳)")
            appendLine("------------------------------------------")
            appendLine("FINANCIAL SUMMARY:")
            appendLine("Total Revenue: ${Formatters.formatCurrency(totalRevenue)}")
            appendLine("Gross Profit:  ${Formatters.formatCurrency(totalEstimatedGrossProfit)}")
            appendLine("Total Expenses:${Formatters.formatCurrency(totalBizExpenses)}")
            appendLine("Net Profit:    ${Formatters.formatCurrency(netProfit)}")
            appendLine("------------------------------------------")
            appendLine("OPERATIONS:")
            appendLine("Total Sales Orders: ${filteredSales.size}")
            appendLine("Total Purchases:    ${purchases.size}")
            appendLine("Courier Parcels:    ${parcels.size}")
            appendLine("Pending COD:        ${Formatters.formatCurrency(parcels.filter { it.settlementStatus != "Settled" }.sumOf { it.codAmount })}")
            appendLine("==========================================")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportContent)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Business Report"))
    }

    Scaffold(
        modifier = modifier.testTag("reports_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { shareReport() },
                containerColor = NavySecondary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                text = { Text("Export & Share Report") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Financial Statements & Analytics",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Generated for ${activeProfile.displayName} as of ${Formatters.currentDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Profit & Loss Statement Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "Profit & Loss (P&L) Statement",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = EmeraldPrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        StatementRow(label = "(+) Sales Revenue", amount = totalRevenue, isPositive = true)
                        StatementRow(label = "(+) Gross Trading Profit", amount = totalEstimatedGrossProfit, isPositive = true)
                        StatementRow(label = "(-) Operating & Delivery Expenses", amount = totalBizExpenses, isPositive = false)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Net Profit / (Loss)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = Formatters.formatCurrency(netProfit),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (netProfit >= 0) EmeraldPrimary else RedDanger
                                )
                            )
                        }
                    }
                }
            }

            // Sales Trend by Category
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sales Performance by Business",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val eshopSales = sales.filter { it.businessId == "eshop" }.sumOf { it.totalAmount }
                        val freshooSales = sales.filter { it.businessId == "freshoo" }.sumOf { it.totalAmount }

                        StatementRow(label = "E-SHOP Fashion & Lifestyle", amount = eshopSales)
                        StatementRow(label = "FRESHOO FOOD Groceries", amount = freshooSales)
                        StatementRow(label = "Personal Account Savings", amount = incomes.filter { it.profileId == "personal" }.sumOf { it.amount })
                    }
                }
            }

            // Courier Settlement Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Courier & COD Performance",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalCod = parcels.sumOf { it.codAmount }
                        val settledCod = parcels.filter { it.settlementStatus == "Settled" }.sumOf { it.codAmount }
                        val pendingCod = parcels.filter { it.settlementStatus != "Settled" && it.deliveryStatus != "Returned" }.sumOf { it.codAmount }

                        StatementRow(label = "Total Booked COD", amount = totalCod)
                        StatementRow(label = "Settled & Realized COD", amount = settledCod, isPositive = true)
                        StatementRow(label = "Pending Receivable from Couriers", amount = pendingCod, isHighlighted = true)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun StatementRow(
    label: String,
    amount: Double,
    isPositive: Boolean? = null,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = Formatters.formatCurrency(amount),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = when (isPositive) {
                    true -> EmeraldPrimary
                    false -> RedDanger
                    null -> MaterialTheme.colorScheme.onSurface
                }
            )
        )
    }
}
