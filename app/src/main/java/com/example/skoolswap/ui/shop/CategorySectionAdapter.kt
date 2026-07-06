package com.example.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.ItemCategorySectionBinding
import com.example.skoolswap.domain.model.ItemCategorySection
import timber.log.Timber

class CategorySectionAdapter(
    private val onItemClick: (String) -> Unit,
    private val onSoldToggle: ((String, Boolean) -> Unit)? = null,
    private val isShopMode: Boolean = false
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
        return SectionViewHolder(binding, onItemClick, onSoldToggle, isShopMode, this)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position], position)
    }

    override fun getItemCount(): Int = sections.size

    class SectionViewHolder(
        private val binding: ItemCategorySectionBinding,
        private val onItemClick: (String) -> Unit,
        private val onSoldToggle: ((String, Boolean) -> Unit)?,
        private val isShopMode: Boolean,
        private val adapter: CategorySectionAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: ItemCategorySection, position: Int) {
            binding.categoryTitle.text = section.categoryName
            binding.itemCount.text = "${section.items.size} items"

            val arrowRes = if (section.isExpanded) {
                R.drawable.ic_chevron_down
            } else {
                R.drawable.ic_chevron_right
            }
            binding.arrowIcon.setImageResource(arrowRes)

            val itemsToShow = if (section.isExpanded) {
                section.items
            } else {
                section.items.take(2)
            }

            val gridAdapter = CategoryGridAdapter(
                onItemClick = { itemId ->
                    onItemClick(itemId)
                },
                onSoldToggle = onSoldToggle,
                isShopMode = isShopMode
            )

            binding.categoryGrid.apply {
                layoutManager = GridLayoutManager(binding.root.context, 2)
                adapter = gridAdapter
            }

            gridAdapter.submitList(itemsToShow)

            binding.headerContainer.setOnClickListener {
                section.isExpanded = !section.isExpanded
                adapter.notifyItemChanged(position)
            }

            binding.arrowIcon.setOnClickListener {
                section.isExpanded = !section.isExpanded
                adapter.notifyItemChanged(position)
            }

            val isLastSection = position == adapter.itemCount - 1
            binding.divider.visibility = if (isLastSection) View.GONE else View.VISIBLE
        }
    }
}