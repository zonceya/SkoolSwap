// data/repository/ProductsRepository.kt (FIXED)
package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.remote.api.RecommendationsApiService
import com.example.skoolswap.data.remote.models.response.home.PaginatedResponse
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsRepository @Inject constructor(
    private val api: RecommendationsApiService,
    private val userSchoolRepository: UserSchoolRepository
) : ProductsRepositoryInterface {

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
                return Result.Error(Exception("No school selected"))
            }

            val response = api.getRecommendedAll(
                schoolId = schoolId,
                page = page,
                perPage = perPage,
                categoryId = categoryId,
                conditionId = conditionId,
                minPrice = minPrice,
                maxPrice = maxPrice
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.Success(PaginatedResponse(
                        items = body.items.map { it.toDomain() },
                        pagination = body.pagination
                    ))
                } else {
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

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
                return Result.Error(Exception("No school selected"))
            }

            val response = api.getEssentialsAll(
                schoolId = schoolId,
                category = category,
                subCategoryId = subCategoryId,
                page = page,
                perPage = perPage,
                conditionId = conditionId,
                minPrice = minPrice,
                maxPrice = maxPrice
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.Success(PaginatedResponse(
                        items = body.items.map { it.toDomain() },
                        pagination = body.pagination
                    ))
                } else {
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    // Add this to your ProductsRepository class
    // In ProductsRepository.kt, update the search method:

    // ProductsRepository.kt
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
                return Result.Error(Exception("No school selected"))
            }

            val response = api.searchItems(
                schoolId = schoolId,
                query = query,
                categoryId = categoryId,
                genderId = genderId,
                brandId = brandId,
                sizeId = sizeId,
                colorId = colorId,
                conditionId = conditionId,
                minPrice = minPrice,
                maxPrice = maxPrice,
                sort = sort,
                page = page,
                perPage = perPage
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val items = body.items.map { itemDto ->
                        itemDto.toDomain()
                    }
                    Result.Success(PaginatedResponse(
                        items = items,
                        pagination = body.pagination
                    ))
                } else {
                    Result.Error(Exception("Failed to search items: ${response.code()}"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ProductsRepository", "Search failed", e)
            Result.Error(e)
        }
    }
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
            val schoolId = getCurrentSchoolId() ?: return Result.Error(Exception("No school selected"))

            val response = api.getTrendingAll(
                schoolId = schoolId,
                period = period,
                page = page,
                perPage = perPage,
                categoryId = categoryId,
                conditionId = conditionId,
                minPrice = minPrice,
                maxPrice = maxPrice
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.Success(PaginatedResponse(
                        items = body.items.map { it.toDomain() },
                        pagination = body.pagination
                    ))
                } else {
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

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
                return Result.Error(Exception("No school selected"))
            }

            val response = api.getRecentAll(
                schoolId = schoolId,
                period = period,
                page = page,
                perPage = perPage,
                categoryId = categoryId,
                conditionId = conditionId,
                minPrice = minPrice,
                maxPrice = maxPrice
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.Success(PaginatedResponse(
                        items = body.items.map { it.toDomain() },
                        pagination = body.pagination
                    ))
                } else {
                    Result.Error(Exception("Failed to load items"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun trackClick(itemId: String, source: String, position: Int) {
        try {
            api.trackClick(itemId, source, position)
        } catch (e: Exception) {
            // Log but don't fail
        }
    }

    private suspend fun getCurrentSchoolId(): Int? {
        return when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
            is Result.Success -> result.data?.schoolId
            is Result.Error -> null
        }
    }
}