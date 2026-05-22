package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemCategoryGridItemBinding
import com.example.skoolswap.domain.model.Item

class CategoryGridAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryGridAdapter.GridViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemCategoryGridItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GridViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GridViewHolder(
        private val binding: ItemCategoryGridItemBinding,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.productName.text = item.name
            binding.productPrice.text = "R ${String.format("%.2f", item.price)}"

            val imageUrl = item.images.firstOrNull()?.url ?: item.coverImage
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_create_item_placeholder)
                    .error(R.drawable.ic_create_item_placeholder)
                    .centerCrop()
                    .into(binding.productImage)
            }

            binding.root.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }
}