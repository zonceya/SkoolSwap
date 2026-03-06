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

    // Callback for navigation - using a property to track changes
    private var _onLoginSuccess: ((User) -> Unit)? = null
    var onLoginSuccess: ((User) -> Unit)?
        get() {
            Log.e("LoginViewModel", "📞 Callback GETTER - current value: ${if (_onLoginSuccess != null) "SET" else "NULL"}")
            return _onLoginSuccess
        }
        set(value) {
            _onLoginSuccess = value
            Log.e("LoginViewModel", "📞 Callback SETTER - new value: ${if (value != null) "SET" else "NULL"}, hash: ${value?.hashCode()}")
        }

    // UI States
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Log.e("LoginViewModel", "🏁🏁🏁 ViewModel INITIALIZED at ${System.currentTimeMillis()}")
        Log.e("LoginViewModel", "🏁 HashCode: ${this.hashCode()}")
        Log.e("LoginViewModel", "🏁 Initial callback state: ${if (_onLoginSuccess != null) "SET" else "NULL"}")
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            Log.e("LoginViewModel", "📞 signInWithGoogle() STARTED at ${System.currentTimeMillis()}")

            _isLoading.value = true
            _error.value = null

            Log.e("LoginViewModel", "📞 Calling authRepository...")
            val result = authRepository.signInWithGoogle(activity)
            Log.e("LoginViewModel", "📞 authRepository returned at ${System.currentTimeMillis()}")

            result.onSuccess { user ->
                Log.e("LoginViewModel", "✅✅✅ SUCCESS! User received")
                Log.e("LoginViewModel", "✅ User email: ${user.email}")
                Log.e("LoginViewModel", "✅ schoolMapped: ${user.schoolMapped}")

                // Check callback status BEFORE invoking
                if (_onLoginSuccess != null) {
                    Log.e("LoginViewModel", "✅ Callback EXISTS at time of success, invoking...")
                    Log.e("LoginViewModel", "✅ Callback hash: ${_onLoginSuccess?.hashCode()}")

                    // Invoke the callback
                    _onLoginSuccess?.invoke(user)

                    Log.e("LoginViewModel", "✅ Callback invoked successfully")
                } else {
                    Log.e("LoginViewModel", "❌❌❌ CRITICAL: Callback is NULL at time of success!")
                    Log.e("LoginViewModel", "❌ Navigation will NOT happen!")

                    // Log stack trace to see where we are
                    Log.e("LoginViewModel", "❌ Stack trace:", RuntimeException().apply { stackTrace = Thread.currentThread().stackTrace })
                }

                // Handle successful login (save to preferences)
                handleSuccessfulLogin(user)

            }.onFailure { throwable ->
                Log.e("LoginViewModel", "❌❌❌ FAILURE! ${throwable.message}")
                _error.value = throwable.message ?: "Sign in failed"
            }

            _isLoading.value = false
            Log.e("LoginViewModel", "📞 signInWithGoogle() COMPLETED at ${System.currentTimeMillis()}")
        }
    }

    private suspend fun handleSuccessfulLogin(user: User) {
        Log.e("LoginViewModel", "📞 handleSuccessfulLogin() STARTED")

        appPreferences.setLoggedIn(true)
        appPreferences.setFirstTimeLogin(false)
        appPreferences.setAuthToken(user.token)
        appPreferences.setUserId(user.id.toString())
        appPreferences.setUserName(user.name)
        appPreferences.setUserEmail(user.email)
        appPreferences.setUserProfileImage(user.profilePictureUrl)

        if (user.schoolMapped) {
            appPreferences.setSchoolMapped(true)
            user.schoolId?.let { schoolId ->
                appPreferences.setSchoolInfo(schoolId, user.schoolName ?: "")
            }
        } else {
            appPreferences.setSchoolMapped(false)
        }

        Log.e("LoginViewModel", "📞 handleSuccessfulLogin() COMPLETED")
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.e("LoginViewModel", "🧹 ViewModel onCleared() - callback was ${if (_onLoginSuccess != null) "SET" else "NULL"}")
    }
}