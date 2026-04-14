package com.example.skoolswap.ui.sport

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.databinding.ItemSportCardBinding
import com.example.skoolswap.domain.model.SportItem

class FeaturedSportsAdapter(
    private val onItemClick: (SportItem) -> Unit
) : RecyclerView.Adapter<FeaturedSportsAdapter.ViewHolder>() {

    private var items: List<SportItem> = emptyList()

    fun submitList(newItems: List<SportItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSportCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

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