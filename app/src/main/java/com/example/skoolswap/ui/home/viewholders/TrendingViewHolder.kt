// ui/home/viewholders/TrendingViewHolder.kt
package com.example.skoolswap.ui.home.viewholders

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemTrendingRowBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.adapter.HorizontalItemsAdapter

class TrendingViewHolder(
    private val binding: ItemTrendingRowBinding,
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(section: Section.Trending) {
        // Set header title (from included header)
        binding.header.sectionTitle.text = section.title

        // Setup horizontal recycler
        val adapter = HorizontalItemsAdapter(section.items, section.type, onItemClick)
        binding.trendingRecycler.apply {
            layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            this.adapter = adapter
        }

        // View all click (from included header)
        binding.header.viewAll.setOnClickListener {
            onViewAllClick(section.type)
        }
    }
}