package com.example.skoolswap.ui.navigationheader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationHeaderViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        // Start observing user changes immediately
        observeUserChanges()
    }

    private fun observeUserChanges() {
        viewModelScope.launch {
            authRepository.getServerUser().collect { user ->
                if (user != null) {
                    _userState.value = UserState.Success(
                        name = user.name,
                        email = user.email,
                        profileImageUrl = user.profilePictureUrl.takeIf { !it.isNullOrEmpty() }
                    )
                    // 🔥 REMOVE THIS LINE - it's causing the infinite loop!
                    // refreshFromApiInBackground()
                } else {
                    _userState.value = UserState.Success(
                        name = "Welcome",
                        email = "Sign in to continue",
                        profileImageUrl = null
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                // Force refresh from API
                val result = authRepository.refreshUserProfile()

                result.onSuccess { user ->
                    if (user != null) {
                        _userState.value = UserState.Success(
                            name = user.name,
                            email = user.email,
                            profileImageUrl = user.profilePictureUrl.takeIf { !it.isNullOrEmpty() }
                        )
                    } else {
                        _userState.value = UserState.Error("No user data")
                    }
                }.onFailure { throwable ->
                    _userState.value = UserState.Error("Failed: ${throwable.message}")
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error("Error: ${e.message}")
            }
        }
    }

  }