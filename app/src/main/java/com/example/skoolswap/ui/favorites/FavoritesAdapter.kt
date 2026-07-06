package com.example.skoolswap.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
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
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    private var items = listOf<Item>()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
        Timber.tag(LogTags.UI).d("Submitted ${newItems.size} favorites")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                productName.text = item.name
                productPrice.text = "R${String.format("%.2f", item.price)}"

                val imageUrl = when {
                    !item.coverImage.isNullOrBlank() -> item.coverImage
                    item.images.isNotEmpty() -> item.images.first().url
                    else -> null
                }

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