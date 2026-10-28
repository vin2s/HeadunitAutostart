package com.vin2s.headunitautostart

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var cooldownMs: Long
        get() = prefs.getLong(KEY_COOLDOWN, 60_000L)
        set(value) = prefs.edit().putLong(KEY_COOLDOWN, value).apply()

    var lastLaunchAt: Long
        get() = prefs.getLong(KEY_LAST_LAUNCH, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LAUNCH, value).apply()

    var targets: List<TargetApp>
        get() {
            val raw = prefs.getString(KEY_TARGETS, null)
            if (!raw.isNullOrBlank()) {
                return parseTargets(raw)
            }
            val pkg = prefs.getString(KEY_PACKAGE, "").orEmpty()
            if (pkg.isBlank()) return emptyList()
            return listOf(
                TargetApp(
                    packageName = pkg,
                    label = prefs.getString(KEY_LABEL, "").orEmpty().ifBlank { pkg },
                    launchDelayMs = prefs.getLong(KEY_LAUNCH_DELAY, 3_000L),
                    minimizeAfterLaunch = prefs.getBoolean(KEY_MINIMIZE, true),
                    minimizeDelayMs = prefs.getLong(KEY_MINIMIZE_DELAY, 2_000L)
                )
            )
        }
        set(value) {
            prefs.edit()
                .putString(KEY_TARGETS, encodeTargets(value))
                .remove(KEY_PACKAGE)
                .remove(KEY_LABEL)
                .remove(KEY_LAUNCH_DELAY)
                .remove(KEY_MINIMIZE)
                .remove(KEY_MINIMIZE_DELAY)
                .apply()
        }

    val isConfigured: Boolean
        get() = enabled && targets.isNotEmpty()

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
        private const val KEY_TARGETS = "targets_json"

        private fun parseTargets(raw: String): List<TargetApp> {
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pkg = obj.optString("package").trim()
                        if (pkg.isBlank()) continue
                        add(
                            TargetApp(
                                packageName = pkg,
                                label = obj.optString("label").ifBlank { pkg },
                                launchDelayMs = obj.optLong("launchDelayMs", 3_000L),
                                minimizeAfterLaunch = obj.optBoolean("minimize", true),
                                minimizeDelayMs = obj.optLong("minimizeDelayMs", 2_000L)
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun encodeTargets(value: List<TargetApp>): String {
            val array = JSONArray()
            value.forEach { app ->
                array.put(
                    JSONObject()
                        .put("package", app.packageName)
                        .put("label", app.label)
                        .put("launchDelayMs", app.launchDelayMs)
                        .put("minimize", app.minimizeAfterLaunch)
                        .put("minimizeDelayMs", app.minimizeDelayMs)
                )
            }
            return array.toString()
        }
    }
}
