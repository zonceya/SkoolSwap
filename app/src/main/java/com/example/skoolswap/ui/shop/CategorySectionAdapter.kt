package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemCategorySectionBinding
import com.example.skoolswap.domain.model.ItemCategorySection

class CategorySectionAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<CategorySectionAdapter.SectionViewHolder>() {

    private var sections: List<ItemCategorySection> = emptyList()

    fun submitSections(newSections: List<ItemCategorySection>) {
        sections = newSections
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemCategorySectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SectionViewHolder(binding, onItemClick, this)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position], position)
    }

    override fun getItemCount(): Int = sections.size

    class SectionViewHolder(
        private val binding: ItemCategorySectionBinding,
        private val onItemClick: (String) -> Unit,
        private val adapter: CategorySectionAdapter  // Pass adapter reference
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: ItemCategorySection, position: Int) {
            binding.categoryTitle.text = section.categoryName
            binding.itemCount.text = "${section.items.size} items"

            // Set arrow direction based on expanded state
            val arrowRes = if (section.isExpanded) {
                R.drawable.ic_chevron_down
            } else {
                R.drawable.ic_chevron_right
            }
            binding.arrowIcon.setImageResource(arrowRes)

            // Determine which items to show
            val itemsToShow = if (section.isExpanded) {
                section.items  // Show ALL items
            } else {
                section.items.take(2)  // Show only FIRST 2 items
            }

            // Setup grid adapter
            val gridAdapter = CategoryGridAdapter { itemId ->
                onItemClick(itemId)
            }

            binding.categoryGrid.apply {
                layoutManager = GridLayoutManager(binding.root.context, 2)
                adapter = gridAdapter
            }

            gridAdapter.submitList(itemsToShow)

            // Handle header click (expand/collapse)
            binding.headerContainer.setOnClickListener {
                section.isExpanded = !section.isExpanded
                adapter.notifyItemChanged(position)  // Use adapter reference
            }

            // Handle arrow click
            binding.arrowIcon.setOnClickListener {
                section.isExpanded = !section.isExpanded
                adapter.notifyItemChanged(position)  // Use adapter reference
            }

            // Hide divider for last category
            val isLastSection = position == adapter.itemCount - 1
            binding.divider.visibility = if (isLastSection) View.GONE else View.VISIBLE
        }
    }
}

