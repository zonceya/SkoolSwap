package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.PaginatedResponse
import com.example.skoolswap.utils.Result

interface ProductsRepositoryInterface {

    suspend fun getRecommendedAll(
        page: Int,
        perPage: Int = 20
    ): Result<PaginatedResponse<Item>>

    suspend fun getEssentialsAll(
        page: Int,
        category: String? = null,
        perPage: Int = 20
    ): Result<PaginatedResponse<Item>>

    suspend fun getTrendingAll(
        period: String,
        page: Int,
        perPage: Int = 20
    ): Result<PaginatedResponse<Item>>

    suspend fun getRecentAll(
        period: String,
        page: Int,
        perPage: Int = 20
    ): Result<PaginatedResponse<Item>>

    suspend fun trackClick(
        itemId: String,
        source: String,
        position: Int
    )
}