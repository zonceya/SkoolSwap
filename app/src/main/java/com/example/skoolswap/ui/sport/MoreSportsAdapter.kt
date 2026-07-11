package com.example.skoolswap.ui.sport

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemChipWhiteBinding
import com.example.skoolswap.domain.model.SportItem
import com.example.skoolswap.utils.extensions.dpToPx

class MoreSportsAdapter(
    private val onItemClick: (SportItem) -> Unit
) : ListAdapter<SportItem, MoreSportsAdapter.ViewHolder>(SportDiffCallback()) {

    // ✅ REMOVED: custom submitList() - use parent's implementation
    // Just call adapter.submitList(items) from Fragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChipWhiteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SportDiffCallback : DiffUtil.ItemCallback<SportItem>() {
        override fun areItemsTheSame(oldItem: SportItem, newItem: SportItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SportItem, newItem: SportItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemChipWhiteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sport: SportItem) {
            binding.chipText.text = sport.name

            val typedValue = android.util.TypedValue()

            itemView.context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurface, typedValue, true
            )
            val bgColor = typedValue.data

            itemView.context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, typedValue, true
            )
            val textAndBorderColor = typedValue.data

            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(bgColor)
                setStroke(6, textAndBorderColor)
                cornerRadius = 16f.dpToPx(itemView.context)
            }
            binding.root.background = drawable
            binding.chipText.setTextColor(textAndBorderColor)

            binding.root.setOnClickListener { onItemClick(sport) }
        }
    }
}