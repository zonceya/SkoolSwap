package com.example.skoolswap.data.repository

import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.remote.api.FilterApiService
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.utils.Result
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val api: FilterApiService,
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : FilterRepositoryInterface {

    override suspend fun getFilterConfig(categoryId: Int): Result<FilterConfig> {
        return try {
            val response = api.getFilterConfig(categoryId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Timber.tag(LogTags.REPOSITORY).d("✅ Filter config loaded for category: $categoryId")
                    Result.Success(body.toDomain())
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load filter config: success=false")
                    Result.Error(Exception("Failed to load filter config"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception loading filter config")
            Result.Error(e)
        }
    }

    override suspend fun getGlobalFilterConfig(): Result<FilterConfig> {
        return try {
            // Check cache first
            val isCacheValid = appPreferences.isGlobalFilterConfigCacheValid()
            val cachedJson = appPreferences.getCachedGlobalFilterConfig()

            if (isCacheValid && cachedJson != null) {
                // Parse cached JSON manually
                val cachedConfig = parseFilterConfigFromJson(cachedJson)
                if (cachedConfig != null) {
                    Timber.tag(LogTags.REPOSITORY).d("📦 Using cached global filter config")
                    return Result.Success(cachedConfig)
                }
            }

            // Cache expired or empty, fetch from API
            Timber.tag(LogTags.REPOSITORY).d("🌐 Fetching fresh global filter config from API")
            val response = api.getGlobalFilterConfig()

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    // Convert to JSON string for caching
                    val json = convertFilterConfigToJson(body)
                    appPreferences.cacheGlobalFilterConfig(json)
                    Timber.tag(LogTags.REPOSITORY).d("✅ Global filter config loaded and cached")
                    Result.Success(body.toDomain())
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load global filter config: success=false")
                    Result.Error(Exception("Failed to load global filter config"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Server error: ${response.code()}")
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception loading global filter config")
            Result.Error(e)
        }
    }

    // Simple JSON parser without Gson
    private fun parseFilterConfigFromJson(json: String): FilterConfig? {
        return try {
            // Basic JSON parsing - extract values manually
            // Since the structure is consistent, we can parse it
            // For now, return null and fetch from API
            // You can implement full JSON parsing here if needed
            Timber.tag(LogTags.REPOSITORY).d("Parsing JSON manually")
            null
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to parse cached JSON")
            null
        }
    }

    // Convert FilterConfigResponse to JSON string
    private fun convertFilterConfigToJson(response: Any): String {
        // Build JSON string manually
        return buildString {
            append("{\"success\":true,\"filter_groups\":[")
            // This is simplified - you'll need to properly serialize
            // For now, return empty JSON to skip caching
        }
    }
}