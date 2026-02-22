package com.example.skoolswap.domain.model.reference

data class SubCategory(
    val id: Int,
    val name: String,
    val description: String?,
    val displayOrder: Int
    // Remove mainCategoryId since it's not in the response
)

