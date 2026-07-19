package com.example.skoolswap.ui.detail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemSimilarProductBinding
import com.example.skoolswap.domain.model.Item
import timber.log.Timber

class SimilarItemsAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, SimilarItemsAdapter.ViewHolder>(SimilarItemDiffCallback()) {

    private val TAG = "SimilarItemsAdapter"

    // ✅ REMOVED: custom submitList() - use parent's implementation directly
    // Just call adapter.submitList(items) from Fragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimilarProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SimilarItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemSimilarProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: Item) {
            binding.productTitle.text = item.name
            binding.productPrice.text = "R${String.format("%.2f", item.price)}"

            // Sold badge
            if (item.status == "sold" || item.quantity <= 0) {
                binding.soldBadge.visibility = View.VISIBLE
            } else {
                binding.soldBadge.visibility = View.GONE
            }

            // ✅ Use resolveImageUrl() helper for consistency
            val imageUrl = item.resolveImageUrl()
                ?: item.coverImage
                ?: item.images.firstOrNull { !it.url.isNullOrBlank() && it.url.startsWith("http") }?.url

            if (!imageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_create_item_placeholder)
                    .error(R.drawable.ic_create_item_placeholder)
                    .centerCrop()
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }

            // Size
            if (!item.sizeName.isNullOrBlank()) {
                binding.productSize.text = item.sizeName.replace("Adult", "UK")
                binding.productSize.visibility = View.VISIBLE
            } else {
                binding.productSize.visibility = View.GONE
            }

            Timber.tag(TAG)
                .d("Binding item: ${item.name}, imageUrl: $imageUrl, images: ${item.images.size}, cover: ${item.coverImage}")
        }
    }
}