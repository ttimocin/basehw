package com.taytek.basehw.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.ui.navigation.Screen
import com.taytek.basehw.ui.screens.addcar.AddCarScreen
import com.taytek.basehw.ui.screens.addcar.AddWantedCarScreen
import com.taytek.basehw.ui.screens.collection.CollectionScreen
import com.taytek.basehw.ui.screens.collections.detail.FolderDetailScreen
import com.taytek.basehw.ui.screens.detail.CarDetailScreen
import com.taytek.basehw.ui.screens.legal.LegalScreen
import com.taytek.basehw.ui.screens.legal.LegalType
import com.taytek.basehw.ui.screens.main.MainScreen
import com.taytek.basehw.ui.screens.profile.ProfileViewModel
import com.taytek.basehw.ui.screens.profile.ProfileEditScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) { navBackStackEntry ->
            val navigateToTab by navBackStackEntry.savedStateHandle
                .getStateFlow("navigate_to_tab", -1)
                .collectAsState()
            val wishlistTab by navBackStackEntry.savedStateHandle
                .getStateFlow("wishlist_tab", -1)
                .collectAsState()
            MainScreen(
                navController = navController,
                navigateToTab = navigateToTab,
                wishlistTab = wishlistTab,
                onConsumeTabNavigation = {
                    navBackStackEntry.savedStateHandle["navigate_to_tab"] = -1
                },
                onConsumeWishlistTab = {
                    navBackStackEntry.savedStateHandle["wishlist_tab"] = -1
                },
                onAddCarClick = { navController.navigate(Screen.AddCar.createRoute()) },
                onAddCarCameraClick = { navController.navigate(Screen.AddCar.createRoute(autoCamera = true)) },
                onAddWantedCarClick = { navController.navigate(Screen.AddWantedCar.route) },
                onCarClick = { carId, fromWishlist ->
                    navController.navigate(Screen.CarDetail.createRoute(carId, fromWishlist))
                },
                onFolderClick = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onPrivacyPolicyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                onTermsOfUseClick = { navController.navigate(Screen.TermsOfUse.route) },
                onForumRulesClick = { navController.navigate(Screen.CommunityRules.route) },
                onAddCarWithMasterIdClick = { masterId, fromWishlist ->
                    navController.navigate(Screen.MasterDetail.createRoute(masterId, fromWishlist))
                },
                onAddCarWithMasterIdAndDeleteClick = { masterId, deleteId, fromWishlist ->
                    if (deleteId != null && deleteId != -1L) {
                        navController.navigate(Screen.CarDetail.createRoute(deleteId, fromWishlist))
                    } else {
                        navController.navigate(Screen.MasterDetail.createRoute(masterId, fromWishlist))
                    }
                },
                onSthCarClick = { masterId ->
                    navController.navigate(Screen.SthMasterDetail.createRoute(masterId))
                },
                onCommunityClick = { navController.navigate(Screen.Community.route) },
                onInboxClick = { navController.navigate(Screen.DirectInbox.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onUserProfileClick = { uid ->
                    navController.navigate(Screen.UserProfile.createRoute(uid))
                },
                onEditProfileClick = {
                    navController.navigate(Screen.ProfileEdit.route)
                },
                onDirectMessageClick = { targetUid, username ->
                    val encoded = URLEncoder.encode(username, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.DirectMessage.createRoute(targetUid, encoded))
                },
                onAdminPanelClick = {
                    navController.navigate(Screen.AdminPanel.route)
                }
            )
        }

        composable(Screen.Community.route) {
            com.taytek.basehw.ui.screens.community.CommunityScreen(
                onUserProfileClick = { uid ->
                    navController.navigate(Screen.UserProfile.createRoute(uid))
                },
                onInboxClick = {
                    navController.navigate(Screen.DirectInbox.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Main.route)
                },
                onNavigateToAdminPanel = {
                    navController.navigate(Screen.AdminPanel.route)
                },
                onLoginClick = {
                    navController.popBackStack(Screen.Main.route, false)
                    navController.getBackStackEntry(Screen.Main.route)
                        .savedStateHandle["navigate_to_tab"] = 2
                },
                onRanksClick = {
                    navController.navigate(Screen.Ranks.route)
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        composable(Screen.AdminPanel.route) {
            com.taytek.basehw.ui.screens.community.AdminPanelRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DirectInbox.route) {
            com.taytek.basehw.ui.screens.community.DirectInboxScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenConversation = { targetUid, username ->
                    val encoded = URLEncoder.encode(username, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.DirectMessage.createRoute(targetUid, encoded))
                }
            )
        }

        composable(Screen.Notifications.route) {
            com.taytek.basehw.ui.screens.community.NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUser = { uid ->
                    navController.navigate(Screen.UserProfile.createRoute(uid))
                }
            )
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            com.taytek.basehw.ui.screens.community.UserProfileScreen(
                userId = uid,
                onNavigateBack = { navController.popBackStack() },
                onEditProfileClick = {
                    navController.navigate(Screen.ProfileEdit.route)
                },
                onMessageClick = { targetUid, username ->
                    val encoded = URLEncoder.encode(username, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.DirectMessage.createRoute(targetUid, encoded))
                },
                onInboxClick = {
                    navController.navigate(Screen.DirectInbox.route)
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onAdminPanelClick = {
                    navController.navigate(Screen.AdminPanel.route)
                },
                onStatsClick = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }

        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DirectMessage.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            val usernameEncoded = backStackEntry.arguments?.getString("username") ?: "User"
            val username = URLDecoder.decode(usernameEncoded, StandardCharsets.UTF_8.toString())
            com.taytek.basehw.ui.screens.community.DirectMessageScreen(
                peerUid = uid,
                peerUsername = username,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddCar.route,
            arguments = listOf(navArgument("masterId") {
                type = NavType.LongType
                defaultValue = -1L
            }, navArgument("autoCamera") {
                type = NavType.BoolType
                defaultValue = false
            }, navArgument("deleteId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val masterId = backStackEntry.arguments?.getLong("masterId") ?: -1L
            val autoCamera = backStackEntry.arguments?.getBoolean("autoCamera") ?: false
            val deleteId = backStackEntry.arguments?.getLong("deleteId") ?: -1L
            AddCarScreen(
                masterDataId = masterId,
                openCameraOnLaunch = autoCamera,
                deleteId = deleteId,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { isWishlist, isSeries ->
                    if (isWishlist) {
                        navController.getBackStackEntry(Screen.Main.route)
                            .savedStateHandle["navigate_to_tab"] = 1
                        navController.getBackStackEntry(Screen.Main.route)
                            .savedStateHandle["wishlist_tab"] = if (isSeries) 1 else 0
                    } else {
                        navController.getBackStackEntry(Screen.Main.route)
                            .savedStateHandle["navigate_to_tab"] = 8
                    }
                    navController.popBackStack(Screen.Main.route, false)
                }
            )
        }

        composable(Screen.AddWantedCar.route) {
            AddWantedCarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CarDetail.route,
            arguments = listOf(
                navArgument("carId") { type = NavType.LongType },
                navArgument("fromWishlist") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val carId = backStackEntry.arguments?.getLong("carId") ?: -1L
            val fromWishlist = backStackEntry.arguments?.getBoolean("fromWishlist") ?: false
            CarDetailScreen(
                carId = carId,
                fromWishlist = fromWishlist,
                onNavigateBack = { navController.popBackStack() },
                onMoveToCollection = { masterId, deleteId ->
                    navController.navigate(Screen.AddCar.createRoute(masterId = masterId, deleteId = deleteId))
                }
            )
        }

        composable(
            route = Screen.MasterDetail.route,
            arguments = listOf(
                navArgument("masterId") { type = NavType.LongType },
                navArgument("fromWishlist") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val masterId = backStackEntry.arguments?.getLong("masterId") ?: -1L
            val fromWishlist = backStackEntry.arguments?.getBoolean("fromWishlist") ?: false
            com.taytek.basehw.ui.screens.detail.MasterDetailScreen(
                masterId = masterId,
                fromWishlist = fromWishlist,
                onNavigateBack = { navController.popBackStack() },
                onAddCarClick = { id ->
                    navController.navigate(Screen.AddCar.createRoute(id))
                },
                onNavigateToWishlist = {
                    navController.getBackStackEntry(Screen.Main.route)
                        .savedStateHandle["navigate_to_tab"] = 1
                    navController.popBackStack(Screen.Main.route, false)
                }
            )
        }


        composable(
            route = Screen.SthMasterDetail.route,
            arguments = listOf(navArgument("masterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val masterId = backStackEntry.arguments?.getLong("masterId") ?: -1L
            com.taytek.basehw.ui.screens.detail.MasterDetailScreen(
                masterId = masterId,
                fromSth = true,
                onNavigateBack = { navController.popBackStack() },
                onAddCarClick = { id ->
                    navController.navigate(Screen.AddCar.createRoute(id))
                },
                onNavigateToWishlist = {
                    navController.getBackStackEntry(Screen.Main.route)
                        .savedStateHandle["navigate_to_tab"] = 1
                    navController.popBackStack(Screen.Main.route, false)
                }
            )
        }


        composable(Screen.PrivacyPolicy.route) {
            LegalScreen(
                type = LegalType.PRIVACY_POLICY,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TermsOfUse.route) {
            LegalScreen(
                type = LegalType.TERMS_OF_USE,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CommunityRules.route) {
            LegalScreen(
                type = LegalType.COMMUNITY_RULES,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: -1L
            FolderDetailScreen(
                folderId = folderId,
                onCarClick = { carId ->
                    navController.navigate(Screen.CarDetail.createRoute(carId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            com.taytek.basehw.ui.screens.statistics.StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Ranks.route) {
            com.taytek.basehw.ui.screens.community.RanksScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NewsDetail.route,
            arguments = listOf(navArgument("newsId") { type = NavType.StringType })
        ) {
            com.taytek.basehw.ui.screens.news.NewsDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
