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
        
        val currentVersionCode = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: Exception) { 1 }

        setContent {
            BaseHWTheme {
                val minVersion by remember { mutableIntStateOf(remoteConfig.getMinVersionCode()) }
                val latestVersion by remember { mutableIntStateOf(remoteConfig.getLatestVersionCode()) }
                val updateUrl by remember { mutableStateOf(remoteConfig.getUpdateUrl()) }
                
                var showOptionalUpdate by remember { mutableStateOf(currentVersionCode < latestVersion) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentVersionCode < minVersion) {
                        ForceUpdateScreen(updateUrl = updateUrl)
                    } else {
                        val navController = rememberNavController()
                        NavGraph(navController = navController)
                        
                        if (showOptionalUpdate) {
                            UpdateDialog(
                                updateUrl = updateUrl,
                                versionName = remoteConfig.getLatestVersionName(),
                                onDismiss = { showOptionalUpdate = false }
                            )
                        }
                    }
                }
            }
        }
    }
}