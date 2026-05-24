package com.armin.garmin.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.armin.garmin.MainActivity

/**
 * A regular started Service that runs in the foreground to keep the process at
 * foreground priority. This prevents Samsung (and other OEMs) from deferring
 * callbacks to our NotificationListenerService via ApplicationThreadDeferred.
 */
class KeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(FOREGROUND_ID, buildNotification())
        Log.i(TAG, "KeepAliveService started in foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "KeepAliveService destroyed")
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Armin Garmin active")
        .setContentText("Transliterating Armenian notifications for Garmin")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Armin Garmin Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the transliteration service running"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "KeepAliveService"
        const val CHANNEL_ID = "armin_foreground"
        private const val FOREGROUND_ID = 1
    }
}
