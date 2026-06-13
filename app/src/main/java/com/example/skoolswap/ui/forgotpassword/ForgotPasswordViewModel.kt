package com.example.skoolswap.ui.forgotpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _resetSent = MutableLiveData(false)
    val resetSent: LiveData<Boolean> = _resetSent

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authRepository.sendPasswordResetEmail(email)

            result.onSuccess {
                _resetSent.value = true
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to send reset email"
            }

            _isLoading.value = false
        }
    }
}