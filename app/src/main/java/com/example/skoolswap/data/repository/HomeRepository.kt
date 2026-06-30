package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.database.dao.HomeFeedDao
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.dao.ItemImageDao
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.mapper.saveItemsToCache
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.RecommendationsApiService
import com.example.skoolswap.domain.model.homefeed.HomeFeed
import com.example.skoolswap.domain.model.homefeed.RecentFeed
import com.example.skoolswap.domain.model.homefeed.SportFeed
import com.example.skoolswap.domain.model.homefeed.UniformFeed
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val recommendationsApiService: RecommendationsApiService,
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences,
    private val homeFeedDao: HomeFeedDao,
    private val itemDao: ItemDao,           // ← ADD THIS
    private val itemImageDao: ItemImageDao
) : HomeRepositoryInterface {

    private companion object {
        private const val TAG = "HomeRepository"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // StateFlow for home feed
    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    override val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    override suspend fun getHomeFeed(schoolId: Int): Result<HomeFeed> {
        Log.d(TAG, "🔄 getHomeFeed called for schoolId: $schoolId")

        return safeApiCall(
            call = {
                Log.d(TAG, "📡 Calling API...")
                recommendationsApiService.getHomeFeed(schoolId)
            },
            errorMessage = "Failed to load home feed",
            onSuccess = { response ->
                Log.d(TAG, "📥 API Response received")
                Log.d(TAG, "   success: ${response.success}")
                Log.d(TAG, "   sections count: ${response.sections?.size ?: 0}")

                if (response.success) {
                    val feed = response.toDomain()
                    Log.d(TAG, "   Domain sections: ${feed.sections.size}")

                    saveHomeFeedToCache(feed)
                    feed.saveItemsToCache(itemDao, itemImageDao)
                    _homeFeed.value = feed
                    Result.Success(feed)
                } else {
                    Log.e(TAG, "API returned success=false: ${response.message}")
                    loadHomeFeedFromCache() ?: Result.Error(Exception(response.message ?: "Unknown error"))
                }
            },
            onError = {
                Log.e(TAG, "API call failed, loading from cache")
                loadHomeFeedFromCache()
            }
        )
    }
    private suspend fun saveHomeFeedToCache(feed: HomeFeed) {
        try {
            // Log what we're saving
            Log.d(TAG, "📝 Saving home feed to cache")
            Log.d(TAG, "   Sections count: ${feed.sections.size}")
            feed.sections.forEachIndexed { index, section ->
                Log.d(TAG, "   Section $index: ${section.javaClass.simpleName}")
            }

            val entity = feed.toEntity()
            homeFeedDao.insertHomeFeed(entity)
            Log.d(TAG, "✅ Home feed cached to Room successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cache home feed: ${e.message}", e)
        }
    }

    private suspend fun loadHomeFeedFromCache(): Result<HomeFeed>? {
        return try {
            val cached = homeFeedDao.getHomeFeed()
            if (cached != null) {
                Log.d(TAG, "📖 Loading home feed from cache")
                Log.d(TAG, "   Cached at: ${java.util.Date(cached.cachedAt)}")
                Log.d(TAG, "   JSON length: ${cached.sectionsJson.length}")

                val feed = cached.toDomain()
                Log.d(TAG, "   Sections count after parsing: ${feed.sections.size}")

                _homeFeed.value = feed
                Result.Success(feed)
            } else {
                Log.d(TAG, "No cached home feed found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load cached home feed: ${e.message}", e)
            null
        }
    }
    override suspend fun getUniforms(schoolId: Int, gender: String?): Result<UniformFeed> {
        return safeApiCall(
            call = { recommendationsApiService.getUniforms(schoolId, gender) },
            errorMessage = "Failed to load uniforms",
            onSuccess = { response ->
                if (response.success) {
                    Result.Success(response.toDomain())
                } else {
                    Result.Error(Exception(response.message ?: "Unknown error"))
                }
            }
        )
    }

    override suspend fun getSportItems(schoolId: Int, sportType: String?): Result<SportFeed> {
        return safeApiCall(
            call = { recommendationsApiService.getSportItems(schoolId, sportType) },
            errorMessage = "Failed to load sport items",
            onSuccess = { response ->
                if (response.success) {
                    Result.Success(response.toDomain())
                } else {
                    Result.Error(Exception(response.message ?: "Unknown error"))
                }
            }
        )
    }

    override suspend fun getRecentItems(schoolId: Int, period: String?): Result<RecentFeed> {
        return safeApiCall(
            call = { recommendationsApiService.getRecentItems(schoolId, period) },
            errorMessage = "Failed to load recent items",
            onSuccess = { response ->
                if (response.success) {
                    Result.Success(response.toDomain())
                } else {
                    Result.Error(Exception(response.message ?: "Unknown error"))
                }
            }
        )
    }

    override suspend fun clearHomeData() {
        coroutineScope.launch {
            _homeFeed.value = null
            Log.d(TAG, "Home data cleared")
        }
    }

    private suspend fun <T, R> safeApiCall(
        call: suspend () -> Response<T>,
        errorMessage: String,
        onSuccess: suspend (T) -> Result<R>,  // ← Changed to suspend
        onError: (suspend () -> Result<R>?)? = null
    ): Result<R> {
        return try {
            val token = authRepository.getAuthToken().first()
                ?: appPreferences.authToken.first()
            if (token.isNullOrBlank()) {
                Log.e(TAG, "No auth token available")
                return onError?.invoke() ?: Result.Error(Exception("Authentication required"))
            }

            val response = call.invoke()

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    onSuccess(body)
                } ?: Result.Error(Exception("Empty response body"))
            } else {
                onError?.invoke() ?: handleErrorResponse(response, errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            onError?.invoke() ?: Result.Error(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            onError?.invoke() ?: Result.Error(Exception("Unexpected error: ${e.message}"))
        }
    }

    private fun <T> handleErrorResponse(
        response: Response<T>,
        errorMessage: String
    ): Result.Error {
        val errorBody = response.errorBody()?.string()
        Log.e(TAG, "API error: ${response.code()} - $errorBody")

        val message = when (response.code()) {
            401 -> "Session expired. Please sign in again."
            403 -> "You don't have permission to access this"
            404 -> "Resource not found"
            500 -> "Server error. Please try again later"
            else -> "$errorMessage: ${response.code()}"
        }

        return Result.Error(Exception(message))
    }
}