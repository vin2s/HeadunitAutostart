package com.vin2s.headunitautostart

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AutostartService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var screenReceiver: BroadcastReceiver? = null
    private var launchRunnable: Runnable? = null
    private var minimizeRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val prefs = Prefs(this)
        if (!prefs.isConfigured) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.getBooleanExtra(EXTRA_LAUNCH, false) == true) {
            scheduleLaunch("intent")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cancelPending()
        unregisterScreenReceiver()
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    scheduleLaunch("screen_on")
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        screenReceiver = receiver
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        screenReceiver = null
    }

    private fun scheduleLaunch(reason: String) {
        val prefs = Prefs(this)
        if (!prefs.isConfigured) return
        val now = System.currentTimeMillis()
        if (now - prefs.lastLaunchAt < prefs.cooldownMs) return

        cancelPending()
        val launch = Runnable {
            val still = Prefs(this)
            if (!still.isConfigured) return@Runnable
            if (System.currentTimeMillis() - still.lastLaunchAt < still.cooldownMs) return@Runnable
            val launched = AppLauncher.launch(this, still.targetPackage)
            if (!launched) return@Runnable
            still.lastLaunchAt = System.currentTimeMillis()
            if (still.minimizeAfterLaunch) {
                val minimize = Runnable { AppLauncher.goHome(this) }
                minimizeRunnable = minimize
                handler.postDelayed(minimize, still.minimizeDelayMs.coerceAtLeast(0L))
            }
        }
        launchRunnable = launch
        handler.postDelayed(launch, prefs.launchDelayMs.coerceAtLeast(0L))
        Log.d(TAG, "scheduled launch after $reason")
    }

    private fun cancelPending() {
        launchRunnable?.let { handler.removeCallbacks(it) }
        minimizeRunnable?.let { handler.removeCallbacks(it) }
        launchRunnable = null
        minimizeRunnable = null
    }

    private fun buildNotification(): Notification {
        val open = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingImmutableFlag()
        )
        val prefs = Prefs(this)
        val text = if (prefs.targetLabel.isNotBlank()) {
            getString(R.string.notification_text, prefs.targetLabel)
        } else {
            getString(R.string.notification_text_generic)
        }
        return NotificationCompat.Builder(this, AutostartApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pending)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun pendingImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    companion object {
        const val EXTRA_LAUNCH = "launch_now"
        private const val TAG = "AutostartService"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, launchNow: Boolean) {
            if (!Prefs(context).isConfigured) return
            acquireWakeLock(context)
            val intent = Intent(context, AutostartService::class.java)
            intent.putExtra(EXTRA_LAUNCH, launchNow)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutostartService::class.java))
        }

        private fun acquireWakeLock(context: Context) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "headunit:autostart")
            lock.setReferenceCounted(false)
            runCatching { lock.acquire(10_000L) }
        }
    }
}
