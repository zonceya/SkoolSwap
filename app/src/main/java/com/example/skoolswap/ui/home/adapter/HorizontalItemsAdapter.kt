package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeProductBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount

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
            // FIXED: Changed from productName to productTitle
            binding.productTitle.text = item.name
            binding.productPrice.text = "R${item.price}"


            // ✅ ADD SOLD BADGE
            if (item.status == "sold" || item.quantity <= 0) {
                binding.soldBadge.visibility = View.VISIBLE
                binding.productTitle.alpha = 0.5f
                binding.productPrice.alpha = 0.5f
            } else {
                binding.soldBadge.visibility = View.GONE
                binding.productTitle.alpha = 1f
                binding.productPrice.alpha = 1f
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
            }
        }
    }
}