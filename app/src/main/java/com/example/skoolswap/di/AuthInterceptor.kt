package com.example.skoolswap.di

import android.util.Log
import com.example.skoolswap.data.local.datastore.AppPreferences
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
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

        Timber.tag(TAG).d("🔍 Intercepting: $path")

        // Synchronous, non-blocking token access (uses cache)
        val token = appPreferences.getAuthTokenSync()

        // Build request with or without token
        val request = if (!token.isNullOrBlank()) {
            Timber.tag(TAG).d("✅ Adding Bearer token to: $path (token: ${token.take(10)}...)")
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