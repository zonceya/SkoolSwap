package com.example.skoolswap.ui.otp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OTPViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _verificationSuccess = MutableLiveData<User?>()
    val verificationSuccess: LiveData<User?> = _verificationSuccess

    private val _resendSuccess = MutableLiveData<Boolean>()
    val resendSuccess: LiveData<Boolean> = _resendSuccess

    // Store the current OTP token for resend
    private var currentOtpToken: String = ""

    fun verifyOtp(email: String, otpToken: String, otpCode: String, purpose: String, name: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = authRepository.verifyOtp(email, otpToken, otpCode, purpose, name)

                result.onSuccess { user ->
                    saveUserToPreferences(user)
                    _verificationSuccess.value = user
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Verification failed"
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Verification failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resendOtp(email: String, purpose: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = authRepository.resendOtp(email, purpose)

                result.onSuccess { otpToken ->
                    // ✅ Store the new OTP token
                    currentOtpToken = otpToken
                    _resendSuccess.value = true
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Failed to resend OTP"
                    _resendSuccess.value = false
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to resend OTP"
                _resendSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCurrentOtpToken(): String = currentOtpToken

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

    fun clearResendSuccess() {
        _resendSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
    }
}