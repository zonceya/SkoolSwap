// domain/repository/AuthRepositoryInterface.kt
package com.example.skoolswap.domain.repository

import android.app.Activity
import com.example.skoolswap.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepositoryInterface {
    // Existing methods
    suspend fun signInWithGoogle(activity: Activity): Result<User>
    suspend fun signOut()
    suspend fun updateMobile(mobile: String): Result<Boolean>
    suspend fun refreshUserProfile(): Result<User?>
    suspend fun deleteProfile(): Result<Boolean>
    fun clearError()
    fun checkCurrentUser()
    fun getServerUser(): StateFlow<User?>
    fun getAuthToken(): StateFlow<String?>
    suspend fun getUserById(userId: Long): Result<User>
    suspend fun restoreSession(): Boolean
    suspend fun getCurrentToken(): String?
    suspend fun validateToken(token: String): Boolean
    val currentUser: StateFlow<FirebaseUser?>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>

    // ==================== EMAIL OTP METHODS ====================
    suspend fun sendSignUpOtp(
        email: String,
        name: String,
        password: String,
        passwordConfirmation: String
    ): Result<String>

    suspend fun sendLoginOtp(email: String): Result<String> // Returns otpToken

    suspend fun verifyOtp(
        email: String,
        otpToken: String,
        otpCode: String,
        purpose: String,
        name: String? = null
    ): Result<User> // Returns authenticated User

    suspend fun resendOtp(email: String, purpose: String): Result<String> // Returns new otpToken
}