package com.example.skoolswap.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.ItemFavoriteCardBinding
import com.example.skoolswap.domain.model.Item
import timber.log.Timber

class FavoritesAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<Item, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    // ✅ REMOVED: custom submitList() - use parent's implementation
    // Just call adapter.submitList(items) from Fragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                productName.text = item.name
                productPrice.text = "R${String.format("%.2f", item.price)}"

                // ✅ Use the helper method for consistency
                val imageUrl = item.resolveImageUrl()
                    ?: item.coverImage
                    ?: item.images.firstOrNull()?.url

                if (!imageUrl.isNullOrBlank()) {
                    Glide.with(binding.root.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(productImage)
                } else {
                    productImage.setImageResource(R.drawable.ic_create_item_placeholder)
                }

                root.setOnClickListener {
                    onItemClick(item.id)
                }
            }
        }
    }
}