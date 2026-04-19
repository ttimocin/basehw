package com.taytek.basehw

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.taytek.basehw.ui.navigation.NavGraph
import com.taytek.basehw.ui.theme.AppThemeMode
import com.taytek.basehw.ui.theme.BaseHWTheme
import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import com.taytek.basehw.ui.components.ForceUpdateScreen
import com.taytek.basehw.ui.components.UpdateDialog
import com.taytek.basehw.data.local.AppSettingsManager
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var remoteConfig: RemoteConfigDataSource

    @Inject
    lateinit var appSettingsManager: AppSettingsManager

    private var isReady by mutableStateOf(false)
    private var lastAppliedIsDark: Boolean = true

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        var lang = prefs.getString("pref_language", "") ?: ""
        
        if (lang.isEmpty()) {
            val systemLocale = Locale.getDefault()
            val systemLang = systemLocale.language
            // Desteklenen diller listesi
            val supportedLangs = listOf("tr", "de", "fr", "ar", "es", "pt", "ru", "uk")
            lang = if (supportedLangs.contains(systemLang)) systemLang else "en"
        }
        
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val contextToUse = newBase.createConfigurationContext(config)
        
        super.attachBaseContext(contextToUse)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        applySystemBars(resolveDarkUiForSystemBars())

        // Keep native splash screen until app is ready
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Custom exit animation for a premium feel
        splashScreen.setOnExitAnimationListener { splashScreenProvider ->
            val iconView = splashScreenProvider.iconView
            
            // Icon scale up animation (zoom effect)
            ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.3f).apply {
                duration = 600
                interpolator = AccelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.3f).apply {
                duration = 600
                interpolator = AccelerateInterpolator()
                start()
            }
            
            // Fade out animation
            ObjectAnimator.ofFloat(splashScreenProvider.view, View.ALPHA, 1f, 0f).apply {
                duration = 600
                interpolator = AccelerateInterpolator()
                doOnEnd { splashScreenProvider.remove() }
                start()
            }
        }



        val currentVersionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }

        setContent {
            val themeState by appSettingsManager.themeFlow.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (AppThemeMode.fromPreference(themeState)) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK, AppThemeMode.CYBER, AppThemeMode.NEON_CYAN -> true
                AppThemeMode.SYSTEM -> isSystemDark
            }

            // Match in-app theme (same source as [AppSettingsManager.themeFlow]).
            DisposableEffect(isDark) {
                applySystemBars(isDark)
                onDispose {}
            }

            // SplashScreen switches to postSplashScreenTheme when dismissed; that re-applies XML
            // windowLightStatusBar and can reset icon color. Re-apply bars once content is shown.
            LaunchedEffect(isReady, isDark) {
                if (isReady) {
                    applySystemBars(isDark)
                }
            }

            val fontFamilyState by appSettingsManager.fontFlow.collectAsState()
            val context = LocalContext.current
            
            BaseHWTheme(themeState = themeState, fontFamilyState = fontFamilyState) {
                var minVersion by remember { mutableStateOf(remoteConfig.getMinVersionName()) }
                var latestVersion by remember { mutableStateOf(remoteConfig.getLatestVersionName()) }
                var updateUrl by remember { mutableStateOf(remoteConfig.getUpdateUrl()) }
                var showOptionalUpdate by remember { mutableStateOf(false) }

                val configUpdate by remoteConfig.configUpdated.collectAsState()
                LaunchedEffect(configUpdate) {
                    minVersion = remoteConfig.getMinVersionName()
                    latestVersion = remoteConfig.getLatestVersionName()
                    updateUrl = remoteConfig.getUpdateUrl()
                    if (isVersionOlder(currentVersionName, latestVersion)) {
                        showOptionalUpdate = true
                    }
                }

                // Signal that the app is ready after initial initialization
                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
                    
                    if (isFirstLaunch) {
                        // First launch: Give it some time to show the logo
                        kotlinx.coroutines.delay(1500) 
                        prefs.edit().putBoolean("is_first_launch", false).apply()
                    } else {
                        // Regular launch: Small delay to ensure compose stabilization
                        kotlinx.coroutines.delay(600)
                    }
                    isReady = true
                }

                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isVersionOlder(currentVersionName, minVersion)) {
                        ForceUpdateScreen(updateUrl = updateUrl)
                    } else {
                        NavGraph(navController = navController)
                        
                        if (showOptionalUpdate) {
                            UpdateDialog(
                                updateUrl = updateUrl,
                                versionName = latestVersion,
                                onDismiss = { showOptionalUpdate = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Some OEM/launcher transitions can override system bar icon appearance; re-apply safely.
        applySystemBars(lastAppliedIsDark)
    }

    override fun onPostResume() {
        super.onPostResume()
        // After splash / theme transition, XML window flags may win; prefs-based bars again.
        applySystemBars(resolveDarkUiForSystemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBars(resolveDarkUiForSystemBars())
        }
    }

    /** Same rules as Compose [BaseHWTheme] / theme flow: prefs + system night for SYSTEM mode. */
    private fun resolveDarkUiForSystemBars(): Boolean {
        val themePrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedTheme = themePrefs.getInt("pref_theme", 2)
        val systemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return when (AppThemeMode.fromPreference(savedTheme)) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK, AppThemeMode.CYBER, AppThemeMode.NEON_CYAN -> true
            AppThemeMode.SYSTEM -> systemDark
        }
    }

    private fun applySystemBars(isDark: Boolean) {
        lastAppliedIsDark = isDark

        val themeMode = AppThemeMode.fromPreference(
            getSharedPreferences("app_settings", Context.MODE_PRIVATE).getInt("pref_theme", 2)
        )
        // Cyber: dark purple top + bright bottom gradient — status needs light icons, nav needs dark icons.
        val cyberBrightBottomNav = themeMode == AppThemeMode.CYBER && isDark

        val transparentLight = SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        )
        val transparentDark = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)

        enableEdgeToEdge(
            statusBarStyle = if (isDark) transparentDark else transparentLight,
            navigationBarStyle = when {
                !isDark -> transparentLight
                cyberBrightBottomNav -> transparentLight
                else -> transparentDark
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        fun applyInsets() {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = when {
                    !isDark -> true
                    cyberBrightBottomNav -> true
                    else -> false
                }
            }
        }
        applyInsets()
        window.decorView.post { applyInsets() }
        window.decorView.postDelayed({ applyInsets() }, 80)
    }

    private fun isVersionOlder(current: String, target: String): Boolean {
        if (current == target) return false
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = target.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(currentParts.size, targetParts.size)
        for (i in 0 until length) {
            val curr = currentParts.getOrElse(i) { 0 }
            val targ = targetParts.getOrElse(i) { 0 }
            if (curr < targ) return true
            if (curr > targ) return false
        }
        return false
    }
}
