package com.example.skoolswap.data.repository

import android.app.Activity
import android.util.Log
import android.util.Log.*
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.ErrorConstants
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.skoolswap.data.remote.models.response.user.FirebaseAuthResponse
import kotlinx.coroutines.delay


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
    }

    private val userDao = database.userDao()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Firebase user flow (property)
    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    override val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    // Server user flow (renamed to avoid conflict)
    private val _serverUser = MutableStateFlow<User?>(null)
    override fun getServerUser() = _serverUser.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    override fun getAuthToken() = _authToken.asStateFlow()

    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            _loading.value = true
            _error.value = null

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")

            val idToken = firebaseUser.getIdToken(false).await().token
                ?: throw Exception("Failed to get ID token")

            // ✅ Use retry
            val user = syncWithRailsApiWithRetry(idToken, firebaseUser, email)

            cacheUserAfterFirebaseAuth(user, user.token)
            Result.success(user)

        } catch (e: Exception) {
            _error.value = e.message ?: "Sign in failed"
            Result.failure(e)
        } finally {
            _loading.value = false
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

            // ✅ Use retry
            val user = syncWithRailsApiWithRetry(idToken, firebaseUser, email, name)

            cacheUserAfterFirebaseAuth(user, user.token)
            Result.success(user)

        } catch (e: Exception) {
            _error.value = e.message ?: "Sign up failed"
            Result.failure(e)
        } finally {
            _loading.value = false
        }
    }


    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            _loading.value = true
            _error.value = null

            firebaseAuth.sendPasswordResetEmail(email).await()

            Timber.tag(TAG).i("📧 Password reset email sent to: $email")
            Result.success(Unit)

        } catch (e: FirebaseAuthInvalidUserException) {
            val error = "No account found with this email"
            _error.value = error
            Result.failure(Exception(error))
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to send reset email"
            Result.failure(e)
        } finally {
            _loading.value = false
        }
    }

    init {
        // Load cached user on initialization
        coroutineScope.launch {
            loadCachedUser()
        }
    }
    private suspend fun syncWithRailsApi(
        idToken: String,
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        email: String,
        name: String? = null
    ): User {
        // ✅ Determine auth mode from Firebase user
        val authMode = when {
            firebaseUser.providerData.any { it.providerId == "google.com" } -> "google"
            firebaseUser.providerData.any { it.providerId == "password" } -> "email_password"
            firebaseUser.providerData.any { it.providerId == "phone" } -> "phone"
            else -> "firebase"
        }

        // Call your Rails firebase_auth endpoint with auth_mode
        val requestBody = mapOf(
            "id_token" to idToken,
            "email" to email.lowercase(),
            "name" to (name ?: firebaseUser.displayName ?: email.split("@")[0]),
            "profile_picture_url" to (firebaseUser.photoUrl?.toString() ?: ""),
            "auth_mode" to authMode  // ✅ PASS AUTH MODE
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
        maxRetries: Int = 3,
        baseDelayMs: Long = 1000
    ): User {
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                Log.e(TAG, "🔄 Sync attempt $attempt/$maxRetries")

                val result = syncWithRailsApi(idToken, firebaseUser, email, name)
                Log.e(TAG, "✅ Sync successful on attempt $attempt")
                return result

            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "⏰ Timeout on attempt $attempt")
                lastException = e
                if (attempt < maxRetries) {
                    val delay = baseDelayMs * (1L shl (attempt - 1))
                    delay(delay)
                }
            } catch (e: java.io.IOException) {
                Log.e(TAG, "🌐 Network error on attempt $attempt")
                lastException = e
                if (attempt < maxRetries) {
                    val delay = baseDelayMs * (1L shl (attempt - 1))
                    delay(delay)
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() in 500..599 && attempt < maxRetries) {
                    Log.e(TAG, "⚠️ Server error ${e.code()} on attempt $attempt, retrying...")
                    val delay = baseDelayMs * (1L shl (attempt - 1))
                    delay(delay)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw e  // Non-retryable
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
                appPreferences.setSchoolInfo(user.schoolId, user.schoolName ?: "")
            } else {
                appPreferences.setSchoolMapped(false)
            }

            Log.i(TAG, "✅ User cached after Firebase auth: ${user.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user after Firebase auth", e)
        }
    }
    // AuthRepository.kt - add this method
    override suspend fun updateUserInRoom(user: UserEntity) {
        try {
            userDao.insertUser(user)
            Timber.tag(TAG).d("✅ Updated user in Room: ${user.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update user in Room")
        }
    }
    private suspend fun loadCachedUser() {
        try {
            val cachedUser = userDao.getCurrentUser()
            cachedUser?.let {
                _serverUser.value = it.toDomain()
                _authToken.value = it.token
                Log.e("DEBUG", "User.schoolMapped: ${cachedUser?.schoolMapped}")
                Log.e("DEBUG", "User.schoolName: ${cachedUser?.schoolName}")

            }
        } catch (e: Exception) {
            e(TAG, "Error loading cached user", e)
        }
    }

    private suspend fun rebuildUserFromPreferences(token: String) {
        val userId = appPreferences.getUserId() ?: 0
        val userName = appPreferences.userName.first() ?: ""
        val userEmail = appPreferences.userEmail.first() ?: ""
        val userProfileImage = appPreferences.userProfileImage.first()
        val schoolMapped = appPreferences.hasSchoolMapped()
        val schoolId = if (schoolMapped) appPreferences.schoolId.first() else null
        val schoolName = if (schoolMapped) appPreferences.schoolName.first() else null

        val restoredUser = User(
            id = userId,
            name = userName,
            email = userEmail,
            mobile = null,
            username = userName,
            profilePictureUrl = userProfileImage,
            authMode = "firebase",
            role = "user",
            token = token,
            createdAt = "",
            updatedAt = "",
            schoolMapped = schoolMapped,
            schoolId = schoolId,
            schoolName = schoolName
        )

        _serverUser.value = restoredUser
        userDao.insertUser(restoredUser.toEntity())
    }
    private suspend fun attemptSessionRecovery(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false

        try {
            Log.e(TAG, "🔄 Attempting session recovery...")
            val idToken = firebaseUser.getIdToken(false).await().token ?: return false

            val user = syncWithRailsApiWithRetry(
                idToken = idToken,
                firebaseUser = firebaseUser,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName,
                maxRetries = 2
            )

            cacheUserAfterFirebaseAuth(user, user.token)
            Log.e(TAG, "✅ Session recovery successful")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Session recovery failed: ${e.message}")
            return false
        }
    }
    override suspend fun getRoomUser(): UserEntity? {
        return try {
            userDao.getCurrentUser()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get user from Room")
            null
        }
    }

    // AuthRepository.kt

    override suspend fun restoreSessionFromRoom(userEntity: UserEntity): Boolean {
        return try {
            Timber.tag(TAG).d("📱 Restoring session from Room for: ${userEntity.name}")

            val user = userEntity.toDomain()
            _serverUser.value = user
            _authToken.value = user.token

            // Update Preferences
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
                appPreferences.setSchoolInfo(user.schoolId, user.schoolName ?: "")
            } else {
                appPreferences.setSchoolMapped(false)
            }

            Timber.tag(TAG).d("✅ Session restored from Room for: ${user.name}")
            return true

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to restore session from Room")
            false
        }
    }
    // AuthRepository.kt - REPLACE signInWithGoogle

    override suspend fun signInWithGoogle(activity: Activity): Result<User> {
        return try {
            _loading.value = true
            _error.value = null
            logNetworkStatus()

            // 1. Get Google ID token
            val googleIdToken = getGoogleIdToken(activity)

            // 2. Authenticate with Firebase
            val firebaseUser = authenticateWithFirebase(googleIdToken)
            _currentUser.value = firebaseUser

            // 3. Get Firebase ID token
            val idToken = firebaseUser.getIdToken(false).await().token
                ?: throw Exception("Failed to get Firebase ID token")

            // 4. ✅ Use UNIFIED firebase_auth endpoint (like email sign-in)
            val user = syncWithRailsApi(
                idToken = idToken,
                firebaseUser = firebaseUser,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName
            )

            // 5. Cache the user
            cacheUserAfterFirebaseAuth(user, user.token)

            Log.e(TAG, "✅ Google Sign-In successful for: ${user.email}")
            Result.success(user)

        } catch (e: Exception) {
            _error.value = "Sign-in failed: ${e.localizedMessage}"
            Log.e(TAG, "Google sign-in failed", e)
            Result.failure(e)
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

            // Case 1: We have both Firebase user and token
            if (firebaseUser != null && !token.isNullOrEmpty()) {
                val isValid = validateToken(token)
                if (isValid) {
                    _authToken.value = token
                    loadCachedUser()
                    Timber.tag(TAG).i("✅ Session restored successfully (valid token)")
                    return true
                }
            }

            // Case 2: Try recovery only if we have Firebase user but no valid token
            if (firebaseUser != null) {
                Timber.tag(TAG).i("🔄 Token invalid or missing → attempting recovery")
                return attemptSessionRecovery()
            }

            Timber.tag(TAG).i("❌ No valid session found")
            false
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Session restore failed")
            false
        }
    }

    override suspend fun getCurrentToken(): String? {
        return _authToken.value ?: appPreferences.authToken.first()
    }

    override suspend fun getCurrentUserId(): Int? {
        return try {
            // Try from memory first
            _serverUser.value?.id ?: run {
                // Fallback to Room
                val roomUser = userDao.getCurrentUser()
                roomUser?.id
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get current user ID")
            null
        }
    }

    override suspend fun refreshToken(): Result<String> {
        return try {
            val currentToken = _authToken.value ?: appPreferences.getAuthTokenSync()
            ?: return Result.failure(Exception("No token available"))

            val response = userApiService.refreshToken("Bearer $currentToken")

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && !body.token.isNullOrEmpty()) {
                    val newToken = body.token!!

                    _authToken.value = newToken
                    appPreferences.setAuthToken(newToken)

                    _serverUser.value?.let { user ->
                        val updated = user.copy(token = newToken)
                        _serverUser.value = updated
                        userDao.insertUser(updated.toEntity())
                    }

                    Result.success(newToken)
                } else {
                    Result.failure(Exception(body?.message ?: "Refresh failed"))
                }
            } else {
                if (response.code() == 401) {
                    Timber.tag(TAG).w("Refresh failed with 401 → forcing logout")
                    signOut()   // This is important
                }
                Result.failure(Exception("Refresh failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Refresh exception")
            Result.failure(e)
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
    private suspend fun handleSignInResponse(
        response: Response<SignInResponse>,
        firebaseUser: com.google.firebase.auth.FirebaseUser
    ): Result<User> {
        return if (response.isSuccessful) {
            val signInResponse = response.body()
            if (signInResponse?.success == true) {
                val domainUser = signInResponse.user.toDomain(signInResponse.token)

                // 🔥 STEP 1: Set token FIRST before anything else
                _authToken.value = signInResponse.token
                appPreferences.setAuthToken(signInResponse.token)

                // 🔥 STEP 2: Then cache user
                cacheUser(domainUser, firebaseUser)

                // 🔥 STEP 3: Then set app state
                appPreferences.setLoggedIn(true)
                appPreferences.setFirstTimeLogin(false)

                Result.success(domainUser)
            } else {
                val errorMsg = signInResponse?.message ?: "Backend sign-in failed"
                _error.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } else {
            // Handle specific HTTP error codes with user-friendly messages
            val errorMsg = when (response.code()) {
                530 -> "Service temporarily unavailable. Please try again later."
                500, 502, 503, 504 -> "Server is currently busy. Please try again in a few moments."
                401 -> "Authentication failed. Please try again."
                403 -> "Access denied. Please contact support."
                404 -> "Service not found. Please update the app."
                408, 504 -> "Request timed out. Please check your connection and try again."
                429 -> "Too many attempts. Please wait a moment before trying again."
                in 400..499 -> "Something went wrong. Please try again."
                in 500..599 -> "Server error. Our team has been notified. Please try again later."
                else -> "Unable to connect to server. Please check your internet connection."
            }

            // Log the actual error for debugging
            Timber.tag(TAG).e(
                "Sign in failed with code: ${response.code()}, error body: ${
                    response.errorBody()?.string()
                }"
            )

            _error.value = errorMsg
            Result.failure(Exception(errorMsg))
        }
    }
    // In AuthRepository.kt - update cacheUser method
    private suspend fun cacheUser(user: User, firebaseUser: com.google.firebase.auth.FirebaseUser) {
        try {
            val userEntity = user.toEntity()
            userDao.insertUser(userEntity)

            _serverUser.value = user
            _authToken.value = user.token

            // If user has school, cache it immediately

            if (user.schoolMapped && user.schoolId != null) {
                val userId = user.id
                val existing = userSchoolDao.getCurrentForUserSync(userId)
                if (existing == null){
                    val tempEntity = UserSchoolEntity(
                        id = "temp_${userId}", // Temporary ID
                        userId = userId,
                        schoolId = user.schoolId,
                        schoolName = user.schoolName ?: "",
                        mappedAt = null,
                        updatedAt = null
                    )
                    userSchoolDao.insert(tempEntity)
                    i(TAG, "✅ Cached school in database: ${user.schoolName}")

                    // Trigger background refresh to get real mapping_id
                    CoroutineScope(Dispatchers.IO).launch {
                        refreshSchoolMapping(userId)
                    }
                }

                // You'll need userSchoolDao here - inject it in AuthRepository
                // userSchoolDao.insert(tempEntity)
                i(TAG, "Cached school from sign-in: ${user.schoolName}")
            }

            i(TAG, "User data cached successfully: ${user.name}")
        } catch (e: Exception) {
            e(TAG, "Error caching user data", e)
        }
    }
    // In AuthRepository.kt
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
    private suspend fun refreshSchoolMapping(userId: Int) {
        try {
            val token = _authToken.value ?: return
            val response = userSchoolApiService.getCurrentSchool("Bearer $token")

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.school_mapped == true && body.school != null) {
                    val school = body.school

                    // Update with real data
                    val entity = UserSchoolEntity(
                        id = school.mapping_id,
                        userId = userId,
                        schoolId = school.id,
                        schoolName = school.name,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )
                    userSchoolDao.insert(entity)

                    i(TAG, "🔄 Refreshed school mapping: ${school.mapping_id}")
                }
            }
        } catch (e: Exception) {
            e(TAG, "Background refresh failed", e)
        }
    }

    override suspend fun updateMobile(mobile: String): Result<Boolean> {
        return try {
            _loading.value = true
            _error.value = null

            // Validate mobile number
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

            // Format mobile for backend
            val formattedMobile = MobileValidator.formatToInternational(mobile)
            val request = UpdateMobileRequest(mobile = formattedMobile)
            val response = userApiService.updateMobile("Bearer $token", request)

            if (response.isSuccessful) {
                val updateResponse = response.body()

                // Update local cache immediately to prevent UI flashing
                val currentUser = _serverUser.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(mobile = formattedMobile)
                    _serverUser.value = updatedUser
                    userDao.insertUser(updatedUser.toEntity())
                    i(TAG, "Mobile updated locally")
                }

                // Optionally refresh from server if needed
                if (updateResponse?.user != null) {
                    // Update with server response (contains full user data)
                    val updatedUser = updateResponse.user.toDomain(token)
                    _serverUser.value = updatedUser
                    userDao.insertUser(updatedUser.toEntity())
                    i(TAG, "Mobile updated with server response")
                }

                Result.success(true)
            } else {
                val errorMsg = "Failed to update mobile: ${response.code()}"
                _error.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            _error.value = "Update mobile failed: ${e.localizedMessage}"
            e(TAG, "Update mobile failed", e)
            Result.failure(e)
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

                    // Update local cache
                    _serverUser.value = updatedUser
                    userDao.insertUser(updatedUser.toEntity())
                    appPreferences.setSchoolMapped(updatedUser.schoolMapped)
                    if (updatedUser.schoolMapped && updatedUser.schoolId != null) {
                        appPreferences.setSchoolInfo(updatedUser.schoolId, updatedUser.schoolName ?: "")
                    }
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("Empty profile response"))
                }
            } else {
                Result.failure(Exception("Failed to get profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // IMPORTANT: Always set loading to false
            _loading.value = false
        }
    }
    // In AuthRepository.kt - update signOut method
    override suspend fun signOut() {
        try {
            // Firebase sign out
            firebaseAuth.signOut()

            // Clear all local data
            clearUserData()

            // Clear database
            userDao.clearAllUsers()

            // Clear preferences including token cache
            appPreferences.setLoggedIn(false)
            appPreferences.clearUserData()
            appPreferences.clearAuthToken()

            // Clear memory state
            _authToken.value = null
            _serverUser.value = null

            Timber.tag(TAG).i("User signed out successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during sign out")
            // Even if there's an error, we should continue with clearing
            _authToken.value = null
            _serverUser.value = null
            appPreferences.clearAuthToken()
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

        // Retry up to 3 times for transient failures
        var lastException: GetCredentialException? = null
        repeat(3) { attempt ->
            try {
                val response = credentialManager.getCredential(activity, request)
                return parseGoogleIdToken(response)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // User cancelled — don't retry
                handleCredentialException(e)
            } catch (e: GetCredentialException) {
                lastException = e
                Log.w(TAG, "Credential attempt ${attempt + 1} failed: ${e.javaClass.simpleName}")
                if (attempt < 2) {
                    // Wait before retrying: 500ms, then 1500ms
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            }
        }

        handleCredentialException(lastException!!)
    }

    private fun handleCredentialException(e: GetCredentialException): Nothing {
        Log.e(TAG, "Credential exception: ${e.javaClass.simpleName} - ${e.message}")

        when (e) {
            is androidx.credentials.exceptions.NoCredentialException -> {
                if (!isNetworkAvailable()) {
                    throw IllegalStateException(ErrorConstants.Auth.NO_INTERNET)
                }
                // Network is available but credential failed — likely a transient
                // Credential Manager issue, NOT necessarily missing accounts
                throw IllegalStateException(ErrorConstants.Auth.GOOGLE_SIGN_IN_FAILED.let {
                    "Sign-in failed. Please try again."
                })
            }
            is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                throw IllegalStateException(ErrorConstants.Auth.SIGN_IN_CANCELLED)
            }
            else -> {
                val errorMessage = ErrorConstants.format(
                    ErrorConstants.Auth.GOOGLE_SIGN_IN_FAILED,
                    e.message ?: "Unknown error"
                )
                throw IllegalStateException(errorMessage)
            }
        }
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
            d(TAG, "Network is available. Type: $networkType")

            val isStable = NetworkUtils.isNetworkStable(context)
            d(TAG, "Network stability: ${if (isStable) "Stable" else "Unstable"}")
        } else {
            w(TAG, "Network is NOT available")
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

            d(TAG, "Calling delete profile API...")
            val response = userApiService.deleteProfile("Bearer $token")

            d(TAG, "Delete profile response code: ${response.code()}")

            if (response.isSuccessful) {
                val deleteResponse: DeleteProfileResponse? = response.body()

                if (deleteResponse != null) {
                    i(TAG, "Profile disabled successfully: ${deleteResponse.message}")

                    // Clear all user data and sign out
                    signOut()

                    Result.success(true)
                } else {
                    e(TAG, "Delete profile response body is null")
                    _error.value = "Server returned empty response"
                    Result.failure(Exception("Server returned empty response"))
                }
            } else {
                // Try to parse the error response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        // Try to parse as DeleteProfileResponse
                        val gson = com.google.gson.Gson()
                        val errorResponse = gson.fromJson(errorBody, DeleteProfileResponse::class.java)
                        errorResponse.message ?: "Delete failed with code: ${response.code()}"
                    } else {
                        "Delete failed with code: ${response.code()}"
                    }
                } catch (e: Exception) {
                    "Delete failed with code: ${response.code()}"
                }

                e(TAG, "Delete profile failed: $errorMessage")
                _error.value = errorMessage
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMsg = "Delete profile failed: ${e.message}"
            e(TAG, errorMsg, e)
            _error.value = errorMsg
            Result.failure(e)
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
                    appPreferences.setSchoolInfo(schoolId, user.schoolName ?: "")
                }
            } else {
                appPreferences.setSchoolMapped(false)
            }

            Timber.tag(TAG).i("✅ User cached after OTP verification: ${user.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error caching user after OTP")
        }
    }

}