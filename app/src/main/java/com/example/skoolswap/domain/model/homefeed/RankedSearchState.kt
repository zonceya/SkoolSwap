package com.example.skoolswap.domain.model.homefeed

import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.RelevanceGroups


data class RankedSearchState(
    val query: String,
    val items: List<Item>,
    val relevanceGroups: RelevanceGroups = RelevanceGroups(emptyList(), emptyList(), emptyList()),
    val totalCount: Int = 0,
    val isLoadingMore: Boolean = false
)