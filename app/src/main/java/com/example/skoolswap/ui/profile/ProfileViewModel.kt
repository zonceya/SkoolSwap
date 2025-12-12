package com.example.skoolswap.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    fun updateMobile(mobile: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false

            val result = authRepository.updateMobile(mobile)

            result.onSuccess {
                _updateSuccess.value = true
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to update mobile"
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _updateSuccess.value = false
    }
}