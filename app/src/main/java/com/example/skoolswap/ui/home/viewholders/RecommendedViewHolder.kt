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
import com.example.skoolswap.ui.home.adapter.VerticalItemsAdapter
import timber.log.Timber

class RecommendedViewHolder(
    private val binding: ItemRecommendedRowBinding,
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val itemsAdapter = VerticalItemsAdapter(
        sectionType = "recommended",
        onItemClick = onItemClick
    )

    init {
        binding.recommendedRecycler.apply {
            layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = itemsAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }

        binding.root.findViewById<TextView>(R.id.viewAll)?.setOnClickListener {
            onViewAllClick("recommended")
        }
    }

    fun bind(section: Section.Recommended) {
        Timber.tag("RecommendedViewHolder").d("========== RECOMMENDED SECTION ==========")
        Timber.tag("RecommendedViewHolder").d("Title: ${section.title}")
        Timber.tag("RecommendedViewHolder").d("Items count: ${section.items.size}")

        val sectionTitle = binding.root.findViewById<TextView>(R.id.sectionTitle)
        val viewAll = binding.root.findViewById<TextView>(R.id.viewAll)

        sectionTitle?.text = section.title

        val isDarkMode = (itemView.context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) {
            ContextCompat.getColor(itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(itemView.context, R.color.black)
        }

        sectionTitle?.setTextColor(textColor)
        viewAll?.setTextColor(textColor)

        itemsAdapter.updateItems(section.items)
    }
}