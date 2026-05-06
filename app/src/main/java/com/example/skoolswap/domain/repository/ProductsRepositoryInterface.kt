package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.data.remote.models.response.home.PaginatedResponse
import com.example.skoolswap.utils.Result

interface ProductsRepositoryInterface {

    suspend fun getRecommendedAll(
        page: Int,
        perPage: Int = 20,
        categoryId: Int? = null,
        conditionId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null
    ): Result<PaginatedResponse<Item>>

    suspend fun getEssentialsAll(
        page: Int,
        category: String? = null,
        perPage: Int = 20,
        conditionId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null
    ): Result<PaginatedResponse<Item>>

    suspend fun getTrendingAll(
        period: String,
        page: Int,
        perPage: Int = 20,
        categoryId: Int? = null,
        conditionId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null
    ): Result<PaginatedResponse<Item>>

    suspend fun getRecentAll(
        period: String,
        page: Int,
        perPage: Int = 20,
        categoryId: Int? = null,
        conditionId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null
    ): Result<PaginatedResponse<Item>>

    // ADD THIS NEW METHOD FOR SEARCH
    suspend fun searchItems(
        query: String,
        categoryId: Int? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<PaginatedResponse<Item>>

    suspend fun trackClick(
        itemId: String,
        source: String,
        position: Int
    )
}