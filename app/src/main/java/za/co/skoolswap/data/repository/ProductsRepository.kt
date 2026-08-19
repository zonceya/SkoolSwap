package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.mapper.toDomain
import za.co.skoolswap.data.remote.api.RecommendationsApiService
import za.co.skoolswap.data.remote.models.response.home.PaginatedResponse
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.repository.ProductsRepositoryInterface
import za.co.skoolswap.domain.repository.RankedItemsResult
import za.co.skoolswap.utils.Result
import timber.log.Timber
import za.co.skoolswap.data.remote.models.response.home.PaginationDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsRepository @Inject constructor(
    private val api: RecommendationsApiService,
    private val userSchoolRepository: UserSchoolRepository
) : ProductsRepositoryInterface {

    // ================================================================
    // 🔥 RECOMMENDED - Using Ranked Endpoint
    // ================================================================
    override suspend fun getRecommendedAll(
        page: Int,
        perPage: Int,
        categoryId: Int?,
        conditionId: Int?,
        minPrice: Float?,
        maxPrice: Float?
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                Timber.tag(LogTags.REPOSITORY).e("No school selected")
                return Result.Error(Exception("No school selected"))
            }

            // ✅ Using NEW ranked endpoint
            val response = api.getRecommendedRanked(
                schoolId = schoolId,
                categoryId = categoryId,
                minPrice = minPrice,
                maxPrice = maxPrice,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { it.toDomain() }
                    val pagination = body.pagination?.let { rankedPagination ->
                        PaginationDto(
                            currentPage = rankedPagination.currentPage,
                            perPage = rankedPagination.perPage,
                            totalPages = rankedPagination.totalPages,
                            totalCount = body.totalCount
                        )
                    }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Recommended items loaded: ${items.size}")
                    Result.Success(
                        PaginatedResponse(
                            items = items,
                            pagination = pagination
                        )
                    )
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load recommended items")
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception loading recommended items")
            Result.Error(e)
        }
    }

    // ================================================================
    // 🔥 ESSENTIALS - Using Ranked Endpoint
    // ================================================================
    override suspend fun getEssentialsAll(
        page: Int,
        category: String?,
        subCategoryId: Int?,
        perPage: Int,
        conditionId: Int?,
        minPrice: Float?,
        maxPrice: Float?
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                Timber.tag(LogTags.REPOSITORY).e("No school selected")
                return Result.Error(Exception("No school selected"))
            }

            // ✅ Using NEW ranked endpoint
            val response = api.getEssentialsRanked(
                schoolId = schoolId,
                category = category,
                subCategoryId = subCategoryId?.takeIf { it > 0 },  // Only pass if > 0
                minPrice = minPrice,
                maxPrice = maxPrice,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { it.toDomain() }
                    val pagination = body.pagination?.let { rankedPagination ->
                        PaginationDto(
                            currentPage = rankedPagination.currentPage,
                            perPage = rankedPagination.perPage,
                            totalPages = rankedPagination.totalPages,
                            totalCount = body.totalCount
                        )
                    }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Essentials items loaded: ${items.size}")
                    Result.Success(
                        PaginatedResponse(
                            items = items,
                            pagination = pagination
                        )
                    )
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load essentials items")
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception loading essentials items")
            Result.Error(e)
        }
    }

    // ================================================================
    // 🔥 SEARCH - Using Ranked Endpoint
    // ================================================================
    override suspend fun searchItems(
        query: String,
        categoryId: Int?,
        genderId: Int?,
        brandId: Int?,
        sizeId: Int?,
        colorId: Int?,
        conditionId: Int?,
        minPrice: Float?,
        maxPrice: Float?,
        sort: String?,
        page: Int,
        perPage: Int
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                Timber.tag(LogTags.REPOSITORY).e("No school selected")
                return Result.Error(Exception("No school selected"))
            }

            // ✅ Using ranked search
            val response = api.searchItemsRanked(
                query = query,
                schoolId = schoolId,
                categoryId = categoryId,
                subCategoryId = null,
                minPrice = minPrice,
                maxPrice = maxPrice,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { it.toDomain() }
                    val pagination = body.pagination?.let { rankedPagination ->
                        PaginationDto(
                            currentPage = rankedPagination.currentPage,
                            perPage = rankedPagination.perPage,
                            totalPages = rankedPagination.totalPages,
                            totalCount = body.totalCount
                        )
                    }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Search returned ${items.size} items")
                    Result.Success(
                        PaginatedResponse(
                            items = items,
                            pagination = pagination
                        )
                    )
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to search items")
                    Result.Error(Exception("Failed to search items"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Search failed")
            Result.Error(e)
        }
    }

    // ================================================================
    // 🔥 TRENDING - Using Ranked Endpoint
    // ================================================================
    override suspend fun getTrendingAll(
        period: String,
        page: Int,
        perPage: Int,
        categoryId: Int?,
        conditionId: Int?,
        minPrice: Float?,
        maxPrice: Float?
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                Timber.tag(LogTags.REPOSITORY).e("No school selected")
                return Result.Error(Exception("No school selected"))
            }

            // ✅ Using NEW ranked endpoint
            val response = api.getTrendingRanked(
                schoolId = schoolId,
                period = period,
                minPrice = minPrice,
                maxPrice = maxPrice,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { it.toDomain() }
                    val pagination = body.pagination?.let { rankedPagination ->
                        PaginationDto(
                            currentPage = rankedPagination.currentPage,
                            perPage = rankedPagination.perPage,
                            totalPages = rankedPagination.totalPages,
                            totalCount = body.totalCount
                        )
                    }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Trending items loaded: ${items.size}")
                    Result.Success(
                        PaginatedResponse(
                            items = items,
                            pagination = pagination
                        )
                    )
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load trending items")
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception loading trending items")
            Result.Error(e)
        }
    }

    // ================================================================
    // 🔥 RECENT - Using Ranked Endpoint
    // ================================================================
    override suspend fun getRecentAll(
        period: String,
        page: Int,
        perPage: Int,
        categoryId: Int?,
        conditionId: Int?,
        minPrice: Float?,
        maxPrice: Float?
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                Timber.tag(LogTags.REPOSITORY).e("No school selected")
                return Result.Error(Exception("No school selected"))
            }

            // ✅ Using NEW ranked endpoint
            val response = api.getRecentItemsRanked(
                schoolId = schoolId,
                period = period,
                minPrice = minPrice,
                maxPrice = maxPrice,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { it.toDomain() }
                    val pagination = body.pagination?.let { rankedPagination ->
                        PaginationDto(
                            currentPage = rankedPagination.currentPage,
                            perPage = rankedPagination.perPage,
                            totalPages = rankedPagination.totalPages,
                            totalCount = body.totalCount
                        )
                    }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Recent items loaded: ${items.size}")
                    Result.Success(
                        PaginatedResponse(
                            items = items,
                            pagination = pagination
                        )
                    )
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load recent items")
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception loading recent items")
            Result.Error(e)
        }
    }

    // ================================================================
    // 🔥 SEARCH RANKED (Direct)
    // ================================================================
    override suspend fun searchItemsRanked(
        query: String,
        schoolId: Int,
        categoryId: Int?,
        subCategoryId: Int?,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return try {
            Timber.tag(LogTags.REPOSITORY).d("🔍 Ranked search: query=$query, schoolId=$schoolId")
            val response = api.searchItemsRanked(
                query = query,
                schoolId = schoolId,
                categoryId = categoryId,
                subCategoryId = subCategoryId?.takeIf { it > 0 },
                minPrice = minPrice,
                maxPrice = maxPrice,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { it.toDomain() }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Ranked search returned ${items.size} items")
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = body.totalCount,
                            currentPage = body.pagination?.currentPage ?: page,
                            totalPages = body.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to search items: success=false")
                    Result.Error(Exception("Failed to search items"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Ranked search failed")
            Result.Error(e)
        }
    }

    // ================================================================
    // 🔥 TRACK CLICK
    // ================================================================
    override suspend fun trackClick(itemId: String, source: String, position: Int) {
        try {
            api.trackClick(itemId, source, position)
            Timber.tag(LogTags.REPOSITORY).d("📊 Tracked click: item=$itemId, source=$source, position=$position")
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).w(e, "⚠️ Failed to track click")
        }
    }

    // ================================================================
    // 🔥 PRIVATE HELPERS
    // ================================================================
    private suspend fun getCurrentSchoolId(): Int? {
        return when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
            is Result.Success -> result.data?.schoolId
            is Result.Error -> {
                Timber.tag(LogTags.REPOSITORY).w("⚠️ Failed to get school ID: ${result.exception.message}")
                null
            }
        }
    }
}