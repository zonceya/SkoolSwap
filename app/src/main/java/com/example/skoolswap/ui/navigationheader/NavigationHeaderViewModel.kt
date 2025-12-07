package com.example.skoolswap.ui.navigationheader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Simple ViewModel for navigation header
 * This only exposes data from AuthRepository - no business logic
 */
class NavigationHeaderViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Just expose the flows from AuthRepository
    val userName: StateFlow<String?> = authRepository.userName
    val userProfileImage: StateFlow<String?> = authRepository.userProfileImage
    val userEmail: StateFlow<String?> = authRepository.userEmail
    val isLoggedIn: StateFlow<Boolean> = authRepository.currentUser
        .map { user -> user != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Simple method to refresh
    fun refresh() {
        authRepository.checkCurrentUser()
    }
}