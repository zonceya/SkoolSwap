package com.example.skoolswap.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.skoolswap.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseUser



class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

    private val _userProfileImage = MutableStateFlow<String?>(null)
    val userProfileImage: StateFlow<String?> = _userProfileImage
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun signInWithGoogle(): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            _loading.value = true
            _error.value = null

            // Step 1: Get Google ID token using Credential Manager
            val googleIdToken = getGoogleIdToken()

            // Step 2: Authenticate with Firebase
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(credential).await()

            val user = authResult.user
            if (user != null) {
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Google sign-in failed: User is null"))
            }
        } catch (e: Exception) {
            _error.value = "Sign-in failed: ${e.message}"
            Result.failure(e)
        } finally {
            _loading.value = false
        }
    }

    private suspend fun getGoogleIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val response = credentialManager.getCredential(
                request = request,
                context = context
            )
            return parseGoogleIdToken(response)
        } catch (e: GetCredentialException) {
            throw Exception("Failed to get Google credential: ${e.message}")
        }
    }

    private fun parseGoogleIdToken(response: GetCredentialResponse): String {
        val credential = response.credential

        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        googleIdTokenCredential.idToken
                    } catch (e: GoogleIdTokenParsingException) {
                        throw Exception("Invalid Google ID token response")
                    }
                } else {
                    throw Exception("Unexpected credential type")
                }
            }
            else -> throw Exception("Unexpected credential type")
        }
    }
    private fun fetchUserProfile(user: FirebaseUser) {
        val name = user.displayName ?: "User"
        val email = user.email ?: ""
        val photoUrl = user.photoUrl?.toString()

        _userName.value = name
        _userEmail.value = email // Set the email
        _userProfileImage.value = photoUrl

        println("User Profile - Name: $name, Email: $email, Photo URL: $photoUrl")
    }
    fun clearError() {
        _error.value = null
    }
    private fun generateNonce(): String {
        // Generate a secure random nonce for additional security
        return java.util.UUID.randomUUID().toString()
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _userName.value = null
        _userEmail.value = null // Clear email on sign out
        _userProfileImage.value = null
    }

    fun checkCurrentUser() {
        val user = auth.currentUser
        _currentUser.value = user
        user?.let { fetchUserProfile(it) }
    }

}