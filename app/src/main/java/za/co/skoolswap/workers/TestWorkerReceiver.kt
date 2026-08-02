package za.co.skoolswap.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class TestWorkerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "TEST_TOKEN_REFRESH" -> {
                Timber.d("🧪 Testing TokenRefreshWorker")
                val request = TokenRefreshWorker.createOneTimeRequest()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "TokenRefreshWork",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
            "TEST_HOME_FEED_SYNC" -> {
                Timber.d("🧪 Testing HomeFeedSyncWorker")
                val request = HomeFeedSyncWorker.createOneTimeRequest()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "HomeFeedSyncWork",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
            "TEST_IMAGE_CLEANUP" -> {
                Timber.d("🧪 Testing ImageCleanupWorker")
                val request = ImageCleanupWorker.createOneTimeRequest()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "ImageCleanupWork",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
            "TEST_OFFLINE_SYNC" -> {
                Timber.d("🧪 Testing OfflineSyncWorker")
                val request = OfflineSyncWorker.createOneTimeRequest()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "OfflineSyncWork",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}