package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeRecentItemBinding
import com.example.skoolswap.domain.model.Item

class RecentItemsAdapter(
    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<RecentItemsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeRecentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemHomeRecentItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }

        fun bind(item: Item) {
            binding.recentTitle.text = item.name
            binding.recentPrice.text = "R${item.price}"
            binding.recentSchool.text = "School ID: ${item.schoolId}" // Or get school name

            // Load image
            if (!item.images.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.images.first().url)
                    .placeholder(R.drawable.item_placeholder)
                    .error(R.drawable.item_placeholder)
                    .centerCrop()
                    .into(binding.recentImage)
            } else {
                binding.recentImage.setImageResource(R.drawable.item_placeholder)
            }
        }
    }
}