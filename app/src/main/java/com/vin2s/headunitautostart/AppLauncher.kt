package com.vin2s.headunitautostart

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object AppLauncher {
    fun launch(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        return try {
            context.startActivity(launch)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun goHome(context: Context) {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(home)
        } catch (_: Exception) {
            // Head units without a standard launcher still keep the target app started.
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
