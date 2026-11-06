package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreRepo"

class FirestoreRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // User Profile
    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        return userDoc(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val data = mapOf(
            "displayName" to profile.displayName,
            "businessName" to profile.businessName,
            "phone" to profile.phone,
            "email" to profile.email,
            "currency" to profile.currency,
            "activeContext" to profile.activeContext,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userDoc(uid).set(data).await()
    }

    suspend fun switchActiveContext(contextId: String) {
        val uid = requireUserId()
        val validContext = if (contextId in listOf("eshop", "freshoo", "personal", "all")) contextId else "all"
        try {
            userDoc(uid).update(
                mapOf(
                    "activeContext" to validContext,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update activeContext in userDoc, fallback set", e)
            val existing = userDoc(uid).get().await()
            if (existing.exists()) {
                userDoc(uid).set(mapOf("activeContext" to validContext), com.google.firebase.firestore.SetOptions.merge()).await()
            }
        }
    }

    // Businesses & Profiles Collections
    fun observeBusinesses(): Flow<List<BusinessProfile>> {
        val uid = requireUserId()
        return userDoc(uid).collection("businesses").snapshots().map { snapshot ->
            snapshot.toObjects(BusinessProfile::class.java)
        }
    }

    fun observeProfiles(): Flow<List<BusinessProfile>> {
        val uid = requireUserId()
        return userDoc(uid).collection("profiles").snapshots().map { snapshot ->
            snapshot.toObjects(BusinessProfile::class.java)
        }
    }

    suspend fun getBusiness(id: String): BusinessProfile? {
        val uid = requireUserId()
        val snap = userDoc(uid).collection("businesses").document(id).get().await()
        return if (snap.exists()) snap.toObject(BusinessProfile::class.java) else null
    }

    suspend fun saveBusiness(business: BusinessProfile) {
        val uid = requireUserId()
        val id = business.id.ifBlank { business.type }
        val payload = mapOf(
            "name" to business.name,
            "type" to business.type,
            "tagline" to business.tagline,
            "phone" to business.phone,
            "address" to business.address,
            "currency" to business.currency,
            "isActive" to business.isActive,
            "userId" to uid,
            "createdAt" to (business.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userDoc(uid).collection("businesses").document(id).set(payload).await()
        userDoc(uid).collection("profiles").document(id).set(payload).await()
    }

    suspend fun updateBusiness(business: BusinessProfile) {
        saveBusiness(business)
    }

    suspend fun deleteBusiness(id: String) {
        val uid = requireUserId()
        userDoc(uid).collection("businesses").document(id).delete().await()
        userDoc(uid).collection("profiles").document(id).delete().await()
    }

    // Initialize Default Profiles & Accounts if new user
    suspend fun initializeDefaultDataIfEmpty() {
        val uid = requireUserId()
        val userSnapshot = userDoc(uid).get().await()
        if (!userSnapshot.exists()) {
            val email = auth.currentUser?.email ?: "user@imranbiz.com"
            val name = auth.currentUser?.displayName ?: "Business Owner"
            saveUserProfile(
                UserProfile(
                    displayName = name,
                    email = email,
                    currency = "BDT",
                    activeContext = "all"
                )
            )
        }

        // Check if businesses & profiles are initialized
        val bizRef = userDoc(uid).collection("businesses")
        val profRef = userDoc(uid).collection("profiles")
        val eshopDoc = bizRef.document("eshop").get().await()
        if (!eshopDoc.exists()) {
            // E-SHOP
            val eshopData = mapOf(
                "name" to "E-SHOP",
                "type" to "eshop",
                "tagline" to "Fashion, Electronics & Lifestyle eCommerce",
                "phone" to "01700-112233",
                "address" to "Dhaka, Bangladesh",
                "currency" to "BDT",
                "isActive" to true,
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            bizRef.document("eshop").set(eshopData).await()
            profRef.document("eshop").set(eshopData).await()

            // FRESHOO FOOD
            val freshooData = mapOf(
                "name" to "FRESHOO FOOD",
                "type" to "freshoo",
                "tagline" to "Fresh Groceries, Fruits & Foods",
                "phone" to "01700-445566",
                "address" to "Dhaka, Bangladesh",
                "currency" to "BDT",
                "isActive" to true,
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            bizRef.document("freshoo").set(freshooData).await()
            profRef.document("freshoo").set(freshooData).await()

            // PERSONAL ACCOUNT
            val personalData = mapOf(
                "name" to "PERSONAL ACCOUNT",
                "type" to "personal",
                "tagline" to "Personal Finance, Savings & Living Expenses",
                "phone" to "",
                "address" to "Personal",
                "currency" to "BDT",
                "isActive" to true,
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            bizRef.document("personal").set(personalData).await()
            profRef.document("personal").set(personalData).await()
        }

        // Initialize default accounts if absent
        val accountsRef = userDoc(uid).collection("accounts")
        val existingAccounts = accountsRef.limit(1).get().await()
        if (existingAccounts.isEmpty) {
            accountsRef.add(
                mapOf(
                    "accountName" to "Cash in Hand",
                    "accountType" to "Cash",
                    "accountNumber" to "Main Cash Drawer",
                    "balance" to 25000.0,
                    "businessId" to "all",
                    "isPersonal" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            accountsRef.add(
                mapOf(
                    "accountName" to "bKash Merchant",
                    "accountType" to "bKash",
                    "accountNumber" to "01700-112233",
                    "balance" to 45000.0,
                    "businessId" to "eshop",
                    "isPersonal" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            accountsRef.add(
                mapOf(
                    "accountName" to "City Bank Corporate",
                    "accountType" to "Bank",
                    "accountNumber" to "110293847501",
                    "balance" to 150000.0,
                    "businessId" to "all",
                    "isPersonal" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            accountsRef.add(
                mapOf(
                    "accountName" to "Personal Nagad Wallet",
                    "accountType" to "Nagad",
                    "accountNumber" to "01800-998877",
                    "balance" to 12500.0,
                    "businessId" to "personal",
                    "isPersonal" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Seed initial sample inventory products for E-SHOP & FRESHOO
            val prodRef = userDoc(uid).collection("products")
            prodRef.add(
                mapOf(
                    "name" to "Wireless Bluetooth Earbuds",
                    "businessId" to "eshop",
                    "sku" to "ESHOP-EB-01",
                    "barcode" to "890123456001",
                    "category" to "Electronics",
                    "unit" to "Pcs",
                    "purchaseCost" to 850.0,
                    "sellingPrice" to 1450.0,
                    "wholesalePrice" to 1100.0,
                    "currentStock" to 42.0,
                    "minStock" to 10.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            prodRef.add(
                mapOf(
                    "name" to "Smart Fitness Tracker Band",
                    "businessId" to "eshop",
                    "sku" to "ESHOP-FIT-02",
                    "barcode" to "890123456002",
                    "category" to "Gadgets",
                    "unit" to "Pcs",
                    "purchaseCost" to 1200.0,
                    "sellingPrice" to 1950.0,
                    "wholesalePrice" to 1600.0,
                    "currentStock" to 8.0,
                    "minStock" to 10.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            prodRef.add(
                mapOf(
                    "name" to "Premium Organic Mustard Oil (1L)",
                    "businessId" to "freshoo",
                    "sku" to "FRSH-OIL-01",
                    "barcode" to "890123456003",
                    "category" to "Grocery",
                    "unit" to "Bottle",
                    "purchaseCost" to 280.0,
                    "sellingPrice" to 360.0,
                    "wholesalePrice" to 320.0,
                    "currentStock" to 65.0,
                    "minStock" to 15.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            prodRef.add(
                mapOf(
                    "name" to "Sundarban Pure Raw Honey (500g)",
                    "businessId" to "freshoo",
                    "sku" to "FRSH-HNY-02",
                    "barcode" to "890123456004",
                    "category" to "Organic Food",
                    "unit" to "Jar",
                    "purchaseCost" to 550.0,
                    "sellingPrice" to 750.0,
                    "wholesalePrice" to 650.0,
                    "currentStock" to 4.0,
                    "minStock" to 8.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Seed customer
            val custRef = userDoc(uid).collection("customers")
            custRef.add(
                mapOf(
                    "name" to "Tanvir Ahmed",
                    "phone" to "01711223344",
                    "email" to "tanvir@gmail.com",
                    "address" to "House 12, Road 4, Dhanmondi",
                    "city" to "Dhaka",
                    "district" to "Dhaka",
                    "totalOrders" to 2L,
                    "totalPurchases" to 4500.0,
                    "outstandingBalance" to 0.0,
                    "notes" to "Regular loyal customer",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Seed supplier
            val suppRef = userDoc(uid).collection("suppliers")
            suppRef.add(
                mapOf(
                    "supplierName" to "Dhaka Wholesale Mart",
                    "contactPerson" to "Rafiqul Islam",
                    "phone" to "01911887766",
                    "email" to "rafiq@wholesalemart.com",
                    "address" to "Chawkbazar, Old Dhaka",
                    "businessId" to "eshop",
                    "openingBalance" to 0.0,
                    "totalPurchases" to 35000.0,
                    "totalPayments" to 25000.0,
                    "outstandingPayable" to 10000.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Seed notification for low stock
            val notifRef = userDoc(uid).collection("notifications")
            notifRef.add(
                mapOf(
                    "title" to "Low Stock Alert: Raw Honey",
                    "message" to "Sundarban Pure Raw Honey (500g) stock is at 4 Jars (Min: 8 Jars). Please restock.",
                    "type" to "LOW_STOCK",
                    "isRead" to false,
                    "businessId" to "freshoo",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    // Products
    fun observeProducts(): Flow<List<Product>> {
        val uid = requireUserId()
        return userDoc(uid).collection("products").snapshots().map { snapshot ->
            snapshot.toObjects(Product::class.java)
        }
    }

    suspend fun addProduct(product: Product): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("products").document()
        val payload = mapOf(
            "name" to product.name,
            "businessId" to product.businessId,
            "sku" to product.sku,
            "barcode" to product.barcode,
            "category" to product.category,
            "unit" to product.unit,
            "purchaseCost" to product.purchaseCost,
            "sellingPrice" to product.sellingPrice,
            "wholesalePrice" to product.wholesalePrice,
            "currentStock" to product.currentStock,
            "minStock" to product.minStock,
            "supplierId" to product.supplierId,
            "supplierName" to product.supplierName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()

        // Also record stock movement
        if (product.currentStock > 0) {
            recordStockMovement(
                StockMovement(
                    productId = doc.id,
                    productName = product.name,
                    businessId = product.businessId,
                    type = "ADJUSTMENT",
                    quantityDelta = product.currentStock,
                    previousStock = 0.0,
                    newStock = product.currentStock,
                    reason = "Initial stock opening balance"
                )
            )
        }
        return doc.id
    }

    suspend fun updateProduct(product: Product) {
        val uid = requireUserId()
        require(product.id.isNotBlank())
        val doc = userDoc(uid).collection("products").document(product.id)
        val payload = mapOf(
            "name" to product.name,
            "businessId" to product.businessId,
            "sku" to product.sku,
            "barcode" to product.barcode,
            "category" to product.category,
            "unit" to product.unit,
            "purchaseCost" to product.purchaseCost,
            "sellingPrice" to product.sellingPrice,
            "wholesalePrice" to product.wholesalePrice,
            "currentStock" to product.currentStock,
            "minStock" to product.minStock,
            "supplierId" to product.supplierId,
            "supplierName" to product.supplierName,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.update(payload).await()
    }

    suspend fun adjustStock(productId: String, newStock: Double, reason: String) {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("products").document(productId)
        val snapshot = doc.get().await()
        if (snapshot.exists()) {
            val product = snapshot.toObject(Product::class.java)!!
            val oldStock = product.currentStock
            val delta = newStock - oldStock
            doc.update("currentStock", newStock, "updatedAt", FieldValue.serverTimestamp()).await()
            recordStockMovement(
                StockMovement(
                    productId = productId,
                    productName = product.name,
                    businessId = product.businessId,
                    type = "ADJUSTMENT",
                    quantityDelta = delta,
                    previousStock = oldStock,
                    newStock = newStock,
                    reason = reason
                )
            )
        }
    }

    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        userDoc(uid).collection("products").document(productId).delete().await()
    }

    // Stock Movements (Ledger)
    fun observeStockMovements(): Flow<List<StockMovement>> {
        val uid = requireUserId()
        return userDoc(uid).collection("stock_movements")
            .snapshots().map { snapshot ->
                snapshot.toObjects(StockMovement::class.java)
            }
    }

    suspend fun recordStockMovement(movement: StockMovement) {
        val uid = requireUserId()
        val payload = mapOf(
            "productId" to movement.productId,
            "productName" to movement.productName,
            "businessId" to movement.businessId,
            "type" to movement.type,
            "quantityDelta" to movement.quantityDelta,
            "previousStock" to movement.previousStock,
            "newStock" to movement.newStock,
            "reason" to movement.reason,
            "referenceId" to movement.referenceId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        userDoc(uid).collection("stock_movements").add(payload).await()
    }

    // Sales
    fun observeSales(): Flow<List<Sale>> {
        val uid = requireUserId()
        return userDoc(uid).collection("sales").snapshots().map { snapshot ->
            snapshot.toObjects(Sale::class.java)
        }
    }

    suspend fun addSale(sale: Sale): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("sales").document()
        val payload = mapOf(
            "invoiceNumber" to sale.invoiceNumber,
            "businessId" to sale.businessId,
            "customerId" to sale.customerId,
            "customerName" to sale.customerName,
            "customerPhone" to sale.customerPhone,
            "items" to sale.items,
            "subtotal" to sale.subtotal,
            "discount" to sale.discount,
            "tax" to sale.tax,
            "deliveryCharge" to sale.deliveryCharge,
            "packagingCost" to sale.packagingCost,
            "totalAmount" to sale.totalAmount,
            "amountReceived" to sale.amountReceived,
            "outstandingAmount" to sale.outstandingAmount,
            "estimatedProfit" to sale.estimatedProfit,
            "paymentMethod" to sale.paymentMethod,
            "paymentStatus" to sale.paymentStatus,
            "saleDate" to sale.saleDate,
            "notes" to sale.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()

        // Deduct inventory stock for each sold item and record ledger movement
        for (item in sale.items) {
            val prodId = item["productId"] as? String ?: continue
            val qty = (item["quantity"] as? Number)?.toDouble() ?: 0.0
            if (qty > 0) {
                val prodDoc = userDoc(uid).collection("products").document(prodId)
                val snapshot = prodDoc.get().await()
                if (snapshot.exists()) {
                    val p = snapshot.toObject(Product::class.java)!!
                    val prevStock = p.currentStock
                    val newStock = prevStock - qty
                    prodDoc.update("currentStock", newStock, "updatedAt", FieldValue.serverTimestamp()).await()
                    recordStockMovement(
                        StockMovement(
                            productId = prodId,
                            productName = p.name,
                            businessId = sale.businessId,
                            type = "SALE",
                            quantityDelta = -qty,
                            previousStock = prevStock,
                            newStock = newStock,
                            reason = "Sale Invoice #${sale.invoiceNumber}",
                            referenceId = doc.id
                        )
                    )
                }
            }
        }

        // If customer exists, update total purchases
        if (sale.customerId.isNotBlank()) {
            val custDoc = userDoc(uid).collection("customers").document(sale.customerId)
            custDoc.update(
                "totalOrders", FieldValue.increment(1),
                "totalPurchases", FieldValue.increment(sale.totalAmount),
                "outstandingBalance", FieldValue.increment(sale.outstandingAmount),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        }

        return doc.id
    }

    suspend fun updateSaleStatus(saleId: String, newStatus: String) {
        val uid = requireUserId()
        userDoc(uid).collection("sales").document(saleId).update(
            "paymentStatus", newStatus,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    // Purchases
    fun observePurchases(): Flow<List<Purchase>> {
        val uid = requireUserId()
        return userDoc(uid).collection("purchases").snapshots().map { snapshot ->
            snapshot.toObjects(Purchase::class.java)
        }
    }

    suspend fun addPurchase(purchase: Purchase): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("purchases").document()
        val payload = mapOf(
            "purchaseId" to purchase.purchaseId,
            "supplierId" to purchase.supplierId,
            "supplierName" to purchase.supplierName,
            "businessId" to purchase.businessId,
            "items" to purchase.items,
            "totalAmount" to purchase.totalAmount,
            "amountPaid" to purchase.amountPaid,
            "outstandingPayable" to purchase.outstandingPayable,
            "additionalCosts" to purchase.additionalCosts,
            "purchaseDate" to purchase.purchaseDate,
            "paymentMethod" to purchase.paymentMethod,
            "notes" to purchase.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()

        // Increase inventory stock for each purchased item
        for (item in purchase.items) {
            val prodId = item["productId"] as? String ?: continue
            val qty = (item["quantity"] as? Number)?.toDouble() ?: 0.0
            if (qty > 0) {
                val prodDoc = userDoc(uid).collection("products").document(prodId)
                val snapshot = prodDoc.get().await()
                if (snapshot.exists()) {
                    val p = snapshot.toObject(Product::class.java)!!
                    val prevStock = p.currentStock
                    val newStock = prevStock + qty
                    prodDoc.update("currentStock", newStock, "updatedAt", FieldValue.serverTimestamp()).await()
                    recordStockMovement(
                        StockMovement(
                            productId = prodId,
                            productName = p.name,
                            businessId = purchase.businessId,
                            type = "PURCHASE",
                            quantityDelta = qty,
                            previousStock = prevStock,
                            newStock = newStock,
                            reason = "Purchase #${purchase.purchaseId}",
                            referenceId = doc.id
                        )
                    )
                }
            }
        }

        // If supplier exists, update payables
        if (purchase.supplierId.isNotBlank()) {
            val suppDoc = userDoc(uid).collection("suppliers").document(purchase.supplierId)
            suppDoc.update(
                "totalPurchases", FieldValue.increment(purchase.totalAmount),
                "totalPayments", FieldValue.increment(purchase.amountPaid),
                "outstandingPayable", FieldValue.increment(purchase.outstandingPayable),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        }

        return doc.id
    }

    // Customers
    fun observeCustomers(): Flow<List<Customer>> {
        val uid = requireUserId()
        return userDoc(uid).collection("customers").snapshots().map { snapshot ->
            snapshot.toObjects(Customer::class.java)
        }
    }

    suspend fun addCustomer(customer: Customer): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("customers").document()
        val payload = mapOf(
            "name" to customer.name,
            "phone" to customer.phone,
            "email" to customer.email,
            "address" to customer.address,
            "city" to customer.city,
            "district" to customer.district,
            "totalOrders" to customer.totalOrders,
            "totalPurchases" to customer.totalPurchases,
            "outstandingBalance" to customer.outstandingBalance,
            "notes" to customer.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun updateCustomer(customer: Customer) {
        val uid = requireUserId()
        require(customer.id.isNotBlank())
        userDoc(uid).collection("customers").document(customer.id).update(
            "name", customer.name,
            "phone", customer.phone,
            "email", customer.email,
            "address", customer.address,
            "city", customer.city,
            "district", customer.district,
            "notes", customer.notes,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    // Suppliers
    fun observeSuppliers(): Flow<List<Supplier>> {
        val uid = requireUserId()
        return userDoc(uid).collection("suppliers").snapshots().map { snapshot ->
            snapshot.toObjects(Supplier::class.java)
        }
    }

    suspend fun addSupplier(supplier: Supplier): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("suppliers").document()
        val payload = mapOf(
            "supplierName" to supplier.supplierName,
            "contactPerson" to supplier.contactPerson,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "address" to supplier.address,
            "businessId" to supplier.businessId,
            "openingBalance" to supplier.openingBalance,
            "totalPurchases" to supplier.totalPurchases,
            "totalPayments" to supplier.totalPayments,
            "outstandingPayable" to supplier.outstandingPayable,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun recordSupplierPayment(supplierId: String, paymentAmount: Double) {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("suppliers").document(supplierId)
        doc.update(
            "totalPayments", FieldValue.increment(paymentAmount),
            "outstandingPayable", FieldValue.increment(-paymentAmount),
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    // Parcels & Courier Management
    fun observeParcels(): Flow<List<Parcel>> {
        val uid = requireUserId()
        return userDoc(uid).collection("parcels").snapshots().map { snapshot ->
            snapshot.toObjects(Parcel::class.java)
        }
    }

    suspend fun addParcel(parcel: Parcel): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("parcels").document()
        val payload = mapOf(
            "parcelId" to parcel.parcelId,
            "orderId" to parcel.orderId,
            "businessId" to parcel.businessId,
            "customerName" to parcel.customerName,
            "customerPhone" to parcel.customerPhone,
            "customerAddress" to parcel.customerAddress,
            "productDetails" to parcel.productDetails,
            "codAmount" to parcel.codAmount,
            "courierCompany" to parcel.courierCompany,
            "trackingNumber" to parcel.trackingNumber,
            "deliveryCharge" to parcel.deliveryCharge,
            "packagingCharge" to parcel.packagingCharge,
            "parcelDate" to parcel.parcelDate,
            "deliveryStatus" to parcel.deliveryStatus,
            "settlementStatus" to parcel.settlementStatus,
            "notes" to parcel.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun updateParcelStatus(parcelId: String, deliveryStatus: String, settlementStatus: String) {
        val uid = requireUserId()
        userDoc(uid).collection("parcels").document(parcelId).update(
            "deliveryStatus", deliveryStatus,
            "settlementStatus", settlementStatus,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    // COD Settlements
    fun observeCodSettlements(): Flow<List<CodSettlement>> {
        val uid = requireUserId()
        return userDoc(uid).collection("cod_settlements").snapshots().map { snapshot ->
            snapshot.toObjects(CodSettlement::class.java)
        }
    }

    suspend fun addCodSettlement(settlement: CodSettlement): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("cod_settlements").document()
        val payload = mapOf(
            "settlementId" to settlement.settlementId,
            "courierCompany" to settlement.courierCompany,
            "businessId" to settlement.businessId,
            "totalCodCollected" to settlement.totalCodCollected,
            "courierCharges" to settlement.courierCharges,
            "additionalFees" to settlement.additionalFees,
            "netSettlement" to settlement.netSettlement,
            "receivedAmount" to settlement.receivedAmount,
            "outstandingAmount" to settlement.outstandingAmount,
            "settlementDate" to settlement.settlementDate,
            "parcelIds" to settlement.parcelIds,
            "status" to settlement.status,
            "notes" to settlement.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()

        // Mark associated parcels as Settled
        for (pId in settlement.parcelIds) {
            try {
                userDoc(uid).collection("parcels").document(pId).update(
                    "settlementStatus", "Settled",
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Error updating parcel settlement: ${e.message}")
            }
        }
        return doc.id
    }

    // Expenses
    fun observeExpenses(): Flow<List<Expense>> {
        val uid = requireUserId()
        return userDoc(uid).collection("expenses").snapshots().map { snapshot ->
            snapshot.toObjects(Expense::class.java)
        }
    }

    suspend fun addExpense(expense: Expense): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("expenses").document()
        val payload = mapOf(
            "profileId" to expense.profileId,
            "category" to expense.category,
            "amount" to expense.amount,
            "date" to expense.date,
            "paymentMethod" to expense.paymentMethod,
            "description" to expense.description,
            "notes" to expense.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun deleteExpense(expenseId: String) {
        val uid = requireUserId()
        userDoc(uid).collection("expenses").document(expenseId).delete().await()
    }

    // Income
    fun observeIncomes(): Flow<List<Income>> {
        val uid = requireUserId()
        return userDoc(uid).collection("incomes").snapshots().map { snapshot ->
            snapshot.toObjects(Income::class.java)
        }
    }

    suspend fun addIncome(income: Income): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("incomes").document()
        val payload = mapOf(
            "profileId" to income.profileId,
            "category" to income.category,
            "amount" to income.amount,
            "date" to income.date,
            "paymentMethod" to income.paymentMethod,
            "description" to income.description,
            "notes" to income.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun deleteIncome(incomeId: String) {
        val uid = requireUserId()
        userDoc(uid).collection("incomes").document(incomeId).delete().await()
    }

    // Accounts
    fun observeAccounts(): Flow<List<Account>> {
        val uid = requireUserId()
        return userDoc(uid).collection("accounts").snapshots().map { snapshot ->
            snapshot.toObjects(Account::class.java)
        }
    }

    suspend fun addAccount(account: Account): String {
        val uid = requireUserId()
        val doc = userDoc(uid).collection("accounts").document()
        val payload = mapOf(
            "accountName" to account.accountName,
            "accountType" to account.accountType,
            "accountNumber" to account.accountNumber,
            "balance" to account.balance,
            "businessId" to account.businessId,
            "isPersonal" to account.isPersonal,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        return doc.id
    }

    // Transfers
    fun observeTransfers(): Flow<List<Transfer>> {
        val uid = requireUserId()
        return userDoc(uid).collection("transfers").snapshots().map { snapshot ->
            snapshot.toObjects(Transfer::class.java)
        }
    }

    suspend fun executeTransfer(transfer: Transfer) {
        val uid = requireUserId()
        // Deduct from source, add to target atomically
        val fromRef = userDoc(uid).collection("accounts").document(transfer.fromAccountId)
        val toRef = userDoc(uid).collection("accounts").document(transfer.toAccountId)
        val transferRef = userDoc(uid).collection("transfers").document()

        db.runTransaction { transaction ->
            val fromSnap = transaction.get(fromRef)
            val toSnap = transaction.get(toRef)
            val fromBal = (fromSnap.getDouble("balance") ?: 0.0) - transfer.amount
            val toBal = (toSnap.getDouble("balance") ?: 0.0) + transfer.amount

            transaction.update(fromRef, "balance", fromBal, "updatedAt", FieldValue.serverTimestamp())
            transaction.update(toRef, "balance", toBal, "updatedAt", FieldValue.serverTimestamp())
            transaction.set(
                transferRef,
                mapOf(
                    "fromAccountId" to transfer.fromAccountId,
                    "fromAccountName" to transfer.fromAccountName,
                    "toAccountId" to transfer.toAccountId,
                    "toAccountName" to transfer.toAccountName,
                    "amount" to transfer.amount,
                    "date" to transfer.date,
                    "note" to transfer.note,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    // Notifications
    fun observeNotifications(): Flow<List<NotificationItem>> {
        val uid = requireUserId()
        return userDoc(uid).collection("notifications").snapshots().map { snapshot ->
            snapshot.toObjects(NotificationItem::class.java)
        }
    }

    suspend fun addNotification(notif: NotificationItem) {
        val uid = requireUserId()
        userDoc(uid).collection("notifications").add(
            mapOf(
                "title" to notif.title,
                "message" to notif.message,
                "type" to notif.type,
                "isRead" to false,
                "businessId" to notif.businessId,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun markNotificationRead(notifId: String) {
        val uid = requireUserId()
        userDoc(uid).collection("notifications").document(notifId).update("isRead", true).await()
    }
}
