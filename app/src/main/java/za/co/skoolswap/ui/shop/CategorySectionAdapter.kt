package za.co.skoolswap.ui.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.R
import za.co.skoolswap.databinding.ItemCategorySectionBinding
import za.co.skoolswap.domain.model.ItemCategorySection

class CategorySectionAdapter(
    private val onItemClick: (String) -> Unit,
    private val onSoldToggle: ((String, Boolean) -> Unit)? = null,
    private val isShopMode: Boolean = false
) : ListAdapter<ItemCategorySection, CategorySectionAdapter.SectionViewHolder>(
    SectionDiffCallback()
) {

    // ✅ Track expanded state outside the adapter data
    private val expandedState = mutableMapOf<String, Boolean>()

    // ✅ Create ONE nested adapter instance and reuse it
    private val nestedAdapter = CategoryGridAdapter(
        onItemClick = onItemClick,
        onSoldToggle = onSoldToggle,
        isShopMode = isShopMode
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemCategorySectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SectionViewHolder(binding, nestedAdapter, this)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = getItem(position)
        val isExpanded = expandedState[section.categoryName] ?: false
        holder.bind(section, isExpanded, position == itemCount - 1)
    }

    // ✅ Toggle expansion state
    fun toggleExpanded(categoryName: String) {
        val current = expandedState[categoryName] ?: false
        expandedState[categoryName] = !current
        // ✅ Only notify that this item changed, not everything
        val position = currentList.indexOfFirst { it.categoryName == categoryName }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    class SectionDiffCallback : DiffUtil.ItemCallback<ItemCategorySection>() {
        override fun areItemsTheSame(
            oldItem: ItemCategorySection,
            newItem: ItemCategorySection
        ): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(
            oldItem: ItemCategorySection,
            newItem: ItemCategorySection
        ): Boolean {
            return oldItem == newItem
        }
    }

    class SectionViewHolder(
        private val binding: ItemCategorySectionBinding,
        private val nestedAdapter: CategoryGridAdapter,  // ✅ Reuse adapter
        private val parentAdapter: CategorySectionAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // ✅ Setup RecyclerView ONCE in init
            binding.categoryGrid.apply {
                layoutManager = GridLayoutManager(binding.root.context, 2)
                adapter = nestedAdapter
                setHasFixedSize(true)
                // ✅ Keep the grid's scroll position when expanding
                isNestedScrollingEnabled = false
            }

            // ✅ Set up click listeners ONCE in init
            binding.headerContainer.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val section = parentAdapter.getItem(position)
                    parentAdapter.toggleExpanded(section.categoryName)
                }
            }

            binding.arrowIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val section = parentAdapter.getItem(position)
                    parentAdapter.toggleExpanded(section.categoryName)
                }
            }
        }

        fun bind(section: ItemCategorySection, isExpanded: Boolean, isLastSection: Boolean) {
            binding.categoryTitle.text = section.categoryName
            binding.itemCount.text = "${section.items.size} items"

            val arrowRes = if (isExpanded) {
                R.drawable.ic_chevron_down
            } else {
                R.drawable.ic_chevron_right
            }
            binding.arrowIcon.setImageResource(arrowRes)

            // ✅ Determine which items to show
            val itemsToShow = if (isExpanded) {
                section.items
            } else {
                section.items.take(2)
            }

            // ✅ Just update the nested adapter's data - no new adapter!
            nestedAdapter.submitList(itemsToShow)

            // ✅ Show/hide divider for last section
            binding.divider.visibility = if (isLastSection) View.GONE else View.VISIBLE
        }
    }
}