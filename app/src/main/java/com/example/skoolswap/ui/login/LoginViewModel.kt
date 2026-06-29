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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : ViewModel() {

    // Google Sign-In LiveData
    private val _loginSuccess = MutableLiveData<User?>()
    val loginSuccess: LiveData<User?> = _loginSuccess

    // UI States
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Log.e("LoginViewModel", "🏁 ViewModel INITIALIZED")
    }

    fun setRestoredUser(user: User) {
        _loginSuccess.value = user
    }
    // In LoginViewModel.kt - add this method
    suspend fun refreshUserToken(): Boolean {
        return try {
            val result = authRepository.refreshToken()
            result.isSuccess
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            false
        }
    }

    // Optional: Add a method to check token validity
    suspend fun checkTokenValidity(): Boolean {
        return try {
            val token = authRepository.getCurrentToken()
            if (token.isNullOrEmpty()) {
                return false
            }
            authRepository.validateToken(token)
        } catch (e: Exception) {
            false
        }
    }
    // ==================== GOOGLE SIGN-IN ====================
    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            Log.e("LoginViewModel", "📞 signInWithGoogle() STARTED")

            _isLoading.value = true
            _error.value = null

            val result = authRepository.signInWithGoogle(activity)

            result.onSuccess { user ->
                Log.e("LoginViewModel", "✅ SUCCESS! User received")
                saveUserToPreferences(user)
                _loginSuccess.value = user
            }.onFailure { throwable ->
                Log.e("LoginViewModel", "❌ FAILURE! ${throwable.message}")
                _error.value = throwable.message ?: "Sign in failed"
            }

            _isLoading.value = false
        }
    }

    // ==================== FIREBASE EMAIL/PASSWORD LOGIN ====================
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.e("LoginViewModel", "📧 Signing in with email: $email")

            val result = authRepository.signInWithEmail(email, password)

            result.onSuccess { user ->
                Log.e("LoginViewModel", "✅ Email sign in successful!")
                saveUserToPreferences(user)
                _loginSuccess.value = user
            }.onFailure { throwable ->
                Log.e("LoginViewModel", "❌ Email sign in failed: ${throwable.message}")
                _error.value = throwable.message ?: "Sign in failed"
            }

            _isLoading.value = false
        }
    }

    fun restoreSession() {
        viewModelScope.launch {
            authRepository.restoreSession()
        }
    }

    fun clearLoginSuccess() {
        _loginSuccess.value = null
    }

    private suspend fun saveUserToPreferences(user: User) {
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
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.e("LoginViewModel", "🧹 ViewModel onCleared()")
    }
}