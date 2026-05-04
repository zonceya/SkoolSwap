package com.example.skoolswap.domain.repository

import android.app.Activity
import com.example.skoolswap.domain.model.SchoolMapping
import com.example.skoolswap.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepositoryInterface {
    suspend fun signInWithGoogle(activity: Activity): Result<User>
    suspend fun signOut()
    suspend fun updateMobile(mobile: String): Result<Boolean>
    suspend fun refreshUserProfile(): Result<User?>
    suspend fun deleteProfile(): Result<Boolean>
     fun clearError()
    fun checkCurrentUser()
    // For server user - renamed to avoid conflict
    fun getServerUser(): StateFlow<User?>
    fun getAuthToken(): StateFlow<String?>
    suspend fun getUserById(userId: Long): Result<User>
    // For Firebase compatibility - properties
    val currentUser: StateFlow<FirebaseUser?>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>
}