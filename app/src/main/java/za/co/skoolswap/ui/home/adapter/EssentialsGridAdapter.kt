package za.co.skoolswap.ui.home.adapter

import android.util.Log
import android.view.LayoutInflater
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

        // EssentialsGridAdapter.kt - bind()

        fun bind(categoryItem: EssentialsCategoryItem) {
            binding.categoryName.text = categoryItem.displayName

            // ✅ Try coverImage first (from API)
            val imageUrl = categoryItem.item.coverImage?.takeIf { it.isNotBlank() }
                ?: categoryItem.item.image?.takeIf { it.isNotBlank() }
                ?: categoryItem.item.images.firstOrNull()?.url?.takeIf { it.isNotBlank() }

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(getPlaceholderDrawable(categoryItem.categoryType))
                    .error(getPlaceholderDrawable(categoryItem.categoryType))
                    .centerCrop()
                    .into(binding.categoryIcon)
                Timber.tag(TAG).d("Loaded image for ${categoryItem.displayName}: $imageUrl")
            } else {
                val iconResId = getPlaceholderDrawable(categoryItem.categoryType)
                binding.categoryIcon.setImageResource(iconResId)
                Timber.tag(TAG).d("Using local placeholder for ${categoryItem.displayName}")
            }

            binding.root.setOnClickListener {
                onCategoryClick(categoryItem)
            }
        }

        private fun getPlaceholderDrawable(categoryType: String): Int {
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