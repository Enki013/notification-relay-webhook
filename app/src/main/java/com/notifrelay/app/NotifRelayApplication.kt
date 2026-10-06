package com.notifrelay.app

import android.app.Application

class NotifRelayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PreferencesManager(this)

        // Ensure NotificationListenerService is bound whenever the app process starts
        if (NotificationAccess.isGranted(this)) {
            NotificationAccess.rebindService(this)
        }

        // Start background keep-alive service if enabled
        if (prefs.isKeepAliveEnabled()) {
            RelayKeepAliveService.start(this)
        }

        // Start the local HTTP server on launch if the user previously enabled it
        if (prefs.isLocalHttpEnabled()) {
            LocalHttpServerService.start(this)
        }
    }
}
