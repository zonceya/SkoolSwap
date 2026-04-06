// ui/home/HomeViewModel.kt
package com.example.skoolswap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.homefeed.HomeFeed
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepositoryInterface,
    private val userSchoolRepository: UserSchoolRepositoryInterface
) : ViewModel() {

    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Cache to preserve data across navigation
    private var cachedHomeFeed: HomeFeed? = null

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        // If we have cached data and not forcing refresh, use it immediately
        if (!forceRefresh && cachedHomeFeed != null) {
            _homeFeed.value = cachedHomeFeed
            return
        }

        // If already loading, don't start another request
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
        when (val result = homeRepository.getHomeFeed(schoolId)) {
            is Result.Success -> {
                cachedHomeFeed = result.data
                _homeFeed.value = result.data
                _error.value = null
            }
            is Result.Error -> {
                _error.value = result.exception.message
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
}