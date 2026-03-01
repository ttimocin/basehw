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
import com.taytek.basehw.ui.components.CustomBottomNavigation
import com.taytek.basehw.ui.screens.collection.CollectionScreen
import com.taytek.basehw.ui.theme.AppBackground
import com.taytek.basehw.ui.theme.AppTextSecondary

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
            CustomBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAddClick = onAddCarClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(AppBackground)
        ) {
            when (selectedTab) {
                0 -> { // Home
                    CollectionScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = onCarClick,
                        onSettingsClick = onSettingsClick
                    )
                }
                1 -> { // Search -> Placeholder
                    SearchPlaceholder()
                }
                2 -> { // Collections -> Placeholder
                    CollectionsPlaceholder()
                }
                3 -> { // Profile 
                    com.taytek.basehw.ui.screens.profile.ProfileScreen()
                }
            }
        }
    }
}

@Composable
fun SearchPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Search Coming Soon", color = AppTextSecondary)
    }
}

@Composable
fun CollectionsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Collections Coming Soon", color = AppTextSecondary)
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
