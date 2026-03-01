package com.taytek.basehw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.taytek.basehw.ui.navigation.NavGraph
import com.taytek.basehw.ui.theme.BaseHWTheme
import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import com.taytek.basehw.ui.components.ForceUpdateScreen
import com.taytek.basehw.ui.components.UpdateDialog
import androidx.compose.runtime.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var remoteConfig: RemoteConfigDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val currentVersionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }

        setContent {
            BaseHWTheme {
                val minVersion by remember { mutableStateOf(remoteConfig.getMinVersionName()) }
                val latestVersion by remember { mutableStateOf(remoteConfig.getLatestVersionName()) }
                val updateUrl by remember { mutableStateOf(remoteConfig.getUpdateUrl()) }
                
                var showOptionalUpdate by remember { mutableStateOf(isVersionOlder(currentVersionName, latestVersion)) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isVersionOlder(currentVersionName, minVersion)) {
                        ForceUpdateScreen(updateUrl = updateUrl)
                    } else {
                        val navController = rememberNavController()
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

    /**
     * Compares two semantic versions (e.g., "1.0.0" and "1.0.1").
     * Returns true if [current] is older than [target].
     */
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