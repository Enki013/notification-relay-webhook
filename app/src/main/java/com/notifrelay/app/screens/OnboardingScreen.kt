package com.notifrelay.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.notifrelay.app.BatteryOptimization
import com.notifrelay.app.NotificationAccess
import com.notifrelay.app.PreferencesManager
import com.notifrelay.app.RelayKeepAliveService

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var notificationAccessGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var batteryOptimizationsIgnored by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
    var keepAliveEnabled by remember { mutableStateOf(prefs.isKeepAliveEnabled()) }
    val isXiaomi = remember { BatteryOptimization.isXiaomiDevice() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = NotificationAccess.isGranted(context)
                batteryOptimizationsIgnored = BatteryOptimization.isIgnoring(context)
                if (notificationAccessGranted) {
                    NotificationAccess.rebindService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Notification Relay",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Forward your incoming phone notifications to any webhook, automatically.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "Initial Setup & Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Step 1: Notification Access (Required)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (notificationAccessGranted) Icons.Filled.CheckCircle else Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (notificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "1. Notification Access",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (notificationAccessGranted) "Permission granted" else "Required to capture notifications",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (notificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Grant notification access and the app reads incoming notifications as they arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!notificationAccessGranted) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { NotificationAccess.openSettings(context) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant notification access")
                        }
                    }
                }
            }

            // Step 2: Battery Optimization (Recommended)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (batteryOptimizationsIgnored) Icons.Filled.CheckCircle else Icons.Filled.BatterySaver,
                            contentDescription = null,
                            tint = if (batteryOptimizationsIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "2. Battery Optimization",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (batteryOptimizationsIgnored) "Unrestricted" else "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (batteryOptimizationsIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Allow unrestricted battery usage so forwarding continues when the screen is locked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!batteryOptimizationsIgnored) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { BatteryOptimization.openSettings(context) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disable battery restrictions")
                        }
                    }
                }
            }

            // Step 3: Xiaomi / HyperOS Auto-Start (Device-specific)
            if (isXiaomi) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "3. HyperOS Auto-Start",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Recommended for Xiaomi / HyperOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enable 'Auto-start' so the relay stays running in the background without needing to lock it in recent apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { BatteryOptimization.openXiaomiAutoStart(context) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Auto-Start settings")
                        }
                    }
                }
            }

            // Step 4: Keep Active in Background (Foreground service switch)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Keep active in background",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Runs a silent persistent notification so the system never kills the relay when cleared from recent apps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = keepAliveEnabled,
                            onCheckedChange = { enabled ->
                                keepAliveEnabled = enabled
                                prefs.setKeepAliveEnabled(enabled)
                                RelayKeepAliveService.sync(context)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onFinish,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get started")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
