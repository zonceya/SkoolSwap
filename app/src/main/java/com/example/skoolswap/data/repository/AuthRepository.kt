package com.example.skoolswap.data.repository

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.skoolswap.R
import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.UserApiService
import com.example.skoolswap.data.remote.models.request.SignInRequest
import com.example.skoolswap.data.remote.models.response.SignInResponse
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
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

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val userApiService: UserApiService,
    private val database: SkoolSwapDatabase,
    private val credentialManager: CredentialManager,
    private val firebaseAuth: FirebaseAuth
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

    init {
        // Load cached user on initialization
        coroutineScope.launch {
            loadCachedUser()
        }
    }

    private suspend fun loadCachedUser() {
        try {
            val cachedUser = userDao.getCurrentUser()
            cachedUser?.let {
                _serverUser.value = it.toDomain()
                _authToken.value = it.token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached user", e)
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): Result<User> {
        return try {
            _loading.value = true
            _error.value = null

            // 1. Get Google ID token
            val googleIdToken = getGoogleIdToken(activity)

            // 2. Authenticate with Firebase
            val firebaseUser = authenticateWithFirebase(googleIdToken)

            // Update Firebase flow
            _currentUser.value = firebaseUser

            // 3. Send to backend API
            val signInRequest = SignInRequest(
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "User",
                profilePictureUrl = firebaseUser.photoUrl?.toString() ?: "",
                authMode = "google"
            )

            val response = userApiService.signIn(signInRequest)
            handleSignInResponse(response, firebaseUser)

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

    private suspend fun handleSignInResponse(
        response: Response<SignInResponse>,
        firebaseUser: com.google.firebase.auth.FirebaseUser
    ): Result<User> {
        return if (response.isSuccessful) {
            val signInResponse = response.body()
            if (signInResponse?.success == true) {
                val domainUser = signInResponse.user.toDomain(signInResponse.token)
                cacheUser(domainUser, firebaseUser)
                Result.success(domainUser)
            } else {
                val errorMsg = signInResponse?.message ?: "Backend sign-in failed"
                _error.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } else {
            val errorMsg = "Server error: ${response.code()}"
            _error.value = errorMsg
            Result.failure(Exception(errorMsg))
        }
    }

    private suspend fun cacheUser(user: User, firebaseUser: com.google.firebase.auth.FirebaseUser) {
        try {
            val userEntity = user.toEntity()
            userDao.insertUser(userEntity)

            _serverUser.value = user
            _authToken.value = user.token

            Log.i(TAG, "User data cached successfully: ${user.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user data", e)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            clearUserData()
            userDao.clearAllUsers()
            Log.i(TAG, "User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
        }
    }

    private fun clearUserData() {
        _currentUser.value = null
        _serverUser.value = null
        _authToken.value = null
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

        return try {
            // Always use Activity - works on all API levels
            val response = credentialManager.getCredential(activity, request)
            parseGoogleIdToken(response)
        } catch (e: GetCredentialException) {
            handleCredentialException(e)
        }
    }
       private fun handleCredentialException(e: GetCredentialException): Nothing {
        Log.e(TAG, "Credential exception: ${e.javaClass.simpleName} - ${e.message}")

        when (e) {
            is androidx.credentials.exceptions.NoCredentialException -> {
                // First check network connectivity
                if (!isNetworkAvailable()) {
                    throw IllegalStateException("No internet connection. Please check your network and try again.")
                }

                // Check if there are Google accounts on device (offline check)
                val hasGoogleAccounts = checkForGoogleAccountsOffline()
                if (!hasGoogleAccounts) {
                    throw IllegalStateException("No Google accounts found on device. Please add one in Settings.")
                } else {
                    throw IllegalStateException("Google Sign-In requires internet connection. Please connect to the internet.")
                }
            }
            is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                throw IllegalStateException("Sign-in was cancelled by user")
            }
            else -> throw IllegalStateException("Google sign-in failed: ${e.message}")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(
                android.content.Context.CONNECTIVITY_SERVICE
            ) as android.net.ConnectivityManager

            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network connectivity", e)
            false
        }
    }

    // Optional: Check for Google accounts offline
    private fun checkForGoogleAccountsOffline(): Boolean {
        return try {
            // This is a basic check - won't work without network for Google accounts
            // but can check for previously cached accounts
            val sharedPrefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.getBoolean("has_google_accounts", false)
        } catch (e: Exception) {
            false
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
}