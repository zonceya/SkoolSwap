// domain/repository/FilterRepositoryInterface.kt
package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.FilterConfig
import za.co.skoolswap.utils.Result

// In FilterRepositoryInterface.kt
interface FilterRepositoryInterface {
    suspend fun getFilterConfig(categoryId: Int): Result<FilterConfig>
    suspend fun getGlobalFilterConfig(): Result<FilterConfig>
    suspend fun preloadCategoryFilters(categoryIds: List<Int>)

    // ✅ Add this
    suspend fun warmUpCache()
}