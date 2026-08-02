package za.co.skoolswap.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import za.co.skoolswap.SkoolSwapApplication
import za.co.skoolswap.data.repository.OfflineQueueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Processes queued offline actions (favorites, likes, etc.)
 * Runs daily at 2 AM when device is online
 */
class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val offlineQueueRepository: OfflineQueueRepository by lazy {
        (applicationContext as SkoolSwapApplication).offlineQueueRepository
    }

    companion object {
        private const val TAG = "OfflineSyncWorker"

        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequest.Builder(
                OfflineSyncWorker::class.java,
                1, TimeUnit.DAYS  // Daily
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
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
                OfflineSyncWorker::class.java
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
            Timber.tag(TAG).d("🔄 Offline sync started")

            withContext(Dispatchers.IO) {
                val pendingActions = offlineQueueRepository.getPendingActionsSync()

                if (pendingActions.isEmpty()) {
                    Timber.tag(TAG).d("⏭️ No pending actions to process")
                    return@withContext
                }

                Timber.tag(TAG).d("📡 Processing ${pendingActions.size} pending actions")

                var successCount = 0
                var failureCount = 0

                for (action in pendingActions) {
                    try {
                        when (action.actionType) {
                            "favorite" -> {
                                // TODO: Implement API call
                                // apiService.favoriteItem(action.itemId!!)
                                Timber.tag(TAG).d("👍 Processing favorite for item: ${action.itemId}")
                            }
                            "unfavorite" -> {
                                // TODO: Implement API call
                                // apiService.unfavoriteItem(action.itemId!!)
                                Timber.tag(TAG).d("💔 Processing unfavorite for item: ${action.itemId}")
                            }
                            "save" -> {
                                // TODO: Implement API call
                                // apiService.saveItem(action.itemId!!)
                                Timber.tag(TAG).d("💾 Processing save for item: ${action.itemId}")
                            }
                            else -> {
                                Timber.tag(TAG).w("⚠️ Unknown action type: ${action.actionType}")
                                offlineQueueRepository.markAsCompleted(action.id)
                                successCount++
                                continue
                            }
                        }
                        offlineQueueRepository.markAsCompleted(action.id)
                        successCount++
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "❌ Failed to process action ${action.id}")
                        if (action.retryCount >= action.maxRetries) {
                            offlineQueueRepository.markAsFailed(action.id)
                            failureCount++
                        } else {
                            offlineQueueRepository.incrementRetry(action.id)
                        }
                    }
                }

                Timber.tag(TAG).d("✅ Offline sync complete: $successCount succeeded, $failureCount failed")

                // Cleanup old completed actions
                offlineQueueRepository.cleanupCompletedActions()
            }

            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Offline sync worker failed")
            ListenableWorker.Result.retry()
        }
    }
}