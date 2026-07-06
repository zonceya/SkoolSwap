package com.example.skoolswap.data.repository

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.common.constants.ErrorConstants
import com.example.skoolswap.common.constants.ErrorConstantsHelper
import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.local.database.dao.UserSchoolDao
import com.example.skoolswap.data.local.database.entities.UserEntity
import com.example.skoolswap.data.local.database.entities.UserSchoolEntity
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.UserApiService
import com.example.skoolswap.data.remote.models.request.SignInRequest
import com.example.skoolswap.data.remote.models.request.UpdateMobileRequest
import com.example.skoolswap.data.remote.models.response.user.SignInResponse
import com.example.skoolswap.data.remote.network.NetworkUtils
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.extensions.MobileValidator
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.skoolswap.data.remote.models.response.profile.DeleteProfileResponse
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.UserSchoolApiService
import com.example.skoolswap.data.remote.models.request.SignUpRequest
import com.example.skoolswap.data.remote.models.request.VerifyLoginRequest
import com.example.skoolswap.data.remote.models.request.VerifySignUpRequest
import kotlinx.coroutines.flow.first
import timber.log.Timber
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.withLock

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val userApiService: UserApiService,
    private val userSchoolApiService: UserSchoolApiService,
    private val database: SkoolSwapDatabase,
    private val credentialManager: CredentialManager,
    private val firebaseAuth: FirebaseAuth,
    private val userSchoolDao: UserSchoolDao,
    private val appPreferences: AppPreferences
) : AuthRepositoryInterface {

    private companion object {
        private const val TAG = "AuthRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val REFRESH_DEBOUNCE_MS = 10000L
        private const val GOOGLE_CREDENTIAL_RETRY_ATTEMPTS = 3
        private const val GOOGLE_CREDENTIAL_RETRY_DELAY_MS = 500L
    }

    private val refreshMutex = kotlinx.coroutines.sync.Mutex()
    private val userDao = database.userDao()
    private val signOutMutex = kotlinx.coroutines.sync.Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Firebase user flow
    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    override val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    // Server user flow
    private val _serverUser = MutableStateFlow<User?>(null)
    override fun getServerUser() = _serverUser.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    override fun getAuthToken() = _authToken.asStateFlow()

    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private var lastRefreshTime = 0L
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    init {
        coroutineScope.launch {
            loadCachedUser()
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            _loading.value = true
            _error.value = null

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")

            val idToken = firebaseUser.getIdToken(false).await().token
                ?: throw Exception("Failed to get ID token")

            val user = syncWithRailsApiWithRetry(idToken, firebaseUser, email)
            cacheUserAfterFirebaseAuth(user, user.token)
            Result.success(user)

        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            _error.value = userMessage
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    private fun isRecentlyRefreshed(): Boolean {
        return System.currentTimeMillis() - lastRefreshTime < REFRESH_DEBOUNCE_MS
    }

    private suspend fun updateTokenAcrossAllLayers(newToken: String) {
        lastRefreshTime = System.currentTimeMillis()

        _authToken.value = newToken
        appPreferences.setAuthToken(newToken)

        _serverUser.value?.let { user ->
            val updated = user.copy(token = newToken)
            _serverUser.value = updated
            userDao.insertUser(updated.toEntity())
        }
    }

    override suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): Result<User> {
        return try {
            _loading.value = true
            _error.value = null

            if (password != passwordConfirmation) throw Exception("Passwords do not match")
            if (password.length < 6) throw Exception("Password must be at least 6 characters")

            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user creation failed")

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val idToken = firebaseUser.getIdToken(false).await().token
                ?: throw Exception("Failed to get ID token")

            val user = syncWithRailsApiWithRetry(idToken, firebaseUser, email, name)
            cacheUserAfterFirebaseAuth(user, user.token)
            Result.success(user)

        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            _error.value = userMessage
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            _loading.value = true
            _error.value = null

            firebaseAuth.sendPasswordResetEmail(email).await()
            Timber.tag(LogTags.AUTH).i("📧 Password reset email sent to: $email")
            Result.success(Unit)

        } catch (e: FirebaseAuthInvalidUserException) {
            val error = "No account found with this email"
            _error.value = error
            Result.failure(Exception(error))
        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            _error.value = userMessage
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    private suspend fun syncWithRailsApi(
        idToken: String,
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        email: String,
        name: String? = null
    ): User {
        val authMode = when {
            firebaseUser.providerData.any { it.providerId == "google.com" } -> "google"
            firebaseUser.providerData.any { it.providerId == "password" } -> "email_password"
            firebaseUser.providerData.any { it.providerId == "phone" } -> "phone"
            else -> "firebase"
        }

        val requestBody = mapOf(
            "id_token" to idToken,
            "email" to email.lowercase(),
            "name" to (name ?: firebaseUser.displayName ?: email.split("@")[0]),
            "profile_picture_url" to (firebaseUser.photoUrl?.toString() ?: AppConstants.EMPTY_STRING),
            "auth_mode" to authMode
        )

        val response = userApiService.firebaseAuth(requestBody)

        if (!response.isSuccessful) {
            val errorMsg = response.errorBody()?.string() ?: "Failed to sync with server"
            throw Exception(errorMsg)
        }

        val body = response.body()
        if (body?.success != true) {
            throw Exception(body?.message ?: "Server sync failed")
        }

        return User(
            id = body.user.id,
            name = body.user.name,
            email = body.user.email,
            mobile = body.user.mobile,
            username = body.user.username,
            profilePictureUrl = body.user.profilePictureUrl,
            authMode = body.user.authMode,
            role = body.user.role,
            token = body.token,
            createdAt = body.user.createdAt,
            updatedAt = body.user.updatedAt,
            schoolMapped = body.user.schoolMapped ?: false,
            schoolId = body.user.schoolId,
            schoolName = body.user.schoolName
        )
    }

    private suspend fun syncWithRailsApiWithRetry(
        idToken: String,
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        email: String,
        name: String? = null,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        baseDelayMs: Long = BASE_RETRY_DELAY_MS
    ): User {
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                Timber.tag(LogTags.AUTH).d("🔄 Sync attempt $attempt/$maxRetries")

                val result = syncWithRailsApi(idToken, firebaseUser, email, name)
                Timber.tag(LogTags.AUTH).d("✅ Sync successful on attempt $attempt")
                return result

            } catch (e: java.net.SocketTimeoutException) {
                Timber.tag(LogTags.AUTH).w("⏰ Timeout on attempt $attempt")
                lastException = e
                if (attempt < maxRetries) {
                    val delay = baseDelayMs * (1L shl (attempt - 1))
                    delay(delay)
                }
            } catch (e: java.io.IOException) {
                Timber.tag(LogTags.AUTH).w("🌐 Network error on attempt $attempt")
                lastException = e
                if (attempt < maxRetries) {
                    val delay = baseDelayMs * (1L shl (attempt - 1))
                    delay(delay)
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() in 500..599 && attempt < maxRetries) {
                    Timber.tag(LogTags.AUTH)
                        .e("⚠️ Server error ${e.code()} on attempt $attempt, retrying...")
                    val delay = baseDelayMs * (1L shl (attempt - 1))
                    delay(delay)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw e
            }
        }

        throw lastException ?: Exception("Failed after $maxRetries attempts")
    }

    private suspend fun cacheUserAfterFirebaseAuth(user: User, token: String) {
        try {
            val userEntity = user.toEntity()
            userDao.insertUser(userEntity)

            _serverUser.value = user
            _authToken.value = token

            appPreferences.setAuthToken(token)
            appPreferences.setLoggedIn(true)
            appPreferences.setFirstTimeLogin(false)
            appPreferences.setUserId(user.id.toString())
            appPreferences.setUserName(user.name)
            appPreferences.setUserEmail(user.email)

            if (user.profilePictureUrl != null) {
                appPreferences.setUserProfileImage(user.profilePictureUrl)
            }

            if (user.schoolMapped && user.schoolId != null) {
                appPreferences.setSchoolMapped(true)
                appPreferences.setSchoolInfo(user.schoolId, user.schoolName ?: AppConstants.EMPTY_STRING)
            } else {
                appPreferences.setSchoolMapped(false)
            }

            Timber.tag(LogTags.AUTH).i("✅ User cached after Firebase auth: ${user.name}")
        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "Error caching user after Firebase auth")
        }
    }

    override suspend fun updateUserInRoom(user: UserEntity) {
        try {
            userDao.insertUser(user)
            Timber.tag(LogTags.DATABASE).d("✅ Updated user in Room: ${user.name}")
        } catch (e: Exception) {
            Timber.tag(LogTags.DATABASE).e(e, "Failed to update user in Room")
        }
    }

    private suspend fun loadCachedUser() {
        try {
            val cachedUser = userDao.getCurrentUser()
            cachedUser?.let {
                _serverUser.value = it.toDomain()
                _authToken.value = it.token
                Timber.tag(LogTags.DATABASE).d("User.schoolMapped: ${cachedUser?.schoolMapped}")
                Timber.tag(LogTags.DATABASE).d("User.schoolName: ${cachedUser?.schoolName}")
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.DATABASE).e(e, "Error loading cached user")
        }
    }

    private suspend fun attemptSessionRecovery(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false

        try {
            Timber.tag(LogTags.AUTH).d("🔄 Attempting session recovery...")
            val idToken = firebaseUser.getIdToken(false).await().token ?: return false

            val user = syncWithRailsApiWithRetry(
                idToken = idToken,
                firebaseUser = firebaseUser,
                email = firebaseUser.email ?: AppConstants.EMPTY_STRING,
                name = firebaseUser.displayName,
                maxRetries = 2
            )

            cacheUserAfterFirebaseAuth(user, user.token)
            Timber.tag(LogTags.AUTH).d("✅ Session recovery successful")
            return true

        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "❌ Session recovery failed")
            return false
        }
    }

    override suspend fun getRoomUser(): UserEntity? {
        return try {
            userDao.getCurrentUser()
        } catch (e: Exception) {
            Timber.tag(LogTags.DATABASE).e(e, "Failed to get user from Room")
            null
        }
    }

    override suspend fun restoreSessionFromRoom(userEntity: UserEntity): Boolean {
        return try {
            Timber.tag(LogTags.AUTH).d("📱 Restoring session from Room for: ${userEntity.name}")

            val user = userEntity.toDomain()
            _serverUser.value = user
            _authToken.value = user.token

            appPreferences.setAuthToken(user.token)
            appPreferences.setLoggedIn(true)
            appPreferences.setUserId(user.id.toString())
            appPreferences.setUserName(user.name)
            appPreferences.setUserEmail(user.email)

            if (user.profilePictureUrl != null) {
                appPreferences.setUserProfileImage(user.profilePictureUrl)
            }

            if (user.schoolMapped && user.schoolId != null) {
                appPreferences.setSchoolMapped(true)
                appPreferences.setSchoolInfo(user.schoolId, user.schoolName ?: AppConstants.EMPTY_STRING)
            } else {
                appPreferences.setSchoolMapped(false)
            }

            Timber.tag(LogTags.AUTH).d("✅ Session restored from Room for: ${user.name}")
            return true

        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "Failed to restore session from Room")
            false
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): Result<User> {
        return try {
            _loading.value = true
            _error.value = null
            logNetworkStatus()

            val googleIdToken = getGoogleIdToken(activity)
            val firebaseUser = authenticateWithFirebase(googleIdToken)
            _currentUser.value = firebaseUser

            val idToken = firebaseUser.getIdToken(false).await().token
                ?: throw Exception("Failed to get Firebase ID token")

            val user = syncWithRailsApi(
                idToken = idToken,
                firebaseUser = firebaseUser,
                email = firebaseUser.email ?: AppConstants.EMPTY_STRING,
                name = firebaseUser.displayName
            )

            cacheUserAfterFirebaseAuth(user, user.token)

            Timber.tag(LogTags.AUTH).d("✅ Google Sign-In successful for: ${user.email}")
            Result.success(user)

        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            _error.value = userMessage
            Timber.tag(LogTags.AUTH).e(e, "Google sign-in failed")
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    private suspend fun authenticateWithFirebase(googleIdToken: String): com.google.firebase.auth.FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        return authResult.user ?: throw IllegalStateException("Firebase user is null")
    }

    override suspend fun restoreSession(): Boolean {
        return try {
            val token = appPreferences.authToken.first()
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser != null && !token.isNullOrEmpty()) {
                val isValid = validateToken(token)
                if (isValid) {
                    _authToken.value = token
                    loadCachedUser()
                    Timber.tag(LogTags.AUTH).i("✅ Session restored successfully (valid token)")
                    return true
                }
            }

            if (firebaseUser != null) {
                Timber.tag(LogTags.AUTH).i("🔄 Token invalid or missing → attempting recovery")
                return attemptSessionRecovery()
            }

            Timber.tag(LogTags.AUTH).i("❌ No valid session found")
            false
        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "Session restore failed")
            false
        }
    }

    override suspend fun getCurrentToken(): String? {
        return _authToken.value ?: appPreferences.authToken.first()
    }

    override suspend fun getCurrentUserId(): Int? {
        return try {
            _serverUser.value?.id ?: run {
                val roomUser = userDao.getCurrentUser()
                roomUser?.id
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "Failed to get current user ID")
            null
        }
    }

    private suspend fun checkForValidSession(): Boolean {
        return try {
            val user = getServerUser().firstOrNull()
            user != null && !user.email.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refreshToken(): Result<String> {
        return refreshMutex.withLock {
            Timber.tag(LogTags.AUTH).d("🔒 Acquired refresh lock")

            val currentToken = getCurrentToken()
            if (currentToken == null) {
                Timber.tag(LogTags.AUTH).w("No token available for refresh")
                return@withLock Result.failure(Exception("No token"))
            }

            if (isRecentlyRefreshed()) {
                Timber.tag(LogTags.AUTH).d("⏭️ Token recently refreshed, using existing token")
                return@withLock Result.success(currentToken)
            }

            try {
                Timber.tag(LogTags.AUTH).d("🔄 Attempting token refresh...")
                val response = userApiService.refreshToken("Bearer $currentToken")

                if (response.isSuccessful) {
                    val body = response.body()
                    val newToken = body?.token

                    if (body?.success == true && !newToken.isNullOrEmpty()) {
                        Timber.tag(LogTags.AUTH).d("✅ Token refreshed successfully")
                        updateTokenAcrossAllLayers(newToken)
                        lastRefreshTime = System.currentTimeMillis()
                        return@withLock Result.success(newToken)
                    } else {
                        val message = body?.message ?: "Refresh failed - invalid response"
                        Timber.tag(LogTags.AUTH).w("⚠️ Refresh failed: $message")
                        return@withLock Result.failure(Exception(message))
                    }
                } else {
                    val errorMsg = "Refresh failed: ${response.code()}"
                    Timber.tag(LogTags.AUTH).w("⚠️ $errorMsg")

                    if (response.code() == 401) {
                        val hasValidSession = checkForValidSession()
                        if (!hasValidSession) {
                            Timber.tag(LogTags.AUTH).w("🔴 No valid session found, signing out")
                            signOut()
                        } else {
                            Timber.tag(LogTags.AUTH).d("ℹ️ Valid session exists elsewhere, not signing out")
                        }
                    }

                    return@withLock Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.AUTH).e(e, "❌ Exception during token refresh")
                return@withLock Result.failure(e)
            }
        }
    }

    override suspend fun validateToken(token: String): Boolean {
        return try {
            val response = userApiService.getProfile("Bearer $token")
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUserById(userId: Long): Result<User> {
        return try {
            val token = _authToken.value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = userApiService.getUserById("Bearer $token", userId)

            if (response.isSuccessful) {
                val userResponse = response.body()
                if (userResponse != null) {
                    val user = User(
                        id = userResponse.id,
                        name = userResponse.name,
                        email = userResponse.email,
                        mobile = userResponse.mobile,
                        username = userResponse.username,
                        profilePictureUrl = userResponse.profilePictureUrl,
                        authMode = userResponse.authMode,
                        role = userResponse.role,
                        token = token,
                        createdAt = userResponse.createdAt,
                        updatedAt = userResponse.updatedAt,
                        schoolMapped = userResponse.schoolMapped ?: false,
                        schoolId = userResponse.schoolId,
                        schoolName = userResponse.schoolName
                    )
                    Result.success(user)
                } else {
                    Result.failure(Exception("User not found"))
                }
            } else {
                Result.failure(Exception("Failed to get user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMobile(mobile: String): Result<Boolean> {
        return try {
            _loading.value = true
            _error.value = null

            val errorMessage = MobileValidator.getErrorMessage(mobile)
            if (errorMessage != null) {
                _error.value = errorMessage
                return Result.failure(Exception(errorMessage))
            }

            val token = _authToken.value
            if (token == null) {
                _error.value = "Not authenticated"
                return Result.failure(Exception("Not authenticated"))
            }

            val formattedMobile = MobileValidator.formatToInternational(mobile)
            val request = UpdateMobileRequest(mobile = formattedMobile)
            val response = userApiService.updateMobile("Bearer $token", request)

            if (response.isSuccessful) {
                val updateResponse = response.body()

                val currentUser = _serverUser.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(mobile = formattedMobile)
                    _serverUser.value = updatedUser
                    userDao.insertUser(updatedUser.toEntity())
                    Timber.tag(LogTags.AUTH).i("Mobile updated locally")
                }

                if (updateResponse?.user != null) {
                    val updatedUser = updateResponse.user.toDomain(token)
                    _serverUser.value = updatedUser
                    userDao.insertUser(updatedUser.toEntity())
                    Timber.tag(LogTags.AUTH).i("Mobile updated with server response")
                }

                Result.success(true)
            } else {
                val errorMsg = "Failed to update mobile: ${response.code()}"
                _error.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            _error.value = userMessage
            Timber.tag(LogTags.AUTH).e(e, "Update mobile failed")
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    override suspend fun refreshUserProfile(): Result<User?> {
        return try {
            _loading.value = true

            val token = _authToken.value
            if (token == null) {
                _loading.value = false
                return Result.failure(Exception("Not authenticated"))
            }

            val response = userApiService.getProfile("Bearer $token")

            if (response.isSuccessful) {
                val profileResponse = response.body()
                if (profileResponse != null) {
                    val updatedUser = User(
                        id = profileResponse.user.id,
                        name = profileResponse.user.name,
                        email = profileResponse.user.email,
                        mobile = profileResponse.user.mobile ?: profileResponse.profile?.mobile,
                        username = profileResponse.user.username,
                        profilePictureUrl = profileResponse.user.profilePictureUrl,
                        authMode = profileResponse.user.authMode,
                        role = profileResponse.user.role,
                        token = token,
                        createdAt = profileResponse.user.createdAt,
                        updatedAt = profileResponse.user.updatedAt,
                        schoolMapped = profileResponse.user.schoolMapped ?: false,
                        schoolId = profileResponse.user.schoolId,
                        schoolName = profileResponse.user.schoolName
                    )

                    _serverUser.value = updatedUser
                    userDao.insertUser(updatedUser.toEntity())
                    appPreferences.setSchoolMapped(updatedUser.schoolMapped)
                    if (updatedUser.schoolMapped && updatedUser.schoolId != null) {
                        appPreferences.setSchoolInfo(updatedUser.schoolId, updatedUser.schoolName ?: AppConstants.EMPTY_STRING)
                    }
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("Empty profile response"))
                }
            } else {
                Result.failure(Exception("Failed to get profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    override suspend fun signOut() {
        if (!signOutMutex.tryLock()) {
            Timber.tag(LogTags.AUTH).d("Sign-out already in progress")
            return
        }

        try {
            firebaseAuth.signOut()
            clearUserData()

            userDao.clearAllUsers()
            appPreferences.setLoggedIn(false)
            appPreferences.clearUserData()
            appPreferences.clearAuthToken()

            _authToken.value = null
            _serverUser.value = null

            Timber.tag(LogTags.AUTH).i("User signed out successfully")
        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "Error during sign out")
        } finally {
            signOutMutex.unlock()
        }
    }

    private fun clearUserData() {
        _currentUser.value = null
        _serverUser.value = null
        _authToken.value = null
        _loading.value = false
        _error.value = null
    }

    override fun clearError() {
        _error.value = null
    }

    override fun checkCurrentUser() {
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        coroutineScope.launch {
            loadCachedUser()
        }
    }

    private suspend fun getGoogleIdToken(activity: Activity): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        var lastException: GetCredentialException? = null
        repeat(GOOGLE_CREDENTIAL_RETRY_ATTEMPTS) { attempt ->
            try {
                val response = credentialManager.getCredential(activity, request)
                return parseGoogleIdToken(response)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                handleCredentialException(e)
            } catch (e: GetCredentialException) {
                lastException = e
                Timber.tag(LogTags.AUTH)
                    .w("Credential attempt ${attempt + 1} failed: ${e.javaClass.simpleName}")
                if (attempt < GOOGLE_CREDENTIAL_RETRY_ATTEMPTS - 1) {
                    kotlinx.coroutines.delay(GOOGLE_CREDENTIAL_RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        handleCredentialException(lastException!!)
    }

    private fun handleCredentialException(e: GetCredentialException): Nothing {
        Timber.tag(LogTags.AUTH).e("Credential exception: ${e.javaClass.simpleName} - ${e.message}")

        val errorMessage = when (e) {
            is androidx.credentials.exceptions.NoCredentialException -> {
                if (!isNetworkAvailable()) {
                    ErrorConstants.Messages.UserFriendly.NO_INTERNET
                } else {
                    ErrorConstants.Messages.UserFriendly.UNKNOWN
                }
            }
            is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                ErrorConstants.Messages.UserFriendly.SIGN_IN_CANCELLED
            }
            else -> {
                ErrorConstants.Messages.UserFriendly.UNKNOWN
            }
        }

        throw IllegalStateException(errorMessage)
    }

    private fun parseGoogleIdToken(response: GetCredentialResponse): String {
        val credential = response.credential
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } catch (e: GoogleIdTokenParsingException) {
                        throw IllegalStateException("Invalid Google ID token response")
                    }
                } else {
                    throw IllegalStateException("Unexpected credential type")
                }
            }
            else -> throw IllegalStateException("Unexpected credential type")
        }
    }

    private fun generateNonce(): String {
        return UUID.randomUUID().toString()
    }

    private fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(context)
    }

    private fun logNetworkStatus() {
        if (isNetworkAvailable()) {
            val networkType = NetworkUtils.getNetworkType(context)
            Timber.tag(LogTags.NETWORK).d("Network is available. Type: $networkType")
        } else {
            Timber.tag(LogTags.NETWORK).w("Network is NOT available")
        }
    }

    override suspend fun deleteProfile(): Result<Boolean> {
        return try {
            _loading.value = true
            _error.value = null

            val token = _authToken.value
            if (token == null) {
                _error.value = "Not authenticated"
                return Result.failure(Exception("Not authenticated"))
            }

            Timber.tag(LogTags.AUTH).d("Calling delete profile API...")
            val response = userApiService.deleteProfile("Bearer $token")

            Timber.tag(LogTags.AUTH).d("Delete profile response code: ${response.code()}")

            if (response.isSuccessful) {
                val deleteResponse: DeleteProfileResponse? = response.body()

                if (deleteResponse != null) {
                    Timber.tag(LogTags.AUTH).i("Profile disabled successfully: ${deleteResponse.message}")
                    signOut()
                    Result.success(true)
                } else {
                    Timber.tag(LogTags.AUTH).e("Delete profile response body is null")
                    _error.value = "Server returned empty response"
                    Result.failure(Exception("Server returned empty response"))
                }
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val gson = com.google.gson.Gson()
                        val errorResponse = gson.fromJson(errorBody, DeleteProfileResponse::class.java)
                        errorResponse.message ?: "Delete failed with code: ${response.code()}"
                    } else {
                        "Delete failed with code: ${response.code()}"
                    }
                } catch (e: Exception) {
                    "Delete failed with code: ${response.code()}"
                }

                Timber.tag(LogTags.AUTH).e("Delete profile failed: $errorMessage")
                _error.value = errorMessage
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val userMessage = ErrorConstantsHelper.getErrorMessage(e)
            Timber.tag(LogTags.AUTH).e(e, "Delete profile failed")
            _error.value = userMessage
            Result.failure(Exception(userMessage))
        } finally {
            _loading.value = false
        }
    }

    private suspend fun cacheUserAfterOtp(user: User, token: String) {
        try {
            val userEntity = user.toEntity()
            userDao.insertUser(userEntity)

            _serverUser.value = user
            _authToken.value = token
            appPreferences.setAuthToken(token)
            appPreferences.setLoggedIn(true)
            appPreferences.setFirstTimeLogin(false)
            appPreferences.setUserId(user.id.toString())
            appPreferences.setUserName(user.name)
            appPreferences.setUserEmail(user.email)

            if (user.profilePictureUrl != null) {
                appPreferences.setUserProfileImage(user.profilePictureUrl)
            }

            if (user.schoolMapped && user.schoolId != null) {
                appPreferences.setSchoolMapped(true)
                user.schoolId?.let { schoolId ->
                    appPreferences.setSchoolInfo(schoolId, user.schoolName ?: AppConstants.EMPTY_STRING)
                }
            } else {
                appPreferences.setSchoolMapped(false)
            }

            Timber.tag(LogTags.AUTH).i("✅ User cached after OTP verification: ${user.name}")
        } catch (e: Exception) {
            Timber.tag(LogTags.AUTH).e(e, "Error caching user after OTP")
        }
    }
}