package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.Item
import kotlinx.coroutines.flow.StateFlow

interface ProductsCacheRepositoryInterface {

    // Cache products for a specific key
    suspend fun cacheProducts(
        cacheKey: String,
        items: List<Item>,
        schoolId: Int? = null,
        sectionType: String? = null,
        period: String? = null,
        categoryId: Int? = null
    )

    // Get cached products by key
    suspend fun getCachedProducts(
        cacheKey: String,
        schoolId: Int? = null,
        maxAgeMs: Long = 24 * 60 * 60 * 1000
    ): List<Item>?

    // Cache entire home feed sections
    suspend fun cacheHomeFeedItems(
        feedItems: List<Item>,
        schoolId: Int,
        sections: Map<String, List<Item>>
    )

    // Get a specific section from cache
    suspend fun getCachedSection(
        sectionType: String,
        schoolId: Int
    ): List<Item>?

    // Clear all cached products
    suspend fun clearCache()

    // Check if cache is valid
    fun isCacheValid(cacheKey: String): Boolean
    // In ProductsCacheRepositoryInterface.kt - Add these

    suspend fun cacheUniforms(schoolId: Int, gender: String?, items: List<Item>)
    suspend fun getCachedUniforms(schoolId: Int, gender: String?): List<Item>?

    suspend fun cacheSports(schoolId: Int, sportType: String?, items: List<Item>)
    suspend fun getCachedSports(schoolId: Int, sportType: String?): List<Item>?

    suspend fun cacheRecent(schoolId: Int, period: String?, items: List<Item>)
    suspend fun getCachedRecent(schoolId: Int, period: String?): List<Item>?

    suspend fun cacheItemDetail(itemId: String, item: Item)
    suspend fun getCachedItemDetail(itemId: String): Item?

    suspend fun cacheSimilarItems(itemId: String, items: List<Item>)
    suspend fun getCachedSimilarItems(itemId: String): List<Item>?

}