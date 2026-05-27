package com.example.skoolswap.ui.detail.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemSimilarProductBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount

class SimilarItemsAdapter(
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<SimilarItemsAdapter.ViewHolder>() {

    private var items: List<Item> = emptyList()
    private val TAG = "SimilarItemsAdapter"

    fun submitList(newItems: List<Item>) {
        Log.d(TAG, "submitList called with ${newItems.size} items")
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimilarProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size


    inner class ViewHolder(
        private val binding: ItemSimilarProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }

        fun bind(item: Item) {
            binding.productTitle.text = item.name
            binding.productPrice.text = "R${String.format("%.2f", item.price)}"

            // ✅ SHOW SOLD BADGE
            if (item.status == "sold" || item.quantity <= 0) {
                binding.soldBadge.visibility = View.VISIBLE
            } else {
                binding.soldBadge.visibility = View.GONE
            }



            // Load image
            if (item.images.isNotEmpty()) {
                val imageUrl = item.images.first().url
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_create_item_placeholder)
                    .error(R.drawable.ic_create_item_placeholder)
                    .centerCrop()
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }

            // Show size if available
            if (!item.sizeName.isNullOrBlank()) {
                binding.productSize.text = item.sizeName
                binding.productSize.visibility = View.VISIBLE
            } else {
                binding.productSize.visibility = View.GONE
            }
        }
    }
}