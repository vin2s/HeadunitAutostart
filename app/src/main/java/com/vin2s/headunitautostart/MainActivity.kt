package com.vin2s.headunitautostart

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vin2s.headunitautostart.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val pickApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            bindTarget()
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* foreground service still works if the user declines */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.launchDelay.setText((prefs.launchDelayMs / 1000L).toString())
        binding.minimizeDelay.setText((prefs.minimizeDelayMs / 1000L).toString())
        binding.cooldown.setText((prefs.cooldownMs / 1000L).toString())
        binding.minimize.isChecked = prefs.minimizeAfterLaunch
        binding.enabled.isChecked = prefs.enabled
        bindTarget()

        binding.pickApp.setOnClickListener {
            pickApp.launch(Intent(this, AppPickerActivity::class.java))
        }
        binding.save.setOnClickListener { saveAndApply() }
        binding.overlay.setOnClickListener { requestOverlay() }
        binding.battery.setOnClickListener { requestBatteryExemption() }
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        bindPermissionState()
    }

    private fun bindTarget() {
        val label = prefs.targetLabel
        val pkg = prefs.targetPackage
        binding.targetApp.text = if (pkg.isBlank()) {
            getString(R.string.target_not_selected)
        } else if (label.isNotBlank()) {
            getString(R.string.target_selected, label, pkg)
        } else {
            pkg
        }
    }

    private fun bindPermissionState() {
        binding.overlayStatus.text = if (AppLauncher.canDrawOverlays(this)) {
            getString(R.string.overlay_granted)
        } else {
            getString(R.string.overlay_needed)
        }
        binding.batteryStatus.text = if (isIgnoringBatteryOptimizations()) {
            getString(R.string.battery_granted)
        } else {
            getString(R.string.battery_needed)
        }
    }

    private fun saveAndApply() {
        val launchSec = binding.launchDelay.text.toString().toLongOrNull() ?: 3L
        val minimizeSec = binding.minimizeDelay.text.toString().toLongOrNull() ?: 2L
        val cooldownSec = binding.cooldown.text.toString().toLongOrNull() ?: 60L
        prefs.launchDelayMs = launchSec.coerceAtLeast(0L) * 1000L
        prefs.minimizeDelayMs = minimizeSec.coerceAtLeast(0L) * 1000L
        prefs.cooldownMs = cooldownSec.coerceAtLeast(5L) * 1000L
        prefs.minimizeAfterLaunch = binding.minimize.isChecked
        prefs.enabled = binding.enabled.isChecked

        if (prefs.enabled && prefs.targetPackage.isBlank()) {
            prefs.enabled = false
            binding.enabled.isChecked = false
            Toast.makeText(this, R.string.select_app_first, Toast.LENGTH_LONG).show()
            return
        }

        if (prefs.isConfigured) {
            AutostartService.start(this, launchNow = false)
            Toast.makeText(this, R.string.saved_running, Toast.LENGTH_LONG).show()
        } else {
            AutostartService.stop(this)
            Toast.makeText(this, R.string.saved_stopped, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (AppLauncher.canDrawOverlays(this)) return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (isIgnoringBatteryOptimizations()) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:$packageName")
        runCatching { startActivity(intent) }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
