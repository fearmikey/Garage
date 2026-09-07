package com.fearmikey.garage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.ui.navigation.GarageNavHost
import com.fearmikey.garage.ui.theme.GarageTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)
        setContent {
            val themeType by mainViewModel.themeType.collectAsStateWithLifecycle()
            val pendingDeepLink by mainViewModel.pendingDeepLink.collectAsStateWithLifecycle()
            val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeType) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            GarageTheme(darkTheme = isDarkTheme) {
                GarageNavHost(
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkHandled = mainViewModel::clearPendingDeepLink,
                    isOnboardingCompleted = isOnboardingCompleted,
                )
            }
        }
    }

    // Handles the case where the activity is already running (e.g. app was already
    // open when a widget button was tapped) so the deep link still gets processed.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent) {
        mainViewModel.handleDeepLinkIntent(
            intent.action,
            intent.getLongExtra(EXTRA_VEHICLE_ID, NO_VEHICLE_ID_EXTRA),
        )
    }

    companion object {
        const val ACTION_LOG_SERVICE = "com.fearmikey.garage.action.LOG_SERVICE"
        const val ACTION_LOG_FUEL = "com.fearmikey.garage.action.LOG_FUEL"
        const val EXTRA_VEHICLE_ID = "vehicleId"
        const val NO_VEHICLE_ID_EXTRA = -1L
    }
}
