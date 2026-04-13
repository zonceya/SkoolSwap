package com.example.skoolswap.ui.home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemHomeCategoryBinding
import com.example.skoolswap.domain.model.Item

private const val TAG = "EssentialsGridAdapter"

class EssentialsGridAdapter(
    private val items: List<Pair<Item, String>>,
    private val onItemClick: (Item, String) -> Unit
) : RecyclerView.Adapter<EssentialsGridAdapter.ViewHolder>() {

    init {
        Log.d(TAG, "=== GRID ADAPTER CREATED ===")
        Log.d(TAG, "Items count: ${items.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, categoryType) = items[position]
        holder.bind(item, categoryType)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemHomeCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item, categoryType: String) {
            // Set category name
            val displayName = when (categoryType) {
                "uniforms" -> "Uniforms"
                "sports" -> "Sports"
                "stationery" -> "Stationery"
                "accessories" -> "Accessories"
                else -> categoryType.replaceFirstChar { it.uppercase() }
            }
            binding.categoryName.text = displayName

            // Set category icon
            val iconResId = when (categoryType) {
                "uniforms" -> R.drawable.ic_uniform_placeholder
                "sports" -> R.drawable.ic_sports_placeholder
                "stationery" -> R.drawable.ic_stationery_placeholder
                "accessories" -> R.drawable.ic_accessories_placeholder
                else -> R.drawable.ic_create_item_placeholder
            }
            binding.categoryIcon.setImageResource(iconResId)

            binding.root.setOnClickListener {
                onItemClick(item, categoryType)
            }
        }
    }
}