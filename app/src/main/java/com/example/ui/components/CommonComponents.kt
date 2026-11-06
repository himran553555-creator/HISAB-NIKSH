package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessProfileType
import com.example.ui.theme.*

@Composable
fun FinancialStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBg: Color = MaterialTheme.colorScheme.primaryContainer,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    testTag: String = "stat_card"
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status.lowercase()) {
        "paid", "delivered", "settled" -> Pair(EmeraldContainer, OnEmeraldContainer)
        "partially paid", "picked up", "in transit", "ready to ship" -> Pair(BlueContainer, BlueInfo)
        "unpaid", "pending" -> Pair(AmberContainer, AmberWarning)
        "returned", "cancelled", "delivery failed", "low stock", "refunded" -> Pair(RedContainer, RedDanger)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BusinessProfileFilterRow(
    selectedProfile: BusinessProfileType,
    onProfileSelected: (BusinessProfileType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BusinessProfileType.entries.forEach { profile ->
            val isSelected = selectedProfile == profile
            val bg = if (isSelected) {
                when (profile) {
                    BusinessProfileType.ESHOP -> EmeraldPrimary
                    BusinessProfileType.FRESHOO -> Color(0xFF0284C7)
                    BusinessProfileType.PERSONAL -> Color(0xFF7C3AED)
                    BusinessProfileType.ALL -> NavySecondary
                }
            } else {
                MaterialTheme.colorScheme.surface
            }
            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

            Surface(
                color = bg,
                shape = RoundedCornerShape(20.dp),
                border = if (!isSelected) CardDefaults.outlinedCardBorder() else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onProfileSelected(profile) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val icon = when (profile) {
                        BusinessProfileType.ESHOP -> Icons.Default.ShoppingCart
                        BusinessProfileType.FRESHOO -> Icons.Default.LocalFlorist
                        BusinessProfileType.PERSONAL -> Icons.Default.AccountBalanceWallet
                        BusinessProfileType.ALL -> Icons.Default.Dashboard
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = profile.displayName,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (profile) {
                            BusinessProfileType.ESHOP -> "E-SHOP"
                            BusinessProfileType.FRESHOO -> "FRESHOO"
                            BusinessProfileType.PERSONAL -> "PERSONAL"
                            BusinessProfileType.ALL -> "ALL BIZ"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImranTopBar(
    title: String,
    activeProfile: BusinessProfileType,
    unreadNotifCount: Int,
    onProfileClick: () -> Unit,
    onNotifClick: () -> Unit,
    onSignOutClick: () -> Unit,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileChange: (BusinessProfileType) -> Unit = {}
) {
    var showProfileDropdown by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Box {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showProfileDropdown = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "হিসাব নিকাশ • ${activeProfile.displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Profile Context",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showProfileDropdown,
                    onDismissRequest = { showProfileDropdown = false }
                ) {
                    Text(
                        text = "SWITCH BUSINESS CONTEXT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    listOf(
                        BusinessProfileType.ALL,
                        BusinessProfileType.ESHOP,
                        BusinessProfileType.FRESHOO,
                        BusinessProfileType.PERSONAL
                    ).forEach { profile ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (activeProfile == profile) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    Text(
                                        text = profile.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (profile) {
                                        BusinessProfileType.ESHOP -> Icons.Default.ShoppingCart
                                        BusinessProfileType.FRESHOO -> Icons.Default.LocalFlorist
                                        BusinessProfileType.PERSONAL -> Icons.Default.AccountBalanceWallet
                                        BusinessProfileType.ALL -> Icons.Default.Dashboard
                                    },
                                    contentDescription = profile.displayName,
                                    tint = if (activeProfile == profile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (activeProfile == profile) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                onProfileChange(profile)
                                showProfileDropdown = false
                            }
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onNotifClick) {
                BadgedBox(
                    badge = {
                        if (unreadNotifCount > 0) {
                            Badge { Text("$unreadNotifCount") }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications"
                    )
                }
            }
            IconButton(onClick = onSignOutClick) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun EmptyView(
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
