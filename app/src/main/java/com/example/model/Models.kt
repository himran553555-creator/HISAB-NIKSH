package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BusinessProfileType(val id: String, val displayName: String, val tagline: String) {
    ESHOP("eshop", "E-SHOP", "Fashion & Lifestyle eCommerce"),
    FRESHOO("freshoo", "FRESHOO FOOD", "Fresh Groceries & Foods"),
    PERSONAL("personal", "PERSONAL ACCOUNT", "Personal Finance & Savings"),
    ALL("all", "CONSOLIDATED (ALL)", "All Businesses Combined");

    companion object {
        fun fromId(id: String): BusinessProfileType =
            entries.find { it.id == id } ?: ESHOP
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val businessName: String = "",
    val phone: String = "",
    val email: String = "",
    val currency: String = "BDT",
    val activeContext: String = "all", // "eshop", "freshoo", "personal", "all"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = "eshop", // "eshop", "freshoo", "personal"
    val tagline: String = "",
    val phone: String = "",
    val address: String = "",
    val currency: String = "BDT",
    val isActive: Boolean = true,
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val businessId: String = "eshop", // "eshop" or "freshoo"
    val sku: String = "",
    val barcode: String = "",
    val category: String = "General",
    val unit: String = "Pcs",
    val purchaseCost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minStock: Double = 5.0,
    val supplierId: String = "",
    val supplierName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StockMovement(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val businessId: String = "eshop",
    val type: String = "SALE", // SALE, PURCHASE, ADJUSTMENT, TRANSFER, RETURN
    val quantityDelta: Double = 0.0,
    val previousStock: Double = 0.0,
    val newStock: Double = 0.0,
    val reason: String = "",
    val referenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "Dhaka",
    val district: String = "Dhaka",
    val totalOrders: Long = 0L,
    val totalPurchases: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Supplier(
    @DocumentId val id: String = "",
    val supplierName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val businessId: String = "eshop",
    val openingBalance: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalPayments: Double = 0.0,
    val outstandingPayable: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SaleItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val purchaseCost: Double = 0.0,
    val totalPrice: Double = 0.0
)

@IgnoreExtraProperties
data class Sale(
    @DocumentId val id: String = "",
    val invoiceNumber: String = "",
    val businessId: String = "eshop",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val packagingCost: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountReceived: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val paymentMethod: String = "Cash on Delivery",
    val paymentStatus: String = "Paid", // Paid, Partially Paid, Unpaid, Refunded
    val saleDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Purchase(
    @DocumentId val id: String = "",
    val purchaseId: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val businessId: String = "eshop",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val outstandingPayable: Double = 0.0,
    val additionalCosts: Double = 0.0,
    val purchaseDate: String = "",
    val paymentMethod: String = "Bank Transfer",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Parcel(
    @DocumentId val id: String = "",
    val parcelId: String = "",
    val orderId: String = "",
    val businessId: String = "eshop",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val productDetails: String = "",
    val codAmount: Double = 0.0,
    val courierCompany: String = "Steadfast", // Steadfast, Pathao, RedX, Paperfly, Sundarban, Other
    val trackingNumber: String = "",
    val deliveryCharge: Double = 0.0,
    val packagingCharge: Double = 0.0,
    val parcelDate: String = "",
    val deliveryStatus: String = "Pending", // Pending, Ready to Ship, Picked Up, In Transit, Delivered, Returned, Cancelled, Delivery Failed
    val settlementStatus: String = "Pending", // Pending, Partially Settled, Settled
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CodSettlement(
    @DocumentId val id: String = "",
    val settlementId: String = "",
    val courierCompany: String = "Steadfast",
    val businessId: String = "eshop",
    val totalCodCollected: Double = 0.0,
    val courierCharges: Double = 0.0,
    val additionalFees: Double = 0.0,
    val netSettlement: Double = 0.0,
    val receivedAmount: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val settlementDate: String = "",
    val parcelIds: List<String> = emptyList(),
    val status: String = "Settled", // Settled, Partially Settled
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val profileId: String = "eshop", // "eshop", "freshoo", "personal"
    val category: String = "General",
    val amount: Double = 0.0,
    val date: String = "",
    val paymentMethod: String = "Cash",
    val description: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Income(
    @DocumentId val id: String = "",
    val profileId: String = "eshop", // "eshop", "freshoo", "personal"
    val category: String = "Sales",
    val amount: Double = 0.0,
    val date: String = "",
    val paymentMethod: String = "Cash",
    val description: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Account(
    @DocumentId val id: String = "",
    val accountName: String = "",
    val accountType: String = "Cash", // Cash, Bank, bKash, Nagad, Rocket, Other
    val accountNumber: String = "",
    val balance: Double = 0.0,
    val businessId: String = "eshop",
    val isPersonal: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Transfer(
    @DocumentId val id: String = "",
    val fromAccountId: String = "",
    val fromAccountName: String = "",
    val toAccountId: String = "",
    val toAccountName: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // LOW_STOCK, PENDING_COURIER, COD_PENDING, PAYMENT_DUE, INFO
    val isRead: Boolean = false,
    val businessId: String = "all",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class FinancialDashboardMetrics(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val grossProfit: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val outstandingReceivables: Double = 0.0,
    val outstandingPayables: Double = 0.0,
    val cashInHand: Double = 0.0,
    val bankBalance: Double = 0.0,
    val inventoryValuation: Double = 0.0,
    val totalOrders: Long = 0L,
    val pendingOrders: Long = 0L,
    val deliveredOrders: Long = 0L,
    val returnedOrders: Long = 0L,
    val pendingCourierParcels: Long = 0L,
    val pendingCodAmount: Double = 0.0,
    val lowStockCount: Long = 0L
)
