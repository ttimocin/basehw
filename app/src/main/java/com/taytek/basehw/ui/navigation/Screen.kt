package com.taytek.basehw.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Collection : Screen("collection")
    data object AddCar : Screen("add_car?masterId={masterId}&autoCamera={autoCamera}&deleteId={deleteId}") {
        fun createRoute(masterId: Long? = null, autoCamera: Boolean = false, deleteId: Long? = null): String {
            val masterIdPart = if (masterId != null) "masterId=$masterId" else "masterId=-1"
            val deleteIdPart = if (deleteId != null) "deleteId=$deleteId" else "deleteId=-1"
            return "add_car?$masterIdPart&autoCamera=$autoCamera&$deleteIdPart"
        }
    }
    data object CarDetail : Screen("car_detail/{carId}?fromWishlist={fromWishlist}") {
        fun createRoute(carId: Long, fromWishlist: Boolean = false) = "car_detail/$carId?fromWishlist=$fromWishlist"
    }
    data object MasterDetail : Screen("master_detail/{masterId}?fromWishlist={fromWishlist}") {
        fun createRoute(masterId: Long, fromWishlist: Boolean = false) = "master_detail/$masterId?fromWishlist=$fromWishlist"
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
    data object Community : Screen("community")
    data object UserProfile : Screen("user_profile/{uid}") {
        fun createRoute(uid: String) = "user_profile/$uid"
    }
}
