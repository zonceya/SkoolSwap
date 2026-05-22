package com.example.skoolswap.ui.products.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeProductBinding
import com.example.skoolswap.domain.model.Item

class ProductsAdapter(
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

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
                    val item = items[position]

                    // Navigate to detail fragment
                    val bundle = bundleOf(
                        "itemId" to item.id,
                        "source" to "products_screen"
                    )

                    binding.root.findNavController().navigate(R.id.itemDetailFragment, bundle)
                }
            }
        }

        fun bind(item: Item) {
            binding.productTitle.text = item.name
            binding.productPrice.text = "R${item.price}"

            if (item.images.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.images.first().url)
                    .placeholder(R.drawable.ic_create_item_placeholder)
                    .error(R.drawable.ic_create_item_placeholder)
                    .fitCenter()
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }
        }
    }
}