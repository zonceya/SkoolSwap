package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeRecentItemBinding
import com.example.skoolswap.domain.model.Item

class RecentItemsAdapter(
    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit,
    private val maxItems: Int = 4
) : RecyclerView.Adapter<RecentItemsAdapter.ViewHolder>() {

    private val displayItems = items.take(maxItems)

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

    inner class ViewHolder(
        private val binding: ItemHomeRecentItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(displayItems[position])
                }
            }
        }

        fun bind(item: Item) {
            binding.recentTitle.text = item.name
            binding.recentPrice.text = "R${item.price}"
            val schoolName = item.schoolName ?: "School Item"
            binding.recentSchool.text = schoolName
            val imageUrl = item.images?.firstOrNull()?.url
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .centerCrop()  // This ensures image fills the 80dp x 80dp space
                    )
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.recentImage)
            } else {
                binding.recentImage.setImageResource(R.drawable.ic_create_item_placeholder)
                // Apply centerCrop to placeholder too
                binding.recentImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
        }
    }
}