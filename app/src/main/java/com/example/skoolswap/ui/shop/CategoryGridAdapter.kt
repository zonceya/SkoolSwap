package com.example.skoolswap.ui.shop

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemCategoryGridItemBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount

class CategoryGridAdapter(
    private val onItemClick: (String) -> Unit
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
        return GridViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GridViewHolder(
        private val binding: ItemCategoryGridItemBinding,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            Log.d("CategoryGridAdapter", "Binding grid item: ${item.name}, viewCount: ${item.viewCount}, status: ${item.status}")

            binding.productName.text = item.name
            binding.productPrice.text = "R${item.price}"

            if (item.viewCount > 0) {
                Log.d("CategoryGridAdapter", "  ✅ Showing view count: ${item.viewCount}")
                binding.viewCountContainer.visibility = View.VISIBLE
                binding.viewCount.text = item.viewCount.formatViewCount()
            } else {
                Log.d("CategoryGridAdapter", "  ❌ Hiding view count (value: ${item.viewCount})")
                binding.viewCountContainer.visibility = View.GONE
            }

            // SOLD BADGE
            if (item.status == "sold" || item.quantity <= 0) {
                Log.d("CategoryGridAdapter", "  ✅ Showing SOLD badge")
                binding.soldBadge.visibility = View.VISIBLE
            } else {
                binding.soldBadge.visibility = View.GONE
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

            // ✅ ADD THIS - Set click listener
            binding.root.setOnClickListener {
                Log.d("CategoryGridAdapter", "✅ Grid item clicked: ${item.name} (ID: ${item.id})")
                onItemClick(item.id)
            }
        }
    }
}