package za.skoolswap.app.ui.uniform

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.R
import za.co.skoolswap.databinding.ItemUniformCategoryBinding
import za.co.skoolswap.domain.model.UniformCategory

class UniformCategoryAdapter(
    private val onCategoryClick: (UniformCategory) -> Unit
) : ListAdapter<UniformCategory, UniformCategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUniformCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<UniformCategory>() {
        override fun areItemsTheSame(oldItem: UniformCategory, newItem: UniformCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UniformCategory, newItem: UniformCategory): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemUniformCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: UniformCategory) {
            binding.categoryTitle.text = category.name
            binding.categoryImage.setImageResource(category.imageResId)

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