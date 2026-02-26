package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemProductBinding
import com.example.skoolswap.domain.model.Item  // Import domain model

class ProductAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<Item>()  // Use domain Item

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

        fun bind(item: Item) {  // Now accepts domain Item
            binding.apply {
                productName.text = item.name
                productPrice.text = "R${item.price}"  // Format price

                // Get first image URL or empty string
                val imageUrl = item.images.firstOrNull()?.url ?: ""

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .thumbnail(0.25f)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(productImage)

                root.setOnClickListener {
                    onItemClick(item.id)  // Pass item ID for editing
                }
            }
        }
    }
}