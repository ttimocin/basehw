package com.taytek.basehw.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taytek.basehw.ui.components.CustomBottomNavigation
import com.taytek.basehw.ui.screens.home.HomeScreen
import com.taytek.basehw.ui.screens.collection.CollectionScreen
import com.taytek.basehw.ui.screens.profile.ProfileScreen
import com.taytek.basehw.ui.screens.statistics.StatisticsScreen
import com.taytek.basehw.ui.screens.wishlist.WishlistScreen
import com.taytek.basehw.ui.theme.AppBackground
import com.taytek.basehw.ui.theme.AppTextSecondary
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.ui.navigation.Screen
import com.taytek.basehw.R

@Composable
fun MainScreen(
    onAddCarClick: () -> Unit,
    onAddCarCameraClick: () -> Unit,
    onAddWantedCarClick: () -> Unit,
    onCarClick: (Long) -> Unit,
    onFolderClick: (Long) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onAddCarWithMasterIdClick: (Long) -> Unit,
    onSthCarClick: (Long) -> Unit = onAddCarWithMasterIdClick,
    navigateToTab: Int = -1,
    onConsumeTabNavigation: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(navigateToTab) {
        if (navigateToTab >= 0) {
            selectedTab = if (navigateToTab == 4) 8 else navigateToTab
            onConsumeTabNavigation()
        }
    }

    Scaffold(
        bottomBar = {
            CustomBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAddClick = onAddCarClick
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> { // ANASAYFA (Figma Redesign)
                    HomeScreen(
                        onCarClick = onCarClick,
                        onProfileClick = { selectedTab = 2 },
                        onCameraClick = onAddCarCameraClick,
                        onAddClick = onAddCarClick,
                        onViewAllClick = { selectedTab = 8 },
                        onMasterCarClick = onAddCarWithMasterIdClick
                    )
                }
                8 -> { // KOLEKSİYON (Legacy)
                    CollectionScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = onCarClick,
                        onStatisticsClick = { selectedTab = 5 }
                    )
                }
                1 -> { // SEARCH / Wishlist
                    WishlistScreen(
                        onAddCarClick = onAddWantedCarClick,
                        onCarClick = onCarClick
                    )
                }
                2 -> { // PROFILE
                    ProfileScreen(
                        onStatisticsClick = { selectedTab = 5 },
                        onSupportClick = { selectedTab = 6 }, // Yeni tab
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onTermsOfUseClick = onTermsOfUseClick
                    )
                }
                5 -> { // Statistics (Profile'dan erişim)
                    StatisticsScreen(
                        onBack = { selectedTab = 2 } // Profile tabına geri dön
                    )
                }
                6 -> { // HELP & SUPPORT
                    com.taytek.basehw.ui.screens.support.SupportScreen(
                        onBack = { selectedTab = 2 } // Profile tabına geri dön
                    )
                }
                7 -> { // STH
                    com.taytek.basehw.ui.screens.sth.SthScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = { masterId ->
                            onSthCarClick(masterId)
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun WantedPlaceholder() {
    // Removed
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
                stringResource(R.string.premium_coming_soon),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
