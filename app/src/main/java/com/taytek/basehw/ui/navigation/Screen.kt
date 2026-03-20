package com.taytek.basehw.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Collection : Screen("collection")
    data object AddCar : Screen("add_car?masterId={masterId}&autoCamera={autoCamera}") {
        fun createRoute(masterId: Long? = null, autoCamera: Boolean = false): String {
            val masterIdPart = if (masterId != null) "masterId=$masterId" else "masterId=-1"
            return "add_car?$masterIdPart&autoCamera=$autoCamera"
        }
    }
    data object CarDetail : Screen("car_detail/{carId}") {
        fun createRoute(carId: Long) = "car_detail/$carId"
    }
    data object MasterDetail : Screen("master_detail/{masterId}") {
        fun createRoute(masterId: Long) = "master_detail/$masterId"
    }
    data object SthMasterDetail : Screen("sth_master_detail/{masterId}") {
        fun createRoute(masterId: Long) = "sth_master_detail/$masterId"
    }
    data object PrivacyPolicy : Screen("privacy_policy")
    data object TermsOfUse : Screen("terms_of_use")
    data object FolderDetail : Screen("folder_detail/{folderId}") {
        fun createRoute(folderId: Long) = "folder_detail/$folderId"
    }
    data object AddWantedCar : Screen("add_wanted_car")
}
