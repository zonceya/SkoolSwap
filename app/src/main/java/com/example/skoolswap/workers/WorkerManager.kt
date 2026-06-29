package com.example.skoolswap.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("✅ Products sync scheduled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule products sync")
        }
    }

    // ✅ FIXED: Use ITEM_CACHE_WORK with itemId appended
    fun cacheItemNow(itemId: String) {
        try {
            val request = ItemCacheWorker.createOneTimeRequest(itemId)
            workManager.enqueueUniqueWork(
                "${ITEM_CACHE_WORK}_${itemId}",  // ← FIXED: Added underscore between constants
                ExistingWorkPolicy.REPLACE,
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
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("🔄 Manual home feed sync triggered")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger home feed sync")
        }
    }

    fun scheduleHomeFeedSyncOnLogin() {
        try {
            val request = HomeFeedSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                HOME_FEED_SYNC_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("✅ Home feed sync scheduled (on-login)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule home feed sync")
        }
    }

    // ==================== TOKEN REFRESH ====================
    fun scheduleTokenRefresh() {
        try {
            val request = TokenRefreshWorker.createPeriodicRequest()
            workManager.enqueueUniquePeriodicWork(
                TOKEN_REFRESH_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
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

    // ==================== HOME FEED SYNC ====================
    fun scheduleHomeFeedSync() {
        try {
            val request = HomeFeedSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                HOME_FEED_SYNC_WORK,
                ExistingWorkPolicy.REPLACE,
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

    // ==================== IMAGE CLEANUP ====================
    fun scheduleImageCleanup() {
        try {
            val request = ImageCleanupWorker.createPeriodicRequest()
            workManager.enqueueUniquePeriodicWork(
                IMAGE_CLEANUP_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
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

    // ==================== OFFLINE SYNC ====================
    fun scheduleOfflineSync() {
        try {
            val request = OfflineSyncWorker.createPeriodicRequest()
            workManager.enqueueUniquePeriodicWork(
                OFFLINE_SYNC_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
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

    // ==================== MANUAL TRIGGERS ====================

    fun refreshTokenNow() {
        try {
            val request = TokenRefreshWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                TOKEN_REFRESH_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("🔄 Manual token refresh triggered")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger manual token refresh")
        }
    }

    fun cleanupImagesNow() {
        try {
            val request = ImageCleanupWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                IMAGE_CLEANUP_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("🔄 Manual image cleanup triggered")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger manual image cleanup")
        }
    }

    fun syncOfflineActionsNow() {
        try {
            val request = OfflineSyncWorker.createOneTimeRequest()
            workManager.enqueueUniqueWork(
                OFFLINE_SYNC_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("🔄 Manual offline sync triggered")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to trigger manual offline sync")
        }
    }

    // ==================== CANCEL ALL ====================

    fun cancelAllWorkers() {
        try {
            workManager.cancelUniqueWork(TOKEN_REFRESH_WORK)
            workManager.cancelUniqueWork(HOME_FEED_SYNC_WORK)
            workManager.cancelUniqueWork(IMAGE_CLEANUP_WORK)
            workManager.cancelUniqueWork(OFFLINE_SYNC_WORK)
            workManager.cancelUniqueWork(PRODUCTS_SYNC_WORK)  // ← Added this
            Timber.d("❌ All workers cancelled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cancel workers")
        }
    }
}