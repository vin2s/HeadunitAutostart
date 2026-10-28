package com.vin2s.headunitautostart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vin2s.headunitautostart.databinding.ItemAppBinding

class AppListAdapter(
    private val onClick: (InstalledApp) -> Unit
) : RecyclerView.Adapter<AppListAdapter.Holder>() {
    private val items = mutableListOf<InstalledApp>()

    fun submit(list: List<InstalledApp>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.icon.setImageDrawable(item.icon)
        holder.binding.title.text = item.label
        holder.binding.subtitle.text = item.packageName
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}
