package za.co.skoolswap.ui.navigationheader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.common.constants.ErrorConstantsHelper
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NavigationHeaderViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        observeUserChanges()
    }

    private fun observeUserChanges() {
        viewModelScope.launch {
            // Load from DataStore (immediate display)
            val storedName = appPreferences.userName.first()
            val storedEmail = appPreferences.userEmail.first()
            val storedImageUrl = appPreferences.userProfileImage.first()

            if (storedName != null) {
                _userState.value = UserState.Success(
                    name = storedName,
                    email = storedEmail ?: AppConstants.EMPTY_STRING,
                    profileImageUrl = storedImageUrl
                )
                Timber.tag(LogTags.UI).d("📱 Loaded user from DataStore: $storedName")
            }

            authRepository.getServerUser().collect { user ->
                if (user != null) {
                    // Update DataStore with latest
                    appPreferences.setUserName(user.name)
                    appPreferences.setUserEmail(user.email)
                    appPreferences.setUserProfileImage(user.profilePictureUrl ?: AppConstants.EMPTY_STRING)

                    _userState.value = UserState.Success(
                        name = user.name,
                        email = user.email,
                        profileImageUrl = user.profilePictureUrl.takeIf { !it.isNullOrEmpty() }
                    )
                    Timber.tag(LogTags.UI).d("✅ Updated user from server: ${user.name}")
                } else if (storedName == null) {
                    // Only show default if no stored data and no user
                    _userState.value = UserState.Success(
                        name = context.getString(R.string.nav_header_welcome),
                        email = context.getString(R.string.nav_header_sign_in_to_continue),
                        profileImageUrl = null
                    )
                    Timber.tag(LogTags.UI).d("ℹ️ No user data, showing default")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            Timber.tag(LogTags.UI).d("🔄 Refreshing user data...")

            try {
                val result = authRepository.refreshUserProfile()

                result.onSuccess { user ->
                    if (user != null) {
                        // Update DataStore with fresh data
                        appPreferences.setUserName(user.name)
                        appPreferences.setUserEmail(user.email)
                        appPreferences.setUserProfileImage(user.profilePictureUrl ?: AppConstants.EMPTY_STRING)

                        _userState.value = UserState.Success(
                            name = user.name,
                            email = user.email,
                            profileImageUrl = user.profilePictureUrl.takeIf { !it.isNullOrEmpty() }
                        )
                        Timber.tag(LogTags.UI).d("✅ Refresh successful: ${user.name}")
                    } else {
                        Timber.tag(LogTags.UI).w("⚠️ Refresh returned null user")
                        _userState.value = UserState.Error(
                            context.getString(R.string.nav_header_error_no_user_data)
                        )
                    }
                }.onFailure { throwable ->
                    val userMessage = ErrorConstantsHelper.getErrorMessage(throwable)
                    Timber.tag(LogTags.UI).e(throwable, "❌ Refresh failed")

                    // On failure, try to use stored data
                    val storedName = appPreferences.userName.first()
                    val storedEmail = appPreferences.userEmail.first()
                    val storedImageUrl = appPreferences.userProfileImage.first()

                    if (storedName != null) {
                        _userState.value = UserState.Success(
                            name = storedName,
                            email = storedEmail ?: AppConstants.EMPTY_STRING,
                            profileImageUrl = storedImageUrl
                        )
                        Timber.tag(LogTags.UI).d("📱 Fallback to stored data: $storedName")
                    } else {
                        _userState.value = UserState.Error(userMessage)
                    }
                }
            } catch (e: Exception) {
                val userMessage = ErrorConstantsHelper.getErrorMessage(e)
                Timber.tag(LogTags.UI).e(e, "❌ Refresh exception")
                _userState.value = UserState.Error(userMessage)
            }
        }
    }
}