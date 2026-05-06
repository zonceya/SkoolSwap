package com.example.skoolswap.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.HomeFeed
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
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
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepositoryInterface,
    private val userSchoolRepository: UserSchoolRepositoryInterface,
    private val productsRepository: ProductsRepositoryInterface
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

    // Track current search query for reset functionality
    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    // NEW: Loading state for background API
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // NEW: Count of items from server
    private val _serverItemsCount = MutableStateFlow(0)
    val serverItemsCount: StateFlow<Int> = _serverItemsCount.asStateFlow()

    private var searchJob: Job? = null
    private var cachedHomeFeed: HomeFeed? = null

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        if (!forceRefresh && cachedHomeFeed != null) {
            _homeFeed.value = cachedHomeFeed
            return
        }

        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                is Result.Success -> {
                    val mapping = result.data
                    if (mapping != null) {
                        loadFeedWithSchoolId(mapping.schoolId, forceRefresh)
                    } else {
                        _error.value = "Please select a school first"
                        _isLoading.value = false
                    }
                }
                is Result.Error -> {
                    _error.value = "Failed to get school: ${result.exception.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun loadFeedWithSchoolId(schoolId: Int, forceRefresh: Boolean) {
        Log.d("HomeViewModel", "🔄 Loading feed for school $schoolId")
        when (val result = homeRepository.getHomeFeed(schoolId)) {
            is Result.Success -> {
                cachedHomeFeed = result.data
                _homeFeed.value = result.data
                _error.value = null
                Log.d("HomeViewModel", "✅ Feed loaded: ${result.data?.sections?.size} sections")
            }
            is Result.Error -> {
                _error.value = result.exception.message
                Log.e("HomeViewModel", "❌ Feed error: ${result.exception.message}")
            }
        }
        _isLoading.value = false
    }

    fun refreshHomeFeed() {
        loadHomeFeed(forceRefresh = true)
    }

    fun clearHomeData() {
        cachedHomeFeed = null
        viewModelScope.launch {
            homeRepository.clearHomeData()
        }
    }

    fun searchItems(query: String, categoryId: Int?) {
        // Store current search query
        _searchQuery.value = query

        // Cancel any ongoing search
        searchJob?.cancel()

        // Minimum query length check
        if (query.length < 2) {
            Log.d("HomeViewModel", "Query too short (< 2 chars), clearing results")
            _searchResults.value = emptyList()
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce 300ms
            delay(300)

            Log.d("HomeViewModel", "🔍 Searching for: $query (after debounce)")

            // Reset server items count for new search
            _serverItemsCount.value = 0

            // STEP 1: Search local cache first (instant results)
            val localResults = searchLocalCache(query, categoryId)

            if (localResults.isNotEmpty()) {
                Log.d("HomeViewModel", "⚡ Found ${localResults.size} results in LOCAL CACHE")
                _searchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _searchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            // STEP 2: Show loading state before API call
            _isLoadingMore.value = true

            // STEP 3: Search server in background (fresh results)
            Log.d("HomeViewModel", "🌐 Fetching fresh results from server...")
            val serverResult = productsRepository.searchItems(
                query = query,
                categoryId = categoryId,
                page = 1,
                perPage = 30
            )

            // STEP 4: Hide loading state
            _isLoadingMore.value = false

            when (serverResult) {
                is com.example.skoolswap.utils.Result.Success -> {
                    val response = serverResult.data
                    val serverItems = response.items
                    _serverItemsCount.value = serverItems.size
                    Log.d("HomeViewModel", "✅ Server returned ${serverItems.size} results")

                    if (serverItems.isNotEmpty()) {
                        val currentResults = _searchResults.value.toMutableList()
                        val existingIds = currentResults.map { it.id }.toSet()

                        // Only add items not already in local results
                        val newItems = serverItems.filter { it.id !in existingIds }

                        if (newItems.isNotEmpty()) {
                            val merged = currentResults + newItems
                            _searchResults.value = merged
                            Log.d("HomeViewModel", "📦 Added ${newItems.size} new items from server")
                        }
                        _isShowingLocalResults.value = false
                    } else if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                        _searchResults.value = emptyList()
                    }
                }
                is com.example.skoolswap.utils.Result.Error -> {
                    Log.e("HomeViewModel", "❌ Server search failed: ${serverResult.exception.message}")
                    if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                        _error.value = "Failed to load results"
                    }
                }
            }
        }
    }

    private fun searchLocalCache(query: String, categoryId: Int?): List<Item> {
        val currentFeed = _homeFeed.value
        if (currentFeed == null) {
            Log.d("HomeViewModel", "No cached feed available")
            return emptyList()
        }

        val allItems = mutableListOf<Item>()

        currentFeed.sections.forEach { section ->
            when (section) {
                is Section.Recommended -> {
                    allItems.addAll(section.items)
                }
                is Section.Essentials -> {
                    allItems.addAll(section.sections.uniforms)
                    allItems.addAll(section.sections.sports)
                    allItems.addAll(section.sections.accessories)
                }
                is Section.Trending -> {
                    allItems.addAll(section.items)
                }
                is Section.Recent -> {
                    allItems.addAll(section.items)
                }
            }
        }

        Log.d("HomeViewModel", "📚 Total cached items: ${allItems.size}")

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

        Log.d("HomeViewModel", "🔍 Local search found ${results.size} matches")
        return results
    }
}