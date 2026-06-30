package com.example.skoolswap.ui.home.viewholders

import android.content.res.Configuration
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemRecommendedRowBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.adapter.HorizontalItemsAdapter
import timber.log.Timber

class RecommendedViewHolder(
    private val binding: ItemRecommendedRowBinding,
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(section: Section.Recommended) {
        // Log what data we received
        Timber.tag("RecommendedViewHolder").d("========== RECOMMENDED SECTION ==========")
        Timber.tag("RecommendedViewHolder").d("Title: ${section.title}")
        Timber.tag("RecommendedViewHolder").d("Items count: ${section.items.size}")

        section.items.forEachIndexed { index, item ->
            Timber.tag("RecommendedViewHolder").d("Item $index: ${item.name}")
            Timber.tag("RecommendedViewHolder").d("  coverImage: ${item.coverImage}")
            Timber.tag("RecommendedViewHolder").d("  images size: ${item.images.size}")
            item.images.forEach { image ->
                Timber.tag("RecommendedViewHolder").d("    image url: ${image.url}")
            }
        }
        val sectionTitle = binding.root.findViewById<TextView>(R.id.sectionTitle)
        val viewAll = binding.root.findViewById<TextView>(R.id.viewAll)

        sectionTitle.text = section.title

        // Set text colors based on theme
        val isDarkMode = (itemView.context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) {
            ContextCompat.getColor(itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(itemView.context, R.color.black)
        }

        sectionTitle.setTextColor(textColor)
        viewAll.setTextColor(textColor)

        val adapter = HorizontalItemsAdapter(section.items, section.type, onItemClick)
        binding.recommendedRecycler.apply {
            layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            this.adapter = adapter
        }

        viewAll.setOnClickListener {
            onViewAllClick(section.type)
        }
    }
}