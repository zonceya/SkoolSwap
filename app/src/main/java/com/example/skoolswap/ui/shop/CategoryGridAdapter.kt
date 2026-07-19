package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.ItemCategoryGridItemBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount
import timber.log.Timber

class CategoryGridAdapter(
    private val onItemClick: (String) -> Unit,
    private val onSoldToggle: ((String, Boolean) -> Unit)? = null,
    private val isShopMode: Boolean = false
) : ListAdapter<Item, CategoryGridAdapter.GridViewHolder>(ItemDiffCallback()) {

    // ✅ REMOVED: private var items: List<Item> = emptyList()
    // ✅ REMOVED: fun submitList() override - use ListAdapter's built-in submitList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemCategoryGridItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GridViewHolder(binding, onItemClick, onSoldToggle, isShopMode)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        // ✅ Use getItem() from ListAdapter, not local list
        holder.bind(getItem(position))
    }

    // ✅ REMOVED: override fun getItemCount() - ListAdapter provides this

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }

        // ✅ OPTIONAL: For better performance when items have same ID but changed content
        override fun getChangePayload(oldItem: Item, newItem: Item): Any? {
            return null // or return a bundle of changed fields for partial updates
        }
    }

    class GridViewHolder(
        private val binding: ItemCategoryGridItemBinding,
        private val onItemClick: (String) -> Unit,
        private val onSoldToggle: ((String, Boolean) -> Unit)?,
        private val isShopMode: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            Timber.tag(LogTags.UI).d("Binding grid item: ${item.name}, viewCount: ${item.viewCount}, status: ${item.status}")

            binding.productName.text = item.name
            binding.productPrice.text = "R${item.price}"

            if (item.viewCount > 0) {
                Timber.tag(LogTags.UI).d("  ✅ Showing view count: ${item.viewCount}")
                binding.viewCountContainer.visibility = View.VISIBLE
                binding.viewCount.text = item.viewCount.formatViewCount()
            } else {
                Timber.tag(LogTags.UI).d("  ❌ Hiding view count (value: ${item.viewCount})")
                binding.viewCountContainer.visibility = View.GONE
            }

            val isSold = item.status == "sold" || item.quantity <= 0

            if (isShopMode) {
                binding.soldBadge.visibility = View.VISIBLE

                if (isSold) {
                    binding.soldBadge.text = binding.root.context.getString(R.string.shop_badge_sold)
                    binding.soldBadge.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    )
                    binding.productName.alpha = 0.5f
                    binding.productPrice.alpha = 0.5f
                } else {
                    binding.soldBadge.text = binding.root.context.getString(R.string.shop_badge_mark_sold)
                    binding.soldBadge.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.green_dark)
                    )
                    binding.productName.alpha = 1f
                    binding.productPrice.alpha = 1f
                }

                if (onSoldToggle != null) {
                    binding.soldBadge.setOnClickListener {
                        val newSoldStatus = !isSold
                        Timber.tag(LogTags.UI).d("🔄 Toggling sold status for ${item.name}: $newSoldStatus")
                        onSoldToggle(item.id, newSoldStatus)
                    }
                }
            } else {
                if (isSold) {
                    binding.soldBadge.visibility = View.VISIBLE
                    binding.soldBadge.text = binding.root.context.getString(R.string.shop_badge_sold)
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

            binding.root.setOnClickListener {
                Timber.tag(LogTags.UI).d("✅ Grid item clicked: ${item.name} (ID: ${item.id})")
                onItemClick(item.id)
            }
        }
    }
}