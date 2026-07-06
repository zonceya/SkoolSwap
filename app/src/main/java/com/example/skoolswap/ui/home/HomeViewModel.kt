package com.example.skoolswap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
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

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY_MS = 500L
        private const val SEARCH_TIMEOUT_MS = 30000L
        private const val FEED_TIMEOUT_MS = 15000L
        private const val PER_PAGE_DEFAULT = 30
        private const val PAGE_DEFAULT = 1
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1000L
        private const val MIN_SEARCH_LENGTH = 2
        private const val MAX_LOCAL_RESULTS = 30
    }

    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _rankedSearchResults = MutableStateFlow<List<Item>>(emptyList())
    val rankedSearchResults: StateFlow<List<Item>> = _rankedSearchResults.asStateFlow()

    private val _searchRelevanceGroups = MutableStateFlow(RelevanceGroups(emptyList(), emptyList(), emptyList()))
    val searchRelevanceGroups: StateFlow<RelevanceGroups> = _searchRelevanceGroups.asStateFlow()

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
        Timber.tag(LogTags.VIEW_MODEL).d("🔑 setKnownSchoolId called with: $schoolId, previous: $knownSchoolId")
        knownSchoolId = schoolId
    }

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        if (!forceRefresh && cachedHomeFeed != null) {
            Timber.tag(LogTags.VIEW_MODEL).d("📦 Using cached feed")
            _homeFeed.value = cachedHomeFeed
            _isFromCache.value = false
            return
        }

        viewModelScope.launch {
            if (!loadMutex.tryLock()) {
                Timber.tag(LogTags.VIEW_MODEL).d("⏳ Already loading, skipping")
                return@launch
            }

            try {
                _isLoading.value = true
                _error.value = null
                _isFromCache.value = false

                val directSchoolId = knownSchoolId
                Timber.tag(LogTags.VIEW_MODEL).d("🔑 knownSchoolId = $directSchoolId")

                if (directSchoolId != null) {
                    loadFeedWithCache(directSchoolId, forceRefresh)
                    return@launch
                }

                Timber.tag(LogTags.VIEW_MODEL).d("🔍 Falling back to Room lookup")
                when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                    is Result.Success -> {
                        val mapping = result.data
                        Timber.tag(LogTags.VIEW_MODEL).d("📍 Room mapping result: ${mapping?.schoolId}")
                        if (mapping != null) {
                            loadFeedWithCache(mapping.schoolId, forceRefresh)
                        } else {
                            Timber.tag(LogTags.VIEW_MODEL).e("❌ No school mapping in Room")
                            _error.value = "Please select a school first"
                        }
                    }
                    is Result.Error -> {
                        Timber.tag(LogTags.VIEW_MODEL).e("❌ Room error: ${result.exception.message}")
                        _error.value = "Failed to get school: ${result.exception.message}"
                    }
                }
            } finally {
                _isLoading.value = false
                loadMutex.unlock()
                Timber.tag(LogTags.VIEW_MODEL).d("🔓 Loading complete, mutex unlocked")
            }
        }
    }

    fun searchLocalOnly(query: String, categoryId: Int?) {
        _searchQuery.value = query

        val localResults = searchLocalCache(query, categoryId)
        if (localResults.isNotEmpty()) {
            Timber.tag(LogTags.VIEW_MODEL).d("⚡ Showing ${localResults.size} LOCAL results immediately")
            val grouped = groupByRelevance(localResults, knownSchoolId)
            _searchRelevanceGroups.value = grouped
            _rankedSearchResults.value = localResults
            _isShowingLocalResults.value = true
        } else {
            _rankedSearchResults.value = emptyList()
        }
    }

    fun searchServer(query: String, categoryId: Int?) {
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            if (query != _searchQuery.value || !isActive) return@launch

            _isLoadingMore.value = true

            try {
                val schoolId = knownSchoolId ?: getSchoolIdFromDb() ?: return@launch

                val serverResult = homeRepository.searchItemsRanked(
                    query = query,
                    schoolId = schoolId,
                    categoryId = if (categoryId != null && categoryId > 0) categoryId else null,
                    page = PAGE_DEFAULT,
                    perPage = PER_PAGE_DEFAULT
                )

                if (query != _searchQuery.value || !isActive) return@launch

                when (serverResult) {
                    is Result.Success -> {
                        val serverItems = serverResult.data.items
                        Timber.tag(LogTags.VIEW_MODEL).d("✅ Server returned ${serverItems.size} items")

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
                        Timber.tag(LogTags.VIEW_MODEL).e("Server search failed: ${serverResult.exception.message}")
                    }
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun loadFeedWithCache(schoolId: Int, forceRefresh: Boolean) {
        Timber.tag(LogTags.VIEW_MODEL).d("🔄 Loading feed for school $schoolId (forceRefresh=$forceRefresh)")

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
                Timber.tag(LogTags.VIEW_MODEL).d("📦 Loaded ${cachedSections.size} sections from cache")
            }
        }

        try {
            val result = withTimeout(FEED_TIMEOUT_MS) {
                homeRepository.getHomeFeed(schoolId)
            }

            when (result) {
                is Result.Success -> {
                    val feed = result.data
                    cachedHomeFeed = feed
                    _homeFeed.value = feed
                    _isFromCache.value = false
                    _error.value = null

                    cacheSections(feed, schoolId)

                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Feed loaded: ${feed.sections.size} sections")
                }
                is Result.Error -> {
                    if (_homeFeed.value == null) {
                        _error.value = result.exception.message
                        Timber.tag(LogTags.VIEW_MODEL).e("❌ Feed error: ${result.exception.message}")
                    } else {
                        Timber.tag(LogTags.VIEW_MODEL).d("⚠️ Feed error but showing cached data")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(LogTags.VIEW_MODEL).e("⏱️ getHomeFeed timed out after ${FEED_TIMEOUT_MS}ms")
            if (_homeFeed.value == null) {
                _error.value = "Request timed out. Please try again."
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.VIEW_MODEL).e("❌ Unexpected error: ${e.message}")
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

        Timber.tag(LogTags.VIEW_MODEL).d("📦 Cache results: recommended=${recommended?.size}, trending=${trending?.size}, recent=${recent?.size}, essentials=${essentials?.size}")

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

            Timber.tag(LogTags.VIEW_MODEL).d("✅ Cached ${sectionsMap.size} sections to Room")
        } catch (e: Exception) {
            Timber.tag(LogTags.VIEW_MODEL).e("❌ Failed to cache sections: ${e.message}")
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
                Timber.tag(LogTags.VIEW_MODEL).e("❌ Failed to clear home data: ${e.message}")
            }
        }
    }

    // ==================== 🔥 RANKED SEARCH ====================

    fun searchItemsRanked(query: String, categoryId: Int?) {
        _searchQuery.value = query

        searchJob?.cancel()

        if (query.length < MIN_SEARCH_LENGTH) {
            Timber.tag(LogTags.VIEW_MODEL).d("Query too short (< ${MIN_SEARCH_LENGTH} chars), clearing results")
            _rankedSearchResults.value = emptyList()
            _searchRelevanceGroups.value = RelevanceGroups(emptyList(), emptyList(), emptyList())
            _searchResults.value = emptyList()
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)

            if (!isActive) {
                Timber.tag(LogTags.VIEW_MODEL).d("⏹️ Search cancelled before starting")
                return@launch
            }

            val currentQuery = _searchQuery.value
            if (query != currentQuery) {
                Timber.tag(LogTags.VIEW_MODEL).d("⏭️ Skipping stale search: $query (current: $currentQuery)")
                return@launch
            }

            Timber.tag(LogTags.VIEW_MODEL).d("🔍 Ranked search for: $query")

            val localResults = searchLocalCache(query, categoryId)
            if (localResults.isNotEmpty()) {
                Timber.tag(LogTags.VIEW_MODEL).d("⚡ ${localResults.size} results from LOCAL CACHE")
                val grouped = groupByRelevance(localResults, knownSchoolId)
                _searchRelevanceGroups.value = grouped
                _rankedSearchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _rankedSearchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            _isLoadingMore.value = true

            try {
                val schoolId = knownSchoolId ?: getSchoolIdFromDb()
                if (schoolId == null) {
                    Timber.tag(LogTags.VIEW_MODEL).e("❌ No school ID available for ranked search")
                    _isLoadingMore.value = false
                    return@launch
                }

                if (!isActive) {
                    Timber.tag(LogTags.VIEW_MODEL).d("⏹️ Search cancelled before API call")
                    _isLoadingMore.value = false
                    return@launch
                }

                val serverResult = withTimeout(SEARCH_TIMEOUT_MS) {
                    if (!isActive) {
                        _isLoadingMore.value = false
                        return@withTimeout Result.Error(Exception("Cancelled"))
                    }
                    homeRepository.searchItemsRanked(
                        query = query,
                        schoolId = schoolId,
                        categoryId = categoryId,
                        page = PAGE_DEFAULT,
                        perPage = PER_PAGE_DEFAULT
                    )
                }

                _isLoadingMore.value = false

                if (query != _searchQuery.value) {
                    Timber.tag(LogTags.VIEW_MODEL).d("⏭️ Skipping stale results for: $query")
                    return@launch
                }

                if (!isActive) {
                    Timber.tag(LogTags.VIEW_MODEL).d("⏹️ Search cancelled after API call")
                    return@launch
                }

                when (serverResult) {
                    is Result.Success -> {
                        val rankedResult = serverResult.data
                        val serverItems = rankedResult.items
                        _serverItemsCount.value = serverItems.size

                        Timber.tag(LogTags.VIEW_MODEL).d("✅ Ranked search returned ${serverItems.size} items")

                        if (serverItems.isNotEmpty()) {
                            val currentResults = _rankedSearchResults.value.toMutableList()
                            val existingIds = currentResults.map { it.id }.toSet()
                            val newItems = serverItems.filter { it.id !in existingIds }

                            if (newItems.isNotEmpty()) {
                                val merged = currentResults + newItems
                                val grouped = groupByRelevance(merged, schoolId)
                                _searchRelevanceGroups.value = grouped
                                _rankedSearchResults.value = merged
                                Timber.tag(LogTags.VIEW_MODEL).d("📦 Added ${newItems.size} new items")
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
                        Timber.tag(LogTags.VIEW_MODEL).e("❌ Server search failed: ${serverResult.exception.message}")
                        if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                            _error.value = "Failed to load results"
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _isLoadingMore.value = false
                Timber.tag(LogTags.VIEW_MODEL).e("⏱️ Server search timed out after ${SEARCH_TIMEOUT_MS}ms")
                if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                    _error.value = "Search timed out. Please try again."
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
                Timber.tag(LogTags.VIEW_MODEL).e("❌ Search error: ${e.message}")
                if (!_isShowingLocalResults.value && _rankedSearchResults.value.isEmpty()) {
                    _error.value = "Failed to load results: ${e.message}"
                }
            }
        }
    }

    private suspend fun searchWithRetry(
        query: String,
        schoolId: Int,
        categoryId: Int?,
        maxRetries: Int = MAX_RETRIES
    ): Result<RankedItemsResult> {
        var lastError: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return withTimeout(SEARCH_TIMEOUT_MS) {
                    homeRepository.searchItemsRanked(
                        query = query,
                        schoolId = schoolId,
                        categoryId = categoryId,
                        page = PAGE_DEFAULT,
                        perPage = PER_PAGE_DEFAULT
                    )
                }
            } catch (e: TimeoutCancellationException) {
                lastError = e
                if (attempt < maxRetries) {
                    Timber.tag(LogTags.VIEW_MODEL).d("⏱️ Search attempt ${attempt + 1} timed out, retrying...")
                    delay(RETRY_DELAY_MS * (attempt + 1))
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
        return null
    }

    private suspend fun getSchoolIdFromDb(): Int? {
        return when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
            is Result.Success -> result.data?.schoolId
            is Result.Error -> {
                Timber.tag(LogTags.VIEW_MODEL).e("❌ Failed to get school ID: ${result.exception.message}")
                null
            }
        }
    }

    // ==================== LEGACY SEARCH ====================

    private fun searchLocalCache(query: String, categoryId: Int?): List<Item> {
        val currentFeed = _homeFeed.value
        if (currentFeed == null) {
            Timber.tag(LogTags.VIEW_MODEL).d("No cached feed available")
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

        Timber.tag(LogTags.VIEW_MODEL).d("📚 Total cached items: ${allItems.size}")

        val searchLower = query.lowercase().trim()

        val results = allItems
            .filter { item ->
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
            .take(MAX_LOCAL_RESULTS)

        Timber.tag(LogTags.VIEW_MODEL).d("🔍 Local search found ${results.size} matches for '$query'")
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

    fun searchItems(query: String, categoryId: Int?) {
        searchItemsRanked(query, categoryId)
    }
}