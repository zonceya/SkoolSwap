package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.homefeed.HomeFeed
import za.co.skoolswap.domain.model.homefeed.RecentFeed
import za.co.skoolswap.domain.model.homefeed.SportFeed
import za.co.skoolswap.domain.model.homefeed.UniformFeed
import za.co.skoolswap.utils.Result
import kotlinx.coroutines.flow.StateFlow

data class RankedItemsResult(
    val items: List<Item>,
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int
)

interface HomeRepositoryInterface {
    // StateFlow for home feed data
    val homeFeed: StateFlow<HomeFeed?>

    // Get home feed
    suspend fun getHomeFeed(schoolId: Int): Result<HomeFeed>

    // Get uniforms
    suspend fun getUniforms(schoolId: Int, gender: String? = null): Result<UniformFeed>

    suspend fun getSportItems(schoolId: Int, sportType: String? = null): Result<SportFeed>

    suspend fun getRecentItems(schoolId: Int, period: String? = null): Result<RecentFeed>

    suspend fun searchItemsRanked(
        query: String,
        schoolId: Int,
        categoryId: Int? = null,
        subCategoryId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun getUniformsRanked(
        schoolId: Int,
        gender: String? = null,
        subCategoryId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun getSportItemsRanked(
        schoolId: Int,
        sportType: String? = null,
        subCategoryId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun getRecentItemsRanked(
        schoolId: Int,
        period: String? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun getRecommendedRanked(
        schoolId: Int,
        categoryId: Int? = null,
        period: String? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun getTrendingRanked(
        schoolId: Int,
        period: String = "today",
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun getEssentialsRanked(
        schoolId: Int,
        category: String? = null,
        subCategoryId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<RankedItemsResult>

    suspend fun clearHomeData()
}