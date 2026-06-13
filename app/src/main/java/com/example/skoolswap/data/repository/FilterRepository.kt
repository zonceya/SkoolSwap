package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.remote.api.FilterApiService
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.utils.Result
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
                    Result.Success(body.toDomain())
                } else {
                    Result.Error(Exception("Failed to load filter config"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getGlobalFilterConfig(): Result<FilterConfig> {
        return try {
            // Check cache first
            val isCacheValid = appPreferences.isGlobalFilterConfigCacheValid()
            val cachedJson = appPreferences.getCachedGlobalFilterConfig()

            if (isCacheValid && cachedJson != null) {
                // Parse cached JSON manually (simple parsing without Gson)
                val cachedConfig = parseFilterConfigFromJson(cachedJson)
                if (cachedConfig != null) {
                    Log.d("FilterRepository", "Using cached global filter config")
                    return Result.Success(cachedConfig)
                }
            }

            // Cache expired or empty, fetch from API
            Log.d("FilterRepository", "Fetching fresh global filter config from API")
            val response = api.getGlobalFilterConfig()

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    // Convert to JSON string for caching
                    val json = convertFilterConfigToJson(body)
                    appPreferences.cacheGlobalFilterConfig(json)
                    Result.Success(body.toDomain())
                } else {
                    Result.Error(Exception("Failed to load global filter config"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
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
            Log.d("FilterRepository", "Parsing JSON manually")
            null
        } catch (e: Exception) {
            Log.e("FilterRepository", "Failed to parse cached JSON: ${e.message}")
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