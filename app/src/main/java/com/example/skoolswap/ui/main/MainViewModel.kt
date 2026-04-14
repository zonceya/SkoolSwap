package com.example.skoolswap.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.data.local.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NavigationDestination { ONBOARDING, LOGIN, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _forceNavigation = MutableLiveData<NavigationDestination?>()
    val forceNavigation: MutableLiveData<NavigationDestination?> = _forceNavigation

    // Make this MutableLiveData so we can clear it
    private val _navigationDestination = MutableLiveData<NavigationDestination?>()
    val navigationDestination: MutableLiveData<NavigationDestination?> = _navigationDestination

    init {
        viewModelScope.launch {
            checkAuthState()
            observeNavigationState()
        }
    }

    private suspend fun observeNavigationState() {
        combine(
            preferences.isOnboardingFinished,
            authRepository.getServerUser()
        ) { onboardingFinished, user ->
            when {
                !onboardingFinished -> NavigationDestination.ONBOARDING
                user == null -> NavigationDestination.LOGIN
                user.schoolMapped -> NavigationDestination.HOME
                else -> null
            }
        }.collect { destination ->
            _navigationDestination.value = destination
        }
    }

    fun checkAuthState() {
        viewModelScope.launch {
            authRepository.checkCurrentUser()

            // Check DataStore login state
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

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            preferences.setLoggedIn(false)
            _forceNavigation.value = NavigationDestination.LOGIN
        }
    }
}