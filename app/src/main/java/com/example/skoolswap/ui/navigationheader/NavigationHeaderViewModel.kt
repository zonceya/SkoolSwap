package com.example.skoolswap.ui.navigationheader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationHeaderViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences  // ✅ Inject AppPreferences
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        // Start observing user changes immediately
        observeUserChanges()
    }

    private fun observeUserChanges() {
        viewModelScope.launch {
            // ✅ FIRST: Load from DataStore (immediate display)
            val storedName = appPreferences.userName.first()
            val storedEmail = appPreferences.userEmail.first()
            val storedImageUrl = appPreferences.userProfileImage.first()

            if (storedName != null) {
                _userState.value = UserState.Success(
                    name = storedName,
                    email = storedEmail ?: "",
                    profileImageUrl = storedImageUrl
                )
            }

               authRepository.getServerUser().collect { user ->
                if (user != null) {
                    // Update DataStore with latest
                    appPreferences.setUserName(user.name)
                    appPreferences.setUserEmail(user.email)
                    appPreferences.setUserProfileImage(user.profilePictureUrl ?: "")

                    _userState.value = UserState.Success(
                        name = user.name,
                        email = user.email,
                        profileImageUrl = user.profilePictureUrl.takeIf { !it.isNullOrEmpty() }
                    )
                } else if (storedName == null) {
                    // Only show default if no stored data and no user
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
                        // ✅ Update DataStore with fresh data
                        appPreferences.setUserName(user.name)
                        appPreferences.setUserEmail(user.email)
                        appPreferences.setUserProfileImage(user.profilePictureUrl ?: "")

                        _userState.value = UserState.Success(
                            name = user.name,
                            email = user.email,
                            profileImageUrl = user.profilePictureUrl.takeIf { !it.isNullOrEmpty() }
                        )
                    } else {
                        _userState.value = UserState.Error("No user data")
                    }
                }.onFailure { throwable ->
                    // On failure, try to use stored data
                    val storedName = appPreferences.userName.first()
                    val storedEmail = appPreferences.userEmail.first()
                    val storedImageUrl = appPreferences.userProfileImage.first()

                    if (storedName != null) {
                        _userState.value = UserState.Success(
                            name = storedName,
                            email = storedEmail ?: "",
                            profileImageUrl = storedImageUrl
                        )
                    } else {
                        _userState.value = UserState.Error("Failed: ${throwable.message}")
                    }
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error("Error: ${e.message}")
            }
        }
    }
}