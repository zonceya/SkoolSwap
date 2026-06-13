package com.example.skoolswap.domain.model

data class ItemCategorySection(
    val categoryName: String,
    val items: List<Item>,
    var isExpanded: Boolean = false  // Make this var so it can be changed
)