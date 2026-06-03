package com.example.skoolswap.ui.home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeProductBinding
import com.example.skoolswap.domain.model.Item
import java.text.NumberFormat
import java.util.Locale

class HorizontalItemsAdapter(
    private val items: List<Item>,
    private val sectionType: String,
    private val onItemClick: (Item, String) -> Unit
) : RecyclerView.Adapter<HorizontalItemsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemHomeProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position], sectionType)
                }
            }
        }

        fun bind(item: Item) {
            Log.d("HorizontalItemsAdapter", "=== BINDING $sectionType ITEM ===")
            Log.d("HorizontalItemsAdapter", "Item: ${item.name}")
            Log.d("HorizontalItemsAdapter", "Price: ${item.price}")
            Log.d("HorizontalItemsAdapter", "coverImage: ${item.coverImage}")
            Log.d("HorizontalItemsAdapter", "images size: ${item.images.size}")

            // Set product title
            binding.productTitle.text = item.name
            binding.productTitle.visibility = View.VISIBLE

            // Format price properly
            binding.productPrice.text = formatPrice(item.price)
            binding.productPrice.visibility = View.VISIBLE

            // Get image URL
            val imageUrl = item.resolveImageUrl()
            Log.d("HorizontalItemsAdapter", "resolved imageUrl: $imageUrl")

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .centerCrop()
                    )
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.productImage)
                Log.d("HorizontalItemsAdapter", "✅ Loading image from: $imageUrl")
            } else {
                Log.w("HorizontalItemsAdapter", "❌ No image URL for: ${item.name}")
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }
// After Glide
            binding.productImage.scaleType = ImageView.ScaleType.CENTER_CROP

// Optional: Force square image feel
            val params = binding.productImage.layoutParams
            params.height = (binding.root.context.resources.displayMetrics.widthPixels * 0.42).toInt() // ~42% of screen
            binding.productImage.layoutParams = params
            // Handle SOLD badge
            val isSold = item.status == "sold" || item.availableQuantity <= 0
            if (isSold) {
                binding.soldBadge.visibility = View.VISIBLE
                binding.productTitle.alpha = 0.6f
                binding.productPrice.alpha = 0.6f
            } else {
                binding.soldBadge.visibility = View.GONE
                binding.productTitle.alpha = 1f
                binding.productPrice.alpha = 1f
            }
        }

        private fun formatPrice(price: Double): String {
            return try {
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                formatter.currency = java.util.Currency.getInstance("ZAR")
                formatter.format(price)
            } catch (e: Exception) {
                "R${String.format("%.2f", price)}"
            }
        }
    }
}