package com.example.data

import android.util.Log
import com.example.model.*
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"

class AiRepository {
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun askBusinessAssistant(
        userQuestion: String,
        activeProfile: BusinessProfileType,
        products: List<Product>,
        sales: List<Sale>,
        purchases: List<Purchase>,
        expenses: List<Expense>,
        incomes: List<Income>,
        parcels: List<Parcel>,
        suppliers: List<Supplier>,
        customers: List<Customer>,
        accounts: List<Account>
    ): Result<String> {
        return try {
            val totalSales = sales.sumOf { it.totalAmount }
            val totalSalesCount = sales.size
            val eshopSales = sales.filter { it.businessId == "eshop" }.sumOf { it.totalAmount }
            val freshooSales = sales.filter { it.businessId == "freshoo" }.sumOf { it.totalAmount }

            val totalExpense = expenses.sumOf { it.amount }
            val bizExpense = expenses.filter { it.profileId != "personal" }.sumOf { it.amount }
            val personalExpense = expenses.filter { it.profileId == "personal" }.sumOf { it.amount }

            val totalIncome = incomes.sumOf { it.amount }
            val personalIncome = incomes.filter { it.profileId == "personal" }.sumOf { it.amount }

            val totalPurchaseAmount = purchases.sumOf { it.totalAmount }
            val supplierPayables = suppliers.sumOf { it.outstandingPayable }
            val customerReceivables = customers.sumOf { it.outstandingBalance }

            val lowStockProducts = products.filter { it.currentStock <= it.minStock }
            val inventoryValuation = products.sumOf { it.currentStock * it.purchaseCost }

            val pendingCod = parcels.filter { it.deliveryStatus != "Returned" && it.settlementStatus != "Settled" }.sumOf { it.codAmount }
            val deliveredParcels = parcels.count { it.deliveryStatus == "Delivered" }
            val returnedParcels = parcels.count { it.deliveryStatus == "Returned" }

            val totalAccountBalance = accounts.sumOf { it.balance }

            val contextPrompt = """
                You are the AI Business Financial Analyst for 'IMRAN BUSINESS MANAGER' (হিসাব নিকাশ).
                Currency is Bangladeshi Taka (BDT / ৳).
                Here is the real-time, verified database state:
                - Active Profile: ${activeProfile.displayName}
                - Total Sales: ${Formatters.formatCurrency(totalSales)} (Orders: $totalSalesCount)
                  * E-SHOP Sales: ${Formatters.formatCurrency(eshopSales)}
                  * FRESHOO FOOD Sales: ${Formatters.formatCurrency(freshooSales)}
                - Total Business Expenses: ${Formatters.formatCurrency(bizExpense)}
                - Total Personal Expenses: ${Formatters.formatCurrency(personalExpense)} (NEVER mixed with business)
                - Total Purchases: ${Formatters.formatCurrency(totalPurchaseAmount)}
                - Gross Estimated Profit: ${Formatters.formatCurrency(sales.sumOf { it.estimatedProfit })}
                - Inventory Valuation: ${Formatters.formatCurrency(inventoryValuation)} across ${products.size} products
                - Low Stock Products (${lowStockProducts.size}): ${lowStockProducts.take(5).joinToString(", ") { "${it.name} (Stock: ${it.currentStock} ${it.unit})" }}
                - Pending Courier COD: ${Formatters.formatCurrency(pendingCod)} (Delivered: $deliveredParcels, Returned: $returnedParcels)
                - Outstanding Supplier Payables: ${Formatters.formatCurrency(supplierPayables)}
                - Outstanding Customer Receivables: ${Formatters.formatCurrency(customerReceivables)}
                - Total Liquid Account Balances: ${Formatters.formatCurrency(totalAccountBalance)}

                Answer the user's question accurately, concisely, and professionally using ONLY the database information above.
                Never invent data. Use Bengali numbers/currency or English as appropriate. Keep formatting clear with bullet points.

                User Question: $userQuestion
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(contextPrompt)
            }

            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Assistant returned an empty response. Please rephrase."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            Result.failure(Exception("Request timed out after 60s. Please check network connection."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed", e)
            Result.failure(e)
        }
    }
}
