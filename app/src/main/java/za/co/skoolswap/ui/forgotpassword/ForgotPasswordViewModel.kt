package za.co.skoolswap.ui.forgotpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.common.constants.ErrorConstantsHelper
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _resetSent = MutableLiveData(false)
    val resetSent: LiveData<Boolean> = _resetSent

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Timber.tag(LogTags.VIEW_MODEL).d("📧 Sending password reset email to: $email")

            val result = authRepository.sendPasswordResetEmail(email)

            result.onSuccess {
                Timber.tag(LogTags.VIEW_MODEL).d("✅ Password reset email sent successfully")
                _resetSent.value = true
            }.onFailure { throwable ->
                val userMessage = ErrorConstantsHelper.getErrorMessage(throwable)
                Timber.tag(LogTags.VIEW_MODEL).e(throwable, "❌ Failed to send reset email")
                _error.value = userMessage
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}