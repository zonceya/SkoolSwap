package com.example.skoolswap.data.repository

import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.remote.api.RecommendationsApiService
import com.example.skoolswap.domain.model.PaginatedResponse
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

    override suspend fun getRecommendedAll(page: Int, perPage: Int): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                return Result.Error(Exception("No school selected"))
            }

            val response = api.getRecommendedAll(schoolId, page, perPage)

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
        perPage: Int
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                return Result.Error(Exception("No school selected"))
            }

            val response = api.getEssentialsAll(schoolId, category, page, perPage)

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

    override suspend fun getTrendingAll(
        period: String,
        page: Int,
        perPage: Int
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId() ?: return Result.Error(Exception("No school selected"))

            val response = api.getTrendingAll(schoolId, period, page, perPage)

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
        perPage: Int
    ): Result<PaginatedResponse<Item>> {
        return try {
            val schoolId = getCurrentSchoolId()
            if (schoolId == null) {
                return Result.Error(Exception("No school selected"))
            }

            val response = api.getRecentAll(schoolId, period, page, perPage)

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