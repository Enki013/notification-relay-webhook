package com.notifrelay.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.notifrelay.app.BatteryOptimization
import com.notifrelay.app.ForwardMode
import com.notifrelay.app.InstalledApp
import com.notifrelay.app.InstalledAppsProvider
import com.notifrelay.app.NotificationAccess
import com.notifrelay.app.NotificationRelayService
import com.notifrelay.app.PreferencesManager
import com.notifrelay.app.R
import com.notifrelay.app.RelayKeepAliveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    notificationAccessGranted: Boolean,
    onGrantNotificationAccess: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenLocalHttpSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var forwardMode by remember { mutableStateOf(prefs.getForwardMode()) }
    var selectedPackages by remember { mutableStateOf(prefs.getSelectedPackages()) }
    var ignoreOngoing by remember { mutableStateOf(prefs.getIgnoreOngoing()) }
    var ignoreGroupSummary by remember { mutableStateOf(prefs.getIgnoreGroupSummary()) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var ignoringBatteryOptimizations by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
    var keepAliveEnabled by remember { mutableStateOf(prefs.isKeepAliveEnabled()) }
    val isConnected by NotificationRelayService.isConnected.collectAsState()
    val isXiaomi = remember { BatteryOptimization.isXiaomiDevice() }

    LaunchedEffect(Unit) {
        ignoringBatteryOptimizations = BatteryOptimization.isIgnoring(context)
    }

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            "Notification Relay",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Notification access status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when {
                        !notificationAccessGranted -> Icons.Filled.Warning
                        !isConnected -> Icons.Filled.Warning
                        else -> Icons.Filled.CheckCircle
                    }
                    val statusTint = when {
                        !notificationAccessGranted -> MaterialTheme.colorScheme.error
                        !isConnected -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        tint = statusTint
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            !notificationAccessGranted -> "Notification access needed"
                            !isConnected -> "Listener disconnected"
                            else -> "Notification access granted"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        !notificationAccessGranted ->
                            "Grant notification access so the app can read and forward incoming notifications."
                        !isConnected ->
                            "Permission granted, but listener service is not connected (common on HyperOS after app restart). Tap 'Reconnect Listener' below."
                        else ->
                            "Captured notifications are forwarded to your enabled webhooks."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!notificationAccessGranted) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onGrantNotificationAccess,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant notification access")
                    }
                } else if (!isConnected) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { NotificationAccess.rebindService(context) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reconnect Listener")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ignoringBatteryOptimizations) Icons.Filled.CheckCircle else Icons.Filled.BatterySaver,
                        contentDescription = null,
                        tint = if (ignoringBatteryOptimizations) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (ignoringBatteryOptimizations) "Battery optimization unrestricted" else "Battery optimization recommended",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (ignoringBatteryOptimizations)
                        "Android is less likely to pause notification forwarding or the local server in the background."
                    else
                        "Allow unrestricted battery usage for more reliable forwarding, retries, and local HTTP server availability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!ignoringBatteryOptimizations) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { BatteryOptimization.openSettings(context) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open battery settings")
                    }
                }
            }
        }

        // Keep active in background (Foreground Service)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_keep_alive_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_keep_alive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
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

        if (isXiaomi) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Xiaomi / HyperOS Settings",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "HyperOS restricts background services. With 'Keep active in background' enabled, you do NOT need to lock the app in recent apps:\n" +
                                "1. Enable 'Auto-start'\n" +
                                "2. Set Battery saver to 'No restrictions'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { BatteryOptimization.openXiaomiAutoStart(context) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Auto-start / App settings")
                    }
                }
            }
        }

        // Forward mode
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Which notifications to forward", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = modeMenuExpanded,
                    onExpandedChange = { modeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = forwardMode.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modeMenuExpanded,
                        onDismissRequest = { modeMenuExpanded = false }
                    ) {
                        ForwardMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(mode.displayName)
                                        Text(
                                            mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    forwardMode = mode
                                    prefs.setForwardMode(mode)
                                    modeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (forwardMode != ForwardMode.ALL) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAppPicker = true },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (forwardMode == ForwardMode.ALLOWLIST)
                                "Select apps to forward (${selectedPackages.size})"
                            else
                                "Select apps to block (${selectedPackages.size})"
                        )
                    }
                }
            }
        }

        // Ignore toggles
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                ToggleRow(
                    title = "Ignore ongoing notifications",
                    subtitle = "Skip persistent/foreground-service notifications",
                    checked = ignoreOngoing
                ) {
                    ignoreOngoing = it
                    prefs.setIgnoreOngoing(it)
                }
                HorizontalDivider(Modifier.padding(vertical = 7.dp))
                ToggleRow(
                    title = "Ignore group summaries",
                    subtitle = "Skip the collapsed summary of grouped notifications",
                    checked = ignoreGroupSummary
                ) {
                    ignoreGroupSummary = it
                    prefs.setIgnoreGroupSummary(it)
                }
            }
        }

        // Entries
        NavRow(icon = Icons.Filled.Notifications, title = "Recent notifications", onClick = onOpenRecent)
        NavRow(icon = Icons.Filled.ChevronRight, title = "Local HTTP server", onClick = onOpenLocalHttpSettings)
    }

    if (showAppPicker) {
        AppPickerSheet(
            initiallySelected = selectedPackages,
            onDismiss = { showAppPicker = false },
            onConfirm = { newSelection ->
                selectedPackages = newSelection
                prefs.setSelectedPackages(newSelection)
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initiallySelected) }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.listApps(context) }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            Text("Select apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show system apps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(checked = showSystemApps, onCheckedChange = { showSystemApps = it })
            }
            Spacer(Modifier.height(8.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = remember(query, apps, showSystemApps) {
                    apps
                        .filter { showSystemApps || !it.isSystemApp }
                        .filter {
                            query.isBlank() ||
                                it.label.contains(query, true) ||
                                it.packageName.contains(query, true)
                        }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(filtered) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bmp = remember(app.packageName) {
                                runCatching { app.icon?.toBitmap(48, 48)?.asImageBitmap() }.getOrNull()
                            }
                            if (bmp != null) {
                                Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(36.dp))
                            } else {
                                Spacer(Modifier.size(36.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(
                                checked = app.packageName in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + app.packageName else selected - app.packageName
                                }
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onConfirm(selected) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save (${selected.size} selected)")
            }
        }
    }
}
