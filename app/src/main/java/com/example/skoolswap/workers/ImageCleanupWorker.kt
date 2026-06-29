package com.example.skoolswap.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.example.skoolswap.SkoolSwapApplication
import com.example.skoolswap.data.local.database.dao.ItemImageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cleans up cached images using:
 * 1. TIME-BASED: Delete images older than 30 days
 * 2. CAP-BASED: If cache > 100MB, delete oldest images
 */
class ImageCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val itemImageDao: ItemImageDao by lazy {
        (applicationContext as SkoolSwapApplication).database.itemImageDao()
    }

    companion object {
        private const val TAG = "ImageCleanupWorker"

        // ✅ TIME-BASED: Delete images older than 30 days
        private const val MAX_CACHE_AGE_DAYS = 30L

        // ✅ CAP-BASED: Max cache size 100MB
        private const val MAX_CACHE_SIZE_MB = 100L

        // ✅ Minimum images to keep (prevents deleting everything)
        private const val MIN_IMAGES_TO_KEEP = 50

        fun createPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequest.Builder(
                ImageCleanupWorker::class.java,
                7, TimeUnit.DAYS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .addTag(TAG)
                .build()
        }

        fun createOneTimeRequest(): androidx.work.OneTimeWorkRequest {
            return androidx.work.OneTimeWorkRequest.Builder(
                ImageCleanupWorker::class.java
            )
                .addTag(TAG)
                .build()
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            Timber.tag(TAG).d("🧹 Image cleanup started")

            withContext(Dispatchers.IO) {
                // ============================================================
                // STEP 1: TIME-BASED CLEANUP (Delete old images)
                // ============================================================
                val cutoffTime = System.currentTimeMillis() - (MAX_CACHE_AGE_DAYS * 24 * 60 * 60 * 1000)
                val oldImagesDeleted = itemImageDao.deleteImagesOlderThan(cutoffTime)

                if (oldImagesDeleted > 0) {
                    Timber.tag(TAG).d("⏰ Deleted $oldImagesDeleted images older than $MAX_CACHE_AGE_DAYS days")
                } else {
                    Timber.tag(TAG).d("⏰ No images older than $MAX_CACHE_AGE_DAYS days")
                }

                // ============================================================
                // STEP 2: CAP-BASED CLEANUP (Delete oldest if over cap)
                // ============================================================
                val totalSize = getCacheSize()
                val maxSizeBytes = MAX_CACHE_SIZE_MB * 1024 * 1024

                Timber.tag(TAG).d("📊 Current cache: ${totalSize / 1024 / 1024}MB / ${MAX_CACHE_SIZE_MB}MB")

                if (totalSize > maxSizeBytes) {
                    Timber.tag(TAG).d("⚠️ Cache over cap, cleaning up...")

                    val images = itemImageDao.getAllImagesSortedByAge()
                    var currentSize = totalSize
                    var deletedCount = 0

                    for (image in images) {
                        if (currentSize <= maxSizeBytes) break
                        if (images.size - deletedCount <= MIN_IMAGES_TO_KEEP) break

                        // Delete physical file
                        image.localPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                currentSize -= file.length()
                                file.delete()
                            }
                        }

                        // Delete from Room
                        itemImageDao.deleteImage(image.id)
                        deletedCount++
                    }

                    Timber.tag(TAG).d("📦 Deleted $deletedCount images for cap cleanup, cache now ${currentSize / 1024 / 1024}MB")
                } else {
                    Timber.tag(TAG).d("✅ Cache under cap (${MAX_CACHE_SIZE_MB}MB), no cap cleanup needed")
                }

                // ============================================================
                // STEP 3: Clear Glide cache
                // ============================================================
                try {
                    com.bumptech.glide.Glide.get(applicationContext).clearDiskCache()
                    Timber.tag(TAG).d("✅ Glide disk cache cleared")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to clear Glide cache")
                }

                // ============================================================
                // STEP 4: Final summary
                // ============================================================
                val finalSize = getCacheSize()
                val finalCount = itemImageDao.getImageCount()
                Timber.tag(TAG).d("✅ Cleanup complete: $finalCount images, ${finalSize / 1024 / 1024}MB")
            }

            Timber.tag(TAG).d("✅ Image cleanup completed successfully")
            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Image cleanup failed")
            ListenableWorker.Result.retry()
        }
    }

    private suspend fun getCacheSize(): Long {
        val dbSize = itemImageDao.getTotalImageSize() ?: 0L

        val glideSize = try {
            val cacheDir = applicationContext.cacheDir
            val glideDir = File(cacheDir, "glide")
            getFolderSize(glideDir)
        } catch (e: Exception) {
            0L
        }

        return dbSize + glideSize
    }

    private fun getFolderSize(folder: File): Long {
        if (!folder.exists()) return 0L

        var size = 0L
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                size += getFolderSize(file)
            } else {
                size += file.length()
            }
        }
        return size
    }
}