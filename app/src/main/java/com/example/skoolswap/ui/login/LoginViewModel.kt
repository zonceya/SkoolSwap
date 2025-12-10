// ui/login/LoginViewModel.kt
package com.example.skoolswap.ui.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activity)
            _isLoading.value = true
            _error.value = null

          //  val result = authRepository.signInWithGoogle()

            result.onSuccess { user ->
                _user.value = user
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Sign in failed"
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}