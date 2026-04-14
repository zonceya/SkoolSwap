package com.example.skoolswap.ui.sport

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemChipBlackBinding
import com.example.skoolswap.domain.model.GearItem

class GearAdapter(
    private val onItemClick: (GearItem) -> Unit
) : RecyclerView.Adapter<GearAdapter.ViewHolder>() {

    private var items: List<GearItem> = emptyList()

    fun submitList(newItems: List<GearItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChipBlackBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemChipBlackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gear: GearItem) {
            binding.chipText.text = gear.name
            binding.root.setOnClickListener {
                onItemClick(gear)
            }
        }
    }
}