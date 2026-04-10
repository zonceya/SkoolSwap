// ui/uniform/UniformViewModel.kt
package com.example.skoolswap.ui.uniform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.R
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // Only these uniform categories should be shown (no sport items)
    private val uniformOnlyTypeIds = setOf(
        27,  // Shirts & Golfers
        28,  // Jerseys & Pullovers
        29,  // Blazers & Jackets
        30,  // Trousers & Shorts
        31,  // Skirts & Dresses
        32,  // Ties & Accessories
        33,  // Socks
        34   // Sportswear (for PE/Tracksuits)
    )

    fun loadUniformCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = filterRepository.getFilterConfig(1)) { // Category ID 1 = Uniforms
                is Result.Success -> {
                    val typeGroup = result.data.filterGroups.find { it.id == "type" }
                    val categories = typeGroup?.options
                        ?.filter { uniformOnlyTypeIds.contains(it.id) }
                        ?.map { option ->
                            UniformCategory(
                                id = option.id,
                                name = option.name,
                                imageResId = getImageResId(option.id)
                            )
                        } ?: getFallbackCategories()
                    _categories.value = categories
                }
                is Result.Error -> {
                    _categories.value = getFallbackCategories()
                }
            }
            _isLoading.value = false
        }
    }

    private fun getImageResId(categoryId: Int): Int {
        return when (categoryId) {
            27 -> R.drawable.ic_uniform_shirt     // Shirts & Golfers
            28 -> R.drawable.ic_uniform_jersey    // Jerseys & Pullovers
            29 -> R.drawable.ic_uniform_blazer    // Blazers & Jackets
            30 -> R.drawable.ic_uniform_pants     // Trousers & Shorts
            31 -> R.drawable.ic_uniform_skirt     // Skirts & Dresses
            32 -> R.drawable.ic_uniform_tie       // Ties & Accessories
            33 -> R.drawable.ic_uniform_socks     // Socks
            34 -> R.drawable.ic_uniform_tracksuit // Sportswear
            else -> R.drawable.ic_uniform_placeholder
        }
    }

    private fun getFallbackCategories(): List<UniformCategory> {
        return listOf(
            UniformCategory(27, "Shirts & Golfers", R.drawable.ic_uniform_shirt),
            UniformCategory(30, "Trousers & Shorts", R.drawable.ic_uniform_pants),
            UniformCategory(29, "Blazers & Jackets", R.drawable.ic_uniform_blazer),
            UniformCategory(28, "Jerseys & Pullovers", R.drawable.ic_uniform_jersey),
            UniformCategory(31, "Skirts & Dresses", R.drawable.ic_uniform_skirt),
            UniformCategory(34, "Tracksuits", R.drawable.ic_uniform_tracksuit),
            UniformCategory(32, "Ties & Accessories", R.drawable.ic_uniform_tie),
            UniformCategory(33, "Socks", R.drawable.ic_uniform_socks)
        )
    }
}

data class UniformCategory(
    val id: Int,
    val name: String,
    val imageResId: Int
)