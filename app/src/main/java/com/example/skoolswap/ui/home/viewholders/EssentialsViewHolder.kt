package com.example.skoolswap.ui.home.viewholders

import android.util.Log
import android.view.View
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

        // Create category cards - now with 4 categories
        val categoryCards = mutableListOf<Pair<Item, String>>()

        // Uniforms category
        if (section.sections.uniforms.isNotEmpty()) {
            val firstUniform = section.sections.uniforms.first()
            categoryCards.add(Pair(firstUniform, "uniforms"))
            Log.d(TAG, "✅ Added Uniforms card")
        } else {
            val placeholder = createPlaceholderItem("No uniforms yet", R.drawable.ic_uniform_placeholder)
            categoryCards.add(Pair(placeholder, "uniforms"))
            Log.d(TAG, "⚠️ Added Uniforms placeholder")
        }

        // Sports category
        if (section.sections.sports.isNotEmpty()) {
            val firstSport = section.sections.sports.first()
            categoryCards.add(Pair(firstSport, "sports"))
            Log.d(TAG, "✅ Added Sports card")
        } else {
            val placeholder = createPlaceholderItem("No sports gear yet", R.drawable.ic_sports_placeholder)
            categoryCards.add(Pair(placeholder, "sports"))
            Log.d(TAG, "⚠️ Added Sports placeholder")
        }

        // Stationery category - NEW!
        val stationeryItems = section.sections.uniforms.filter {
            it.name.contains("Stationery") || it.name.contains("Book") || it.name.contains("Pen")
        }
        if (stationeryItems.isNotEmpty()) {
            categoryCards.add(Pair(stationeryItems.first(), "stationery"))
            Log.d(TAG, "✅ Added Stationery card")
        } else {
            val placeholder = createPlaceholderItem("Stationery", R.drawable.ic_stationery_placeholder)
            categoryCards.add(Pair(placeholder, "stationery"))
            Log.d(TAG, "⚠️ Added Stationery placeholder")
        }

        // If you want to keep accessories as well, you'll have 5 categories
        // Accessories category (optional)
        if (section.sections.accessories.isNotEmpty()) {
            val firstAccessory = section.sections.accessories.first()
            categoryCards.add(Pair(firstAccessory, "accessories"))
            Log.d(TAG, "✅ Added Accessories card")
        } else {
            val placeholder = createPlaceholderItem("Accessories", R.drawable.ic_accessories_placeholder)
            categoryCards.add(Pair(placeholder, "accessories"))
            Log.d(TAG, "⚠️ Added Accessories placeholder")
        }

        Log.d(TAG, "Total category cards: ${categoryCards.size}")

        // Setup grid adapter with appropriate span count
        val spanCount = if (categoryCards.size == 4) 2 else 2 // Still 2 columns, will show 2 rows of 2

        val adapter = EssentialsGridAdapter(categoryCards, onItemClick)

        binding.essentialsRecycler.apply {
            layoutManager = GridLayoutManager(itemView.context, spanCount)
            this.adapter = adapter
            Log.d(TAG, "RecyclerView configured with $spanCount columns")
        }
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