package za.co.skoolswap.ui.sport

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import za.co.skoolswap.databinding.ItemChipBlackBinding
import za.co.skoolswap.domain.model.GearItem
import za.co.skoolswap.utils.extensions.dpToPx

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

            val typedValue = TypedValue()

            // Resolve colors
            itemView.context.theme.resolveAttribute(
                R.attr.colorOnSurface, typedValue, true
            )
            val bgColor = typedValue.data

            itemView.context.theme.resolveAttribute(
                R.attr.colorSurface, typedValue, true
            )
            val textAndBorderColor = typedValue.data

            // Apply rounded background with border
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
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