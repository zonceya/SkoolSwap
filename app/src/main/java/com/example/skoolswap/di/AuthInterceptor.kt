package com.example.skoolswap.di

import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appPreferences: AppPreferences,
    private val authRepository: dagger.Lazy<AuthRepositoryInterface>
) : Interceptor {

    private val refreshLock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = appPreferences.getAuthTokenSync()

        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(request)

        // Handle 401 + refresh
        if (response.code == 401 &&
            !originalRequest.url.encodedPath.contains("refresh")) {

            response.close() // Safe to close failed response

            val newToken = performTokenRefresh(token)

            if (!newToken.isNullOrEmpty()) {
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                response = chain.proceed(retryRequest)
            } else {
                // Force logout on refresh failure
                runBlocking { authRepository.get().signOut() }
            }
        }

        return response
    }

    private fun performTokenRefresh(oldToken: String?): String? {
        if (!refreshLock.tryLock()) {
            // Another thread is refreshing
            refreshLock.lock()
            try {
                return appPreferences.getAuthTokenSync()
            } finally {
                refreshLock.unlock()
            }
        }

        return try {
            val result = runBlocking { authRepository.get().refreshToken() }
            result.getOrNull()
        } finally {
            refreshLock.unlock()
        }
    }
}