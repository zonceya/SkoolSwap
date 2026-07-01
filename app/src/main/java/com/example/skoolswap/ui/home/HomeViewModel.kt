package com.example.skoolswap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.RelevanceGroups
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.HomeFeed
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.domain.model.homefeed.getAllItems
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.domain.repository.RankedItemsResult
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepositoryInterface,
    private val userSchoolRepository: UserSchoolRepositoryInterface,
    private val productsRepository: ProductsRepositoryInterface,
    private val productsCacheRepository: ProductsCacheRepositoryInterface
) : ViewModel() {

    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 🔥 NEW: Ranked search results
    private val _rankedSearchResults = MutableStateFlow<List<Item>>(emptyList())
    val rankedSearchResults: StateFlow<List<Item>> = _rankedSearchResults.asStateFlow()

    // 🔥 NEW: Relevance groups for debugging/UI
    private val _searchRelevanceGroups = MutableStateFlow(RelevanceGroups(emptyList(), emptyList(), emptyList()))
    val searchRelevanceGroups: StateFlow<RelevanceGroups> = _searchRelevanceGroups.asStateFlow()

    // Legacy (keep for backward compatibility)
    private val _searchResults = MutableStateFlow<List<Item>>(emptyList())
    val searchResults: StateFlow<List<Item>> = _searchResults.asStateFlow()

    private val _isShowingLocalResults = MutableStateFlow(false)
    val isShowingLocalResults: StateFlow<Boolean> = _isShowingLocalResults.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _serverItemsCount = MutableStateFlow(0)
    val serverItemsCount: StateFlow<Int> = _serverItemsCount.asStateFlow()

    private val _isFromCache = MutableStateFlow(false)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    private var searchJob: Job? = null
    private var cachedHomeFeed: HomeFeed? = null
    private var knownSchoolId: Int? = null

    private val loadMutex = Mutex()


    // ==================== LOAD HOME FEED ====================

    fun setKnownSchoolId(schoolId: Int) {
        Timber.tag("HomeViewModel").d("🔑 setKnownSchoolId called with: $schoolId, previous: $knownSchoolId")
        knownSchoolId = schoolId
    }

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        if (!forceRefresh && cachedHomeFeed != null) {
            Timber.tag("HomeViewModel").d("📦 Using cached feed")
            _homeFeed.value = cachedHomeFeed
            _isFromCache.value = false
            return
        }

        viewModelScope.launch {
            if (!loadMutex.tryLock()) {
                Timber.tag("HomeViewModel").d("⏳ Already loading, skipping")
                return@launch
            }

            try {
                _isLoading.value = true
                _error.value = null
                _isFromCache.value = false

                val directSchoolId = knownSchoolId
                Timber.tag("HomeViewModel").d("🔑 knownSchoolId = $directSchoolId")

                if (directSchoolId != null) {
                    loadFeedWithCache(directSchoolId, forceRefresh)
                    return@launch
                }

                Timber.tag("HomeViewModel").d("🔍 Falling back to Room lookup")
                when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                    is Result.Success -> {
                        val mapping = result.data
                        Timber.tag("HomeViewModel").d("📍 Room mapping result: ${mapping?.schoolId}")
                        if (mapping != null) {
                            loadFeedWithCache(mapping.schoolId, forceRefresh)
                        } else {
                            Timber.tag("HomeViewModel").e("❌ No school mapping in Room")
                            _error.value = "Please select a school first"
                        }
                    }
                    is Result.Error -> {
                        Timber.tag("HomeViewModel").e("❌ Room error: ${result.exception.message}")
                        _error.value = "Failed to get school: ${result.exception.message}"
                    }
                }
            } finally {
                _isLoading.value = false
                loadMutex.unlock()
                Timber.tag("HomeViewModel").d("🔓 Loading complete, mutex unlocked")
            }
        }
    }
    fun searchLocalOnly(query: String, categoryId: Int?) {
        _searchQuery.value = query

        val localResults = searchLocalCache(query, categoryId)
        if (localResults.isNotEmpty()) {
            Timber.tag("HomeViewModel").d("⚡ Showing ${localResults.size} LOCAL results immediately")
            val grouped = groupByRelevance(localResults, knownSchoolId)
            _searchRelevanceGroups.value = grouped
            _rankedSearchResults.value = localResults
            _isShowingLocalResults.value = true
        } else {
            _rankedSearchResults.value = emptyList()
        }
    }

    fun searchServer(query: String, categoryId: Int?) {
        searchJob?.cancel() // Cancel any previous server job

        searchJob = viewModelScope.launch {
            if (query != _searchQuery.value || !isActive) return@launch

            _isLoadingMore.value = true

            try {
                val schoolId = knownSchoolId ?: getSchoolIdFromDb() ?: return@launch

                val serverResult = homeRepository.searchItemsRanked(
                    query = query,
                    schoolId = schoolId,
                    categoryId = if (categoryId != null && categoryId > 0) categoryId else null,
                    page = 1,
                    perPage = 30
                )

                if (query != _searchQuery.value || !isActive) return@launch

                when (serverResult) {
                    is Result.Success -> {
                        val serverItems = serverResult.data.items
                        Timber.d("✅ Server returned ${serverItems.size} items")

                        val current = _rankedSearchResults.value.toMutableList()
                        val existingIds = current.map { it.id }.toSet()
                        val newItems = serverItems.filter { it.id !in existingIds }

                        if (newItems.isNotEmpty()) {
                            current.addAll(newItems)
                            _rankedSearchResults.value = current
                            _searchRelevanceGroups.value = groupByRelevance(current, schoolId)
                        }
                    }
                    is Result.Error -> {
                        Timber.e("Server search failed: ${serverResult.exception.message}")
                    }
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    private suspend fun loadFeedWithCache(schoolId: Int, forceRefresh: Boolean) {
        Timber.tag("HomeViewModel").d("🔄 Loading feed for school $schoolId (forceRefresh=$forceRefresh)")

        // STEP 1: Check Room cache first (instant load)
        if (!forceRefresh) {
            val cachedSections = loadCachedSections(schoolId)
            if (cachedSections.isNotEmpty()) {
                val cachedFeed = HomeFeed(
                    success = true,
                    schoolId = schoolId,
                    message = "Cached data",
                    sections = cachedSections
                )
                cachedHomeFeed = cachedFeed
                _homeFeed.value = cachedFeed
                _isFromCache.value = true
                Timber.tag("HomeViewModel").d("📦 Loaded ${cachedSections.size} sections from cache")
            }
        }

        // STEP 2: Fetch from API in background (fresh data)
        try {
            val result = withTimeout(15_000L) {
                homeRepository.getHomeFeed(schoolId)
            }

            when (result) {
                is Result.Success -> {
                    val feed = result.data
                    cachedHomeFeed = feed
                    _homeFeed.value = feed
                    _isFromCache.value = false
                    _error.value = null

                    // Cache sections for next time
                    cacheSections(feed, schoolId)

                    Timber.tag("HomeViewModel").d("✅ Feed loaded: ${feed.sections.size} sections")
                }
                is Result.Error -> {
                    if (_homeFeed.value == null) {
                        _error.value = result.exception.message
                        Timber.tag("HomeViewModel").e("❌ Feed error: ${result.exception.message}")
                    } else {
                        Timber.tag("HomeViewModel").d("⚠️ Feed error but showing cached data")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag("HomeViewModel").e("⏱️ getHomeFeed timed out after 15s")
            if (_homeFeed.value == null) {
                _error.value = "Request timed out. Please try again."
            }
        } catch (e: Exception) {
            Timber.tag("HomeViewModel").e("❌ Unexpected error: ${e.message}")
            if (_homeFeed.value == null) {
                _error.value = "Failed to load feed: ${e.message}"
            }
        }
    }

    private suspend fun loadCachedSections(schoolId: Int): List<Section> {
        val sections = mutableListOf<Section>()

        val recommended = productsCacheRepository.getCachedSection("recommended", schoolId)
        val trending = productsCacheRepository.getCachedSection("trending", schoolId)
        val recent = productsCacheRepository.getCachedSection("recent", schoolId)
        val essentials = productsCacheRepository.getCachedSection("essentials", schoolId)

        Timber.tag("HomeViewModel").d("📦 Cache results: recommended=${recommended?.size}, trending=${trending?.size}, recent=${recent?.size}, essentials=${essentials?.size}")

        if (!recommended.isNullOrEmpty()) {
            sections.add(Section.Recommended(
                title = "Recommended For You",
                type = "recommended",
                items = recommended
            ))
        }

        if (!trending.isNullOrEmpty()) {
            sections.add(Section.Trending(
                title = "Trending Today",
                type = "trending",
                items = trending
            ))
        }

        if (!recent.isNullOrEmpty()) {
            sections.add(Section.Recent(
                title = "Recently Added",
                type = "recent",
                items = recent
            ))
        }

        if (!essentials.isNullOrEmpty()) {
            val uniforms = essentials.filter { it.mainCategoryId == 1 }
            val sports = essentials.filter { it.mainCategoryId == 2 }
            val accessories = essentials.filter { it.mainCategoryId == 3 }

            sections.add(Section.Essentials(
                title = "School Essentials",
                type = "essentials",
                sections = com.example.skoolswap.domain.model.homefeed.EssentialsSections(
                    uniforms = uniforms,
                    sports = sports,
                    accessories = accessories
                )
            ))
        }

        return sections
    }

    private suspend fun cacheSections(feed: HomeFeed, schoolId: Int) {
        try {
            val sectionsMap = mutableMapOf<String, List<Item>>()

            feed.sections.forEach { section ->
                when (section) {
                    is Section.Recommended -> {
                        sectionsMap["recommended"] = section.items
                    }
                    is Section.Trending -> {
                        sectionsMap["trending"] = section.items
                    }
                    is Section.Recent -> {
                        sectionsMap["recent"] = section.items
                    }
                    is Section.Essentials -> {
                        sectionsMap["essentials"] = section.sections.uniforms +
                                section.sections.sports +
                                section.sections.accessories
                    }
                }
            }

            productsCacheRepository.cacheHomeFeedItems(
                feedItems = feed.getAllItems(),
                schoolId = schoolId,
                sections = sectionsMap
            )

            Timber.tag("HomeViewModel").d("✅ Cached ${sectionsMap.size} sections to Room")
        } catch (e: Exception) {
            Timber.tag("HomeViewModel").e("❌ Failed to cache sections: ${e.message}")
        }
    }

    // ==================== REFRESH ====================

    fun refreshHomeFeed() {
        loadHomeFeed(forceRefresh = true)
    }

    fun clearHomeData() {
        cachedHomeFeed = null
        viewModelScope.launch {
            try {
                homeRepository.clearHomeData()
            } catch (e: Exception) {
                Timber.tag("HomeViewModel").e("❌ Failed to clear home data: ${e.message}")
            }
        }
    }

    // ==================== 🔥 RANKED SEARCH ====================

    // HomeViewModel.kt - Updated searchItemsRanked

    fun searchItemsRanked(query: String, categoryId: Int?) {
        _searchQuery.value = query

        // ✅ Cancel any existing search job immediately
        searchJob?.cancel()

        if (query.length < 2) {
            Timber.tag("HomeViewModel").d("Query too short (< 2 chars), clearing results")
            _rankedSearchResults.value = emptyList()
            _searchRelevanceGroups.value = RelevanceGroups(emptyList(), emptyList(), emptyList())
            _searchResults.value = emptyList()
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            // ✅ Increase debounce delay to 500ms to reduce cancellations
            delay(500)

            // ✅ Check if this job is still active
            if (!isActive) {
                Timber.tag("HomeViewModel").d("⏹️ Search cancelled before starting")
                return@launch
            }

            // ✅ Check if this is still the latest query
            val currentQuery = _searchQuery.value
            if (query != currentQuery) {
                Timber.tag("HomeViewModel").d("⏭️ Skipping stale search: $query (current: $currentQuery)")
                return@launch
            }

            Timber.tag("HomeViewModel").d("🔍 Ranked search for: $query")

            // STEP 1: Search local cache first (instant results)
            val localResults = searchLocalCache(query, categoryId)
            if (localResults.isNotEmpty()) {
                Timber.tag("HomeViewModel").d("⚡ ${localResults.size} results from LOCAL CACHE")
                val grouped = groupByRelevance(localResults, knownSchoolId)
                _searchRelevanceGroups.value = grouped
                _rankedSearchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _rankedSearchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            // STEP 2: Fetch from server with ranking
            _isLoadingMore.value = true

            try {
                val schoolId = knownSchoolId ?: getSchoolIdFromDb()
                if (schoolId == null) {
                    Timber.tag("HomeViewModel").e("❌ No school ID available for ranked search")
                    _isLoadingMore.value = false
                    return@launch
                }

                // ✅ Check cancellation before API call
                if (!isActive) {
                    Timber.tag("HomeViewModel").d("⏹️ Search cancelled before API call")
                    _isLoadingMore.value = false
                    return@launch
                }

                // ✅ Increase timeout to 30 seconds
                val serverResult = withTimeout(30_000L) {
                    // ✅ Check cancellation inside timeout
                    if (!isActive) {
                        _isLoadingMore.value = false
                        return@withTimeout Result.Error(Exception("Cancelled"))
                    }
                    homeRepository.searchItemsRanked(
                        query = query,
                        schoolId = schoolId,
                        categoryId = categoryId,
                        page = 1,
                        perPage = 30
                    )
                }

                _isLoadingMore.value = false

                // ✅ Check if this is still the latest query
                if (query != _searchQuery.value) {
                    Timber.tag("HomeViewModel").d("⏭️ Skipping stale results for: $query")
                    return@launch
                }

                // ✅ Check if job was cancelled during API call
                if (!isActive) {
                    Timber.tag("HomeViewModel").d("⏹️ Search cancelled after API call")
                    return@launch
                }

                when (serverResult) {
                    is Result.Success -> {
                        val rankedResult = serverResult.data
                        val serverItems = rankedResult.items
                        _serverItemsCount.value = serverItems.size

                        Timber.tag("HomeViewModel").d("✅ Ranked search returned ${serverItems.size} items")

                        if (serverItems.isNotEmpty()) {
                            val currentResults = _rankedSearchResults.value.toMutableList()
                            val existingIds = currentResults.map { it.id }.toSet()
                            val newItems = serverItems.filter { it.id !in existingIds }

                            if (newItems.isNotEmpty()) {
                                val merged = currentResults + newItems
                                val grouped = groupByRelevance(merged, schoolId)
                                _searchRelevanceGroups.value = grouped
                                _rankedSearchResults.value = merged
                                Timber.tag("HomeViewModel").d("📦 Added ${newItems.size} new items")
                            } else {
                                val grouped = groupByRelevance(currentResults, schoolId)
                                _searchRelevanceGroups.value = grouped
                            }
                            _isShowingLocalResults.value = false
                        } else if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                            _rankedSearchResults.value = emptyList()
                            _searchRelevanceGroups.value = RelevanceGroups(emptyList(), emptyList(), emptyList())
                        }
                    }
                    is Result.Error -> {
                        Timber.tag("HomeViewModel").e("❌ Server search failed: ${serverResult.exception.message}")
                        if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                            _error.value = "Failed to load results"
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _isLoadingMore.value = false
                Timber.tag("HomeViewModel").e("⏱️ Server search timed out after 30s")
                if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                    _error.value = "Search timed out. Please try again."
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
                Timber.tag("HomeViewModel").e("❌ Search error: ${e.message}")
                if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                    _error.value = "Failed to load results: ${e.message}"
                }
            }
        }
    }

    // HomeViewModel.kt - Add retry logic

    private suspend fun searchWithRetry(
        query: String,
        schoolId: Int,
        categoryId: Int?,
        maxRetries: Int = 2
    ): Result<RankedItemsResult> {
        var lastError: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return withTimeout(30_000L) {
                    homeRepository.searchItemsRanked(
                        query = query,
                        schoolId = schoolId,
                        categoryId = categoryId,
                        page = 1,
                        perPage = 30
                    )
                }
            } catch (e: TimeoutCancellationException) {
                lastError = e
                if (attempt < maxRetries) {
                    Timber.tag("HomeViewModel").d("⏱️ Search attempt ${attempt + 1} timed out, retrying...")
                    delay(1000L * (attempt + 1)) // Exponential backoff
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        return Result.Error(lastError ?: Exception("All retries failed"))
    }

    private fun groupByRelevance(items: List<Item>, schoolId: Int?): RelevanceGroups {
        if (schoolId == null) {
            return RelevanceGroups(emptyList(), emptyList(), items)
        }

        val schoolMatch = mutableListOf<Item>()
        val nearbyMatch = mutableListOf<Item>()
        val other = mutableListOf<Item>()

        // Get nearby school IDs (if we have them)
        val nearbyIds = getNearbySchoolIds() ?: emptyList()

        items.forEach { item ->
            when {
                item.schoolId == schoolId -> schoolMatch.add(item)
                nearbyIds.contains(item.schoolId) -> nearbyMatch.add(item)
                else -> other.add(item)
            }
        }

        return RelevanceGroups(
            schoolMatch = schoolMatch,
            nearbyMatch = nearbyMatch,
            other = other
        )
    }

    private fun getNearbySchoolIds(): List<Int>? {
        // This would come from your repository
        // For now, return null (will treat as 'other')
        return null
    }

    private suspend fun getSchoolIdFromDb(): Int? {
        return when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
            is Result.Success -> result.data?.schoolId
            is Result.Error -> {
                Timber.tag("HomeViewModel").e("❌ Failed to get school ID: ${result.exception.message}")
                null
            }
        }
    }

    // ==================== LEGACY SEARCH (Keep for compatibility) ====================

    private fun searchLocalCache(query: String, categoryId: Int?): List<Item> {
        val currentFeed = _homeFeed.value
        if (currentFeed == null) {
            Timber.tag("HomeViewModel").d("No cached feed available")
            return emptyList()
        }

        val allItems = mutableListOf<Item>()

        currentFeed.sections.forEach { section ->
            when (section) {
                is Section.Recommended -> allItems.addAll(section.items)
                is Section.Essentials -> {
                    allItems.addAll(section.sections.uniforms)
                    allItems.addAll(section.sections.sports)
                    allItems.addAll(section.sections.accessories)
                }
                is Section.Trending -> allItems.addAll(section.items)
                is Section.Recent -> allItems.addAll(section.items)
            }
        }

        Timber.tag("HomeViewModel").d("📚 Total cached items: ${allItems.size}")

        val searchLower = query.lowercase().trim()

        val results = allItems
            .filter { item ->
                // More flexible matching
                item.name.lowercase().contains(searchLower) ||
                        (item.description?.lowercase()?.contains(searchLower) == true) ||
                        (item.brandName?.lowercase()?.contains(searchLower) == true)
            }
            .let { list ->
                if (categoryId != null && categoryId > 0) {
                    list.filter { it.mainCategoryId == categoryId }
                } else {
                    list
                }
            }
            .distinctBy { it.id }
            .take(30)

        Timber.tag("HomeViewModel").d("🔍 Local search found ${results.size} matches for '$query'")
        return results
    }

    fun clearSearch() {
        _searchQuery.value = null
        _rankedSearchResults.value = emptyList()
        _searchRelevanceGroups.value = RelevanceGroups(emptyList(), emptyList(), emptyList())
        _searchResults.value = emptyList()
        _isShowingLocalResults.value = false
        _serverItemsCount.value = 0
    }
    // Legacy search method (keep for backward compatibility)
    fun searchItems(query: String, categoryId: Int?) {
        // Forward to ranked search
        searchItemsRanked(query, categoryId)
    }
}



















































