package za.co.skoolswap.ui.sport

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.databinding.ItemSportCardBinding
import za.co.skoolswap.domain.model.SportItem

class FeaturedSportsAdapter(
    private val onItemClick: (SportItem) -> Unit
) : ListAdapter<SportItem, FeaturedSportsAdapter.ViewHolder>(SportDiffCallback()) {

    // ✅ REMOVED: custom submitList() - use parent's implementation
    // Just call adapter.submitList(items) from Fragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSportCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SportDiffCallback : DiffUtil.ItemCallback<SportItem>() {
        override fun areItemsTheSame(oldItem: SportItem, newItem: SportItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SportItem, newItem: SportItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemSportCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sport: SportItem) {
            binding.sportName.text = sport.name
            binding.sportImage.setImageResource(sport.imageResId)

            binding.root.setOnClickListener {
                onItemClick(sport)
            }
        }
    }
}