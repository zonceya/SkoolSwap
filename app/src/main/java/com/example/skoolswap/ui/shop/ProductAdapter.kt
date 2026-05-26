package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.View
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

        fun bind(item: Item) {
            binding.apply {
                productName.text = item.name
                productPrice.text = "R${item.price}"

                // Show sold badge if item is sold
                if (item.status == "sold" || item.quantity <= 0) {
                    soldBadge.visibility = View.VISIBLE
                    productName.alpha = 0.5f
                    productPrice.alpha = 0.5f
                } else {
                    soldBadge.visibility = View.GONE
                    productName.alpha = 1f
                    productPrice.alpha = 1f
                }

                // Load image
                if (!item.images.isNullOrEmpty()) {
                    Glide.with(binding.root.context)
                        .load(item.images.first().url)
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .centerCrop()
                        .into(binding.productImage)
                } else {
                    binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
                }

                root.setOnClickListener {
                    onItemClick(item.id)
                }
            }
        }
    }
}