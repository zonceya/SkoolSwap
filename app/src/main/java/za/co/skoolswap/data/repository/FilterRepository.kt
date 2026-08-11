package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.data.mapper.toDomain
import za.co.skoolswap.data.remote.api.FilterApiService
import za.co.skoolswap.data.remote.models.response.home.FilterConfigResponse
import za.co.skoolswap.domain.model.FilterConfig
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.FilterRepositoryInterface
import za.co.skoolswap.utils.Result
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val api: FilterApiService,
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : FilterRepositoryInterface {

    private val categoryFilterCache = mutableMapOf<Int, FilterConfig>()
    private var globalFilterCache: FilterConfig? = null

    override suspend fun getFilterConfig(categoryId: Int, forceRefresh: Boolean): Result<FilterConfig> {
        // If forceRefresh is true, bypass cache entirely
        if (forceRefresh) {
            Timber.tag(LogTags.REPOSITORY).d("🔄 Force refresh – bypassing cache for category: $categoryId")
            categoryFilterCache.remove(categoryId)
            // Clear DataStore cache for this category
            appPreferences.clearCategoryFilterCache(categoryId)
            return fetchFromApi(categoryId)
        }

        // 1. Check memory cache FIRST (super fast)
        categoryFilterCache[categoryId]?.let {
            Timber.tag(LogTags.REPOSITORY).d("📦 Memory cache HIT for category: $categoryId")
            return Result.Success(it)
        }

        // 2. Check DataStore cache
        try {
            val isCacheValid = appPreferences.isCategoryFilterValid(categoryId)
            val cachedJson = appPreferences.getCachedFilterConfig(categoryId)

            if (isCacheValid && cachedJson != null) {
                val cached = parseFilterConfigFromJson(cachedJson)
                if (cached != null) {
                    Timber.tag(LogTags.REPOSITORY).d("📦 DataStore cache HIT for category: $categoryId")
                    categoryFilterCache[categoryId] = cached
                    return Result.Success(cached)
                }
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to read from DataStore cache")
        }

        // 3. Cache miss - fetch from API
        return fetchFromApi(categoryId)
    }
    override suspend fun getGlobalFilterConfig(forceRefresh: Boolean): Result<FilterConfig> {
        if (forceRefresh) {
            Timber.tag(LogTags.REPOSITORY).d("🔄 Force refresh – bypassing global cache")
            globalFilterCache = null
            appPreferences.clearGlobalFilterCache()
            return fetchGlobalFromApi()
        }

        // 1. Check memory cache
        globalFilterCache?.let {
            Timber.tag(LogTags.REPOSITORY).d("📦 Global filter memory cache HIT")
            return Result.Success(it)
        }

        // 2. Check DataStore cache
        try {
            val isCacheValid = appPreferences.isGlobalFilterConfigCacheValid()
            val cachedJson = appPreferences.getCachedGlobalFilterConfig()

            if (isCacheValid && cachedJson != null) {
                val cached = parseFilterConfigFromJson(cachedJson)
                if (cached != null) {
                    Timber.tag(LogTags.REPOSITORY).d("📦 Global filter DataStore cache HIT")
                    globalFilterCache = cached
                    return Result.Success(cached)
                }
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to read global cache from DataStore")
        }

        // 3. Cache miss - fetch from API
        return fetchGlobalFromApi()
    }
    private suspend fun fetchFromApi(categoryId: Int): Result<FilterConfig> {
        Timber.tag(LogTags.REPOSITORY).d("🌐 Fetching fresh filter config for category: $categoryId")

        return try {
            val response = api.getFilterConfig(categoryId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val config = body.toDomain()

                    // Save to memory cache
                    categoryFilterCache[categoryId] = config

                    // Save to DataStore
                    try {
                        val json = convertFilterConfigToJson(body)
                        appPreferences.cacheFilterConfig(categoryId, json)
                        Timber.tag(LogTags.REPOSITORY).d("✅ Filter config cached for category: $categoryId")
                    } catch (e: Exception) {
                        Timber.tag(LogTags.REPOSITORY).e(e, "Failed to cache to DataStore")
                    }

                    Result.Success(config)
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
    private suspend fun fetchGlobalFromApi(): Result<FilterConfig> {
        Timber.tag(LogTags.REPOSITORY).d("🌐 Fetching fresh global filter config")

        return try {
            val response = api.getGlobalFilterConfig()

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val config = body.toDomain()

                    // Save to memory
                    globalFilterCache = config

                    // Save to DataStore
                    try {
                        val json = convertFilterConfigToJson(body)
                        appPreferences.cacheGlobalFilterConfig(json)
                        Timber.tag(LogTags.REPOSITORY).d("✅ Global filter config cached")
                    } catch (e: Exception) {
                        Timber.tag(LogTags.REPOSITORY).e(e, "Failed to cache global filter")
                    }

                    Result.Success(config)
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
    override suspend fun getFilterConfig(categoryId: Int): Result<FilterConfig> {
        // 1. Check memory cache FIRST (super fast)
        categoryFilterCache[categoryId]?.let {
            Timber.tag(LogTags.REPOSITORY).d("📦 Memory cache HIT for category: $categoryId")
            return Result.Success(it)
        }

        // 2. Check DataStore cache
        try {
            val isCacheValid = appPreferences.isCategoryFilterValid(categoryId)
            val cachedJson = appPreferences.getCachedFilterConfig(categoryId)

            if (isCacheValid && cachedJson != null) {
                val cached = parseFilterConfigFromJson(cachedJson)
                if (cached != null) {
                    Timber.tag(LogTags.REPOSITORY).d("📦 DataStore cache HIT for category: $categoryId")
                    categoryFilterCache[categoryId] = cached
                    return Result.Success(cached)
                }
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to read from DataStore cache")
        }

        // 3. Cache miss - fetch from API
        Timber.tag(LogTags.REPOSITORY).d("🌐 Fetching fresh filter config for category: $categoryId")

        return try {
            val response = api.getFilterConfig(categoryId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val config = body.toDomain()

                    // Save to memory cache
                    categoryFilterCache[categoryId] = config

                    // Save to DataStore
                    try {
                        val json = convertFilterConfigToJson(body)
                        appPreferences.cacheFilterConfig(categoryId, json)
                        Timber.tag(LogTags.REPOSITORY).d("✅ Filter config cached for category: $categoryId")
                    } catch (e: Exception) {
                        Timber.tag(LogTags.REPOSITORY).e(e, "Failed to cache to DataStore")
                    }

                    Result.Success(config)
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
        // 1. Check memory cache
        globalFilterCache?.let {
            Timber.tag(LogTags.REPOSITORY).d("📦 Global filter memory cache HIT")
            return Result.Success(it)
        }

        // 2. Check DataStore cache
        try {
            val isCacheValid = appPreferences.isGlobalFilterConfigCacheValid()
            val cachedJson = appPreferences.getCachedGlobalFilterConfig()

            if (isCacheValid && cachedJson != null) {
                val cached = parseFilterConfigFromJson(cachedJson)
                if (cached != null) {
                    Timber.tag(LogTags.REPOSITORY).d("📦 Global filter DataStore cache HIT")
                    globalFilterCache = cached
                    return Result.Success(cached)
                }
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to read global cache from DataStore")
        }

        // 3. Cache miss - fetch from API
        Timber.tag(LogTags.REPOSITORY).d("🌐 Fetching fresh global filter config")

        return try {
            val response = api.getGlobalFilterConfig()

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val config = body.toDomain()

                    // Save to memory
                    globalFilterCache = config

                    // Save to DataStore
                    try {
                        val json = convertFilterConfigToJson(body)
                        appPreferences.cacheGlobalFilterConfig(json)
                        Timber.tag(LogTags.REPOSITORY).d("✅ Global filter config cached")
                    } catch (e: Exception) {
                        Timber.tag(LogTags.REPOSITORY).e(e, "Failed to cache global filter")
                    }

                    Result.Success(config)
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

    // ✅ Fixed JSON parser - use Gson or Moshi
    private fun parseFilterConfigFromJson(json: String): FilterConfig? {
        return try {
            // Option 1: Using Gson
            val gson = com.google.gson.Gson()
            val response = gson.fromJson(json, FilterConfigResponse::class.java)
            response.toDomain()

            // Option 2: Using Moshi
            // val moshi = Moshi.Builder().build()
            // val adapter = moshi.adapter(FilterConfigResponse::class.java)
            // val response = adapter.fromJson(json)
            // response?.toDomain()
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to parse cached JSON")
            null
        }
    }

    // ✅ Fixed JSON serializer
    private fun convertFilterConfigToJson(response: Any): String {
        return try {
            val gson = com.google.gson.Gson()
            gson.toJson(response)
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to serialize filter config")
            "{}"
        }
    }
// In FilterRepository.kt

    /**
     * ✅ Load filters from DataStore into Memory IMMEDIATELY
     * This is called when the app starts - before the user can interact
     * It takes < 100ms because it's just reading from DataStore
     */
    override suspend fun warmUpCache() {
        Timber.tag(LogTags.REPOSITORY).d("🔥 Warming up filter cache from DataStore...")
        val startTime = System.currentTimeMillis()

        val categoryIds = listOf(1, 2, 3, 4)
        var loadedCount = 0

        categoryIds.forEach { categoryId ->
            try {
                // Check if already in memory
                if (categoryFilterCache.containsKey(categoryId)) {
                    Timber.tag(LogTags.REPOSITORY).d("✅ Category $categoryId already in memory")
                    return@forEach
                }

                // Load from DataStore
                val cachedJson = appPreferences.getCachedFilterConfig(categoryId)
                if (cachedJson != null) {
                    val config = parseFilterConfigFromJson(cachedJson)
                    if (config != null) {
                        categoryFilterCache[categoryId] = config
                        loadedCount++
                        Timber.tag(LogTags.REPOSITORY).d("✅ Loaded category $categoryId from DataStore to Memory")
                    } else {
                        Timber.tag(LogTags.REPOSITORY).d("⚠️ Failed to parse category $categoryId from DataStore")
                    }
                } else {
                    Timber.tag(LogTags.REPOSITORY).d("⏭️ No cached data for category $categoryId")
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.REPOSITORY).e(e, "❌ Failed to load category $categoryId from DataStore")
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Timber.tag(LogTags.REPOSITORY).d("✅ Cache warmed up: $loadedCount categories loaded in ${elapsed}ms")
    }
    override suspend fun preloadCategoryFilters(categoryIds: List<Int>) {
        Timber.tag(LogTags.REPOSITORY).d("🔄 Preloading ${categoryIds.size} category filters")

        kotlinx.coroutines.coroutineScope {
            categoryIds.forEach { categoryId ->
                launch {
                    try {
                        getFilterConfig(categoryId)
                        Timber.tag(LogTags.REPOSITORY).d("✅ Preloaded filter for category: $categoryId")
                    } catch (e: Exception) {
                        Timber.tag(LogTags.REPOSITORY).e(e, "⚠️ Failed to preload filter for category $categoryId")
                    }
                }
            }
        }

        Timber.tag(LogTags.REPOSITORY).d("✅ All ${categoryIds.size} filters preloaded")
    }

    // ✅ Clear all caches (useful for logout or force refresh)
    suspend fun clearAllCaches() {
        categoryFilterCache.clear()
        globalFilterCache = null
        appPreferences.clearAllCategoryFilterCaches()
        appPreferences.clearGlobalFilterCache()
        Timber.tag(LogTags.REPOSITORY).d("🗑️ All filter caches cleared")
    }
}