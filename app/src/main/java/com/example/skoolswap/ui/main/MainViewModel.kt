package com.example.skoolswap.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.ui.item.CreateItemViewModel.Companion.TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class NavigationDestination { ONBOARDING, LOGIN, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {
    var suppressNextResumeRefresh = false
    private val _forceNavigation = MutableLiveData<NavigationDestination?>()
    val forceNavigation: MutableLiveData<NavigationDestination?> = _forceNavigation

    private val _navigationDestination = MutableLiveData<NavigationDestination?>()
    val navigationDestination: MutableLiveData<NavigationDestination?> = _navigationDestination

    // Guard — only emit initial navigation once
    private var initialNavigationEmitted = false

    init {
        viewModelScope.launch {
            checkAuthState()
            emitInitialNavigation()
        }
    }

    // Called ONCE on startup to determine where to go
    private suspend fun emitInitialNavigation() {
        if (initialNavigationEmitted) return
        initialNavigationEmitted = true

        val onboardingFinished = preferences.isOnboardingFinished.firstOrNull() ?: false
        val user = authRepository.getServerUser().firstOrNull()

        val destination = when {
            !onboardingFinished -> NavigationDestination.ONBOARDING
            user == null -> NavigationDestination.LOGIN
            user.schoolMapped -> NavigationDestination.HOME
            else -> NavigationDestination.LOGIN
        }

        _navigationDestination.value = destination
    }

    fun checkAuthState() {
        viewModelScope.launch {
            authRepository.checkCurrentUser()

            val isLoggedIn = preferences.isLoggedIn.firstOrNull() ?: false
            if (!isLoggedIn) {
                _forceNavigation.value = NavigationDestination.LOGIN
            }
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingFinished(true)
        }
    }

    fun clearNavigationDestination() {
        _navigationDestination.value = null
    }

    fun clearForceNavigation() {
        _forceNavigation.value = null
    }

    fun setLoggedIn() {
        viewModelScope.launch {
            preferences.setLoggedIn(true)
        }
    }

    suspend fun hasContactNumber(): Boolean {
        return try {
            val userProfile = authRepository.getServerUser().firstOrNull()
            !userProfile?.mobile.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                preferences.setLoggedIn(false)
                _forceNavigation.value = NavigationDestination.LOGIN
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Logout error in ViewModel")
            }
        }
    }
}