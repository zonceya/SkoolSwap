package za.co.skoolswap.ui.login

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.common.constants.ErrorConstantsHelper
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.model.User
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    // Google Sign-In LiveData
    private val _loginSuccess = MutableLiveData<User?>()
    val loginSuccess: LiveData<User?> = _loginSuccess

    // UI States
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Timber.tag(TAG).d("🏁 ViewModel INITIALIZED")
    }

    fun setRestoredUser(user: User) {
        _loginSuccess.value = user
    }

    suspend fun refreshUserToken(): Boolean {
        return try {
            val result = authRepository.refreshToken()
            result.isSuccess
        } catch (e: Exception) {
            Timber.tag(AppConstants.LogTags.AUTH).e(e, "Token refresh failed")
            false
        }
    }

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
            Timber.tag(TAG).d("📞 signInWithGoogle() STARTED")

            _isLoading.value = true
            _error.value = null

            val result = authRepository.signInWithGoogle(activity)

            result.onSuccess { user ->
                Timber.tag(TAG).d("✅ SUCCESS! User received")
                saveUserToPreferences(user)
                _loginSuccess.value = user
            }.onFailure { throwable ->
                Timber.tag(TAG).e("❌ FAILURE! ${throwable.message}")
                val userMessage = ErrorConstantsHelper.getErrorMessage(throwable)
                _error.value = userMessage
            }

            _isLoading.value = false
        }
    }

    // ==================== FIREBASE EMAIL/PASSWORD LOGIN ====================
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Timber.tag(TAG).d("📧 Signing in with email: $email")

            val result = authRepository.signInWithEmail(email, password)

            result.onSuccess { user ->
                Timber.tag(TAG).d("✅ Email sign in successful!")
                saveUserToPreferences(user)
                _loginSuccess.value = user
            }.onFailure { throwable ->
                Timber.tag(TAG).e("❌ Email sign in failed: ${throwable.message}")
                val userMessage = ErrorConstantsHelper.getErrorMessage(throwable)
                _error.value = userMessage
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
        appPreferences.setUserProfileImage(user.profilePictureUrl ?: AppConstants.EMPTY_STRING)

        if (user.schoolMapped) {
            appPreferences.setSchoolMapped(true)
            user.schoolId?.let { schoolId ->
                appPreferences.setSchoolInfo(schoolId, user.schoolName ?: AppConstants.EMPTY_STRING)
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
        Timber.tag(TAG).d("🧹 ViewModel onCleared()")
    }
}