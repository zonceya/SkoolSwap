package com.example.skoolswap.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.repository.ImageUploadRepository
import com.example.skoolswap.data.repository.ItemRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

// ✅ EntryPoint for manual injection
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ItemCreationWorkerEntryPoint {
    fun itemDao(): ItemDao
    fun itemRepository(): ItemRepository
    fun imageUploadRepository(): ImageUploadRepository
}

class ItemCreationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // ✅ Manual injection using EntryPoint
    private val entryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        ItemCreationWorkerEntryPoint::class.java
    )

    private val itemDao = entryPoint.itemDao()
    private val itemRepository = entryPoint.itemRepository()
    private val imageUploadRepository = entryPoint.imageUploadRepository()

    override suspend fun doWork(): ListenableWorker.Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: run {
            Timber.tag(TAG).e("❌ No item ID provided")
            return ListenableWorker.Result.failure()
        }

        val imageUriStrings = inputData.getStringArray(KEY_IMAGE_URIS)
        val imageUris = imageUriStrings?.mapNotNull { it?.let { Uri.parse(it) } } ?: emptyList()

        Timber.tag(TAG).d("🔄 Starting background creation for item: $itemId")
        Timber.tag(TAG).d("   Images to upload: ${imageUris.size}")

        return try {
            val localItem = itemDao.getItemById(itemId)
            if (localItem == null) {
                Timber.tag(TAG).e("❌ Item not found in local DB: $itemId")
                return ListenableWorker.Result.failure()
            }

            Timber.tag(TAG).d("📦 Local item found: ${localItem.name}")

            // 1. Upload images FIRST (before creating item)
            var uploadedUrls = emptyList<String>()
            if (imageUris.isNotEmpty()) {
                Timber.tag(TAG).d("📤 Uploading ${imageUris.size} images to R2...")
                uploadedUrls = imageUploadRepository.uploadImages(
                    context = applicationContext,
                    imageUris = imageUris
                )
                Timber.tag(TAG).d("✅ Uploaded ${uploadedUrls.size} images to R2")
            }

            // 2. Create item on Rails API
            val createdItem = itemRepository.createItemSimple(
                name = localItem.name,
                description = localItem.description,
                mainCategoryId = localItem.mainCategoryId ?: 0,
                subCategoryId = localItem.subCategoryId ?: 0,
                brandId = localItem.brandId,
                price = localItem.price,
                quantity = localItem.quantity,
                itemConditionId = localItem.itemConditionId,
                provinceId = localItem.provinceId,
                locationId = localItem.locationId,
                genderId = localItem.genderId,
                schoolId = localItem.schoolId,
                sizeId = localItem.sizeId,
                colorId = localItem.colorId,
                tagIds = null
            )

            if (createdItem.isFailure) {
                val error = createdItem.exceptionOrNull()?.message ?: "Unknown error"
                Timber.tag(TAG).e("❌ API creation failed: $error")
                updateItemStatus(itemId, "FAILED", error)
                return ListenableWorker.Result.retry()
            }

            val item = createdItem.getOrNull()!!
            Timber.tag(TAG).d("✅ Item created on server: ${item.id}")

            // 3. Attach uploaded images to the item (if any)
            if (uploadedUrls.isNotEmpty()) {
                Timber.tag(TAG).d("📎 Attaching ${uploadedUrls.size} images to item...")
                val imageResult = itemRepository.addItemImages(
                    context = applicationContext,
                    itemId = item.id,
                    imageUris = imageUris
                )
                if (imageResult.isFailure) {
                    Timber.tag(TAG).w("⚠️ Image attachment failed: ${imageResult.exceptionOrNull()?.message}")
                } else {
                    Timber.tag(TAG).d("✅ Images attached: ${imageResult.getOrNull()?.size ?: 0}")
                }
            }

            // 4. Update local item with server ID and status
            val updatedEntity = localItem.copy(
                id = item.id,
                shopId = item.shopId,
                syncStatus = "ACTIVE",
                syncError = null,
                retryCount = 0,
                lastSyncAttempt = System.currentTimeMillis(),
                price = item.price,
                quantity = item.quantity,
                status = item.status,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
            itemDao.insertItem(updatedEntity)

            if (itemId != item.id) {
                itemDao.deleteItemById(itemId)
                Timber.tag(TAG).d("🗑️ Deleted placeholder item: $itemId")
            }

            Timber.tag(TAG).d("✅ Item creation completed successfully: ${item.id}")
            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Worker failed")
            updateItemStatus(itemId, "FAILED", e.message)

            when {
                e.message?.contains("network", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("🔁 Network error - will retry")
                    ListenableWorker.Result.retry()
                }
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("🔁 Timeout - will retry")
                    ListenableWorker.Result.retry()
                }
                else -> {
                    Timber.tag(TAG).d("❌ Non-retryable error")
                    ListenableWorker.Result.failure()
                }
            }
        }
    }

    private suspend fun updateItemStatus(itemId: String, status: String, error: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                val item = itemDao.getItemById(itemId)
                if (item != null) {
                    val updated = item.copy(
                        syncStatus = status,
                        syncError = error,
                        lastSyncAttempt = System.currentTimeMillis(),
                        retryCount = item.retryCount + 1
                    )
                    itemDao.insertItem(updated)
                    Timber.tag(TAG).d("📝 Updated item status: $status (retry: ${updated.retryCount})")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to update status")
            }
        }
    }

    companion object {
        private const val TAG = "ItemCreationWorker"
        const val KEY_ITEM_ID = "item_id"
        const val KEY_IMAGE_URIS = "image_uris"

        fun createOneTimeRequest(itemId: String, imageUris: List<Uri>): OneTimeWorkRequest {
            val uriStrings: Array<String?> = imageUris.map { it.toString() }.toTypedArray()

            val inputData = androidx.work.Data.Builder()
                .putString(KEY_ITEM_ID, itemId)
                .putStringArray(KEY_IMAGE_URIS, uriStrings)
                .build()

            return OneTimeWorkRequest.Builder(ItemCreationWorker::class.java)
                .setInputData(inputData)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag("item_creation")
                .build()
        }
    }
}