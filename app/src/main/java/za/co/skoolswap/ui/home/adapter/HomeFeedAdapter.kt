package za.co.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.databinding.*
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.homefeed.Section
import za.co.skoolswap.ui.home.viewholders.*
import timber.log.Timber

class HomeFeedAdapter(
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : ListAdapter<Section, RecyclerView.ViewHolder>(SectionDiffCallback()) {

    companion object {
        private const val TYPE_RECOMMENDED = 1
        private const val TYPE_ESSENTIALS = 2
        private const val TYPE_TRENDING = 3
        private const val TYPE_RECENT = 4
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {  // ✅ Use getItem() from ListAdapter
            is Section.Recommended -> TYPE_RECOMMENDED
            is Section.Essentials -> TYPE_ESSENTIALS
            is Section.Trending -> TYPE_TRENDING
            is Section.Recent -> TYPE_RECENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RECOMMENDED -> {
                val binding = ItemRecommendedRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                RecommendedViewHolder(binding, onItemClick, onViewAllClick)
            }
            TYPE_ESSENTIALS -> {
                val binding = ItemEssentialsRowBinding.inflate(
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
        val section = getItem(position)  // ✅ Use getItem() from ListAdapter

        when (holder) {
            is RecommendedViewHolder -> {
                Timber.tag("HomeFeedAdapter").d("Binding Recommended at position $position")
                holder.bind(section as Section.Recommended)
            }
            is EssentialsViewHolder -> {
                Timber.tag("HomeFeedAdapter").d("Binding Essentials at position $position")
                holder.bind(section as Section.Essentials)
            }
            is TrendingViewHolder -> {
                Timber.tag("HomeFeedAdapter").d("Binding Trending at position $position")
                holder.bind(section as Section.Trending)
            }
            is RecentViewHolder -> {
                Timber.tag("HomeFeedAdapter").d("Binding Recent at position $position")
                holder.bind(section as Section.Recent)
            }
        }
    }

    // ✅ REMOVED: override fun getItemCount() - ListAdapter provides this

    private class SectionDiffCallback : DiffUtil.ItemCallback<Section>() {
        override fun areItemsTheSame(oldItem: Section, newItem: Section): Boolean {
            // Sections are identified by their type (only one of each type in the feed)
            return oldItem::class == newItem::class
        }

        override fun areContentsTheSame(oldItem: Section, newItem: Section): Boolean {
            return oldItem == newItem
        }
    }
}