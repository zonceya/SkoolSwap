package com.example.skoolswap.ui.shop

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemCategoryGridItemBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount

class CategoryGridAdapter(
    private val onItemClick: (String) -> Unit,
    private val onSoldToggle: ((String, Boolean) -> Unit)? = null,
    private val isShopMode: Boolean = false
) : RecyclerView.Adapter<CategoryGridAdapter.GridViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemCategoryGridItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GridViewHolder(binding, onItemClick, onSoldToggle, isShopMode)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GridViewHolder(
        private val binding: ItemCategoryGridItemBinding,
        private val onItemClick: (String) -> Unit,
        private val onSoldToggle: ((String, Boolean) -> Unit)?,
        private val isShopMode: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            Log.d("CategoryGridAdapter", "Binding grid item: ${item.name}, viewCount: ${item.viewCount}, status: ${item.status}")

            binding.productName.text = item.name
            binding.productPrice.text = "R${item.price}"

            // ✅ SHOW VIEW COUNT BADGE (Eye icon + count)
            if (item.viewCount > 0) {
                Log.d("CategoryGridAdapter", "  ✅ Showing view count: ${item.viewCount}")
                binding.viewCountContainer.visibility = View.VISIBLE
                binding.viewCount.text = item.viewCount.formatViewCount()
            } else {
                Log.d("CategoryGridAdapter", "  ❌ Hiding view count (value: ${item.viewCount})")
                binding.viewCountContainer.visibility = View.GONE
            }

            // Check if item is sold
            val isSold = item.status == "sold" || item.quantity <= 0

            // ===== SOLD BADGE =====
            if (isShopMode) {
                // SHOP MODE: Always visible, acts as toggle
                binding.soldBadge.visibility = View.VISIBLE

                if (isSold) {
                    binding.soldBadge.text = "SOLD"
                    binding.soldBadge.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    )
                    // Gray out text for sold items
                    binding.productName.alpha = 0.5f
                    binding.productPrice.alpha = 0.5f
                } else {
                    binding.soldBadge.text = "MARK SOLD"
                    binding.soldBadge.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.teal_200)
                    )
                    binding.productName.alpha = 1f
                    binding.productPrice.alpha = 1f
                }

                // Click to toggle
                if (onSoldToggle != null) {
                    binding.soldBadge.setOnClickListener {
                        val newSoldStatus = !isSold
                        Log.d("CategoryGridAdapter", "🔄 Toggling sold status for ${item.name}: $newSoldStatus")
                        onSoldToggle(item.id, newSoldStatus)
                    }
                }
            } else {
                // HOME MODE: Only show if sold
                if (isSold) {
                    binding.soldBadge.visibility = View.VISIBLE
                    binding.soldBadge.text = "SOLD"
                    binding.soldBadge.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    )
                    binding.soldBadge.setOnClickListener(null)
                    binding.productName.alpha = 0.5f
                    binding.productPrice.alpha = 0.5f
                } else {
                    binding.soldBadge.visibility = View.GONE
                    binding.productName.alpha = 1f
                    binding.productPrice.alpha = 1f
                }
            }

            // Load image
            val imageUrl = item.images.firstOrNull()?.url ?: ""
            if (imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_create_item_placeholder)
                    .error(R.drawable.ic_create_item_placeholder)
                    .centerCrop()
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }

            // Click listener for entire card
            binding.root.setOnClickListener {
                Log.d("CategoryGridAdapter", "✅ Grid item clicked: ${item.name} (ID: ${item.id})")
                onItemClick(item.id)
            }
        }
    }
}