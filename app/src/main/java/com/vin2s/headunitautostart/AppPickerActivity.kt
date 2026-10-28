package com.vin2s.headunitautostart

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vin2s.headunitautostart.databinding.ActivityAppPickerBinding

class AppPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppPickerBinding
    private lateinit var adapter: AppListAdapter
    private var apps: List<InstalledApp> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val excluded = intent.getStringArrayListExtra(EXTRA_EXCLUDE)?.toSet().orEmpty()

        adapter = AppListAdapter { app ->
            val data = Intent()
                .putExtra(EXTRA_PACKAGE, app.packageName)
                .putExtra(EXTRA_LABEL, app.label)
            setResult(RESULT_OK, data)
            finish()
        }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        apps = loadLaunchableApps().filter { it.packageName !in excluded }
        adapter.submit(apps)

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText.orEmpty().trim().lowercase()
                adapter.submit(
                    if (q.isEmpty()) apps
                    else apps.filter {
                        it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                    }
                )
                return true
            }
        })
    }

    private fun loadLaunchableApps(): List<InstalledApp> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val resolved = pm.queryIntentActivities(intent, flags)
        return resolved.mapNotNull { info ->
            val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
            if (appInfo.packageName == packageName) return@mapNotNull null
            InstalledApp(
                label = info.loadLabel(pm).toString(),
                packageName = appInfo.packageName,
                icon = info.loadIcon(pm)
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_LABEL = "label"
        const val EXTRA_EXCLUDE = "exclude"
    }
}
