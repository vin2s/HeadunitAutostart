package com.vin2s.headunitautostart

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vin2s.headunitautostart.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: TargetAppsAdapter

    private val pickApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val pkg = data.getStringExtra(AppPickerActivity.EXTRA_PACKAGE).orEmpty()
        val label = data.getStringExtra(AppPickerActivity.EXTRA_LABEL).orEmpty()
        if (pkg.isBlank()) return@registerForActivityResult
        val current = adapter.snapshot().toMutableList()
        if (current.any { it.packageName == pkg }) {
            Toast.makeText(this, R.string.app_already_added, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        current.add(TargetApp(packageName = pkg, label = label.ifBlank { pkg }))
        adapter.submit(current)
        bindEmptyState()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* foreground service still works if the user declines */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TargetAppsAdapter { bindEmptyState() }
        binding.targets.layoutManager = LinearLayoutManager(this)
        binding.targets.adapter = adapter
        adapter.submit(prefs.targets)
        bindEmptyState()

        binding.cooldown.setText((prefs.cooldownMs / 1000L).toString())
        binding.enabled.isChecked = prefs.enabled

        binding.pickApp.setOnClickListener {
            val intent = Intent(this, AppPickerActivity::class.java)
            intent.putStringArrayListExtra(
                AppPickerActivity.EXTRA_EXCLUDE,
                ArrayList(adapter.packages())
            )
            pickApp.launch(intent)
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

    private fun bindEmptyState() {
        binding.emptyTargets.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
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
        val cooldownSec = binding.cooldown.text.toString().toLongOrNull() ?: 60L
        prefs.cooldownMs = cooldownSec.coerceAtLeast(5L) * 1000L
        prefs.targets = adapter.snapshot()
        prefs.enabled = binding.enabled.isChecked

        if (prefs.enabled && prefs.targets.isEmpty()) {
            prefs.enabled = false
            binding.enabled.isChecked = false
            Toast.makeText(this, R.string.select_app_first, Toast.LENGTH_LONG).show()
            return
        }

        if (prefs.isConfigured) {
            AutostartService.start(this, launchNow = false)
            moveTaskToBack(true)
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
