package com.example.skoolswap.ui.main


import androidx.lifecycle.*
import com.example.skoolswap.data.local.AppPreferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class NavigationDestination { ONBOARDING, LOGIN, HOME }

class MainViewModel(private val preferences: AppPreferences) : ViewModel() {

    private val _forceNavigation = MutableLiveData<NavigationDestination?>(null)
    val forceNavigation: LiveData<NavigationDestination?> = _forceNavigation

    // Combine onboarding + login Flows to determine destination
    val navigationDestination: LiveData<NavigationDestination> = combine(
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