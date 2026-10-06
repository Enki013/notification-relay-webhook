package com.notifrelay.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts services after device reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferencesManager(context)

            // Rebind the notification listener service immediately
            if (NotificationAccess.isGranted(context)) {
                NotificationAccess.rebindService(context)
            }

            // Start keep-alive foreground service if enabled
            if (prefs.isKeepAliveEnabled()) {
                RelayKeepAliveService.start(context)
            }

            // Start local HTTP server if enabled
            if (prefs.isLocalHttpEnabled()) {
                LocalHttpServerService.start(context)
            }
        }
    }
}
