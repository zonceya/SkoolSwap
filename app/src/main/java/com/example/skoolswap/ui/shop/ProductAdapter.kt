package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemProductBinding

// Data class for products
data class Product(
    val name: String,
    val price: String,
    val imageUrl: String
)

// Product Adapter
class ProductAdapter : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<Product>()

    fun submitList(newItems: List<Product>) {
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

        fun bind(product: Product) {
            binding.apply {
                productName.text = product.name
                productPrice.text = product.price

                // Load image from URL using Glide
                Glide.with(itemView.context)
                    .load(product.imageUrl)
                    .thumbnail(0.25f)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(productImage)

                root.setOnClickListener {
                    Toast.makeText(
                        root.context,
                        "Clicked: ${product.name} - ${product.price}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}