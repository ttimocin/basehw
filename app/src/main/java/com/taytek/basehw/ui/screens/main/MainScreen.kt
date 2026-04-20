package com.taytek.basehw.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taytek.basehw.R
import com.taytek.basehw.ui.components.CustomBottomNavigation
import com.taytek.basehw.ui.screens.collection.CollectionScreenWithTabs
import com.taytek.basehw.ui.screens.community.CommunityViewModel
import com.taytek.basehw.ui.screens.community.UserProfileScreen
import com.taytek.basehw.ui.screens.home.HomeScreen
import com.taytek.basehw.ui.screens.profile.NonUserProfileScreen
import com.taytek.basehw.ui.screens.profile.ProfileViewModel
import com.taytek.basehw.ui.screens.community.CommunityScreen
import com.taytek.basehw.ui.screens.statistics.StatisticsScreen
import com.taytek.basehw.ui.navigation.Screen
import com.taytek.basehw.ui.theme.CyberGradientBottom
import com.taytek.basehw.ui.theme.CyberGradientMid
import com.taytek.basehw.ui.theme.CyberGradientTop
import com.taytek.basehw.ui.theme.LocalThemeVariant
import com.taytek.basehw.ui.theme.NeonCyanGradientBottom
import com.taytek.basehw.ui.theme.NeonCyanGradientMid
import com.taytek.basehw.ui.theme.NeonCyanGradientTop
import com.taytek.basehw.ui.theme.ThemeVariant
import com.taytek.basehw.ui.theme.cyberRootBackgroundColor
import com.taytek.basehw.ui.theme.isNeonShellTheme
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

private data class MainDrawerItem(
    val tab: Int?,
    val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: DrawerAction = DrawerAction.SwitchTab
)

private enum class DrawerAction {
    SwitchTab,
    OpenSettings,
    SignOut
}

@Composable
fun MainScreen(
    navController: NavHostController,
    onAddCarClick: () -> Unit,
    onAddCarCameraClick: () -> Unit,
    onAddWantedCarClick: () -> Unit,
    onCarClick: (Long, Boolean) -> Unit,
    onFolderClick: (Long) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onForumRulesClick: () -> Unit,
    onAddCarWithMasterIdClick: (Long, Boolean) -> Unit,
    onAddCarWithMasterIdAndDeleteClick: (Long, Long?, Boolean) -> Unit = { _, _, _ -> },
    onSthCarClick: (Long) -> Unit = { id -> onAddCarWithMasterIdClick(id, false) },
    navigateToTab: Int = -1,
    wishlistTab: Int = -1,
    onConsumeTabNavigation: () -> Unit = {},
    onConsumeWishlistTab: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onUserProfileClick: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onDirectMessageClick: (String, String) -> Unit = { _, _ -> },
    onAdminPanelClick: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var previousTab by rememberSaveable { mutableStateOf(0) }
    var selectedCollectionContentTab by rememberSaveable { mutableStateOf(0) }
    var reopenProfileSettingsOnReturn by rememberSaveable { mutableStateOf(false) }

    val profileViewModel: ProfileViewModel = hiltViewModel()
    val communityViewModel: CommunityViewModel = hiltViewModel()
    val currentUser by profileViewModel.currentUser.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val drawerScope = rememberCoroutineScope()

    LaunchedEffect(navigateToTab) {
        if (navigateToTab >= 0) {
            previousTab = selectedTab
            if (navigateToTab == 1) {
                selectedTab = 8
                selectedCollectionContentTab = 1
            } else {
                selectedTab = if (navigateToTab == 4) 8 else navigateToTab
                if (selectedTab == 8) selectedCollectionContentTab = 0
            }
            onConsumeTabNavigation()
        }
    }

    val themeVariant = LocalThemeVariant.current
    val neonBackgroundBrush = when (themeVariant) {
        ThemeVariant.Cyber ->
            Brush.verticalGradient(listOf(CyberGradientTop, CyberGradientMid, CyberGradientBottom))
        ThemeVariant.NeonCyan ->
            Brush.verticalGradient(listOf(NeonCyanGradientTop, NeonCyanGradientMid, NeonCyanGradientBottom))
        else -> null
    }

    val drawerItems = listOf(
        MainDrawerItem(tab = 0, label = R.string.nav_anasayfa, icon = Icons.Filled.Home),
        MainDrawerItem(tab = 8, label = R.string.nav_home, icon = Icons.Filled.DirectionsCar),
        MainDrawerItem(tab = 7, label = R.string.sth_label, icon = Icons.Filled.Star),
        MainDrawerItem(tab = 2, label = R.string.nav_community, icon = Icons.Filled.Groups),
        MainDrawerItem(tab = 3, label = R.string.nav_profile, icon = Icons.Filled.AccountCircle),
        MainDrawerItem(tab = 3, label = R.string.settings_title, icon = Icons.Filled.Settings, action = DrawerAction.OpenSettings),
        MainDrawerItem(tab = null, label = R.string.logout, icon = Icons.Filled.Logout, action = DrawerAction.SignOut)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.drawer_menu_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.size(8.dp))
                drawerItems.forEach { item ->
                    val isSelected = item.tab != null && selectedTab == item.tab && item.action == DrawerAction.SwitchTab
                    val itemColors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = stringResource(item.label),
                                style = MaterialTheme.typography.titleSmall
                            )
                        },
                        selected = isSelected,
                        colors = itemColors,
                        onClick = {
                            when (item.action) {
                                DrawerAction.SwitchTab -> {
                                    item.tab?.let { tab ->
                                        previousTab = selectedTab
                                        if (tab == 8) selectedCollectionContentTab = 0
                                        selectedTab = tab
                                    }
                                }
                                DrawerAction.OpenSettings -> {
                                    previousTab = selectedTab
                                    reopenProfileSettingsOnReturn = true
                                    selectedTab = 3
                                }
                                DrawerAction.SignOut -> {
                                    profileViewModel.signOut()
                                    previousTab = selectedTab
                                    selectedTab = 0
                                }
                            }
                            drawerScope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    if (item.action == DrawerAction.OpenSettings) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = cyberRootBackgroundColor(),
            bottomBar = {
                CustomBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        previousTab = selectedTab
                        if (it == 8) selectedCollectionContentTab = 0
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
                    .then(
                        if (isNeonShellTheme() && neonBackgroundBrush != null) {
                            Modifier.background(neonBackgroundBrush)
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.background)
                        }
                    )
            ) {
            // Profile tab (3) - NonUserProfileScreen tam ekran, kendi BottomNav'ı var
            if (selectedTab == 3) {
                val shouldRenderAuthFlow =
                    currentUser?.uid.isNullOrBlank() ||
                        profileUiState.showMandatoryConsentDialog ||
                        profileUiState.showUsernamePrompt ||
                        profileUiState.pendingUsernamePromptAfterConsent
                if (shouldRenderAuthFlow) {
                    NonUserProfileScreen(
                        onSettingsClick = {},
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onTermsOfUseClick = onTermsOfUseClick,
                        onForumRulesClick = onForumRulesClick,
                        viewModel = profileViewModel
                    )
                } else {
                    UserProfileScreen(
                        userId = currentUser!!.uid,
                        viewModel = communityViewModel,
                        onNavigateBack = { selectedTab = previousTab },
                        showBackButton = false,
                        openSettingsOnEnter = reopenProfileSettingsOnReturn,
                        onConsumeOpenSettings = { reopenProfileSettingsOnReturn = false },
                        onMessageClick = onDirectMessageClick,
                        onInboxClick = onInboxClick,
                        onNotificationsClick = onNotificationsClick,
                        onSupportClick = {
                            previousTab = selectedTab
                            reopenProfileSettingsOnReturn = true
                            selectedTab = 6
                        },
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onTermsOfUseClick = onTermsOfUseClick,
                        onForumRulesClick = onForumRulesClick,
                        onSettingsClick = {
                            // Profile içinden açılan ayarlarda geri, profilde kalmalı.
                            // External (drawer/support) açılışlarda bu flag zaten ayrı set ediliyor.
                        },
                        onEditProfileClick = onEditProfileClick,
                        onStatsClick = {
                            previousTab = selectedTab
                            selectedTab = 5
                        },
                        onAdminPanelClick = onAdminPanelClick
                    )
                }
            }

            // Diğer ekranlar - MainScreen Scaffold padding ile
            when (selectedTab) {
                0 -> {
                    HomeScreen(
                        onCarClick = { id -> onCarClick(id, false) },
                        onProfileClick = { selectedTab = 3 },
                        onMenuClick = {
                            drawerScope.launch { drawerState.open() }
                        },
                        onCameraClick = onAddCarCameraClick,
                        onAddClick = onAddCarClick,
                        onViewAllClick = {
                            selectedCollectionContentTab = 0
                            selectedTab = 8
                        },
                        onMasterCarClick = { id -> onAddCarWithMasterIdClick(id, false) },
                        onCommunityClick = {
                            onCommunityClick()
                            selectedTab = 2
                        },
                        onNewsClick = { newsId ->
                            navController.navigate(Screen.NewsDetail.createRoute(newsId))
                        },
                        contentPadding = paddingValues
                    )
                }

                8 -> {
                    CollectionScreenWithTabs(
                        selectedContentTab = selectedCollectionContentTab,
                        onContentTabSelected = { selectedCollectionContentTab = it },
                        onAddCollectionCarClick = onAddCarClick,
                        onCollectionCarClick = { id -> onCarClick(id, false) },
                        onAddWishlistCarClick = onAddWantedCarClick,
                        onWishlistCarClick = { id -> onCarClick(id, true) },
                        onAddCarWithMasterId = { masterId, deleteId ->
                            onAddCarWithMasterIdAndDeleteClick(masterId, deleteId, true)
                        },
                        initialWishlistTab = wishlistTab,
                        onConsumeWishlistTab = onConsumeWishlistTab,
                        onStatisticsClick = {
                            previousTab = selectedTab
                            selectedTab = 5
                        },
                        contentPadding = paddingValues
                    )
                }

                2 -> {
                    CommunityScreen(
                        viewModel = communityViewModel,
                        onUserProfileClick = onUserProfileClick,
                        onInboxClick = onInboxClick,
                        onProfileClick = { selectedTab = 3 },
                        onLoginClick = { selectedTab = 3 },
                        onNavigateToAdminPanel = onAdminPanelClick,
                        onRanksClick = {
                            navController.navigate(Screen.Ranks.route)
                        },
                        onNotificationsClick = onNotificationsClick
                    )
                }

                5 -> {
                    StatisticsScreen(
                        onBack = { selectedTab = previousTab },
                        contentPadding = paddingValues
                    )
                }

                6 -> {
                    com.taytek.basehw.ui.screens.support.SupportScreen(
                        onBack = {
                            if (reopenProfileSettingsOnReturn) {
                                selectedTab = 3
                            } else {
                                selectedTab = previousTab
                            }
                        }
                    )
                }

                7 -> {
                    com.taytek.basehw.ui.screens.sth.SthScreen(
                        onAddCarClick = onAddCarClick,
                        onCarClick = { masterId -> onSthCarClick(masterId) },
                        contentPadding = paddingValues
                    )
                }
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
            Spacer(Modifier.size(16.dp))
            Text(
                stringResource(R.string.premium_coming_soon),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
