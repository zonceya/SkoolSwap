package com.example.skoolswap.ui.home.viewholders

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemEssentialsRowBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.adapter.EssentialsGridAdapter

private const val TAG = "EssentialsViewHolder"

class EssentialsViewHolder(
    private val binding: ItemEssentialsRowBinding,
    private val onItemClick: (Item, String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(section: Section.Essentials) {
        Log.d(TAG, "=== BINDING ESSENTIALS SECTION ===")

        binding.header.sectionTitle.text = section.title
        binding.header.viewAll.visibility = View.GONE
        Log.d(TAG, "Header title set to: ${section.title}")

        // Create category cards with their display names and category IDs
        val categoryCards = mutableListOf<EssentialsCategoryItem>()

        // Uniforms category (ID: 1)
        val uniformsItem = if (section.sections.uniforms.isNotEmpty()) {
            EssentialsCategoryItem(
                item = section.sections.uniforms.first(),
                categoryType = "uniforms",
                displayName = "Uniforms",
                categoryId = 1,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/uniforms/56859424-a3cc-411d-9098-d7eda59743ae.jpg"
            )
        } else {
            EssentialsCategoryItem(
                item = createPlaceholderItem("No uniforms yet", R.drawable.ic_uniform_placeholder),
                categoryType = "uniforms",
                displayName = "Uniforms",
                categoryId = 1,
                isPlaceholder = true,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/uniforms/56859424-a3cc-411d-9098-d7eda59743ae.jpg"
            )
        }
        categoryCards.add(uniformsItem)
        Log.d(TAG, "✅ Added Uniforms card")

        // Sports category (ID: 2)
        val sportsItem = if (section.sections.sports.isNotEmpty()) {
            EssentialsCategoryItem(
                item = section.sections.sports.first(),
                categoryType = "sports",
                displayName = "Sports",
                categoryId = 2,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/sport/cricket.jpg"
            )
        } else {
            EssentialsCategoryItem(
                item = createPlaceholderItem("No sports gear yet", R.drawable.ic_sports_placeholder),
                categoryType = "sports",
                displayName = "Sports",
                categoryId = 2,
                isPlaceholder = true,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/sport/cricket.jpg"
            )
        }
        categoryCards.add(sportsItem)
        Log.d(TAG, "✅ Added Sports card")

        // Stationery category (ID: 5)
        val stationeryItems = section.sections.uniforms.filter {
            it.name.contains("Stationery") || it.name.contains("Book") || it.name.contains("Pen")
        }
        val stationeryItem = if (stationeryItems.isNotEmpty()) {
            EssentialsCategoryItem(
                item = stationeryItems.first(),
                categoryType = "stationery",
                displayName = "Stationery",
                categoryId = 5,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/textbooks/fad19ac4-6f06-41be-8f5c-6f2f5f304477.jpg"
            )
        } else {
            EssentialsCategoryItem(
                item = createPlaceholderItem("Stationery", R.drawable.ic_stationery_placeholder),
                categoryType = "stationery",
                displayName = "Stationery",
                categoryId = 5,
                isPlaceholder = true,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/textbooks/fad19ac4-6f06-41be-8f5c-6f2f5f304477.jpg"
            )
        }
        categoryCards.add(stationeryItem)
        Log.d(TAG, "✅ Added Stationery card")

        // Accessories category (ID: 3)
        val accessoriesItem = if (section.sections.accessories.isNotEmpty()) {
            EssentialsCategoryItem(
                item = section.sections.accessories.first(),
                categoryType = "accessories",
                displayName = "Accessories",
                categoryId = 3,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/accessories/39f6a4a9-8a3f-4f3c-8f83-c052070b4b7f.jpg"
            )
        } else {
            EssentialsCategoryItem(
                item = createPlaceholderItem("Accessories", R.drawable.ic_accessories_placeholder),
                categoryType = "accessories",
                displayName = "Accessories",
                categoryId = 3,
                isPlaceholder = true,
                defaultImageUrl = "https://cdn.skoolswap.co.za/products/accessories/39f6a4a9-8a3f-4f3c-8f83-c052070b4b7f.jpg"
            )
        }
        categoryCards.add(accessoriesItem)
        Log.d(TAG, "✅ Added Accessories card")

        Log.d(TAG, "Total category cards: ${categoryCards.size}")

        // Setup grid adapter
        val spanCount = 2
        val adapter = EssentialsGridAdapter(categoryCards) { categoryItem ->
            handleCategoryClick(categoryItem)
        }

        binding.essentialsRecycler.apply {
            layoutManager = GridLayoutManager(itemView.context, spanCount)
            this.adapter = adapter
            Log.d(TAG, "RecyclerView configured with $spanCount columns")
        }
    }

    private fun handleCategoryClick(categoryItem: EssentialsCategoryItem) {
        Log.d(TAG, "Category clicked: ${categoryItem.displayName}, categoryId: ${categoryItem.categoryId}")

        // Navigate to ProductsFragment with category ID for filters
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", categoryItem.categoryType)
            putString("SECTION_TITLE", categoryItem.displayName)
            putInt("CATEGORY_ID", categoryItem.categoryId)
        }

        // Navigate using the root view's nav controller
        itemView.findNavController().navigate(
            R.id.action_homeFragment_to_productsFragment,
            bundle
        )
    }

    private fun createPlaceholderItem(message: String, iconResId: Int): Item {
        return Item(
            id = "placeholder_${System.currentTimeMillis()}",
            name = message,
            description = "",
            price = 0.0,
            images = emptyList(),
            shopId = 0L,
            quantity = 0,
            status = "",
            createdAt = "",
        )
    }
}

// Data class for category items
data class EssentialsCategoryItem(
    val item: Item,
    val categoryType: String,
    val displayName: String,
    val categoryId: Int,
    val isPlaceholder: Boolean = false,
    val defaultImageUrl: String? = null
)