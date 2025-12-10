package com.example.skoolswap.ui.navigationheader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NavigationHeaderViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Get user data from serverUser flow
    val userName: StateFlow<String?> = authRepository.getServerUser()
        .map { user -> user?.name }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userProfileImage: StateFlow<String?> = authRepository.getServerUser()
        .map { user -> user?.profilePictureUrl }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userEmail: StateFlow<String?> = authRepository.getServerUser()
        .map { user -> user?.email }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isLoggedIn: StateFlow<Boolean> = authRepository.getServerUser()
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