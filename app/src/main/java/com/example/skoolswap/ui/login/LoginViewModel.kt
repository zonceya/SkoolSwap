package com.example.skoolswap.ui.login

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : ViewModel() {

    // Use LiveData instead of callbacks
    private val _loginSuccess = MutableLiveData<User?>()
    val loginSuccess: LiveData<User?> = _loginSuccess

    // UI States
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Log.e("LoginViewModel", "🏁 ViewModel INITIALIZED at ${System.currentTimeMillis()}")
    }

    fun setRestoredUser(user: User) {
        // For session restoration without triggering sign-in flow
        _loginSuccess.value = user
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            Log.e("LoginViewModel", "📞 signInWithGoogle() STARTED")

            _isLoading.value = true
            _error.value = null

            val result = authRepository.signInWithGoogle(activity)

            result.onSuccess { user ->
                Log.e("LoginViewModel", "✅ SUCCESS! User received")
                Log.e("LoginViewModel", "✅ User email: ${user.email}")
                Log.e("LoginViewModel", "✅ schoolMapped: ${user.schoolMapped}")

                // Save to preferences
                saveUserToPreferences(user)

                // Emit success via LiveData
                _loginSuccess.value = user

            }.onFailure { throwable ->
                Log.e("LoginViewModel", "❌ FAILURE! ${throwable.message}")
                _error.value = throwable.message ?: "Sign in failed"
            }

            _isLoading.value = false
        }
    }

    private suspend fun saveUserToPreferences(user: User) {
        Log.e("LoginViewModel", "📞 saveUserToPreferences() STARTED")

        appPreferences.setLoggedIn(true)
        appPreferences.setFirstTimeLogin(false)
        appPreferences.setAuthToken(user.token)
        appPreferences.setUserId(user.id.toString())
        appPreferences.setUserName(user.name)
        appPreferences.setUserEmail(user.email)
        appPreferences.setUserProfileImage(user.profilePictureUrl ?: "")

        if (user.schoolMapped) {
            appPreferences.setSchoolMapped(true)
            user.schoolId?.let { schoolId ->
                appPreferences.setSchoolInfo(schoolId, user.schoolName ?: "")
            }
        } else {
            appPreferences.setSchoolMapped(false)
        }

        Log.e("LoginViewModel", "📞 saveUserToPreferences() COMPLETED")
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.e("LoginViewModel", "🧹 ViewModel onCleared()")
    }
}