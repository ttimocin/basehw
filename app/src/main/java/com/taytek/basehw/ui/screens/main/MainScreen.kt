package com.taytek.basehw.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taytek.basehw.ui.screens.collection.CollectionScreen
import com.taytek.basehw.ui.screens.statistics.StatisticsScreen
import com.taytek.basehw.ui.screens.wishlist.WishlistScreen

@Composable
fun MainScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    // We default to Profile (index 3) based on the user's explicit design request for demonstration, 
    // but practically 0 (Home) is best. We will set it to 0.
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            CustomBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    if (index == 2) {
                        onAddCarClick() // "Add" triggers action
                    } else {
                        selectedTab = index
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> { // Home
                    CollectionScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = onCarClick,
                        onSettingsClick = onSettingsClick
                    )
                }
                1 -> { // Search
                    WishlistScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = onCarClick,
                        onSettingsClick = onSettingsClick
                    )
                }
                3 -> { // Profile 
                    com.taytek.basehw.ui.screens.profile.ProfileScreen()
                }
                4 -> { // Premium -> Placeholder
                    PremiumPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun CustomBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "Home" to Icons.Outlined.Home,
                "Search" to Icons.Outlined.Search,
                "Add" to Icons.Outlined.AddCircleOutline,
                "Profile" to Icons.Outlined.Person,
                "Premium" to Icons.Outlined.StarOutline
            )

            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedTab == index && index != 2 // Add is never "selected" visually
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Premium Features Coming Soon",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
