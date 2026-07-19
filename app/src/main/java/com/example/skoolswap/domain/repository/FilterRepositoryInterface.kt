// domain/repository/FilterRepositoryInterface.kt
package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.utils.Result

// In FilterRepositoryInterface.kt
interface FilterRepositoryInterface {
    suspend fun getFilterConfig(categoryId: Int): Result<FilterConfig>
    suspend fun getGlobalFilterConfig(): Result<FilterConfig>
    suspend fun preloadCategoryFilters(categoryIds: List<Int>)

    // ✅ Add this
    suspend fun warmUpCache()
}