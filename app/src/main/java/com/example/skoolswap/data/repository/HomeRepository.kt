package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.remote.api.RecommendationsApiService
import com.example.skoolswap.domain.model.HomeFeed
import com.example.skoolswap.domain.model.RecentFeed
import com.example.skoolswap.domain.model.SportFeed
import com.example.skoolswap.domain.model.UniformFeed
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
    private val authRepository: AuthRepositoryInterface
) : HomeRepositoryInterface {

    private companion object {
        private const val TAG = "HomeRepository"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // StateFlow for home feed
    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    override val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    override suspend fun getHomeFeed(schoolId: Int): Result<HomeFeed> {
        return safeApiCall(
            call = { recommendationsApiService.getHomeFeed(schoolId) },
            errorMessage = "Failed to load home feed",
            onSuccess = { response ->
                if (response.success) {
                    val feed = response.toDomain()
                    _homeFeed.value = feed
                    Result.Success(feed)
                } else {
                    Result.Error(Exception(response.message ?: "Unknown error"))
                }
            }
        )
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
        onSuccess: (T) -> Result<R>
    ): Result<R> {
        return try {
            // Check authentication
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrBlank()) {
                Log.e(TAG, "No auth token available")
                return Result.Error(Exception("Authentication required. Please sign in again."))
            }

            val response = call.invoke()

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    onSuccess(body)
                } ?: Result.Error(Exception("Empty response body"))
            } else {
                handleErrorResponse(response, errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            Result.Error(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.Error(Exception("Unexpected error: ${e.message}"))
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