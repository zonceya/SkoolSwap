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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NavigationDestination { ONBOARDING, LOGIN, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _forceNavigation = MutableLiveData<NavigationDestination?>(null)
    val forceNavigation = _forceNavigation

    // Combine onboarding + auth state from Room database
    val navigationDestination = combine(
        preferences.isOnboardingFinished,
        authRepository.getServerUser()
    ) { onboardingFinished, user ->
        when {
            !onboardingFinished -> NavigationDestination.ONBOARDING
            user == null -> NavigationDestination.LOGIN
            else -> NavigationDestination.HOME
        }
    }.asLiveData()

    // Check auth state on initialization
    init {
        viewModelScope.launch {
            checkAuthState()
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
            // Force navigation to login
            _forceNavigation.value = NavigationDestination.LOGIN
        }
    }
}