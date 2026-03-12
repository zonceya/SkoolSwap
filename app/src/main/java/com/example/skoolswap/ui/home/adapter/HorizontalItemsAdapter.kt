package com.example.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeProductBinding
import com.example.skoolswap.domain.model.Item

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

            // FIXED: Using existing ic_create_item_placeholder instead of missing item_placeholder
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