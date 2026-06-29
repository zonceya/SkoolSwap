package com.example.skoolswap.ui.sport

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.SportConstants
import com.example.skoolswap.domain.model.GearItem
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.SportItem
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
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
    private val productsRepository: ProductsRepositoryInterface
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
                _gearItems.value = SportConstants.GEAR_ITEMS
                Log.d("SportViewModel", "Using SET 1")
            } else {
                _featuredSports.value = SportConstants.FEATURED_SET_2
                _moreSports.value = SportConstants.MORE_SET_2
                _gearItems.value = SportConstants.GEAR_ITEMS
                Log.d("SportViewModel", "Using SET 2")
            }

            // ✅ Gear items from constants
            _gearItems.value = SportConstants.GEAR_ITEMS

            loadAllSportItems()
        }
    }

    fun refreshSports() {
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
        }
    }

    private suspend fun loadAllSportItems() {
        val result = productsRepository.getRecommendedAll(
            page = 1,
            categoryId = 2,           // Sports category
            conditionId = null,
            minPrice = null,
            maxPrice = null
        )

        when (result) {
            is Result.Success -> {
                _allSportItems.value = result.data.items
                Log.d("SportViewModel", "✅ All sports loaded: ${result.data.items.size} items")
            }
            is Result.Error -> {
                _error.value = result.exception.message
                _allSportItems.value = emptyList()
                Log.e("SportViewModel", "❌ Failed: ${result.exception.message}")
            }
        }
        _isLoading.value = false
    }
}