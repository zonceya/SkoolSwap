package com.example.skoolswap.di

import android.util.Log
import com.example.skoolswap.data.local.datastore.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appPreferences: AppPreferences
) : Interceptor {

    private companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        Log.d(TAG, "🔍 Intercepting: $path")

        // Get token from preferences
        val token = runBlocking {
            try {
                val tokenValue = appPreferences.authToken.first()
                Log.d(TAG, "📦 Token from preferences: ${if (tokenValue != null) "Present (${tokenValue.take(10)}...)" else "NULL"}")
                tokenValue
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting auth token", e)
                null
            }
        }

        // Build request with or without token
        val request = if (!token.isNullOrBlank()) {
            Log.d(TAG, "✅ Adding Bearer token to: $path")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.w(TAG, "⚠️ No token available for: $path")
            originalRequest
        }

        return chain.proceed(request)
    }
}