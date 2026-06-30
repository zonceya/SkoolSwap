package com.example.skoolswap.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.example.skoolswap.SkoolSwapApplication
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Syncs home feed every 6 hours in the background
 */
class HomeFeedSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val homeRepository: HomeRepositoryInterface by lazy {
        (applicationContext as SkoolSwapApplication).homeRepository
    }

    private val appPreferences: AppPreferences by lazy {
        (applicationContext as SkoolSwapApplication).appPreferences
    }

    companion object {
        private const val TAG = "HomeFeedSyncWorker"
        private const val SYNC_INTERVAL_HOURS = 6L

        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequest.Builder(
                HomeFeedSyncWorker::class.java,
                SYNC_INTERVAL_HOURS,
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
                HomeFeedSyncWorker::class.java
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
            Timber.tag(TAG).d("🔄 Home feed sync started")

            val schoolId = appPreferences.schoolId.first()
            if (schoolId == null || schoolId <= 0) {
                Timber.tag(TAG).d("⏭️ No school selected, skipping sync")
                return ListenableWorker.Result.success()
            }

            Timber.tag(TAG).d("📡 Syncing home feed for school: $schoolId")
            val result = homeRepository.getHomeFeed(schoolId)

            return when (result) {
                is com.example.skoolswap.utils.Result.Success -> {
                    Timber.tag(TAG).d("✅ Home feed synced successfully")
                    ListenableWorker.Result.success()
                }
                is com.example.skoolswap.utils.Result.Error -> {
                    val error = result.exception.message ?: "Unknown error"
                    Timber.tag(TAG).e("❌ Home feed sync failed: $error")

                    // Retry on network errors
                    if (result.exception is java.io.IOException) {
                        ListenableWorker.Result.retry()
                    } else {
                        ListenableWorker.Result.success()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Home feed sync worker failed")
            ListenableWorker.Result.retry()
        }
    }

}