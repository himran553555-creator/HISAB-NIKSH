package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.AiRepository
import com.example.data.FirestoreRepository
import com.example.model.*
import com.example.ui.components.ImranTopBar
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.ai.AiAssistantScreen
import com.example.ui.screens.auth.signOut
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.courier.CourierScreen
import com.example.ui.screens.dashboard.MainDashboardScreen
import com.example.ui.screens.finance.ExpenseIncomeScreen
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.purchases.PurchasesScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SALES("Sales & Invoices", Icons.Default.PointOfSale),
    PURCHASES("Purchases", Icons.Default.ShoppingBag),
    INVENTORY("Inventory", Icons.Default.Inventory2),
    COURIER("Courier & COD", Icons.Default.LocalShipping),
    FINANCE("Income & Expenses", Icons.Default.ReceiptLong),
    CONTACTS("Customers & Suppliers", Icons.Default.People),
    ACCOUNTS("Cash & Bank", Icons.Default.AccountBalance),
    REPORTS("Reports & P&L", Icons.Default.BarChart),
    AI_ASSISTANT("Gemini AI", Icons.Default.AutoAwesome),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    repository: FirestoreRepository,
    currentUserId: String,
    onSignOutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val aiRepository = remember { AiRepository() }

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var activeProfile by remember { mutableStateOf(BusinessProfileType.ALL) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Collect real-time Firestore flows
    val userProfile by repository.observeUserProfile(currentUserId).collectAsState(initial = null)
    val businesses by repository.observeBusinesses().collectAsState(initial = emptyList())
    val products by repository.observeProducts().collectAsState(initial = emptyList())
    val stockMovements by repository.observeStockMovements().collectAsState(initial = emptyList())
    val sales by repository.observeSales().collectAsState(initial = emptyList())
    val purchases by repository.observePurchases().collectAsState(initial = emptyList())
    val customers by repository.observeCustomers().collectAsState(initial = emptyList())
    val suppliers by repository.observeSuppliers().collectAsState(initial = emptyList())
    val parcels by repository.observeParcels().collectAsState(initial = emptyList())
    val codSettlements by repository.observeCodSettlements().collectAsState(initial = emptyList())
    val expenses by repository.observeExpenses().collectAsState(initial = emptyList())
    val incomes by repository.observeIncomes().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val transfers by repository.observeTransfers().collectAsState(initial = emptyList())
    val notifications by repository.observeNotifications().collectAsState(initial = emptyList())

    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }

    // Restore user's saved active context from Firestore
    LaunchedEffect(userProfile?.activeContext) {
        userProfile?.activeContext?.let { ctxId ->
            if (ctxId.isNotBlank()) {
                activeProfile = BusinessProfileType.fromId(ctxId)
            }
        }
    }

    val onSelectProfile: (BusinessProfileType) -> Unit = { newProfile ->
        activeProfile = newProfile
        coroutineScope.launch {
            repository.switchActiveContext(newProfile.id)
        }
    }

    // Seed default data if new user
    LaunchedEffect(currentUserId) {
        repository.initializeDefaultDataIfEmpty()
    }

    // Android back handler
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.ic_hn_3d_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "HISAB NIKASH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "হিসাব নিকাশ • Smart Business",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Active Business Context Switcher in Drawer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ACTIVE BUSINESS CONTEXT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf(
                        BusinessProfileType.ALL,
                        BusinessProfileType.ESHOP,
                        BusinessProfileType.FRESHOO,
                        BusinessProfileType.PERSONAL
                    ).forEach { p ->
                        val isSelected = activeProfile == p
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectProfile(p)
                                    coroutineScope.launch { drawerState.close() }
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) null else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (p) {
                                        BusinessProfileType.ESHOP -> Icons.Default.ShoppingCart
                                        BusinessProfileType.FRESHOO -> Icons.Default.LocalFlorist
                                        BusinessProfileType.PERSONAL -> Icons.Default.AccountBalanceWallet
                                        BusinessProfileType.ALL -> Icons.Default.Dashboard
                                    },
                                    contentDescription = p.displayName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = p.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Screen.entries.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Sign Out") },
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = {
                        signOut(context, credentialManager, onSignOutSuccess, coroutineScope)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                ImranTopBar(
                    title = currentScreen.title,
                    activeProfile = activeProfile,
                    unreadNotifCount = unreadNotifCount,
                    onProfileClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onNotifClick = { currentScreen = Screen.NOTIFICATIONS },
                    onSignOutClick = {
                        signOut(context, credentialManager, onSignOutSuccess, coroutineScope)
                    },
                    canNavigateBack = currentScreen != Screen.DASHBOARD,
                    onBackClick = { currentScreen = Screen.DASHBOARD },
                    onProfileChange = onSelectProfile
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val bottomScreens = listOf(
                        Screen.DASHBOARD,
                        Screen.SALES,
                        Screen.INVENTORY,
                        Screen.COURIER,
                        Screen.AI_ASSISTANT
                    )
                    bottomScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, maxLines = 1) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> MainDashboardScreen(
                        activeProfile = activeProfile,
                        onProfileSelected = onSelectProfile,
                        products = products,
                        sales = sales,
                        purchases = purchases,
                        expenses = expenses,
                        incomes = incomes,
                        parcels = parcels,
                        suppliers = suppliers,
                        customers = customers,
                        accounts = accounts,
                        onNavigateToSales = { currentScreen = Screen.SALES },
                        onNavigateToPurchases = { currentScreen = Screen.PURCHASES },
                        onNavigateToInventory = { currentScreen = Screen.INVENTORY },
                        onNavigateToCourier = { currentScreen = Screen.COURIER },
                        onNavigateToExpenses = { currentScreen = Screen.FINANCE },
                        onNavigateToAi = { currentScreen = Screen.AI_ASSISTANT }
                    )

                    Screen.SALES -> SalesScreen(
                        sales = sales,
                        products = products,
                        customers = customers,
                        activeBusinessId = activeProfile.id,
                        onSaveSale = { sale ->
                            coroutineScope.launch { repository.addSale(sale) }
                        },
                        onUpdateSaleStatus = { sId, st ->
                            coroutineScope.launch { repository.updateSaleStatus(sId, st) }
                        }
                    )

                    Screen.PURCHASES -> PurchasesScreen(
                        purchases = purchases,
                        products = products,
                        suppliers = suppliers,
                        activeBusinessId = activeProfile.id,
                        onSavePurchase = { p ->
                            coroutineScope.launch { repository.addPurchase(p) }
                        }
                    )

                    Screen.INVENTORY -> InventoryScreen(
                        products = products,
                        stockMovements = stockMovements,
                        activeBusinessId = activeProfile.id,
                        onAddProduct = { p ->
                            coroutineScope.launch { repository.addProduct(p) }
                        },
                        onAdjustStock = { pId, newQty, reason ->
                            coroutineScope.launch { repository.adjustStock(pId, newQty, reason) }
                        }
                    )

                    Screen.COURIER -> CourierScreen(
                        parcels = parcels,
                        codSettlements = codSettlements,
                        activeBusinessId = activeProfile.id,
                        onAddParcel = { p ->
                            coroutineScope.launch { repository.addParcel(p) }
                        },
                        onUpdateParcelStatus = { pId, dSt, sSt ->
                            coroutineScope.launch { repository.updateParcelStatus(pId, dSt, sSt) }
                        },
                        onAddSettlement = { s ->
                            coroutineScope.launch { repository.addCodSettlement(s) }
                        }
                    )

                    Screen.FINANCE -> ExpenseIncomeScreen(
                        expenses = expenses,
                        incomes = incomes,
                        activeProfile = activeProfile,
                        onAddExpense = { e ->
                            coroutineScope.launch { repository.addExpense(e) }
                        },
                        onDeleteExpense = { eId ->
                            coroutineScope.launch { repository.deleteExpense(eId) }
                        },
                        onAddIncome = { i ->
                            coroutineScope.launch { repository.addIncome(i) }
                        },
                        onDeleteIncome = { iId ->
                            coroutineScope.launch { repository.deleteIncome(iId) }
                        }
                    )

                    Screen.CONTACTS -> ContactsScreen(
                        customers = customers,
                        suppliers = suppliers,
                        onAddCustomer = { c ->
                            coroutineScope.launch { repository.addCustomer(c) }
                        },
                        onAddSupplier = { s ->
                            coroutineScope.launch { repository.addSupplier(s) }
                        }
                    )

                    Screen.ACCOUNTS -> AccountsScreen(
                        accounts = accounts,
                        transfers = transfers,
                        onAddAccount = { a ->
                            coroutineScope.launch { repository.addAccount(a) }
                        },
                        onExecuteTransfer = { t ->
                            coroutineScope.launch { repository.executeTransfer(t) }
                        }
                    )

                    Screen.REPORTS -> ReportsScreen(
                        sales = sales,
                        purchases = purchases,
                        expenses = expenses,
                        incomes = incomes,
                        parcels = parcels,
                        accounts = accounts,
                        activeProfile = activeProfile
                    )

                    Screen.AI_ASSISTANT -> AiAssistantScreen(
                        aiRepository = aiRepository,
                        activeProfile = activeProfile,
                        products = products,
                        sales = sales,
                        purchases = purchases,
                        expenses = expenses,
                        incomes = incomes,
                        parcels = parcels,
                        suppliers = suppliers,
                        customers = customers,
                        accounts = accounts
                    )

                    Screen.NOTIFICATIONS -> NotificationsScreen(
                        notifications = notifications,
                        onMarkRead = { nId ->
                            coroutineScope.launch { repository.markNotificationRead(nId) }
                        }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        userProfile = userProfile,
                        activeProfile = activeProfile,
                        businesses = businesses,
                        onSwitchBusiness = onSelectProfile,
                        onSignOut = {
                            signOut(context, credentialManager, onSignOutSuccess, coroutineScope)
                        }
                    )
                }
            }
        }
    }
}
