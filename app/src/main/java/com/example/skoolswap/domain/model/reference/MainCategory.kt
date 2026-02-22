package com.example.skoolswap.domain.model.reference

data class MainCategory(
    val id: Int,
    val name: String,
    val description: String?,
    val iconName: String?,
    val displayOrder: Int,
    val isActive: Boolean,
    val itemTypes: List<ItemType> = emptyList()  // ADD THIS
)

// Add this new data class
