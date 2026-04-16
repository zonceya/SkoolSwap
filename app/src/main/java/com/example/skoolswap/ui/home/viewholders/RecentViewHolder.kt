// ui/home/viewholders/RecentViewHolder.kt
package com.example.skoolswap.ui.home.viewholders

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemRecentRowBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.adapter.RecentItemsAdapter

class RecentViewHolder(
    private val binding: ItemRecentRowBinding,
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(section: Section.Recent) {
        binding.header.sectionTitle.text = section.title

        // Set up View All click
        binding.header.viewAll.setOnClickListener {
            onViewAllClick(section.type)
        }

        // Only show first 4 items
        val adapter = RecentItemsAdapter(
            items = section.items,
            onItemClick = { item ->
                onItemClick(item, section.type)
            },
            maxItems = 4  // Show only 4 items
        )

        binding.recentRecycler.apply {
            layoutManager = LinearLayoutManager(itemView.context)
            this.adapter = adapter
        }
    }
}