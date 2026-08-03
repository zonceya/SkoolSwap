package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.common.constants.ErrorConstants
import za.co.skoolswap.data.local.database.dao.HomeFeedDao
import za.co.skoolswap.data.local.database.dao.ItemDao
import za.co.skoolswap.data.local.database.dao.ItemImageDao
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.data.mapper.saveItemsToCache
import za.co.skoolswap.data.mapper.toDomain
import za.co.skoolswap.data.mapper.toEntity
import za.co.skoolswap.data.remote.api.RecommendationsApiService
import za.co.skoolswap.domain.model.homefeed.HomeFeed
import za.co.skoolswap.domain.model.homefeed.RecentFeed
import za.co.skoolswap.domain.model.homefeed.SportFeed
import za.co.skoolswap.domain.model.homefeed.UniformFeed
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.HomeRepositoryInterface
import za.co.skoolswap.domain.repository.RankedItemsResult
import za.co.skoolswap.utils.Result
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
import timber.log.Timber

@Singleton
class HomeRepository @Inject constructor(
    private val recommendationsApiService: RecommendationsApiService,
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences,
    private val homeFeedDao: HomeFeedDao,
    private val itemDao: ItemDao,
    private val itemImageDao: ItemImageDao
) : HomeRepositoryInterface {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    override val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    companion object {
        private const val PER_PAGE_DEFAULT = 30
        private const val PAGE_DEFAULT = 1
        private const val TIMEOUT_DURATION_MS = 15000L
        private const val TIMEOUT_SEARCH_MS = 30000L
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1000L
    }

    // ================================================================
    // LEGACY ENDPOINTS
    // ================================================================

    override suspend fun getHomeFeed(schoolId: Int): Result<HomeFeed> {
        Timber.tag(LogTags.REPOSITORY).d("🔄 getHomeFeed called for schoolId: $schoolId")

        return safeApiCall(
            call = {
                Timber.tag(LogTags.REPOSITORY).d("📡 Calling API...")
                recommendationsApiService.getHomeFeed(schoolId)
            },
            errorMessage = "Failed to load home feed",
            onSuccess = { response ->
                Timber.tag(LogTags.REPOSITORY).d("📥 API Response received")
                Timber.tag(LogTags.REPOSITORY).d("   success: ${response.success}")

                // ✅ SAFE: Handle null sections
                val sections = response.sections ?: emptyList()
                Timber.tag(LogTags.REPOSITORY).d("   sections count: ${sections.size}")

                if (response.success) {
                    val feed = response.toDomain()
                    Timber.tag(LogTags.REPOSITORY).d("   Domain sections: ${feed.sections.size}")

                    saveHomeFeedToCache(feed)
                    feed.saveItemsToCache(itemDao, itemImageDao)
                    _homeFeed.value = feed
                    Result.Success(feed)
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("API returned success=false: ${response.message}")
                    loadHomeFeedFromCache() ?: Result.Error(Exception(response.message ?: "Unknown error"))
                }
            },
            onError = {
                Timber.tag(LogTags.REPOSITORY).e("API call failed, loading from cache")
                loadHomeFeedFromCache()
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

    // ================================================================
    // 🔥 NEW RANKED ENDPOINTS
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
        return safeApiCall(
            call = {
                recommendationsApiService.searchItemsRanked(
                    query = query,
                    schoolId = schoolId,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to search items",
            onSuccess = { response ->
                if (response.success) {
                    // ✅ SAFE: Handle null items list
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to search items"))
                }
            }
        )
    }

    override suspend fun getUniformsRanked(
        schoolId: Int,
        gender: String?,
        subCategoryId: Int?,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return safeApiCall(
            call = {
                recommendationsApiService.getUniformsRanked(
                    schoolId = schoolId,
                    gender = gender,
                    subCategoryId = subCategoryId,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to load uniforms",
            onSuccess = { response ->
                if (response.success) {
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to load uniforms"))
                }
            }
        )
    }

    override suspend fun getSportItemsRanked(
        schoolId: Int,
        sportType: String?,
        subCategoryId: Int?,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return safeApiCall(
            call = {
                recommendationsApiService.getSportItemsRanked(
                    schoolId = schoolId,
                    sportType = sportType,
                    subCategoryId = subCategoryId,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to load sport items",
            onSuccess = { response ->
                if (response.success) {
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to load sport items"))
                }
            }
        )
    }

    override suspend fun getRecentItemsRanked(
        schoolId: Int,
        period: String?,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return safeApiCall(
            call = {
                recommendationsApiService.getRecentItemsRanked(
                    schoolId = schoolId,
                    period = period,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to load recent items",
            onSuccess = { response ->
                if (response.success) {
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to load recent items"))
                }
            }
        )
    }

    override suspend fun getRecommendedRanked(
        schoolId: Int,
        categoryId: Int?,
        period: String?,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return safeApiCall(
            call = {
                recommendationsApiService.getRecommendedRanked(
                    schoolId = schoolId,
                    categoryId = categoryId,
                    period = period,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to load recommended items",
            onSuccess = { response ->
                if (response.success) {
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to load recommended items"))
                }
            }
        )
    }

    override suspend fun getTrendingRanked(
        schoolId: Int,
        period: String,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return safeApiCall(
            call = {
                recommendationsApiService.getTrendingRanked(
                    schoolId = schoolId,
                    period = period,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to load trending items",
            onSuccess = { response ->
                if (response.success) {
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to load trending items"))
                }
            }
        )
    }

    override suspend fun getEssentialsRanked(
        schoolId: Int,
        category: String?,
        subCategoryId: Int?,
        minPrice: Float?,
        maxPrice: Float?,
        page: Int,
        perPage: Int
    ): Result<RankedItemsResult> {
        return safeApiCall(
            call = {
                recommendationsApiService.getEssentialsRanked(
                    schoolId = schoolId,
                    category = category,
                    subCategoryId = subCategoryId,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    page = page,
                    perPage = perPage
                )
            },
            errorMessage = "Failed to load essentials",
            onSuccess = { response ->
                if (response.success) {
                    val items = response.items?.map { it.toDomain() } ?: emptyList()
                    Result.Success(
                        RankedItemsResult(
                            items = items,
                            totalCount = response.totalCount ?: items.size,
                            currentPage = response.pagination?.currentPage ?: page,
                            totalPages = response.pagination?.totalPages ?: 1
                        )
                    )
                } else {
                    Result.Error(Exception("Failed to load essentials"))
                }
            }
        )
    }

    // ================================================================
    // CLEAR HOME DATA
    // ================================================================

    override suspend fun clearHomeData() {
        coroutineScope.launch {
            _homeFeed.value = null
            Timber.tag(LogTags.REPOSITORY).d("Home data cleared")
        }
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private suspend fun saveHomeFeedToCache(feed: HomeFeed) {
        try {
            Timber.tag(LogTags.REPOSITORY).d("📝 Saving home feed to cache")
            Timber.tag(LogTags.REPOSITORY).d("   Sections count: ${feed.sections.size}")

            val entity = feed.toEntity()
            homeFeedDao.insertHomeFeed(entity)
            Timber.tag(LogTags.REPOSITORY).d("✅ Home feed cached to Room successfully")
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Failed to cache home feed")
        }
    }

    private suspend fun loadHomeFeedFromCache(): Result<HomeFeed>? {
        return try {
            val cached = homeFeedDao.getHomeFeed()
            if (cached != null) {
                Timber.tag(LogTags.REPOSITORY).d("📖 Loading home feed from cache")
                Timber.tag(LogTags.REPOSITORY).d("   Cached at: ${java.util.Date(cached.cachedAt)}")
                Timber.tag(LogTags.REPOSITORY).d("   JSON length: ${cached.sectionsJson.length}")

                val feed = cached.toDomain()
                Timber.tag(LogTags.REPOSITORY).d("   Sections count after parsing: ${feed.sections.size}")

                _homeFeed.value = feed
                Result.Success(feed)
            } else {
                Timber.tag(LogTags.REPOSITORY).d("No cached home feed found")
                null
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Failed to load cached home feed")
            null
        }
    }

    private suspend fun <T, R> safeApiCall(
        call: suspend () -> Response<T>,
        errorMessage: String,
        onSuccess: suspend (T) -> Result<R>,
        onError: (suspend () -> Result<R>?)? = null
    ): Result<R> {
        return try {
            val token = authRepository.getAuthToken().first()
                ?: appPreferences.authToken.first()
            if (token.isNullOrBlank()) {
                Timber.tag(LogTags.REPOSITORY).e("No auth token available")
                return onError?.invoke() ?: Result.Error(Exception(ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED))
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
            Timber.tag(LogTags.REPOSITORY).e(e, "Network error")
            onError?.invoke() ?: Result.Error(Exception(ErrorConstants.Messages.UserFriendly.NO_INTERNET))
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Unexpected error")
            onError?.invoke() ?: Result.Error(Exception(ErrorConstants.Messages.UserFriendly.UNKNOWN))
        }
    }

    private fun <T> handleErrorResponse(
        response: Response<T>,
        errorMessage: String
    ): Result.Error {
        val errorBody = response.errorBody()?.string()
        Timber.tag(LogTags.REPOSITORY).e("API error: ${response.code()} - $errorBody")

        val message = when (response.code()) {
            ErrorConstants.HttpStatus.UNAUTHORIZED -> ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED
            ErrorConstants.HttpStatus.FORBIDDEN -> ErrorConstants.Messages.UserFriendly.ACCESS_DENIED
            ErrorConstants.HttpStatus.NOT_FOUND -> ErrorConstants.Messages.UserFriendly.NOT_FOUND
            ErrorConstants.HttpStatus.INTERNAL_SERVER -> ErrorConstants.Messages.UserFriendly.SERVER_DOWN
            ErrorConstants.HttpStatus.BAD_GATEWAY -> ErrorConstants.Messages.UserFriendly.SERVER_DOWN
            ErrorConstants.HttpStatus.SERVICE_UNAVAILABLE -> ErrorConstants.Messages.UserFriendly.SERVICE_UNAVAILABLE
            ErrorConstants.HttpStatus.GATEWAY_TIMEOUT -> ErrorConstants.Messages.UserFriendly.TIMEOUT
            ErrorConstants.HttpStatus.AUTHENTICATION_TIMEOUT -> ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED
            ErrorConstants.HttpStatus.TOO_MANY_REQUESTS -> ErrorConstants.Messages.UserFriendly.SERVER_BUSY
            else -> "$errorMessage: ${response.code()}"
        }

        return Result.Error(Exception(message))
    }
}