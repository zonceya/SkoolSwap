package com.example.skoolswap.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * IMPORTANT: This worker is constructed by Hilt via HiltWorkerFactory, NOT by
 * WorkManager's default reflection-based factory. That requires:
 *   1. @AssistedInject + @Assisted on the constructor (below) so Hilt can
 *      generate a factory that supplies authRepository/appPreferences.
 *   2. SkoolSwapApplication implementing Configuration.Provider and supplying
 *      a HiltWorkerFactory (see SkoolSwapApplication.kt).
 *   3. The default androidx.work.WorkManagerInitializer removed from the
 *      manifest (see AndroidManifest.xml snippet provided alongside this file).
 *
 * Do NOT remove @AssistedInject/@Assisted "to simplify" — WorkManager's
 * reflection fallback only knows how to call a (Context, WorkerParameters)
 * constructor, and removing these annotations causes:
 *   NoSuchMethodException: TokenRefreshWorker.<init> [Context, WorkerParameters]
 */
@HiltWorker
class TokenRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TokenRefreshWorker"
        private const val REFRESH_INTERVAL_HOURS = 12L
        private const val EXPIRY_BUFFER_HOURS = 24L

        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequest.Builder(
                TokenRefreshWorker::class.java,
                REFRESH_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .addTag(TAG)
                .build()
        }

        fun createOneTimeRequest(): androidx.work.OneTimeWorkRequest {
            return androidx.work.OneTimeWorkRequest.Builder(
                TokenRefreshWorker::class.java
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .addTag(TAG)
                .build()
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            Timber.tag(TAG).d("🔄 Token refresh worker started")

            val token = appPreferences.authToken.first()

            if (token.isNullOrEmpty()) {
                Timber.tag(TAG).d("⏭️ No token found, skipping refresh")
                return ListenableWorker.Result.success()
            }

            if (!shouldRefreshToken(token)) {
                Timber.tag(TAG).d("✅ Token still valid, no refresh needed")
                return ListenableWorker.Result.success()
            }

            Timber.tag(TAG).d("🔄 Token needs refresh, attempting...")

            // NOTE: AuthRepository.refreshToken() returns kotlin.Result<String>
            // (the stdlib type), NOT the custom com.example.skoolswap.utils.Result
            // sealed class. Use kotlin.Result's own API (fold) here — don't
            // pattern-match with is Success/is Error, those only exist on the
            // custom sealed class and have no meaning for kotlin.Result.
            val result = authRepository.refreshToken()

            result.fold(
                onSuccess = {
                    Timber.tag(TAG).d("✅ Token refreshed successfully")
                    ListenableWorker.Result.success()
                },
                onFailure = { exception ->
                    Timber.tag(TAG).e(exception, "❌ Token refresh failed")

                    if (exception is HttpException && exception.code() == 401) {
                        // Refresh token itself is invalid/expired — no point retrying.
                        Timber.tag(TAG).d("🔒 Refresh token rejected (401), signing out")
                        authRepository.signOut()
                        ListenableWorker.Result.success()
                    } else {
                        // Transient/network/server error — let WorkManager's
                        // exponential backoff (configured on the request) retry.
                        Timber.tag(TAG).d("🔁 Transient error, will retry")
                        ListenableWorker.Result.retry()
                    }
                }
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Token refresh worker failed")
            ListenableWorker.Result.retry()
        }
    }

    private fun shouldRefreshToken(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = parts[1]
                val decoded = String(android.util.Base64.decode(payload, android.util.Base64.DEFAULT))
                val json = org.json.JSONObject(decoded)
                val expSeconds = json.optLong("exp", 0)

                if (expSeconds > 0) {
                    val expiryTime = expSeconds * 1000
                    val currentTime = System.currentTimeMillis()
                    val bufferMillis = EXPIRY_BUFFER_HOURS * 60 * 60 * 1000L

                    val shouldRefresh = (expiryTime - currentTime) < bufferMillis
                    val hoursUntilExpiry = (expiryTime - currentTime) / (60 * 60 * 1000)

                    Timber.tag(TAG).d("Token expires in $hoursUntilExpiry hours, should refresh: $shouldRefresh")
                    shouldRefresh
                } else {
                    Timber.tag(TAG).d("No expiry claim in token, refreshing to be safe")
                    true
                }
            } else {
                Timber.tag(TAG).d("Invalid token format, refreshing to be safe")
                true
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking token expiry")
            true
        }
    }
}