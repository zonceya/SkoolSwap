package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
            // Set product title
            binding.productTitle.text = item.name

            // Format price properly (e.g., "R1,104.00" instead of "R1104.0")
            binding.productPrice.text = formatPrice(item.price)

            // ✅ USE THE EXISTING resolveImageUrl() METHOD
            val imageUrl = item.resolveImageUrl()

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_create_item_placeholder)
                    .error(R.drawable.ic_create_item_placeholder)
                    .centerCrop()
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }

            // Handle SOLD badge (using availableQuantity from your model)
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