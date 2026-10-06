package com.notifrelay.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "notifrelay_prefs"
        private const val KEY_WEBHOOK_CONFIGS = "webhook_configs"
        private const val KEY_WEBHOOK_LOGS = "webhook_logs"
        private const val KEY_NOTIFICATION_QUEUE = "notification_queue"
        private const val KEY_FORWARD_MODE = "forward_mode"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
        private const val KEY_IGNORE_ONGOING = "ignore_ongoing"
        private const val KEY_IGNORE_GROUP_SUMMARY = "ignore_group_summary"
        private const val KEY_KEEP_ALIVE_ENABLED = "keep_alive_enabled"
        private const val KEY_LOCAL_HTTP_ENABLED = "local_http_enabled"
        private const val KEY_LOCAL_HTTP_PORT = "local_http_port"
        private const val KEY_LOCAL_HTTP_AUTH_ENABLED = "local_http_auth_enabled"
        private const val KEY_LOCAL_HTTP_TOKEN = "local_http_token"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val DEFAULT_LOCAL_HTTP_PORT = 8787
        private const val MAX_LOGS = 200
        private const val MAX_NOTIFICATION_HISTORY = 300
    }

    private val json = Json { ignoreUnknownKeys = true }

    // ── Webhooks ─────────────────────────────────────────────────────────────
    fun getWebhookConfigs(): List<WebhookConfig> {
        val configsJson = prefs.getString(KEY_WEBHOOK_CONFIGS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WebhookConfig>>(configsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setWebhookConfigs(configs: List<WebhookConfig>) {
        prefs.edit().putString(KEY_WEBHOOK_CONFIGS, json.encodeToString(configs)).apply()
    }

    // ── Forward filter ───────────────────────────────────────────────────────
    fun getForwardMode(): ForwardMode = ForwardMode.fromName(prefs.getString(KEY_FORWARD_MODE, null))

    fun setForwardMode(mode: ForwardMode) {
        prefs.edit().putString(KEY_FORWARD_MODE, mode.name).apply()
    }

    fun getSelectedPackages(): Set<String> =
        prefs.getStringSet(KEY_SELECTED_PACKAGES, emptySet()) ?: emptySet()

    fun setSelectedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_SELECTED_PACKAGES, packages).apply()
    }

    fun getIgnoreOngoing(): Boolean = prefs.getBoolean(KEY_IGNORE_ONGOING, true)
    fun setIgnoreOngoing(value: Boolean) { prefs.edit().putBoolean(KEY_IGNORE_ONGOING, value).apply() }

    fun getIgnoreGroupSummary(): Boolean = prefs.getBoolean(KEY_IGNORE_GROUP_SUMMARY, true)
    fun setIgnoreGroupSummary(value: Boolean) { prefs.edit().putBoolean(KEY_IGNORE_GROUP_SUMMARY, value).apply() }

    // ── Logs ─────────────────────────────────────────────────────────────────
    fun getWebhookLogs(): List<WebhookLog> {
        val logsJson = prefs.getString(KEY_WEBHOOK_LOGS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WebhookLog>>(logsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addWebhookLog(log: WebhookLog) {
        val current = getWebhookLogs().toMutableList()
        current.add(0, log)
        val trimmed = current.take(MAX_LOGS)
        prefs.edit().putString(KEY_WEBHOOK_LOGS, json.encodeToString(trimmed)).apply()
    }

    fun clearWebhookLogs() {
        prefs.edit().remove(KEY_WEBHOOK_LOGS).apply()
    }

    // ── Notification queue ──────────────────────────────────────────────────
    fun getNotificationQueue(): List<NotificationQueueItem> {
        val queueJson = prefs.getString(KEY_NOTIFICATION_QUEUE, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<NotificationQueueItem>>(queueJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun enqueueNotification(
        notification: NotificationData,
        payload: String,
        status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING,
        error: String? = null
    ): NotificationQueueItem {
        val item = NotificationQueueItem(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            notification = notification,
            payload = payload,
            status = status,
            lastError = error
        )
        saveNotificationQueue((listOf(item) + getNotificationQueue()).take(MAX_NOTIFICATION_HISTORY))
        return item
    }

    fun updateNotificationQueueItem(
        id: String,
        status: NotificationDeliveryStatus,
        error: String? = null,
        attempted: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        saveNotificationQueue(
            getNotificationQueue().map { item ->
                if (item.id != id) item else item.copy(
                    status = status,
                    attemptCount = item.attemptCount + if (attempted) 1 else 0,
                    lastAttemptAt = if (attempted) now else item.lastAttemptAt,
                    deliveredAt = if (status == NotificationDeliveryStatus.DELIVERED) now else item.deliveredAt,
                    lastError = error
                )
            }
        )
    }

    fun removeNotificationQueueItem(id: String) {
        saveNotificationQueue(getNotificationQueue().filterNot { it.id == id })
    }

    fun clearNotificationQueue() {
        prefs.edit().remove(KEY_NOTIFICATION_QUEUE).apply()
    }

    private fun saveNotificationQueue(items: List<NotificationQueueItem>) {
        prefs.edit().putString(KEY_NOTIFICATION_QUEUE, json.encodeToString(items.take(MAX_NOTIFICATION_HISTORY))).apply()
    }

    // ── Local HTTP server ────────────────────────────────────────────────────
    fun isLocalHttpEnabled(): Boolean = prefs.getBoolean(KEY_LOCAL_HTTP_ENABLED, false)
    fun setLocalHttpEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_LOCAL_HTTP_ENABLED, enabled).apply() }

    fun getLocalHttpPort(): Int {
        val port = prefs.getInt(KEY_LOCAL_HTTP_PORT, DEFAULT_LOCAL_HTTP_PORT)
        return if (port in 1024..65535) port else DEFAULT_LOCAL_HTTP_PORT
    }

    fun setLocalHttpPort(port: Int) {
        prefs.edit().putInt(KEY_LOCAL_HTTP_PORT, port.coerceIn(1024, 65535)).apply()
    }

    fun isLocalHttpAuthEnabled(): Boolean = prefs.getBoolean(KEY_LOCAL_HTTP_AUTH_ENABLED, false)
    fun setLocalHttpAuthEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_LOCAL_HTTP_AUTH_ENABLED, enabled).apply() }

    fun getLocalHttpToken(): String {
        val existing = prefs.getString(KEY_LOCAL_HTTP_TOKEN, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_LOCAL_HTTP_TOKEN, generated).apply()
        return generated
    }

    fun regenerateLocalHttpToken(): String {
        val token = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_LOCAL_HTTP_TOKEN, token).apply()
        return token
    }

    // ── Keep alive / background persistence ──────────────────────────────────
    fun isKeepAliveEnabled(): Boolean = prefs.getBoolean(KEY_KEEP_ALIVE_ENABLED, true)
    fun setKeepAliveEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_KEEP_ALIVE_ENABLED, enabled).apply() }

    // ── Onboarding / version ─────────────────────────────────────────────────
    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    fun setHasSeenOnboarding() { prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply() }

    fun getLastSeenVersionCode(): Int = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, 0)
    fun setLastSeenVersionCode(code: Int) { prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, code).apply() }

    // ── Export / import ──────────────────────────────────────────────────────
    fun exportSettings(): SettingsExport = SettingsExport(
        exportedAt = System.currentTimeMillis(),
        webhookConfigs = getWebhookConfigs(),
        forwardMode = getForwardMode().name,
        selectedPackages = getSelectedPackages().toList(),
        ignoreOngoing = getIgnoreOngoing(),
        ignoreGroupSummary = getIgnoreGroupSummary(),
        localHttpEnabled = isLocalHttpEnabled(),
        localHttpPort = getLocalHttpPort()
    )

    fun importSettings(export: SettingsExport) {
        setWebhookConfigs(export.webhookConfigs)
        setForwardMode(ForwardMode.fromName(export.forwardMode))
        setSelectedPackages(export.selectedPackages.toSet())
        setIgnoreOngoing(export.ignoreOngoing)
        setIgnoreGroupSummary(export.ignoreGroupSummary)
        setLocalHttpEnabled(export.localHttpEnabled)
        setLocalHttpPort(export.localHttpPort)
    }
}
