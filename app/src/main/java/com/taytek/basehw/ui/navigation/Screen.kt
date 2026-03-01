package com.taytek.basehw.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Collection : Screen("collection")
    data object AddCar : Screen("add_car")
    data object CarDetail : Screen("car_detail/{carId}") {
        fun createRoute(carId: Long) = "car_detail/$carId"
    }
    data object Settings : Screen("settings")
}
