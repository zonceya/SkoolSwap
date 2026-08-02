package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.data.remote.models.response.home.PaginatedResponse
import za.co.skoolswap.utils.Result

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
        subCategoryId: Int? = null,
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


        suspend fun searchItems(
            query: String,
            categoryId: Int? = null,
            genderId: Int? = null,
            brandId: Int? = null,
            sizeId: Int? = null,
            colorId: Int? = null,
            conditionId: Int? = null,
            minPrice: Float? = null,
            maxPrice: Float? = null,
            sort: String? = null,
            page: Int = 1,
            perPage: Int = 30
        ): Result<PaginatedResponse<Item>>

    suspend fun trackClick(
        itemId: String,
        source: String,
        position: Int
    )
    suspend fun searchItemsRanked(
        query: String,
        schoolId: Int,
        categoryId: Int? = null,
        subCategoryId: Int? = null,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        page: Int = 1,
        perPage: Int = 30
    ): Result<RankedItemsResult>
}