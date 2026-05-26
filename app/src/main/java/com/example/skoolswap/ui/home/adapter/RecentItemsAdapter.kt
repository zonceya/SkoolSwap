package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeRecentItemBinding
import com.example.skoolswap.domain.model.Item

class RecentItemsAdapter(
    private val onItemClick: (Item) -> Unit,
    private val maxItems: Int = 4
) : RecyclerView.Adapter<RecentItemsAdapter.ViewHolder>() {

    private var displayItems: List<Item> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeRecentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayItems[position])
    }

    override fun getItemCount() = displayItems.size

    fun updateItems(newItems: List<Item>) {
        val newDisplayItems = newItems.take(maxItems)
        if (displayItems != newDisplayItems) {
            displayItems = newDisplayItems
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(
        private val binding: ItemHomeRecentItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < displayItems.size) {
                    onItemClick(displayItems[position])
                }
            }
        }

        fun bind(item: Item) {
            binding.recentTitle.text = item.name
            binding.recentSchool.text = item.schoolName ?: "School Item"

            // Fix price display
            val price = item.price
            if (price != null && price > 0) {
                binding.recentPrice.text = "R${price}"
                binding.recentPrice.visibility = android.view.View.VISIBLE
            } else {
                binding.recentPrice.visibility = android.view.View.GONE
            }

            val imageUrl = item.images?.firstOrNull()?.url
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .fitCenter()
                    )
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.recentImage)
            } else {
                binding.recentImage.setImageResource(R.drawable.ic_create_item_placeholder)
                binding.recentImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
        }
    }
}