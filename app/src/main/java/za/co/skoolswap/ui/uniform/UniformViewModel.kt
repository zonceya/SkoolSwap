package za.co.skoolswap.ui.uniform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.R
import za.co.skoolswap.domain.model.UniformCategory
import za.co.skoolswap.domain.repository.FilterRepositoryInterface
import za.co.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniformViewModel @Inject constructor(
    private val filterRepository: FilterRepositoryInterface
) : ViewModel() {

    private val _categories = MutableLiveData<List<UniformCategory>>()
    val categories: LiveData<List<UniformCategory>> = _categories

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // The REAL Uniform main category ID from the backend
    private var uniformMainCategoryId: Int = 1  // Default is now 1

    fun loadUniformCategories(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _isLoading.value = true
            }
            _error.value = null

            when (val result = filterRepository.getFilterConfig(1, forceRefresh)) {
                is Result.Success -> {
                    val filterConfig = result.data
                    uniformMainCategoryId = filterConfig.mainCategoryId ?: 1

                    val typeGroup = filterConfig.filterGroups.find { it.id == "type" }

                    // Only show these Uniform categories
                    val allowedCategoryIds = setOf(1, 2, 3, 4, 5, 6, 7, 63, 81)

                    val categories = typeGroup?.options
                        ?.filter { option -> option.id in allowedCategoryIds }
                        ?.map { option ->
                            UniformCategory(
                                id = option.id,
                                name = option.name,
                                categoryId = uniformMainCategoryId,
                                imageResId = getImageResId(option.name)
                            )
                        } ?: getFallbackCategories()

                    _categories.value = categories
                }
                is Result.Error -> {
                    _error.value = result.exception.message
                    if (_categories.value.isNullOrEmpty()) {
                        _categories.value = getFallbackCategories()
                    }
                }
            }
            _isLoading.value = false
        }
    }

    // UniformViewModel.kt

    private fun getFallbackCategories(): List<UniformCategory> {
        return listOf(
            UniformCategory(1, "Shirts & Golfers", 1, R.drawable.ic_uniform_shirt),
            UniformCategory(2, "Jerseys & Pullovers", 1, R.drawable.ic_uniform_jersey),
            UniformCategory(3, "Blazers & Jackets", 1, R.drawable.ic_uniform_blazer),
            UniformCategory(4, "Trousers & Shorts", 1, R.drawable.ic_uniform_pants),
            UniformCategory(5, "Skirts & Dresses", 1, R.drawable.ic_uniform_skirt),
            UniformCategory(6, "Ties & Accessories", 1, R.drawable.ic_uniform_tie),
            UniformCategory(7, "Socks", 1, R.drawable.ic_uniform_socks),
            UniformCategory(63, "Jackets & Hoodies", 1, R.drawable.ic_uniform_blazer),
            UniformCategory(81, "Shoes", 1, R.drawable.ic_uniform_shoes)  // ADD THIS - ID 81
        )
    }

    private fun getImageResId(categoryName: String): Int {
        return when {
            categoryName.contains("Shirt", ignoreCase = true) ||
                    categoryName.contains("Golfer", ignoreCase = true) -> R.drawable.ic_uniform_shirt
            categoryName.contains("Jersey", ignoreCase = true) ||
                    categoryName.contains("Pullover", ignoreCase = true) -> R.drawable.ic_uniform_jersey
            categoryName.contains("Blazer", ignoreCase = true) ||
                    categoryName.contains("Jacket", ignoreCase = true) ||
                    categoryName.contains("Hoodie", ignoreCase = true) -> R.drawable.ic_uniform_blazer
            categoryName.contains("Trouser", ignoreCase = true) ||
                    categoryName.contains("Short", ignoreCase = true) -> R.drawable.ic_uniform_pants
            categoryName.contains("Skirt", ignoreCase = true) ||
                    categoryName.contains("Dress", ignoreCase = true) -> R.drawable.ic_uniform_skirt
            categoryName.contains("Tie", ignoreCase = true) ||
                    categoryName.contains("Accessory", ignoreCase = true) -> R.drawable.ic_uniform_tie
            categoryName.contains("Sock", ignoreCase = true) -> R.drawable.ic_uniform_socks
            categoryName.contains("Shoe", ignoreCase = true) -> R.drawable.ic_uniform_shoes  // ADD THIS
            else -> R.drawable.ic_uniform_placeholder
        }
    }
}