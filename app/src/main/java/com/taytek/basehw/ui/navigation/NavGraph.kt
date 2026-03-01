package com.taytek.basehw.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taytek.basehw.ui.screens.addcar.AddCarScreen
import com.taytek.basehw.ui.screens.collection.CollectionScreen
import com.taytek.basehw.ui.screens.detail.CarDetailScreen
import com.taytek.basehw.ui.screens.main.MainScreen
import com.taytek.basehw.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onAddCarClick = { navController.navigate(Screen.AddCar.route) },
                onCarClick = { carId ->
                    navController.navigate(Screen.CarDetail.createRoute(carId))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.AddCar.route) {
            AddCarScreen(
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
