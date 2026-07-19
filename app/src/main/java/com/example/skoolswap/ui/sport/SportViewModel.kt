package com.example.skoolswap.ui.sport

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.common.constants.SportConstants
import com.example.skoolswap.domain.model.GearItem
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.SportItem
import com.example.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SportViewModel @Inject constructor(
    private val productsRepository: ProductsRepositoryInterface,
    private val productsCacheRepository: ProductsCacheRepositoryInterface,
    private val userSchoolRepository: UserSchoolRepositoryInterface
) : ViewModel() {

    private val _featuredSports = MutableLiveData<List<SportItem>>()
    val featuredSports: LiveData<List<SportItem>> = _featuredSports

    private val _moreSports = MutableLiveData<List<SportItem>>()
    val moreSports: LiveData<List<SportItem>> = _moreSports

    private val _allSportItems = MutableLiveData<List<Item>>()
    val allSportItems: LiveData<List<Item>> = _allSportItems

    private val _gearItems = MutableLiveData<List<GearItem>>()
    val gearItems: LiveData<List<GearItem>> = _gearItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ✅ In-memory cache for the life of the process
    // This mirrors FilterRepository's categoryFilterCache pattern
    private var memoryCachedItems: List<Item>? = null

    fun loadSportData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("SportViewModel", "loadSportData - START")

            // ✅ Randomly choose between Set 1 and Set 2
            val useSet1 = Random.nextBoolean()

            if (useSet1) {
                _featuredSports.value = SportConstants.FEATURED_SET_1
                _moreSports.value = SportConstants.MORE_SET_1
                Log.d("SportViewModel", "Using SET 1")
            } else {
                _featuredSports.value = SportConstants.FEATURED_SET_2
                _moreSports.value = SportConstants.MORE_SET_2
                Log.d("SportViewModel", "Using SET 2")
            }

            // ✅ Gear items from constants
            _gearItems.value = SportConstants.GEAR_ITEMS

            // ✅ Load items with caching (matches Uniform pattern)
            loadAllSportItemsCacheFirst()
        }
    }

    /**
     * ✅ Load sport items with 3-layer caching strategy
     * Matches FilterRepository.getFilterConfig() pattern exactly:
     * 1. Memory cache - HIT means STOP, return immediately
     * 2. Disk cache (Room) - HIT means STOP, promote to memory
     * 3. Network - ONLY on cache miss
     */
    private suspend fun loadAllSportItemsCacheFirst() {
        // 1️⃣ MEMORY CACHE - INSTANT! (< 1ms)
        memoryCachedItems?.let {
            Log.d("SportViewModel", "📦 Memory cache HIT: ${it.size} items")
            _allSportItems.value = it
            _isLoading.value = false
            return  // ✅ STOP - no further checks, no network call
        }

        // 2️⃣ DISK CACHE (Room) - FAST! (~10-50ms)
        val schoolId = getSchoolId()
        if (schoolId != null) {
            // ✅ Use the correct cache key: "sports_${schoolId}_all"
            val cached = productsCacheRepository.getCachedSports(schoolId, sportType = null)
            if (!cached.isNullOrEmpty()) {
                Log.d("SportViewModel", "📦 Disk cache HIT: ${cached.size} items")
                memoryCachedItems = cached  // ✅ Promote to memory for next time
                _allSportItems.value = cached
                _isLoading.value = false
                return  // ✅ STOP - no network call
            } else {
                Log.d("SportViewModel", "⏭️ Disk cache MISS for schoolId: $schoolId")
            }
        } else {
            Log.d("SportViewModel", "⏭️ No schoolId available for cache lookup")
        }

        // 3️⃣ NETWORK - ONLY ON CACHE MISS (matches Uniform's network fallback)
        Log.d("SportViewModel", "🌐 Cache MISS, fetching from network")
        fetchFromNetwork(schoolId)
    }

    /**
     * ✅ Fetch from network and cache results
     * Only called when both memory and disk cache are empty/expired
     */
    private suspend fun fetchFromNetwork(schoolId: Int? = null) {
        val result = productsRepository.getRecommendedAll(
            page = 1,
            categoryId = 2,           // Sports category
            conditionId = null,
            minPrice = null,
            maxPrice = null
        )

        when (result) {
            is Result.Success -> {
                val items = result.data.items
                _allSportItems.value = items
                memoryCachedItems = items  // ✅ Warm memory cache
                Log.d("SportViewModel", "✅ Network loaded ${items.size} items")

                // ✅ Persist to disk cache for next app start
                val resolvedSchoolId = schoolId ?: getSchoolId()
                if (resolvedSchoolId != null) {
                    productsCacheRepository.cacheSports(
                        schoolId = resolvedSchoolId,
                        sportType = null,
                        items = items
                    )
                    Log.d("SportViewModel", "✅ Persisted ${items.size} items to disk cache")
                } else {
                    Log.d("SportViewModel", "⚠️ No schoolId, skipping disk cache")
                }
            }
            is Result.Error -> {
                _error.value = result.exception.message
                _allSportItems.value = emptyList()
                Log.e("SportViewModel", "❌ Failed: ${result.exception.message}")
            }
        }
        _isLoading.value = false
    }

    /**
     * ✅ Get current school ID from database
     */
    private suspend fun getSchoolId(): Int? {
        return when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
            is Result.Success -> result.data?.schoolId
            is Result.Error -> {
                Log.e("SportViewModel", "❌ Failed to get school ID: ${result.exception.message}")
                null
            }
        }
    }

    /**
     * ✅ User-triggered refresh - bypasses all caches
     * This is the ONLY place that should force a network hit
     * Matches the refresh behavior in Uniform
     */
    fun refreshSports() {
        // ✅ Clear memory cache to force network refresh
        memoryCachedItems = null

        viewModelScope.launch {
            // ✅ Randomly switch between sets on refresh
            val useSet1 = Random.nextBoolean()

            if (useSet1) {
                _featuredSports.value = SportConstants.FEATURED_SET_1
                _moreSports.value = SportConstants.MORE_SET_1
                Log.d("SportViewModel", "REFRESH - Using SET 1")
            } else {
                _featuredSports.value = SportConstants.FEATURED_SET_2
                _moreSports.value = SportConstants.MORE_SET_2
                Log.d("SportViewModel", "REFRESH - Using SET 2")
            }

            // ✅ Force network fetch
            fetchFromNetwork()
        }
    }

    /**
     * ✅ Clear all caches (useful for logout)
     */
    fun clearCache() {
        memoryCachedItems = null
        Log.d("SportViewModel", "🗑️ Memory cache cleared")
    }
}