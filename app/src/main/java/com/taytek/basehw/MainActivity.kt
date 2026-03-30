package com.taytek.basehw

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.taytek.basehw.ui.navigation.NavGraph
import com.taytek.basehw.ui.theme.BaseHWTheme
import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import com.taytek.basehw.ui.components.ForceUpdateScreen
import com.taytek.basehw.ui.components.UpdateDialog
import com.taytek.basehw.data.local.AppSettingsManager
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var remoteConfig: RemoteConfigDataSource

    @Inject
    lateinit var appSettingsManager: AppSettingsManager

    private var isReady = false

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

        // Keep splash visible until isReady = true
        splashScreen.setKeepOnScreenCondition { !isReady }

        enableEdgeToEdge()
        
        val currentVersionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }

        setContent {
            val themeState by appSettingsManager.themeFlow.collectAsState()
            val languageState by appSettingsManager.languageFlow.collectAsState()
            val context = LocalContext.current
            
            BaseHWTheme(themeState = themeState) {
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

                // Signal that the app is ready after initial composition stabilizes
                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
                    
                    if (isFirstLaunch) {
                        // Wait long enough for Firebase config + DB creation to settle on first install
                        kotlinx.coroutines.delay(2500)
                        prefs.edit().putBoolean("is_first_launch", false).apply()
                    } else {
                        // Very brief delay for stability on subsequent launches
                        kotlinx.coroutines.delay(800)
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