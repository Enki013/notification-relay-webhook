package com.notifrelay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Lightweight Foreground Service that keeps Notification Relay active in memory.
 * Prevents HyperOS / MIUI and aggressive battery managers from killing the app process
 * when the app is swiped away from Recents or when the screen is turned off.
 */
class RelayKeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                try {
                    val notification = buildNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Ensure the NotificationListenerService is bound and active
                if (NotificationAccess.isGranted(this) && !NotificationAccess.isConnected()) {
                    NotificationAccess.rebindService(this)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.keep_alive_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.keep_alive_notification_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.keep_alive_notification_title))
            .setContentText(getString(R.string.keep_alive_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "relay_keep_alive_channel"
        private const val NOTIFICATION_ID = 1743
        private const val ACTION_START = "com.notifrelay.app.action.KEEP_ALIVE_START"
        private const val ACTION_STOP = "com.notifrelay.app.action.KEEP_ALIVE_STOP"

        fun start(context: Context): Boolean {
            val intent = Intent(context, RelayKeepAliveService::class.java).apply { action = ACTION_START }
            return runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.isSuccess
        }

        fun stop(context: Context) {
            val intent = Intent(context, RelayKeepAliveService::class.java).apply { action = ACTION_STOP }
            runCatching {
                context.startService(intent)
            }.onFailure {
                context.stopService(Intent(context, RelayKeepAliveService::class.java))
            }
        }

        fun sync(context: Context) {
            val prefs = PreferencesManager(context)
            if (prefs.isKeepAliveEnabled()) {
                start(context)
            } else {
                stop(context)
            }
        }
    }
}
