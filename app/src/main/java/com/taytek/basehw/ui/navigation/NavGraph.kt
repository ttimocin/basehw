package com.taytek.basehw.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.taytek.basehw.ui.navigation.Screen
import com.taytek.basehw.ui.screens.addcar.AddCarScreen
import com.taytek.basehw.ui.screens.addcar.AddWantedCarScreen
import com.taytek.basehw.ui.screens.collection.CollectionScreen
import com.taytek.basehw.ui.screens.collections.detail.FolderDetailScreen
import com.taytek.basehw.ui.screens.detail.CarDetailScreen
import com.taytek.basehw.ui.screens.legal.LegalScreen
import com.taytek.basehw.ui.screens.legal.LegalType
import com.taytek.basehw.ui.screens.main.MainScreen

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
            MainScreen(
                navigateToTab = navigateToTab,
                onConsumeTabNavigation = {
                    navBackStackEntry.savedStateHandle["navigate_to_tab"] = -1
                },
                onAddCarClick = { navController.navigate(Screen.AddCar.createRoute()) },
                onAddCarCameraClick = { navController.navigate(Screen.AddCar.createRoute(autoCamera = true)) },
                onAddWantedCarClick = { navController.navigate(Screen.AddWantedCar.route) },
                onCarClick = { carId ->
                    navController.navigate(Screen.CarDetail.createRoute(carId))
                },
                onFolderClick = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onPrivacyPolicyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                onTermsOfUseClick = { navController.navigate(Screen.TermsOfUse.route) },
                onAddCarWithMasterIdClick = { masterId ->
                    navController.navigate(Screen.MasterDetail.createRoute(masterId))
                },
                onSthCarClick = { masterId ->
                    navController.navigate(Screen.SthMasterDetail.createRoute(masterId))
                }
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
            })
        ) { backStackEntry ->
            val masterId = backStackEntry.arguments?.getLong("masterId") ?: -1L
            val autoCamera = backStackEntry.arguments?.getBoolean("autoCamera") ?: false
            AddCarScreen(
                masterDataId = masterId,
                openCameraOnLaunch = autoCamera,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = {
                    // After adding a car, go to Collection tab (tab index 8)
                    navController.getBackStackEntry(Screen.Main.route)
                        .savedStateHandle["navigate_to_tab"] = 8
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
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { backStackEntry ->
            val carId = backStackEntry.arguments?.getLong("carId") ?: -1L
            CarDetailScreen(
                carId = carId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MasterDetail.route,
            arguments = listOf(navArgument("masterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val masterId = backStackEntry.arguments?.getLong("masterId") ?: -1L
            com.taytek.basehw.ui.screens.detail.MasterDetailScreen(
                masterId = masterId,
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
    }
}
