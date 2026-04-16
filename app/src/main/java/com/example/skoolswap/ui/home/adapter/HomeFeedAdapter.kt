package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.*
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.viewholders.*

class HomeFeedAdapter(
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val sections = mutableListOf<Section>()

    companion object {
        private const val TYPE_RECOMMENDED = 1
        private const val TYPE_ESSENTIALS = 2
        private const val TYPE_TRENDING = 3
        private const val TYPE_RECENT = 4
    }

    fun submitList(newSections: List<Section>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (sections[position]) {
            is Section.Recommended -> TYPE_RECOMMENDED
            is Section.Essentials -> TYPE_ESSENTIALS
            is Section.Trending -> TYPE_TRENDING
            is Section.Recent -> TYPE_RECENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RECOMMENDED -> {
                // FIXED: Use correct binding name
                val binding = ItemRecommendedRowBinding.inflate(  // Changed from ItemHomeRecommendedSectionBinding
                    LayoutInflater.from(parent.context), parent, false
                )
                RecommendedViewHolder(binding, onItemClick, onViewAllClick)
            }
            TYPE_ESSENTIALS -> {
                // You'll need to fix this one too - what's your XML name for essentials?
                val binding = ItemEssentialsRowBinding.inflate(  // Assuming this is correct
                    LayoutInflater.from(parent.context), parent, false
                )
                EssentialsViewHolder(binding, onItemClick)
            }
            TYPE_TRENDING -> {
                val binding = ItemTrendingRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                TrendingViewHolder(binding, onItemClick, onViewAllClick)
            }
            TYPE_RECENT -> {
                val binding = ItemRecentRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                RecentViewHolder(binding, onItemClick, onViewAllClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RecommendedViewHolder -> holder.bind(sections[position] as Section.Recommended)
            is EssentialsViewHolder -> holder.bind(sections[position] as Section.Essentials)
            is TrendingViewHolder -> holder.bind(sections[position] as Section.Trending)
            is RecentViewHolder -> holder.bind(sections[position] as Section.Recent)
        }
    }

    override fun getItemCount() = sections.size
}