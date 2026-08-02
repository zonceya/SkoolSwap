package za.co.skoolswap.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.model.User
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _signUpSuccess = MutableLiveData<User?>()
    val signUpSuccess: LiveData<User?> = _signUpSuccess

    fun signUpWithEmail(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authRepository.signUpWithEmail(name, email, password, passwordConfirmation)

            result.onSuccess { user ->
                saveUserToPreferences(user)
                _signUpSuccess.value = user
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Sign up failed"
            }

            _isLoading.value = false
        }
    }

    private suspend fun saveUserToPreferences(user: User) {
        appPreferences.setLoggedIn(true)
        appPreferences.setFirstTimeLogin(false)
        appPreferences.setAuthToken(user.token)
        appPreferences.setUserId(user.id.toString())
        appPreferences.setUserName(user.name)
        appPreferences.setUserEmail(user.email)

        if (user.profilePictureUrl != null) {
            appPreferences.setUserProfileImage(user.profilePictureUrl)
        }

        if (user.schoolMapped && user.schoolId != null) {
            appPreferences.setSchoolMapped(true)
            appPreferences.setSchoolInfo(user.schoolId, user.schoolName ?: "")
        } else {
            appPreferences.setSchoolMapped(false)
        }
    }

    fun clearError() {
        _error.value = null
    }
}