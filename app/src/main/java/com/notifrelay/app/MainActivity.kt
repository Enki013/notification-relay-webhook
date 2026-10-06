package com.notifrelay.app

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.notifrelay.app.screens.AboutScreen
import com.notifrelay.app.screens.ConfigurationScreen
import com.notifrelay.app.screens.LocalHttpSettingsScreen
import com.notifrelay.app.screens.LogsScreen
import com.notifrelay.app.screens.OnboardingScreen
import com.notifrelay.app.screens.RecentNotificationsScreen
import com.notifrelay.app.screens.SettingsBackupScreen
import com.notifrelay.app.screens.WebhooksScreen
import com.notifrelay.app.screens.WhatsNewSheet
import com.notifrelay.app.ui.theme.NotifRelayTheme

class MainActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private val notificationAccessGranted = mutableStateOf(false)
    internal val openLocalHttpRequest = mutableStateOf(false)

    private val postNotificationsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(this)
        val accessGranted = NotificationAccess.isGranted(this)
        notificationAccessGranted.value = accessGranted
        if (accessGranted) {
            NotificationAccess.rebindService(this)
        }
        RelayKeepAliveService.sync(this)
        maybeRequestPostNotifications()

        if (intent?.getBooleanExtra(LocalHttpServerService.EXTRA_OPEN_LOCAL_HTTP, false) == true) {
            openLocalHttpRequest.value = true
        }

        setContent {
            NotifRelayTheme {
                var showOnboarding by remember { mutableStateOf(!preferencesManager.hasSeenOnboarding()) }
                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        preferencesManager.setHasSeenOnboarding()
                        showOnboarding = false
                    })
                } else {
                    MainScreenWithNav(onRestartOnboarding = { showOnboarding = true })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val accessGranted = NotificationAccess.isGranted(this)
        notificationAccessGranted.value = accessGranted
        if (accessGranted && !NotificationAccess.isConnected()) {
            NotificationAccess.rebindService(this)
        }
        RelayKeepAliveService.sync(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(LocalHttpServerService.EXTRA_OPEN_LOCAL_HTTP, false)) {
            openLocalHttpRequest.value = true
        }
    }

    private fun maybeRequestPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Composable
    private fun MainScreenWithNav(onRestartOnboarding: () -> Unit) {
        var selectedScreen by remember { mutableStateOf<NavigationScreen>(NavigationScreen.Home) }
        var showLocalHttpSettings by remember { mutableStateOf(false) }
        var showSettingsBackup by remember { mutableStateOf(false) }
        var showRecent by remember { mutableStateOf(false) }
        var showWhatsNew by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val currentCode = BuildConfig.VERSION_CODE
            val lastSeen = preferencesManager.getLastSeenVersionCode()
            when {
                lastSeen == 0 -> preferencesManager.setLastSeenVersionCode(currentCode)
                currentCode > lastSeen -> showWhatsNew = true
            }
        }

        LaunchedEffect(openLocalHttpRequest.value) {
            if (openLocalHttpRequest.value) {
                showLocalHttpSettings = true
                openLocalHttpRequest.value = false
            }
        }

        val accessGranted by notificationAccessGranted

        Scaffold(
            bottomBar = {
                if (!showLocalHttpSettings && !showSettingsBackup && !showRecent) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleResId)) },
                                label = { Text(stringResource(screen.titleResId)) },
                                selected = selectedScreen == screen,
                                onClick = { selectedScreen = screen }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            BackHandler {
                when {
                    showLocalHttpSettings -> showLocalHttpSettings = false
                    showSettingsBackup -> showSettingsBackup = false
                    showRecent -> showRecent = false
                    selectedScreen != NavigationScreen.Home -> selectedScreen = NavigationScreen.Home
                    else -> finish()
                }
            }
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier = Modifier.padding(padding)) {
                when {
                    showLocalHttpSettings -> LocalHttpSettingsScreen(onBack = { showLocalHttpSettings = false })
                    showSettingsBackup -> SettingsBackupScreen(onBack = { showSettingsBackup = false })
                    showRecent -> RecentNotificationsScreen(onBack = { showRecent = false })
                    else -> saveableStateHolder.SaveableStateProvider(selectedScreen.route) {
                        when (selectedScreen) {
                            is NavigationScreen.Home -> ConfigurationScreen(
                                notificationAccessGranted = accessGranted,
                                onGrantNotificationAccess = { NotificationAccess.openSettings(this@MainActivity) },
                                onOpenRecent = { showRecent = true },
                                onOpenLocalHttpSettings = { showLocalHttpSettings = true }
                            )
                            is NavigationScreen.Webhooks -> WebhooksScreen()
                            is NavigationScreen.Logs -> LogsScreen()
                            is NavigationScreen.About -> AboutScreen(
                                onRestartOnboarding = onRestartOnboarding,
                                onOpenLocalHttpSettings = { showLocalHttpSettings = true },
                                onOpenSettingsBackup = { showSettingsBackup = true }
                            )
                        }
                    }
                }
            }
        }

        WhatsNewSheet(
            visible = showWhatsNew,
            onDismiss = {
                showWhatsNew = false
                preferencesManager.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            }
        )
    }
}
