package com.example.skoolswap.ui.home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeCategoryBinding
import com.example.skoolswap.ui.home.viewholders.EssentialsCategoryItem

private const val TAG = "EssentialsGridAdapter"

class EssentialsGridAdapter(
    private val items: List<EssentialsCategoryItem>,
    private val onCategoryClick: (EssentialsCategoryItem) -> Unit
) : RecyclerView.Adapter<EssentialsGridAdapter.ViewHolder>() {

    init {
        Log.d(TAG, "=== GRID ADAPTER CREATED ===")
        Log.d(TAG, "Items count: ${items.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemHomeCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryItem: EssentialsCategoryItem) {
            // Set category name
            binding.categoryName.text = categoryItem.displayName

            // Determine which image to use
            // Priority: actual item image > default image > local placeholder
            val actualImageUrl = categoryItem.item.images.firstOrNull()?.url
            val imageUrl = if (!actualImageUrl.isNullOrEmpty()) {
                actualImageUrl
            } else {
                categoryItem.defaultImageUrl
            }

            if (!imageUrl.isNullOrEmpty()) {
                // Load image from URL (either actual item or default)
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(getPlaceholderForCategory(categoryItem.categoryType))
                    .error(getPlaceholderForCategory(categoryItem.categoryType))
                    .centerCrop()
                    .into(binding.categoryIcon)
                Log.d(TAG, "Loaded image for ${categoryItem.displayName}: $imageUrl")
            } else {
                // Fallback to local drawable placeholder
                val iconResId = getPlaceholderForCategory(categoryItem.categoryType)
                binding.categoryIcon.setImageResource(iconResId)
                Log.d(TAG, "Using placeholder for ${categoryItem.displayName}")
            }

            // Handle click
            binding.root.setOnClickListener {
                onCategoryClick(categoryItem)
            }
        }

        private fun getPlaceholderForCategory(categoryType: String): Int {
            return when (categoryType) {
                "uniforms" -> R.drawable.ic_uniform_placeholder
                "sports" -> R.drawable.ic_sports_placeholder
                "stationery" -> R.drawable.ic_stationery_placeholder
                "accessories" -> R.drawable.ic_accessories_placeholder
                else -> R.drawable.ic_create_item_placeholder
            }
        }
    }
}