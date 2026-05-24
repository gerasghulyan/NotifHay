package com.armin.garmin.settings

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var transliterationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var cancelOriginal: Boolean
        get() = prefs.getBoolean(KEY_CANCEL_ORIGINAL, true)
        set(value) = prefs.edit().putBoolean(KEY_CANCEL_ORIGINAL, value).apply()

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG, value).apply()

    var enabledApps: Set<String>
        get() = prefs.getStringSet(KEY_ENABLED_APPS, DEFAULT_APPS) ?: DEFAULT_APPS
        set(value) = prefs.edit().putStringSet(KEY_ENABLED_APPS, value).apply()

    fun isAppEnabled(packageName: String): Boolean = packageName in enabledApps

    companion object {
        private const val PREFS_NAME = "armin_garmin_prefs"
        private const val KEY_ENABLED = "transliteration_enabled"
        private const val KEY_CANCEL_ORIGINAL = "cancel_original"
        private const val KEY_DEBUG = "debug_mode"
        private const val KEY_ENABLED_APPS = "enabled_apps"

        val DEFAULT_APPS: Set<String> = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.beta",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.facebook.orca",
            "com.viber.voip",
            "com.google.android.gm",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging"
        )
    }
}
