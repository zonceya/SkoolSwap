package com.example.skoolswap.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = authRepository.currentUser
    val loading: StateFlow<Boolean> = authRepository.loading
    val error: StateFlow<String?> = authRepository.error

    fun signInWithGoogle() {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle()
            result.onFailure { error ->
                // Error is automatically handled in the repository and exposed via error StateFlow
                println("Sign-in failed: ${error.message}")
            }
        }
    }

    fun checkCurrentUser() {
        authRepository.checkCurrentUser()
    }

    fun clearError() {
        authRepository.clearError()
    }
}