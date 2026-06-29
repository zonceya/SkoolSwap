package com.example.skoolswap.di

import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appPreferences: AppPreferences,
    private val authRepository: dagger.Lazy<AuthRepositoryInterface>  // Lazy to prevent circular dependency
) : Interceptor {

    private companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = appPreferences.getAuthTokenSync()

        // Attach token if available
        val requestWithToken = if (!token.isNullOrBlank()) {
            Timber.tag(TAG).d("Adding Bearer token to: ${originalRequest.url.encodedPath}")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Timber.tag(TAG).w("No token available for: ${originalRequest.url.encodedPath}")
            originalRequest
        }

        var response = chain.proceed(requestWithToken)

        // Auto-refresh on 401 (except for refresh endpoint itself)
        if (response.code == 401 &&
            !originalRequest.url.encodedPath.contains("auth/refresh")) {

            Timber.tag(TAG).w("401 received → Attempting token refresh")

            response.close() // Important: close previous response

            val refreshResult = runBlocking {
                authRepository.get().refreshToken()
            }

            if (refreshResult.isSuccess) {
                val newToken = refreshResult.getOrNull()
                if (!newToken.isNullOrEmpty()) {
                    Timber.tag(TAG).i("Token refreshed successfully, retrying original request")

                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()

                    response = chain.proceed(retryRequest)
                }
            } else {
                Timber.tag(TAG).e("Token refresh failed after 401")
                // Optional: Auto logout
                // runBlocking { authRepository.get().signOut() }
            }
        }

        return response
    }
}