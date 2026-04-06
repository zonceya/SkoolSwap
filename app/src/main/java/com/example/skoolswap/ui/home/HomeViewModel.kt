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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepositoryInterface,
    private val userSchoolRepository: UserSchoolRepositoryInterface
) : ViewModel() {

    // Observe home feed from repository
    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    val homeFeed: StateFlow<HomeFeed?> = _homeFeed

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Collect home feed from repository
        viewModelScope.launch {
            homeRepository.homeFeed.collectLatest { feed ->
                _homeFeed.value = feed
            }
        }
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                is Result.Success -> {
                    val mapping = result.data
                    if (mapping != null) {
                        loadFeedWithSchoolId(mapping.schoolId)
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

    private suspend fun loadFeedWithSchoolId(schoolId: Int) {
        when (val result = homeRepository.getHomeFeed(schoolId)) {
            is Result.Success -> {
                // Feed is automatically updated via StateFlow
                _error.value = null
            }
            is Result.Error -> {
                _error.value = result.exception.message
            }
        }
        _isLoading.value = false
    }

    fun clearHomeData() {
        viewModelScope.launch {
            homeRepository.clearHomeData()
        }
    }
}