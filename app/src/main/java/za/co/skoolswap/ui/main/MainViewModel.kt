package za.co.skoolswap.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

enum class NavigationDestination { ONBOARDING, LOGIN, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    var suppressNextResumeRefresh = false

    // Used ONLY for forced logout / auth-invalidation overrides.
    // Initial routing (onboarding → login → home) is decided exclusively by
    // IntroFragment.determineDestination(). Do NOT add a second initial-routing
    // decider here — it races with IntroFragment and crashes the nav graph.
    private val _forceNavigation = MutableLiveData<NavigationDestination?>()
    val forceNavigation: MutableLiveData<NavigationDestination?> = _forceNavigation

    init {
        viewModelScope.launch {
            checkAuthState()
        }
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
    // Inside MainViewModel
    fun syncFcmTokenOnStartup() {
        viewModelScope.launch {
            try {
                // Check if user is logged in
                val isLoggedIn = preferences.isLoggedIn.first()
                if (!isLoggedIn) return@launch

                // Fetch fresh FCM token from Firebase
                val fcmToken = FirebaseMessaging.getInstance().token.await()

                // Save to Rails
                val result = authRepository.saveFcmToken(fcmToken)

                if (result.isSuccess) {
                    Timber.tag("MainViewModel").d("✅ FCM token synced on app startup")
                } else {
                    Timber.tag("MainViewModel").w("⚠️ FCM token sync failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.tag("MainViewModel").e(e, "❌ Startup FCM token sync failed")
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

    suspend fun hasContactNumber(): Boolean {
        return try {
            val userProfile = authRepository.getServerUser().firstOrNull()
            !userProfile?.mobile.isNullOrEmpty()
        } catch (e: Exception) {
            Timber.tag(LogTags.VIEW_MODEL).e(e, "Failed to check contact number")
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
                Timber.tag(LogTags.VIEW_MODEL).e(e, "Logout error in ViewModel")
            }
        }
    }
}