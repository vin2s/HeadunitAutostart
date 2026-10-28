package com.vin2s.headunitautostart

import android.content.Context

class Prefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var targetPackage: String
        get() = prefs.getString(KEY_PACKAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PACKAGE, value).apply()

    var targetLabel: String
        get() = prefs.getString(KEY_LABEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LABEL, value).apply()

    var launchDelayMs: Long
        get() = prefs.getLong(KEY_LAUNCH_DELAY, 3_000L)
        set(value) = prefs.edit().putLong(KEY_LAUNCH_DELAY, value).apply()

    var minimizeDelayMs: Long
        get() = prefs.getLong(KEY_MINIMIZE_DELAY, 2_000L)
        set(value) = prefs.edit().putLong(KEY_MINIMIZE_DELAY, value).apply()

    var minimizeAfterLaunch: Boolean
        get() = prefs.getBoolean(KEY_MINIMIZE, true)
        set(value) = prefs.edit().putBoolean(KEY_MINIMIZE, value).apply()

    var cooldownMs: Long
        get() = prefs.getLong(KEY_COOLDOWN, 60_000L)
        set(value) = prefs.edit().putLong(KEY_COOLDOWN, value).apply()

    var lastLaunchAt: Long
        get() = prefs.getLong(KEY_LAST_LAUNCH, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LAUNCH, value).apply()

    val isConfigured: Boolean
        get() = enabled && targetPackage.isNotBlank()

    companion object {
        private const val NAME = "headunit_autostart"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PACKAGE = "target_package"
        private const val KEY_LABEL = "target_label"
        private const val KEY_LAUNCH_DELAY = "launch_delay_ms"
        private const val KEY_MINIMIZE_DELAY = "minimize_delay_ms"
        private const val KEY_MINIMIZE = "minimize"
        private const val KEY_COOLDOWN = "cooldown_ms"
        private const val KEY_LAST_LAUNCH = "last_launch_at"
    }
}
