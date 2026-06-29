package com.example.skoolswap.domain.model.homefeed

import com.example.skoolswap.domain.model.Item

// Add this extension function to HomeFeed.kt
fun HomeFeed.getAllItems(): List<Item> {
    val allItems = mutableListOf<Item>()

    sections.forEach { section ->
        when (section) {
            is Section.Recommended -> allItems.addAll(section.items)
            is Section.Trending -> allItems.addAll(section.items)
            is Section.Recent -> allItems.addAll(section.items)
            is Section.Essentials -> {
                allItems.addAll(section.sections.uniforms)
                allItems.addAll(section.sections.sports)
                allItems.addAll(section.sections.accessories)
            }
        }
    }

    return allItems.distinctBy { it.id }
}