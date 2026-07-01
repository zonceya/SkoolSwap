package com.example.skoolswap.domain.model

data class RelevanceGroups(
    val schoolMatch: List<Item>,
    val nearbyMatch: List<Item>,
    val other: List<Item>
)