package za.co.skoolswap.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class TokenRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepositoryInterface,
    private val appPreferences: AppPreferences
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TokenRefreshWorker"

        // Updated for 30-day token lifetime
        private const val REFRESH_INTERVAL_DAYS = 7L      // Check every 7 days
        private const val EXPIRY_BUFFER_DAYS = 10L        // Refresh when ~10 days left

        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequest.Builder(
                TokenRefreshWorker::class.java,
                REFRESH_INTERVAL_DAYS,
                TimeUnit.DAYS
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
                Timber.tag(TAG).d("✅ Token still has enough lifetime, skipping refresh")
                return ListenableWorker.Result.success()
            }

            Timber.tag(TAG).d("🔄 Token needs refresh, attempting...")

            val result = authRepository.refreshToken()

            result.fold(
                onSuccess = {
                    Timber.tag(TAG).d("✅ Token refreshed successfully")
                    ListenableWorker.Result.success()
                },
                onFailure = { exception ->
                    Timber.tag(TAG).e(exception, "❌ Token refresh failed")

                    if (exception is HttpException && exception.code() == 401) {
                        Timber.tag(TAG).w("🔒 Refresh token rejected (401) → signing out")
                        authRepository.signOut()
                        ListenableWorker.Result.success()
                    } else {
                        // Transient error → retry later
                        ListenableWorker.Result.retry()
                    }
                }
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Token refresh worker failed")
            ListenableWorker.Result.retry()
        }
    }

    /**
     * Checks if token should be refreshed based on expiry
     * Refreshes when less than EXPIRY_BUFFER_DAYS (10 days) remain
     */
    private fun shouldRefreshToken(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Timber.tag(TAG).d("Invalid token format, will refresh")
                return true
            }

            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT))
            val json = org.json.JSONObject(payload)
            val expSeconds = json.optLong("exp", 0)

            if (expSeconds > 0) {
                val expiryTime = expSeconds * 1000L
                val currentTime = System.currentTimeMillis()
                val bufferMillis = EXPIRY_BUFFER_DAYS * 24 * 60 * 60 * 1000L

                val daysUntilExpiry = (expiryTime - currentTime) / (24 * 60 * 60 * 1000L)

                val shouldRefresh = (expiryTime - currentTime) < bufferMillis

                Timber.tag(TAG).d("Token expires in $daysUntilExpiry days → should refresh: $shouldRefresh")
                return shouldRefresh
            }

            Timber.tag(TAG).d("No expiry claim found, refreshing to be safe")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing token expiry")
            true // Refresh on any parsing error
        }
    }
}