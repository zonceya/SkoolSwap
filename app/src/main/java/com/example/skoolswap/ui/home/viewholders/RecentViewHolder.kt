package com.example.skoolswap.ui.home.viewholders

import android.content.res.Configuration
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemRecentRowBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.adapter.RecentItemsAdapter

class RecentViewHolder(
    private val binding: ItemRecentRowBinding,
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    // Create adapter once and reuse it
    private val recentAdapter = RecentItemsAdapter(
        onItemClick = { item ->
            onItemClick(item, "recent")
        },
        maxItems = 4
    )

    init {
        // Set up the recycler view once
        binding.recentRecycler.apply {
            layoutManager = LinearLayoutManager(itemView.context)
            adapter = recentAdapter
        }
    }

    fun bind(section: Section.Recent) {
        val sectionTitle = binding.root.findViewById<TextView>(R.id.sectionTitle)
        val viewAll = binding.root.findViewById<TextView>(R.id.viewAll)

        sectionTitle.text = section.title

        val isDarkMode = (itemView.context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) {
            ContextCompat.getColor(itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(itemView.context, R.color.black)
        }

        sectionTitle.setTextColor(textColor)
        viewAll.setTextColor(textColor)
        binding.header.sectionTitle.text = section.title

        // Set up View All click
        binding.header.viewAll.setOnClickListener {
            onViewAllClick(section.type)
        }

        // Update the existing adapter with new items
        recentAdapter.updateItems(section.items)
    }
}