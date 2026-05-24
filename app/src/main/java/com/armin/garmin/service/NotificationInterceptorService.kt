package com.armin.garmin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.armin.garmin.settings.AppPreferences
import com.armin.garmin.transliteration.ArmenianTransliterator

class NotificationInterceptorService : NotificationListenerService() {

    private lateinit var prefs: AppPreferences
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = AppPreferences(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        createChannels()
        Log.i(TAG, "NotificationInterceptorService started")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        Log.i(TAG, "NotificationInterceptorService destroyed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener connected")
        dbg("connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "Listener disconnected")
        dbg("disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.i(TAG, "onNotificationPosted: pkg=${sbn.packageName} id=${sbn.id}")
        dbg("posted pkg=${sbn.packageName}")
        handleSbn(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val notifId = (sbn.packageName + sbn.tag + sbn.id).hashCode()
        notificationManager.cancel(TRANSLITERATED_TAG, notifId)
    }

    // --- core transliteration logic ---

    private fun handleSbn(sbn: StatusBarNotification) {
        if (!prefs.transliterationEnabled) return
        if (sbn.packageName == packageName) return
        if (!prefs.isAppEnabled(sbn.packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title   = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text    = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val bodyToCheck = "$title $text ${bigText ?: ""}"
        if (!ArmenianTransliterator.containsArmenian(bodyToCheck)) {
            Log.i(TAG, "SKIP: no Armenian in '$title'"); return
        }

        val transTitle   = ArmenianTransliterator.transliterate(title)
        val transText    = ArmenianTransliterator.transliterate(text)
        val transBigText = bigText?.let { ArmenianTransliterator.transliterate(it) }
        val transSubText = subText?.let { ArmenianTransliterator.transliterate(it) }

        Log.i(TAG, "Posting transliterated: '$transTitle' / '$transText'")
        dbg("transliterating: $transTitle / $transText")

        val icon = IconCompat.createFromIcon(this, notification.smallIcon)
            ?: IconCompat.createWithResource(this, android.R.drawable.ic_dialog_info)

        val builder = NotificationCompat.Builder(this, CHANNEL_GARMIN)
            .setContentTitle(transTitle)
            .setContentText(transText)
            .setSmallIcon(icon)
            .setLocalOnly(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(notification.category ?: NotificationCompat.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(true)

        if (transBigText != null) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(transBigText)
                    .setBigContentTitle(transTitle)
            )
        }
        transSubText?.let { builder.setSubText(it) }
        notification.contentIntent?.let { builder.setContentIntent(it) }

        val groupKey = sbn.notification.group ?: "armin_garmin_${sbn.packageName}"
        builder.setGroup(groupKey)

        val notifId = (sbn.packageName + sbn.tag + sbn.id).hashCode()
        notificationManager.notify(TRANSLITERATED_TAG, notifId, builder.build())
        Log.i(TAG, "Posted transliterated notification id=$notifId")
    }

    private fun dbg(msg: String) {
        try {
            java.io.File(filesDir, "nls_debug.log")
                .appendText("${System.currentTimeMillis()} $msg\n")
        } catch (_: Exception) {}
    }

    private fun createChannels() {
        // Delete old low-importance channel
        notificationManager.deleteNotificationChannel("garmin_silent")
        val garminChannel = NotificationChannel(
            CHANNEL_GARMIN,
            "Garmin Watch",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Transliterated notifications forwarded to Garmin watch"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(garminChannel)
    }

    companion object {
        private const val TAG = "NotifInterceptor"
        const val CHANNEL_GARMIN = "garmin_watch"
        const val TRANSLITERATED_TAG = "armin_garmin_transliterated"
        var instance: NotificationInterceptorService? = null
    }
}
