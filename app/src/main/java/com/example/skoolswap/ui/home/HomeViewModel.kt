package com.example.skoolswap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.HomeFeed
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.domain.model.homefeed.getAllItems
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
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

    // ✅ Cache state
    private val _isFromCache = MutableStateFlow(false)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    private var searchJob: Job? = null
    private var cachedHomeFeed: HomeFeed? = null
    private var knownSchoolId: Int? = null

    // ✅ FIX: Mutex for re-entrancy protection
    private val loadMutex = Mutex()

    fun setKnownSchoolId(schoolId: Int) {
        Timber.tag("HomeViewModel")
            .d("🔑 setKnownSchoolId called with: $schoolId, previous: $knownSchoolId")
        knownSchoolId = schoolId
    }

    // ==================== LOAD HOME FEED ====================

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        // Use cached feed if available and not forcing refresh
        if (!forceRefresh && cachedHomeFeed != null) {
            Timber.tag("HomeViewModel").d("📦 Using cached feed")
            _homeFeed.value = cachedHomeFeed
            _isFromCache.value = false
            return
        }

        // ✅ FIX: Launch coroutine and use Mutex for proper re-entrancy protection
        viewModelScope.launch {
            // Try to acquire lock - if already loading, skip
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
                // ✅ FIX: Always reset loading state and unlock mutex
                _isLoading.value = false
                loadMutex.unlock()
                Timber.tag("HomeViewModel").d("🔓 Loading complete, mutex unlocked")
            }
        }
    }

    // ==================== LOAD WITH CACHE ====================

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
                // ✅ Don't reset _isLoading here - let the caller handle it via finally
                Timber.tag("HomeViewModel").d("📦 Loaded ${cachedSections.size} sections from cache")
            }
        }

        // STEP 2: Fetch from API in background (fresh data)
        // ✅ FIX: Wrap in try-catch with timeout and finally block
        try {
            // ✅ FIX: Add timeout to prevent infinite hangs
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
                        // We have cached data, so don't show error
                        Timber.tag("HomeViewModel").d("⚠️ Feed error but showing cached data")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag("HomeViewModel").e("⏱️ getHomeFeed timed out after 15s")
            if (_homeFeed.value == null) {
                _error.value = "Request timed out. Please try again."
            } else {
                // We have cached data, so don't show error
                Timber.tag("HomeViewModel").d("⚠️ Feed timeout but showing cached data")
            }
        } catch (e: Exception) {
            Timber.tag("HomeViewModel").e("❌ Unexpected error in loadFeedWithCache: ${e.message}")
            if (_homeFeed.value == null) {
                _error.value = "Failed to load feed: ${e.message}"
            }
        }
        // ✅ _isLoading reset is handled by the caller's finally block
    }

    // ==================== LOAD CACHED SECTIONS ====================

    private suspend fun loadCachedSections(schoolId: Int): List<Section> {
        val sections = mutableListOf<Section>()

        // Try to load each section from cache
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
            // Split essentials into categories
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

    // ==================== CACHE SECTIONS ====================

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
            // Non-critical, don't propagate
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

    // ==================== SEARCH ====================

    fun searchItems(query: String, categoryId: Int?) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length < 2) {
            Timber.tag("HomeViewModel").d("Query too short (< 2 chars), clearing results")
            _searchResults.value = emptyList()
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)

            Timber.tag("HomeViewModel").d("🔍 Searching for: $query")

            _serverItemsCount.value = 0

            val localResults = searchLocalCache(query, categoryId)

            if (localResults.isNotEmpty()) {
                Timber.tag("HomeViewModel").d("⚡ ${localResults.size} results from LOCAL CACHE")
                _searchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _searchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            // STEP 2: Search server in background
            _isLoadingMore.value = true

            try {
                // ✅ Add timeout to server search
                val serverResult = withTimeout(10_000L) {
                    productsRepository.searchItems(
                        query = query,
                        categoryId = categoryId,
                        page = 1,
                        perPage = 30
                    )
                }

                _isLoadingMore.value = false

                when (serverResult) {
                    is Result.Success -> {
                        val serverItems = serverResult.data.items
                        _serverItemsCount.value = serverItems.size
                        Timber.tag("HomeViewModel").d("✅ Server returned ${serverItems.size} results")

                        if (serverItems.isNotEmpty()) {
                            val currentResults = _searchResults.value.toMutableList()
                            val existingIds = currentResults.map { it.id }.toSet()
                            val newItems = serverItems.filter { it.id !in existingIds }

                            if (newItems.isNotEmpty()) {
                                val merged = currentResults + newItems
                                _searchResults.value = merged
                                Timber.tag("HomeViewModel").d("📦 Added ${newItems.size} new items")
                            }
                            _isShowingLocalResults.value = false
                        } else if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                            _searchResults.value = emptyList()
                        }
                    }
                    is Result.Error -> {
                        Timber.tag("HomeViewModel").e("❌ Server search failed: ${serverResult.exception.message}")
                        if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                            _error.value = "Failed to load results"
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _isLoadingMore.value = false
                Timber.tag("HomeViewModel").e("⏱️ Server search timed out")
                if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                    _error.value = "Search timed out. Please try again."
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
                Timber.tag("HomeViewModel").e("❌ Search error: ${e.message}")
                if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                    _error.value = "Failed to load results: ${e.message}"
                }
            }
        }
    }

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

        val filteredByCategory = if (categoryId != null) {
            allItems.filter { it.mainCategoryId == categoryId }
        } else {
            allItems
        }

        val searchLower = query.lowercase()
        val results = filteredByCategory
            .filter { item ->
                item.name.lowercase().contains(searchLower) ||
                        item.description.lowercase().contains(searchLower)
            }
            .distinctBy { it.id }
            .take(30)

        Timber.tag("HomeViewModel").d("🔍 Local search found ${results.size} matches")
        return results
    }
}