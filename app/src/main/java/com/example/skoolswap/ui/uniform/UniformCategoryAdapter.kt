// ui/uniform/UniformCategoryAdapter.kt
package com.example.skoolswap.ui.uniform

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemUniformCategoryBinding

class UniformCategoryAdapter(
    private val onCategoryClick: (UniformCategory) -> Unit
) : RecyclerView.Adapter<UniformCategoryAdapter.ViewHolder>() {

    private var categories = listOf<UniformCategory>()

    fun submitList(newCategories: List<UniformCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUniformCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    inner class ViewHolder(
        private val binding: ItemUniformCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: UniformCategory) {
            binding.categoryTitle.text = category.name
            binding.categoryImage.setImageResource(category.imageResId)

            // Apply special background for Jerseys & Pullovers (ID 28)
            when (category.id) {
                28 -> { // Jerseys & Pullovers
                    binding.root.setBackgroundResource(R.drawable.bg_shop_offer)
                }
                else -> {
                    binding.root.setBackgroundResource(R.drawable.bg_shop_card)
                }
            }

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}