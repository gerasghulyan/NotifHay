package com.armin.garmin.receiver

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.armin.garmin.service.NotificationInterceptorService
import com.armin.garmin.settings.AppPreferences
import com.armin.garmin.transliteration.ArmenianTransliterator

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        // Macrodroid sends extras as String arrays; fall back to plain String for flexibility
        val pkg   = intent.getStringArrayExtra("pkg")?.firstOrNull()
            ?: intent.getStringExtra("pkg") ?: return
        val title = intent.getStringArrayExtra("title")?.firstOrNull()
            ?: intent.getStringExtra("title") ?: ""
        val text  = intent.getStringArrayExtra("text")?.firstOrNull()
            ?: intent.getStringExtra("text") ?: ""

        Log.i(TAG, "received pkg=$pkg title='$title' text='$text'")

        val prefs = AppPreferences(context)
        if (!prefs.transliterationEnabled) return
        // App filtering is handled by Macrodroid trigger — no per-app check needed here

        val powerManager    = context.getSystemService(PowerManager::class.java)
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (powerManager.isInteractive && !keyguardManager.isKeyguardLocked) {
            Log.i(TAG, "SKIP: screen on and unlocked")
            return
        }

        val bodyToCheck = "$title $text"
        val hasArmenian = ArmenianTransliterator.containsArmenian(bodyToCheck)

        val transTitle = if (hasArmenian) ArmenianTransliterator.transliterate(title) else title
        val transText  = if (hasArmenian) ArmenianTransliterator.transliterate(text) else text

        if (transTitle.isBlank() && transText.isBlank()) return

        Log.i(TAG, "posting: '$transTitle' / '$transText'")

        val notifManager = context.getSystemService(NotificationManager::class.java)
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_dialog_info)
        val notif = NotificationCompat.Builder(context, NotificationInterceptorService.CHANNEL_GARMIN)
            .setContentTitle(transTitle)
            .setContentText(transText)
            .setSmallIcon(icon)
            .setLocalOnly(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(true)
            .build()

        val notifId = (pkg + title + text).hashCode()
        notifManager.notify(NotificationInterceptorService.TRANSLITERATED_TAG, notifId, notif)
        Log.i(TAG, "posted id=$notifId")

        Handler(Looper.getMainLooper()).postDelayed({
            notifManager.cancel(NotificationInterceptorService.TRANSLITERATED_TAG, notifId)
        }, 8000)
    }

    companion object {
        const val ACTION = "com.armin.garmin.NOTIFICATION_RECEIVED"
        private const val TAG = "NotifBroadcastReceiver"
    }
}
