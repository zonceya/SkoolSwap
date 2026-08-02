package za.co.skoolswap.di

import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
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

        val response = chain.proceed(request)

        if (response.code == 401 &&
            !originalRequest.url.encodedPath.contains("refresh")) {

            val newToken = performTokenRefresh(token)

            return if (!newToken.isNullOrEmpty()) {
                response.close() // only close now, because we're replacing it
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                chain.proceed(retryRequest)
            } else {
                // Force logout on refresh failure — but don't touch `response`,
                // just let it flow back up unread/unclosed so the chain (and
                // whoever eventually consumes the body) can handle it normally.
                runBlocking { authRepository.get().signOut() }
                response
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