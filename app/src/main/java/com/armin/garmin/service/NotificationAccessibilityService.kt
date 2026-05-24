package com.armin.garmin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import android.app.KeyguardManager
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.armin.garmin.settings.AppPreferences
import com.armin.garmin.transliteration.ArmenianTransliterator

/**
 * Accessibility-based notification interceptor.
 *
 * Samsung devices block onNotificationPosted for non-whitelisted NLS UIDs.
 * AccessibilityService receives TYPE_NOTIFICATION_STATE_CHANGED events through
 * a separate framework path that is not subject to Samsung's NLS trust gate.
 */
class NotificationAccessibilityService : AccessibilityService() {

    private lateinit var prefs: AppPreferences
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager
    private lateinit var keyguardManager: KeyguardManager
    private val handler = Handler(Looper.getMainLooper())
    // Recently posted content hashes to avoid re-posting the same message
    private val recentHashes = mutableMapOf<String, Long>() // hash -> timestamp
    private val DEDUP_WINDOW_MS = 15_000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = AppPreferences(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)
        keyguardManager = getSystemService(KeyguardManager::class.java)
        recentHashes.clear()
        createChannel()
        val info = serviceInfo
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = info.flags or 0x00000100 // FLAG_IS_ACCESSIBILITY_TOOL (API 33+)
        serviceInfo = info
        Log.i(TAG, "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return

        val notification = event.parcelableData as? Notification
        val pkg = event.packageName?.toString() ?: return

        Log.i(TAG, "a11y notif pkg=$pkg hasNotif=${notification != null}")
        if (pkg == packageName) return   // ignore our own notifications
        if (!prefs.transliterationEnabled) { Log.i(TAG, "SKIP: transliteration disabled"); return }
        if (!prefs.isAppEnabled(pkg)) { Log.i(TAG, "SKIP: app not enabled pkg=$pkg"); return }

        val title: String
        val text: String
        val bigText: String?
        val subText: String?

        if (notification != null) {
            val extras = notification.extras ?: run { Log.i(TAG, "SKIP: no extras"); return }

            // Try MessagingStyle first (WhatsApp uses this)
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            val lastMsg = (messages?.lastOrNull() as? android.os.Bundle)
            val msgText = lastMsg?.getCharSequence("text")?.toString()?.takeIf { it.isNotBlank() }

            if (msgText != null) {
                val sender = lastMsg?.getCharSequence("sender")?.toString() ?: ""
                val convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                title   = if (sender.isNotBlank() && sender != convTitle) "$sender: $convTitle" else convTitle
                text    = msgText
                bigText = null
                subText = null
                Log.i(TAG, "a11y MessagingStyle sender='$sender' text='$msgText'")
            } else {
                title   = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                // InboxStyle (Gmail): real content is in EXTRA_TEXT_LINES
                val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                text    = if (rawText.isBlank() && textLines != null && textLines.isNotEmpty())
                    textLines.joinToString(" | ") { it.toString() }
                else rawText
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                Log.i(TAG, "a11y textLines=${textLines?.toList()}")
            }
        } else {
            // Fallback 1: raw text lines from the accessibility event
            val lines = event.text.map { it.toString() }.filter { it.isNotBlank() }
            Log.i(TAG, "a11y fallback lines=$lines")
            if (lines.isNotEmpty()) {
                title   = lines.firstOrNull() ?: ""
                text    = lines.drop(1).joinToString(" ")
                bigText = null
                subText = null
            } else {
                // Fallback 2: ask NLS for active notifications (works when accessibility event has no content)
                val sbn = NotificationInterceptorService.instance
                    ?.getActiveNotifications()
                    ?.filter { it.packageName == pkg }
                    ?.maxByOrNull { it.postTime }
                Log.i(TAG, "a11y NLS fallback sbn=${sbn?.key}")
                if (sbn == null) {
                    // Last resort: post a generic placeholder so the watch at least buzzes
                    Log.i(TAG, "a11y placeholder for pkg=$pkg")
                    title   = appName(pkg)
                    text    = "New message"
                    bigText = null
                    subText = null
                } else {
                    val extras = sbn.notification.extras
                    title   = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    text    = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                }
            }
        }

        Log.i(TAG, "a11y title='$title' text='$text'")
        val bodyToCheck = "$title $text ${bigText ?: ""}"
        val hasArmenian = ArmenianTransliterator.containsArmenian(bodyToCheck)

        val transTitle   = if (hasArmenian) ArmenianTransliterator.transliterate(title) else title
        val transText    = if (hasArmenian) ArmenianTransliterator.transliterate(text) else text
        val transBigText = if (hasArmenian) bigText?.let { ArmenianTransliterator.transliterate(it) } else bigText
        val transSubText = if (hasArmenian) subText?.let { ArmenianTransliterator.transliterate(it) } else subText

        Log.i(TAG, "Posting transliterated via A11y: '$transTitle' / '$transText'")

        val icon = notification?.smallIcon?.let { smallIcon ->
            try { IconCompat.createFromIcon(this, smallIcon) } catch (_: Exception) { null }
        } ?: IconCompat.createWithResource(this, android.R.drawable.ic_dialog_info)

        val builder = NotificationCompat.Builder(this, NotificationInterceptorService.CHANNEL_GARMIN)
            .setContentTitle(transTitle)
            .setContentText(transText)
            .setSmallIcon(icon)
            .setLocalOnly(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(notification?.category ?: NotificationCompat.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(true)

        if (transBigText != null) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(transBigText)
                    .setBigContentTitle(transTitle)
            )
        }
        transSubText?.let { builder.setSubText(it) }
        notification?.contentIntent?.let { builder.setContentIntent(it) }

        val screenOn = powerManager.isInteractive
        val locked = keyguardManager.isKeyguardLocked
        Log.i(TAG, "screenOn=$screenOn locked=$locked")

        if (screenOn && !locked) {
            Log.i(TAG, "SKIP: screen is on and unlocked, user has phone")
            return
        }

        val notifId = (pkg + (notification?.`when` ?: System.currentTimeMillis())).hashCode()
        val notif = builder.build()

        // Skip generic summaries — real content will arrive in a follow-up event
        if (isGenericSummary(transTitle, transText)) {
            Log.i(TAG, "SKIP: generic summary")
            return
        }

        // Deduplication: skip if same content was posted recently
        val contentHash = "$pkg|$transTitle|$transText"
        val now = System.currentTimeMillis()
        recentHashes.entries.removeAll { now - it.value > DEDUP_WINDOW_MS }
        if (recentHashes.containsKey(contentHash)) {
            Log.i(TAG, "SKIP: duplicate content")
            return
        }
        recentHashes[contentHash] = now

        notificationManager.notify(NotificationInterceptorService.TRANSLITERATED_TAG, notifId, notif)
        Log.i(TAG, "Posted transliterated notification id=$notifId pkg=$pkg")
        handler.postDelayed({
            notificationManager.cancel(NotificationInterceptorService.TRANSLITERATED_TAG, notifId)
        }, 8000)
    }

    private fun isGenericSummary(title: String, text: String): Boolean {
        // Only skip "Telegram / New message" — that specific pattern always has real content following
        // Do NOT filter count-based summaries ("X new messages") — for WhatsApp/Gmail that IS the content
        return text.trim() == "New message"
    }

    private fun appName(pkg: String) = when (pkg) {
        "com.viber.voip"                  -> "Viber"
        "org.telegram.messenger"          -> "Telegram"
        "org.telegram.messenger.beta"     -> "Telegram"
        "com.whatsapp"                    -> "WhatsApp"
        "com.whatsapp.w4b"                -> "WhatsApp"
        "com.facebook.orca"               -> "Messenger"
        "com.google.android.gm"           -> "Gmail"
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging"   -> "Messages"
        else -> pkg.substringAfterLast(".").replaceFirstChar { it.uppercaseChar() }
    }

    override fun onInterrupt() {
        Log.i(TAG, "AccessibilityService interrupted")
    }

    private fun createChannel() {
        // Delete old low-importance channel
        notificationManager.deleteNotificationChannel("garmin_silent")
        val channel = NotificationChannel(
            NotificationInterceptorService.CHANNEL_GARMIN,
            "Garmin Watch",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Transliterated notifications forwarded to Garmin watch"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "A11yNotifInterceptor"
    }
}
