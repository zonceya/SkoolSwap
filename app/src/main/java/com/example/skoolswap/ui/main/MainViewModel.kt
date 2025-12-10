// ui/main/MainViewModel.kt
package com.example.skoolswap.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NavigationDestination { ONBOARDING, LOGIN, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _forceNavigation = MutableLiveData<NavigationDestination?>(null)
    val forceNavigation = _forceNavigation

    // Combine onboarding + login Flows to determine destination
    val navigationDestination = combine(
        preferences.isOnboardingFinished,
        preferences.isLoggedIn
    ) { onboardingFinished, loggedIn ->
        when {
            !onboardingFinished -> NavigationDestination.ONBOARDING
            !loggedIn -> NavigationDestination.LOGIN
            else -> NavigationDestination.HOME
        }
    }.asLiveData()

    // Helper functions to update preferences
    fun finishOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingFinished(true)
            // Force immediate navigation to login
            _forceNavigation.value = NavigationDestination.LOGIN // or HOME if you want to skip login
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
}