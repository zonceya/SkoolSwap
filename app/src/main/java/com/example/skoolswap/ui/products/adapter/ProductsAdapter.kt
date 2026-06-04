package com.example.skoolswap.ui.products.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeProductBinding
import com.example.skoolswap.databinding.ItemProductGridBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount
import timber.log.Timber

class ProductsAdapter(
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemProductGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]

                    val bundle = bundleOf(
                        "itemId" to item.id,
                        "source" to "products_screen"
                    )

                    binding.root.findNavController().navigate(R.id.itemDetailFragment, bundle)
                }
            }
        }

        fun bind(item: Item) {
            Timber.tag("ProductsAdapter")
                .d("Binding: ${item.name} | cover: ${item.coverImage} | images: ${item.images.size} | status: ${item.status}")

            binding.productTitle.text = item.name
            binding.productPrice.text = "R${String.format("%.2f", item.price)}"

            // Sold badge
            if (item.status == "sold" || item.quantity <= 0) {
                binding.soldBadge.visibility = View.VISIBLE
            } else {
                binding.soldBadge.visibility = View.GONE
            }
            val imageUrl = item.resolveImageUrl()

            if (!imageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
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