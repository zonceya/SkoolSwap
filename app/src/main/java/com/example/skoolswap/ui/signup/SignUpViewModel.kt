package com.example.skoolswap.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _signUpSuccess = MutableLiveData<String?>() // Returns otpToken
    val signUpSuccess: LiveData<String?> = _signUpSuccess

    fun sendSignUpOtp(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // sendSignUpOtp returns Result<String> - the otpToken directly
                val result = authRepository.sendSignUpOtp(email, name, password, passwordConfirmation)

                result.onSuccess { otpToken ->
                    // otpToken is String directly, not an object
                    _signUpSuccess.value = otpToken
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Sign up failed"
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Sign up failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}