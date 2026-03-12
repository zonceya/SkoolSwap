// ui/home/viewholders/RecentViewHolder.kt
package com.example.skoolswap.ui.home.viewholders

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemRecentRowBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.Section
import com.example.skoolswap.ui.home.adapter.RecentItemsAdapter

class RecentViewHolder(
    private val binding: ItemRecentRowBinding,
    private val onItemClick: (Item, String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(section: Section.Recent) {
        binding.header.sectionTitle.text = section.title

        // Create adapter that drops the second parameter for the click
        val adapter = RecentItemsAdapter(
            items = section.items,
            onItemClick = { item ->
                onItemClick(item, section.type)  // Pass both item and section type
            }
        )

        binding.recentRecycler.apply {
            layoutManager = LinearLayoutManager(itemView.context)
            this.adapter = adapter
        }
    }
}