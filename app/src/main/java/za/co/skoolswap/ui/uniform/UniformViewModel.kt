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
    private var uniformMainCategoryId: Int = 1

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

                    // Only show Uniform categories (IDs 1-15)
                    val allowedCategoryIds = (1..15).toSet()

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

    private fun getFallbackCategories(): List<UniformCategory> {
        return listOf(
            UniformCategory(1, "Blazer", 1, R.drawable.ic_uniform_blazer),
            UniformCategory(2, "Jersey", 1, R.drawable.ic_uniform_jersey),
            UniformCategory(3, "Shirt", 1, R.drawable.ic_uniform_shirt),
            UniformCategory(4, "Golf Shirt", 1, R.drawable.ic_uniform_shirt),
            UniformCategory(5, "Skirt", 1, R.drawable.ic_uniform_skirt),
            UniformCategory(6, "Dress", 1, R.drawable.ic_uniform_skirt),
            UniformCategory(7, "Trousers", 1, R.drawable.ic_uniform_pants),
            UniformCategory(8, "Shorts", 1, R.drawable.ic_uniform_pants),
            UniformCategory(9, "Tracksuit", 1, R.drawable.ic_uniform_tracksuit),
            UniformCategory(10, "PE Kit", 1, R.drawable.ic_uniform_tracksuit),
            UniformCategory(11, "Sports Uniform", 1, R.drawable.ic_uniform_tracksuit),
            UniformCategory(12, "Tie", 1, R.drawable.ic_uniform_tie),
            UniformCategory(13, "Socks", 1, R.drawable.ic_uniform_socks),
            UniformCategory(14, "Hat", 1, R.drawable.ic_uniform_hat),
            UniformCategory(15, "Jacket", 1, R.drawable.ic_uniform_blazer)
        )
    }

    private fun getImageResId(categoryName: String): Int {
        return when {
            categoryName.contains("Blazer", ignoreCase = true) ||
                    categoryName.contains("Jacket", ignoreCase = true) -> R.drawable.ic_uniform_blazer
            categoryName.contains("Jersey", ignoreCase = true) -> R.drawable.ic_uniform_jersey
            categoryName.contains("Shirt", ignoreCase = true) ||
                    categoryName.contains("Golf", ignoreCase = true) -> R.drawable.ic_uniform_shirt
            categoryName.contains("Skirt", ignoreCase = true) ||
                    categoryName.contains("Dress", ignoreCase = true) -> R.drawable.ic_uniform_skirt
            categoryName.contains("Trouser", ignoreCase = true) ||
                    categoryName.contains("Short", ignoreCase = true) -> R.drawable.ic_uniform_pants
            categoryName.contains("Tracksuit", ignoreCase = true) ||
                    categoryName.contains("PE Kit", ignoreCase = true) ||
                    categoryName.contains("Sports Uniform", ignoreCase = true) -> R.drawable.ic_uniform_tracksuit
            categoryName.contains("Tie", ignoreCase = true) -> R.drawable.ic_uniform_tie
            categoryName.contains("Sock", ignoreCase = true) -> R.drawable.ic_uniform_socks
            categoryName.contains("Hat", ignoreCase = true) -> R.drawable.ic_uniform_hat
            else -> R.drawable.ic_uniform_placeholder
        }
    }
}