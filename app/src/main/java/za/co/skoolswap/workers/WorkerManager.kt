package za.co.skoolswap.workers

import android.content.Context
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TOKEN_REFRESH_WORK = "TokenRefreshWork"
        private const val HOME_FEED_SYNC_WORK = "HomeFeedSyncWork"
        private const val PRODUCTS_SYNC_WORK = "ProductsSyncWork"
        private const val ITEM_CACHE_WORK = "ItemCacheWork"
        private const val IMAGE_CLEANUP_WORK = "ImageCleanupWork"
        private const val OFFLINE_SYNC_WORK = "OfflineSyncWork"
    }

    fun scheduleAllWorkers() {
        scheduleTokenRefresh()
        scheduleHomeFeedSync()
        scheduleImageCleanup()
        scheduleOfflineSync()
        scheduleProductsSync()
    }

    fun scheduleProductsSync() {
        try {
            val request = ProductsSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                PRODUCTS_SYNC_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Products sync scheduled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule products sync")
        }
    }

    fun cacheItemNow(itemId: String) {
        try {
            val request = ItemCacheWorker.createOneTimeRequest(itemId)
            workManager.enqueueUniqueWork(
                "${ITEM_CACHE_WORK}_${itemId}",
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("🔄 Item cache triggered for: $itemId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger item cache")
        }
    }

    fun syncHomeFeedNow() {
        try {
            val request = HomeFeedSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                HOME_FEED_SYNC_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("🔄 Home feed sync requested (will run if not already running)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger home feed sync")
        }
    }

    fun scheduleHomeFeedSyncOnLogin() {
        try {
            val request = HomeFeedSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                HOME_FEED_SYNC_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Home feed sync scheduled (on-login)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule home feed sync")
        }
    }

    fun scheduleTokenRefresh() {
        try {
            val request = TokenRefreshWorker.createPeriodicRequest()
            workManager.enqueueUniquePeriodicWork(
                TOKEN_REFRESH_WORK,
                ExistingPeriodicWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Token refresh worker scheduled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule token refresh worker")
        }
    }

    fun cancelTokenRefresh() {
        try {
            workManager.cancelUniqueWork(TOKEN_REFRESH_WORK)
            Timber.d("❌ Token refresh worker cancelled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cancel token refresh worker")
        }
    }

    fun scheduleHomeFeedSync() {
        try {
            val request = HomeFeedSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                HOME_FEED_SYNC_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Home feed sync scheduled (on-demand)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule home feed sync")
        }
    }

    fun cancelHomeFeedSync() {
        try {
            workManager.cancelUniqueWork(HOME_FEED_SYNC_WORK)
            Timber.d("❌ Home feed sync worker cancelled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cancel home feed sync worker")
        }
    }

    fun scheduleImageCleanup() {
        try {
            val request = ImageCleanupWorker.createPeriodicRequest()
            workManager.enqueueUniquePeriodicWork(
                IMAGE_CLEANUP_WORK,
                ExistingPeriodicWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Image cleanup worker scheduled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule image cleanup worker")
        }
    }

    fun cancelImageCleanup() {
        try {
            workManager.cancelUniqueWork(IMAGE_CLEANUP_WORK)
            Timber.d("❌ Image cleanup worker cancelled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cancel image cleanup worker")
        }
    }

    fun scheduleOfflineSync() {
        try {
            val request = OfflineSyncWorker.createPeriodicRequest()
            workManager.enqueueUniquePeriodicWork(
                OFFLINE_SYNC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Offline sync worker scheduled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule offline sync worker")
        }
    }

    fun cancelOfflineSync() {
        try {
            workManager.cancelUniqueWork(OFFLINE_SYNC_WORK)
            Timber.d("❌ Offline sync worker cancelled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cancel offline sync worker")
        }
    }

    fun refreshTokenNow() {
        try {
            val request = TokenRefreshWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                TOKEN_REFRESH_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("🔄 Token refresh requested")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger token refresh")
        }
    }

    fun cleanupImagesNow() {
        try {
            val request = ImageCleanupWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                IMAGE_CLEANUP_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("🔄 Image cleanup requested")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger image cleanup")
        }
    }

    fun syncOfflineActionsNow() {
        try {
            val request = OfflineSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                OFFLINE_SYNC_WORK,
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("🔄 Manual offline sync requested")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger manual offline sync")
        }
    }

    fun cancelAllWorkers() {
        try {
            workManager.cancelUniqueWork(TOKEN_REFRESH_WORK)
            workManager.cancelUniqueWork(HOME_FEED_SYNC_WORK)
            workManager.cancelUniqueWork(IMAGE_CLEANUP_WORK)
            workManager.cancelUniqueWork(OFFLINE_SYNC_WORK)
            workManager.cancelUniqueWork(PRODUCTS_SYNC_WORK)
            Timber.d("❌ All workers cancelled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cancel workers")
        }
    }

    fun scheduleItemCreation(itemId: String, imageUris: List<Uri>) {
        try {
            val request = ItemCreationWorker.createOneTimeRequest(itemId, imageUris)
            workManager.enqueueUniqueWork(
                "item_creation_$itemId",
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("✅ Item creation worker scheduled for: $itemId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule item creation worker")
        }
    }

    fun retryFailedItem(itemId: String) {
        try {
            val request = ItemCreationWorker.createOneTimeRequest(itemId, emptyList())
            workManager.enqueueUniqueWork(
                "item_retry_$itemId",
                ExistingWorkPolicy.KEEP,  // ✅ Don't cancel existing work
                request
            )
            Timber.d("🔄 Retry scheduled for item: $itemId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule retry")
        }
    }
}