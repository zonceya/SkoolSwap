package com.example.skoolswap.ui.sport

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemChipBlackBinding
import com.example.skoolswap.domain.model.GearItem
import com.example.skoolswap.utils.extensions.dpToPx

class GearAdapter(
    private val onItemClick: (GearItem) -> Unit
) : ListAdapter<GearItem, GearAdapter.ViewHolder>(GearDiffCallback()) {

    // ✅ REMOVED: custom submitList() - use parent's implementation
    // Just call adapter.submitList(items) from Fragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChipBlackBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GearDiffCallback : DiffUtil.ItemCallback<GearItem>() {
        override fun areItemsTheSame(oldItem: GearItem, newItem: GearItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GearItem, newItem: GearItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemChipBlackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gear: GearItem) {
            binding.chipText.text = gear.name

            val typedValue = android.util.TypedValue()

            // Resolve colors
            itemView.context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, typedValue, true
            )
            val bgColor = typedValue.data

            itemView.context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurface, typedValue, true
            )
            val textAndBorderColor = typedValue.data

            // Apply rounded background with border
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(bgColor)
                setStroke(2, textAndBorderColor)
                cornerRadius = 16f.dpToPx(itemView.context)
            }
            binding.root.background = drawable
            binding.chipText.setTextColor(textAndBorderColor)

            binding.root.setOnClickListener { onItemClick(gear) }
        }
    }
}