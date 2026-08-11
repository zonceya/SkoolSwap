package za.co.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import timber.log.Timber
import za.co.skoolswap.R
import za.co.skoolswap.databinding.ItemHomeCategoryBinding
import za.co.skoolswap.ui.home.viewholders.EssentialsCategoryItem

private const val TAG = "EssentialsGridAdapter"

class EssentialsGridAdapter(
    private var items: List<EssentialsCategoryItem> = emptyList(),
    private val onCategoryClick: (EssentialsCategoryItem) -> Unit
) : RecyclerView.Adapter<EssentialsGridAdapter.ViewHolder>() {

    fun updateItems(newItems: List<EssentialsCategoryItem>) {
        items = newItems
        notifyDataSetChanged()
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
            val item = categoryItem.item

            binding.categoryName.text = categoryItem.displayName

            // Sold badge
            if (categoryItem.isPlaceholder) {
                binding.soldBadge.visibility = View.GONE
            } else if (item.isSold || item.quantity <= 0) {
                binding.soldBadge.visibility = View.VISIBLE
            } else {
                binding.soldBadge.visibility = View.GONE
            }

            val placeholderRes = getPlaceholderDrawable(categoryItem.categoryType)
            val imageUrl = item.resolveImageUrl()
                ?: categoryItem.defaultImageUrl

            // Always clear previous Glide request (important when recycling)
            Glide.with(binding.root.context).clear(binding.categoryIcon)

            if (!imageUrl.isNullOrBlank() && !categoryItem.isPlaceholder) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .centerCrop()
                    .into(binding.categoryIcon)
                Timber.tag(TAG).d("Loaded remote image for ${categoryItem.displayName}: $imageUrl")
            } else {
                // ✅ Explicit fallback – must show local image
                binding.categoryIcon.setImageResource(placeholderRes)
                binding.categoryIcon.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                Timber.tag(TAG).d("Using local placeholder for ${categoryItem.displayName} res=$placeholderRes")
            }

            binding.root.setOnClickListener {
                onCategoryClick(categoryItem)
            }
        }

        private fun getPlaceholderDrawable(categoryType: String): Int {
            return when (categoryType.lowercase()) {
                "uniforms", "uniform" -> R.drawable.ic_uniform_placeholder
                "sports", "sport" -> R.drawable.ic_sports_placeholder
                "stationery" -> R.drawable.ic_stationery_placeholder
                "accessories", "accessory" -> R.drawable.ic_accessories_placeholder
                else -> R.drawable.ic_create_item_placeholder
            }
        }
    }
}