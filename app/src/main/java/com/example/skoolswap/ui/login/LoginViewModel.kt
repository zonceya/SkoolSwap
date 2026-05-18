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

    // Google Sign-In LiveData
    private val _loginSuccess = MutableLiveData<User?>()
    val loginSuccess: LiveData<User?> = _loginSuccess

    // Email OTP LiveData
    private val _otpSent = MutableLiveData<String?>() // Returns otpToken
    val otpSent: LiveData<String?> = _otpSent

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

    // ==================== EMAIL OTP LOGIN ====================
    fun sendLoginOtp(email: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.e("LoginViewModel", "📧 Sending login OTP to: $email")

                val result = authRepository.sendLoginOtp(email)

                result.onSuccess { otpToken ->
                    Log.e("LoginViewModel", "✅ OTP sent successfully")
                    _otpSent.value = otpToken
                }.onFailure { throwable ->
                    Log.e("LoginViewModel", "❌ Failed to send OTP: ${throwable.message}")
                    _error.value = throwable.message ?: "Failed to send OTP"
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "❌ Error: ${e.message}")
                _error.value = e.message ?: "Failed to send OTP"
            } finally {
                _isLoading.value = false
            }
        }
    }


    // ✅ Add this
    fun clearLoginSuccess() {
        _loginSuccess.value = null
    }
    fun resendOtp(email: String, purpose: String = "LOGIN") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.e("LoginViewModel", "🔄 Resending OTP to: $email")

                val result = authRepository.resendOtp(email, purpose)

                result.onSuccess { otpToken ->
                    Log.e("LoginViewModel", "✅ OTP resent successfully")
                    _otpSent.value = otpToken
                }.onFailure { throwable ->
                    Log.e("LoginViewModel", "❌ Failed to resend OTP: ${throwable.message}")
                    _error.value = throwable.message ?: "Failed to resend OTP"
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "❌ Error: ${e.message}")
                _error.value = e.message ?: "Failed to resend OTP"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearOtpSent() {
        _otpSent.value = null
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