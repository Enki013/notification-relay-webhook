package com.notifrelay.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/**
 * Helpers for the notification-listener access grant (Settings > Notification access).
 */
object NotificationAccess {

    private const val TAG = "NotificationAccess"

    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun isConnected(): Boolean = NotificationRelayService.isConnected.value

    /**
     * Forces the Android system to reconnect / rebind to our NotificationListenerService.
     * Essential on HyperOS / MIUI and after app restarts where the system drops the binder connection.
     */
    fun rebindService(context: Context) {
        if (!isGranted(context)) return

        val componentName = ComponentName(context, NotificationRelayService::class.java)

        // 1. Android N+ (API 24+) requestRebind
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                NotificationListenerService.requestRebind(componentName)
                Log.i(TAG, "Requested rebind via NotificationListenerService.requestRebind")
            }.onFailure { e ->
                Log.w(TAG, "NotificationListenerService.requestRebind failed: ${e.message}")
            }
        }

        // 2. Component toggle workaround (essential for HyperOS, MIUI, and OEM ROMs)
        // Disabling and quickly re-enabling triggers NotificationManagerService's
        // internal PackageMonitor to recreate and bind the listener.
        runCatching {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.i(TAG, "Component enabled state toggled to force rebind")
        }.onFailure { e ->
            Log.e(TAG, "Failed to toggle component state for rebind", e)
        }
    }

    /** Opens the system "Notification access" settings screen so the user can toggle us on. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
