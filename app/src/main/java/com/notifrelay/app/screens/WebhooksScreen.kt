package com.notifrelay.app.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notifrelay.app.NotificationDelivery
import com.notifrelay.app.PreferencesManager
import com.notifrelay.app.WebhookConfig
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhooksScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var configs by remember { mutableStateOf(prefs.getWebhookConfigs()) }
    var showAdd by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var newUrl by remember { mutableStateOf("") }

    fun persist(updated: List<WebhookConfig>) {
        configs = updated
        prefs.setWebhookConfigs(updated)
    }

    fun sendTest(config: WebhookConfig) {
        coroutineScope.launch {
            val result = NotificationDelivery.sendPayload(
                context = context,
                payload = NotificationDelivery.buildTestPayload(),
                sourcePackage = "com.notifrelay.app",
                configs = listOf(config.copy(isEnabled = true))
            )
            val error = result.exceptionOrNull()?.let {
                it.message?.takeIf { message -> message.isNotBlank() }
                    ?: it.javaClass.simpleName
            }
            snackbarHostState.showSnackbar(
                if (result.isSuccess) "Test notification sent" else "Test failed: $error"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Webhooks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            WebhookPanel(
                configs = configs,
                newUrl = newUrl,
                onNewUrlChange = { newUrl = it },
                onAdd = {
                    showAdd = true
                },
                onToggle = { index, enabled ->
                    val config = configs[index]
                    persist(configs.toMutableList().also { it[index] = config.copy(isEnabled = enabled) })
                },
                onTest = { sendTest(it) },
                onEdit = { editingIndex = it },
                onDelete = { index ->
                    persist(configs.toMutableList().also { it.removeAt(index) })
                }
            )
            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showAdd) {
        WebhookSheet(
            title = "Add webhook",
            actionLabel = "Add",
            initialUrl = newUrl,
            onDismiss = { showAdd = false },
            onSave = { url, headers, enabled ->
                persist(configs + WebhookConfig(url = url.trim(), headers = headers, isEnabled = enabled))
                newUrl = ""
                showAdd = false
            }
        )
    }
    editingIndex?.let { index ->
        val config = configs.getOrNull(index)
        if (config != null) {
            WebhookSheet(
                title = "Edit webhook",
                actionLabel = "Save",
                initialUrl = config.url,
                initialHeaders = config.headers,
                initialEnabled = config.isEnabled,
                onDismiss = { editingIndex = null },
                onSave = { url, headers, enabled ->
                    persist(configs.toMutableList().also {
                        it[index] = config.copy(url = url.trim(), headers = headers, isEnabled = enabled)
                    })
                    editingIndex = null
                }
            )
        }
    }
}

@Composable
private fun WebhookPanel(
    configs: List<WebhookConfig>,
    newUrl: String,
    onNewUrlChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onTest: (WebhookConfig) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(
                "Webhook URLs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (configs.isEmpty()) {
                Text(
                    "No endpoints configured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                configs.forEachIndexed { index, config ->
                    WebhookRow(
                        config = config,
                        onToggle = { onToggle(index, it) },
                        onTest = { onTest(config) },
                        onEdit = { onEdit(index) },
                        onDelete = { onDelete(index) }
                    )
                    if (index != configs.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

            CompactFormField(
                label = "New webhook URL",
                value = newUrl,
                onValueChange = onNewUrlChange,
                placeholder = "https://example.com/hook",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onAdd,
                enabled = newUrl.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Add Webhook")
            }
        }
    }
}

@Composable
private fun WebhookRow(
    config: WebhookConfig,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                config.url,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (config.headers.isEmpty()) "No custom headers" else "${config.getHeaderCount()} headers configured",
                style = MaterialTheme.typography.bodySmall,
                color = if (config.headers.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        Switch(checked = config.isEnabled, onCheckedChange = onToggle)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Webhook actions")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Test") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onTest()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private data class HeaderDraft(val key: String, val value: String)

private fun List<HeaderDraft>.toHeaderMap(): Map<String, String> =
    mapNotNull { draft ->
        val key = draft.key.trim()
        if (key.isBlank()) null else key to draft.value.trim()
    }.toMap()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebhookSheet(
    title: String,
    actionLabel: String,
    initialUrl: String = "",
    initialHeaders: Map<String, String> = emptyMap(),
    initialEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (String, Map<String, String>, Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf(initialUrl) }
    var enabled by remember { mutableStateOf(initialEnabled) }
    var manageHeaders by remember { mutableStateOf(initialHeaders.isNotEmpty()) }
    var headerDrafts by remember {
        mutableStateOf(
            initialHeaders.map { (key, value) -> HeaderDraft(key = key, value = value) }
        )
    }
    var headerTab by remember { mutableStateOf(0) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var jsonPaste by remember { mutableStateOf("") }
    var testInProgress by remember { mutableStateOf(false) }
    val currentHeaders = if (manageHeaders) headerDrafts.toHeaderMap() else emptyMap()
    val urlValid = url.trim().let { it.startsWith("http://") || it.startsWith("https://") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = title,
                enabled = enabled,
                onEnabledChange = { enabled = it }
            )

            SheetSection(
                title = "Request",
                subtitle = "POST JSON"
            ) {
                CompactFormField(
                    label = "Webhook URL",
                    value = url,
                    onValueChange = { url = it },
                    placeholder = "https://example.com/hook",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = url.isNotBlank() && !urlValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SheetSection(
                title = "Headers",
                trailing = {
                    Switch(
                        checked = manageHeaders,
                        onCheckedChange = {
                            manageHeaders = it
                            if (!it) {
                                headerDrafts = emptyList()
                                newKey = ""
                                newValue = ""
                                jsonPaste = ""
                            }
                        }
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (manageHeaders) {
                        if (headerDrafts.isEmpty()) {
                            Text(
                                "No headers configured.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            headerDrafts.forEachIndexed { index, draft ->
                                HeaderEditorRow(
                                    keyValue = draft.key,
                                    headerValue = draft.value,
                                    onKeyChange = { value ->
                                        headerDrafts = headerDrafts.toMutableList().also {
                                            it[index] = draft.copy(key = value)
                                        }
                                    },
                                    onValueChange = { value ->
                                        headerDrafts = headerDrafts.toMutableList().also {
                                            it[index] = draft.copy(value = value)
                                        }
                                    },
                                    onRemove = {
                                        headerDrafts = headerDrafts.toMutableList().also { it.removeAt(index) }
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        TabRow(
                            selectedTabIndex = headerTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                        ) {
                            Tab(selected = headerTab == 0, onClick = { headerTab = 0 }) {
                                Text(
                                    "Form",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Tab(selected = headerTab == 1, onClick = { headerTab = 1 }) {
                                Text(
                                    "JSON",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        if (headerTab == 0) {
                            HeaderInputRow(
                                keyValue = newKey,
                                headerValue = newValue,
                                onKeyChange = { newKey = it },
                                onValueChange = { newValue = it }
                            )
                            OutlinedButton(
                                onClick = {
                                    if (newKey.isNotBlank()) {
                                        headerDrafts = headerDrafts + HeaderDraft(newKey.trim(), newValue.trim())
                                        newKey = ""
                                        newValue = ""
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("Add Header")
                            }
                        } else {
                            OutlinedTextField(
                                value = jsonPaste,
                                onValueChange = { jsonPaste = it },
                                label = { Text("Headers JSON") },
                                placeholder = {
                                    Text(
                                        "{\"Authorization\": \"Bearer token\"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                minLines = 3,
                                maxLines = 6,
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val obj = JSONObject(jsonPaste.trim())
                                        val parsed = mutableListOf<HeaderDraft>()
                                        obj.keys().forEach { key ->
                                            parsed.add(HeaderDraft(key, obj.getString(key)))
                                        }
                                        headerDrafts = headerDrafts + parsed
                                        jsonPaste = ""
                                        headerTab = 0
                                    }.onFailure {
                                        Toast.makeText(context, "Invalid headers JSON", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("Apply JSON")
                            }
                        }
                    }
                }
            }

            SheetActions(
                actionLabel = actionLabel,
                testInProgress = testInProgress,
                testEnabled = urlValid && !testInProgress,
                saveEnabled = url.isNotBlank(),
                onTest = {
                    testInProgress = true
                    coroutineScope.launch {
                        val result = NotificationDelivery.sendPayload(
                            context = context,
                            payload = NotificationDelivery.buildTestPayload(),
                            sourcePackage = "com.notifrelay.app",
                            configs = listOf(
                                WebhookConfig(
                                    url = url.trim(),
                                    headers = currentHeaders,
                                    isEnabled = true
                                )
                            )
                        )
                        testInProgress = false
                        val error = result.exceptionOrNull()?.let {
                            it.message?.takeIf { message -> message.isNotBlank() }
                                ?: it.javaClass.simpleName
                        }
                        Toast.makeText(
                            context,
                            if (result.isSuccess) "Test notification sent" else "Test failed: $error",
                            if (result.isSuccess) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onSave = {
                    if (!urlValid) {
                        Toast.makeText(context, "Enter a valid http(s) URL", Toast.LENGTH_SHORT).show()
                        return@SheetActions
                    }
                    onSave(url, currentHeaders, enabled)
                }
            )
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                if (enabled) "Active" else "Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun SheetSection(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun HeaderEditorRow(
    keyValue: String,
    headerValue: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderInputRow(
                keyValue = keyValue,
                headerValue = headerValue,
                onKeyChange = onKeyChange,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove header",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeaderInputRow(
    keyValue: String,
    headerValue: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactHeaderField(
            label = "Key",
            value = keyValue,
            onValueChange = onKeyChange,
            modifier = Modifier.weight(0.42f)
        )
        CompactHeaderField(
            label = "Value",
            value = headerValue,
            onValueChange = onValueChange,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun CompactHeaderField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactFormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodySmall,
        height = 36.dp,
        modifier = modifier
    )
}

@Composable
private fun CompactFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
    height: androidx.compose.ui.unit.Dp = 38.dp
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth().height(height)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank() && placeholder.isNotBlank()) {
                            Text(
                                placeholder,
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SheetActions(
    actionLabel: String,
    testInProgress: Boolean,
    testEnabled: Boolean,
    saveEnabled: Boolean,
    onTest: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onTest,
            enabled = testEnabled,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            if (testInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text("Test")
        }

        Button(
            onClick = onSave,
            enabled = saveEnabled,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            Text(actionLabel)
        }
    }
}
