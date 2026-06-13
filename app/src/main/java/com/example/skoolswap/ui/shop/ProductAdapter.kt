package com.example.skoolswap.ui.shop

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemProductBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount

class ProductAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<Item>()

    fun submitList(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                // ✅ ADD LOGGING
                Log.d("ProductAdapter", "Binding item: ${item.name}, viewCount: ${item.viewCount}, status: ${item.status}")

                productName.text = item.name
                productPrice.text = "R${item.price}"

                // Show view count badge
                if (item.viewCount > 0) {
                    Log.d("ProductAdapter", "  ✅ Showing view count: ${item.viewCount}")
                    viewCountContainer.visibility = View.VISIBLE
                    viewCount.text = item.viewCount.formatViewCount()
                } else {
                    Log.d("ProductAdapter", "  ❌ Hiding view count (value: ${item.viewCount})")
                    viewCountContainer.visibility = View.GONE
                }

                // Show sold badge
                if (item.status == "sold" || item.quantity <= 0) {
                    Log.d("ProductAdapter", "  ✅ Showing SOLD badge")
                    soldBadge.visibility = View.VISIBLE
                    productName.alpha = 0.5f
                    productPrice.alpha = 0.5f
                } else {
                    soldBadge.visibility = View.GONE
                    productName.alpha = 1f
                    productPrice.alpha = 1f
                }

                // Load image
                val imageUrl = item.images.firstOrNull()?.url ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .centerCrop()
                        .into(productImage)
                } else {
                    productImage.setImageResource(R.drawable.ic_create_item_placeholder)
                }

                root.setOnClickListener { onItemClick(item.id) }
            }
        }
    }
}