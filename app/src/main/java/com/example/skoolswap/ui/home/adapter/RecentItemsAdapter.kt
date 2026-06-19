package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeRecentItemBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import java.text.NumberFormat
import java.util.Locale
import kotlin.collections.List

class RecentItemsAdapter(
    private val onItemClick: (Item) -> Unit,
    private val maxItems: Int = 4
) : ListAdapter<Item, RecentItemsAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(old: Item, new: Item) = old.id == new.id
            override fun areContentsTheSame(old: Item, new: Item) = old == new
        }
    }

    fun updateItems(newItems: List<Item>) {
        submitList(newItems.take(maxItems))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeRecentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHomeRecentItemBinding
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
            binding.recentTitle.text = item.name

            // Use schoolName, fall back to brandName, then generic label
            binding.recentSchool.text = when {
                !item.schoolName.isNullOrBlank() -> item.schoolName
                !item.brandName.isNullOrBlank() -> item.brandName
                else -> "School Item"
            }

            // Format price - NO trailing .00
            val price = item.price
            if (price > 0) {
                binding.recentPrice.text = formatPrice(price)
                binding.recentPrice.visibility = View.VISIBLE
            } else {
                binding.recentPrice.visibility = View.GONE
            }

            // Sold badge
            binding.soldBadge.visibility = if (item.status == "sold" || item.quantity <= 0) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Image - fitCenter to show full image
            val imageUrl = item.coverImage
                ?: item.images?.firstOrNull()?.url

            Glide.with(binding.root.context)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .fitCenter()
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.recentImage)
        }

        private fun formatPrice(price: Double): String {
            return try {
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                formatter.currency = java.util.Currency.getInstance("ZAR")
                val formatted = formatter.format(price)
                // Remove the trailing .00 if present
                if (formatted.endsWith(".00")) {
                    formatted.replace(".00", "")
                } else {
                    formatted
                }
            } catch (e: Exception) {
                // Fallback formatting
                if (price % 1.0 == 0.0) {
                    "R${String.format("%,d", price.toInt())}"
                } else {
                    "R${String.format("%,.2f", price)}"
                }
            }
        }
    }
}