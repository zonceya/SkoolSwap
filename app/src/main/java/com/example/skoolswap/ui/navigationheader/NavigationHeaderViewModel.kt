package com.example.skoolswap.ui.navigationheader

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
class NavigationHeaderViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val result = authRepository.refreshUserProfile()
                result.onSuccess { user ->
                    _userState.value = UserState.Success(
                        name = user?.name,
                        email = user?.email,
                        profileImageUrl = user?.profilePictureUrl
                    )
                }.onFailure { throwable ->
                    _userState.value = UserState.Error("Failed to load profile")
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error("Error: ${e.message}")
            }
        }
    }
}