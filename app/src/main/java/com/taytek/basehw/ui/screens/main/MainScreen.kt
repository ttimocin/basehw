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
import com.taytek.basehw.ui.navigation.Screen
import com.taytek.basehw.R

@Composable
fun MainScreen(
    onAddCarClick: () -> Unit,
    onAddCarCameraClick: () -> Unit,
    onAddWantedCarClick: () -> Unit,
    onCarClick: (Long, Boolean) -> Unit,
    onFolderClick: (Long) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onAddCarWithMasterIdClick: (Long, Boolean) -> Unit,
    onAddCarWithMasterIdAndDeleteClick: (Long, Long?, Boolean) -> Unit = { _, _, _ -> },
    onSthCarClick: (Long) -> Unit = { id -> onAddCarWithMasterIdClick(id, false) },
    navigateToTab: Int = -1,
    wishlistTab: Int = -1,
    onConsumeTabNavigation: () -> Unit = {},
    onConsumeWishlistTab: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onUserProfileClick: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var previousTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(navigateToTab) {
        if (navigateToTab >= 0) {
            previousTab = selectedTab
            selectedTab = if (navigateToTab == 4) 8 else navigateToTab
            onConsumeTabNavigation()
        }
    }

    Scaffold(
        bottomBar = {
            CustomBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { 
                    previousTab = selectedTab
                    selectedTab = it 
                },
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
                        onCarClick = { id -> onCarClick(id, false) },
                        onProfileClick = { selectedTab = 3 },
                        onCameraClick = onAddCarCameraClick,
                        onAddClick = onAddCarClick,
                        onViewAllClick = { selectedTab = 8 },
                        onMasterCarClick = { id -> onAddCarWithMasterIdClick(id, false) },
                        onCommunityClick = { selectedTab = 2 }
                    )
                }
                8 -> { // KOLEKSİYON (Legacy)
                    CollectionScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = { id -> onCarClick(id, false) },
                        onStatisticsClick = { 
                            previousTab = selectedTab
                            selectedTab = 5 
                        }
                    )
                }
                1 -> { // SEARCH / Wishlist
                    WishlistScreen(
                        onAddCarClick = onAddWantedCarClick,
                        onCarClick = { id -> onCarClick(id, true) },
                        onAddCarWithMasterId = { masterId, deleteId -> onAddCarWithMasterIdAndDeleteClick(masterId, deleteId, true) },
                        initialTab = wishlistTab,
                        onConsumeInitialTab = onConsumeWishlistTab
                    )
                }
                2 -> { // COMMUNITY
                    com.taytek.basehw.ui.screens.community.CommunityScreen(
                        onUserProfileClick = onUserProfileClick
                    )
                }
                3 -> { // PROFILE
                    ProfileScreen(
                        onStatisticsClick = { 
                            previousTab = selectedTab
                            selectedTab = 5 
                        },
                        onSupportClick = { 
                            previousTab = selectedTab
                            selectedTab = 6 
                        }, // Yeni tab
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onTermsOfUseClick = onTermsOfUseClick
                    )
                }
                5 -> { // Statistics (Profile'dan veya Collection'dan erişim)
                    StatisticsScreen(
                        onBack = { selectedTab = previousTab } // Geldiği yere geri dön
                    )
                }
                6 -> { // HELP & SUPPORT
                    com.taytek.basehw.ui.screens.support.SupportScreen(
                        onBack = { selectedTab = previousTab } // Geldiği yere geri dön
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
