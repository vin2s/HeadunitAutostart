package com.vin2s.headunitautostart

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vin2s.headunitautostart.databinding.ItemTargetBinding

class TargetAppsAdapter(
    private val onRemove: (TargetApp) -> Unit
) : RecyclerView.Adapter<TargetAppsAdapter.Holder>() {
    private val items = mutableListOf<TargetApp>()

    fun submit(list: List<TargetApp>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun snapshot(): List<TargetApp> = items.toList()

    fun packages(): Set<String> = items.map { it.packageName }.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemTargetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(val binding: ItemTargetBinding) : RecyclerView.ViewHolder(binding.root) {
        private var launchWatcher: TextWatcher? = null
        private var minimizeWatcher: TextWatcher? = null

        fun bind(item: TargetApp) {
            binding.title.text = item.label
            binding.subtitle.text = item.packageName
            replaceLaunchText(item.launchDelayMs)
            replaceMinimizeText(item.minimizeDelayMs)
            binding.minimize.setOnCheckedChangeListener(null)
            binding.minimize.isChecked = item.minimizeAfterLaunch
            binding.minimize.setOnCheckedChangeListener { _, checked ->
                updateItem { it.copy(minimizeAfterLaunch = checked) }
            }
            binding.remove.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val removed = items.removeAt(pos)
                notifyItemRemoved(pos)
                onRemove(removed)
            }
        }

        private fun replaceLaunchText(delayMs: Long) {
            launchWatcher?.let { binding.launchDelay.removeTextChangedListener(it) }
            val text = (delayMs / 1000L).toString()
            if (binding.launchDelay.text?.toString() != text) {
                binding.launchDelay.setText(text)
            }
            val watcher = afterChange { seconds ->
                updateItem { it.copy(launchDelayMs = seconds.coerceAtLeast(0L) * 1000L) }
            }
            launchWatcher = watcher
            binding.launchDelay.addTextChangedListener(watcher)
        }

        private fun replaceMinimizeText(delayMs: Long) {
            minimizeWatcher?.let { binding.minimizeDelay.removeTextChangedListener(it) }
            val text = (delayMs / 1000L).toString()
            if (binding.minimizeDelay.text?.toString() != text) {
                binding.minimizeDelay.setText(text)
            }
            val watcher = afterChange { seconds ->
                updateItem { it.copy(minimizeDelayMs = seconds.coerceAtLeast(0L) * 1000L) }
            }
            minimizeWatcher = watcher
            binding.minimizeDelay.addTextChangedListener(watcher)
        }

        private fun updateItem(transform: (TargetApp) -> TargetApp) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return
            items[pos] = transform(items[pos])
        }

        private fun afterChange(onChange: (Long) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    onChange(s?.toString()?.toLongOrNull() ?: 0L)
                }
            }
        }
    }
}
