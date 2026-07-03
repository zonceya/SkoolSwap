// Create new file: workers/EditItemWorker.kt

package com.example.skoolswap.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.dao.ItemImageDao
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.repository.ImageUploadRepository
import com.example.skoolswap.data.repository.ItemRepository
import com.example.skoolswap.domain.repository.EditItemWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class EditItemWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val entryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        EditItemWorkerEntryPoint::class.java
    )

    private val itemDao = entryPoint.itemDao()
    private val itemImageDao = entryPoint.itemImageDao()
    private val itemRepository = entryPoint.itemRepository()
    private val imageUploadRepository = entryPoint.imageUploadRepository()

    override suspend fun doWork(): ListenableWorker.Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: run {
            Timber.tag(TAG).e("❌ No item ID provided")
            return ListenableWorker.Result.failure()
        }

        val imageUriStrings = inputData.getStringArray(KEY_ADD_IMAGE_URIS)
        val removeImageIds = inputData.getLongArray(KEY_REMOVE_IMAGE_IDS)?.toList() ?: emptyList()

        val addImageUris = imageUriStrings?.mapNotNull {
            try { Uri.parse(it) } catch (e: Exception) { null }
        } ?: emptyList()

        Timber.tag(TAG).d("🚀 EditItemWorker.doWork() START")
        Timber.tag(TAG).d("   Item ID: $itemId")
        Timber.tag(TAG).d("   Add images: ${addImageUris.size}")
        Timber.tag(TAG).d("   Remove image IDs: ${removeImageIds.size}")

        return try {
            // 1. Get local item
            Timber.tag(TAG).d("🔍 Step 1: Fetching local item...")
            val localItem = itemDao.getItemById(itemId)
            if (localItem == null) {
                Timber.tag(TAG).e("❌ Item not found")
                return ListenableWorker.Result.failure()
            }

            Timber.tag(TAG).d("✅ Local item found:")
            Timber.tag(TAG).d("   Name: ${localItem.name}")
            Timber.tag(TAG).d("   mainCategoryId: ${localItem.mainCategoryId}")
            Timber.tag(TAG).d("   subCategoryId: ${localItem.subCategoryId}")
            Timber.tag(TAG).d("   syncStatus: ${localItem.syncStatus}")

            // 2. Upload new images (if any)
            var uploadedUrls = emptyList<String>()
            if (addImageUris.isNotEmpty()) {
                Timber.tag(TAG).d("📤 Step 2: Uploading ${addImageUris.size} images...")
                try {
                    uploadedUrls = imageUploadRepository.uploadImages(
                        context = applicationContext,
                        imageUris = addImageUris
                    )
                    Timber.tag(TAG).d("✅ Uploaded ${uploadedUrls.size}/${addImageUris.size} images")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "❌ Image upload failed")
                    // Continue with update, images can be uploaded later
                }
            } else {
                Timber.tag(TAG).d("📤 Step 2: No images to upload")
            }

            // 3. Delete images from server (if any)
            if (removeImageIds.isNotEmpty()) {
                Timber.tag(TAG).d("🗑️ Removing ${removeImageIds.size} images from server...")
                removeImageIds.forEach { imageId ->
                    try {
                        val result = itemRepository.removeItemImage(itemId, imageId)
                        if (result.isSuccess) {
                            Timber.tag(TAG).d("✅ Removed image ID: $imageId")
                        } else {
                            Timber.tag(TAG).w("⚠️ Failed to remove image $imageId: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "❌ Failed to remove image $imageId")
                    }
                }
            }

            // 4. Attach new images
            if (uploadedUrls.isNotEmpty()) {
                Timber.tag(TAG).d("🔄 Attaching ${uploadedUrls.size} images to item...")
                val attachResult = itemRepository.attachImagesToItem(itemId, uploadedUrls)
                if (attachResult.isFailure) {
                    Timber.tag(TAG).w("⚠️ Failed to attach images: ${attachResult.exceptionOrNull()?.message}")
                } else {
                    Timber.tag(TAG).d("✅ Images attached")
                }
            }

            // 5. Update item on server
            Timber.tag(TAG).d("🔄 Step 3: Updating item on server...")

            // Convert to domain for update
            val domainItem = localItem.toDomain()

            val updateResult = itemRepository.updateItemSimple(
                itemId = itemId,
                name = domainItem.name,
                description = domainItem.description,
                mainCategoryId = domainItem.mainCategoryId,
                subCategoryId = domainItem.subCategoryId,
                brandId = domainItem.brandId,
                price = domainItem.price,
                quantity = domainItem.quantity,
                itemConditionId = domainItem.itemConditionId,
                provinceId = domainItem.provinceId,
                locationId = domainItem.locationId,
                genderId = domainItem.genderId,
                schoolId = domainItem.schoolId,
                sizeId = domainItem.sizeId,
                colorId = domainItem.colorId,
                tagIds = null,
                status = domainItem.status
            )

            if (updateResult.isFailure) {
                val error = updateResult.exceptionOrNull()?.message ?: "Unknown error"
                Timber.tag(TAG).e("❌ API update failed: $error")
                updateItemStatus(itemId, "UPDATE_FAILED", error)

                // Retry on network errors
                if (error.contains("network", ignoreCase = true) ||
                    error.contains("timeout", ignoreCase = true) ||
                    error.contains("502", ignoreCase = true)) {
                    Timber.tag(TAG).d("🔁 Retryable error")
                    return ListenableWorker.Result.retry()
                }
                return ListenableWorker.Result.failure()
            }

            val serverItem = updateResult.getOrNull()!!
            Timber.tag(TAG).d("✅ Item updated on server:")
            Timber.tag(TAG).d("   Server ID: ${serverItem.id}")
            Timber.tag(TAG).d("   Status: ${serverItem.status}")

            // 6. Update local item with server data
            Timber.tag(TAG).d("🔄 Step 4: Updating local item...")
            val updatedEntity = localItem.copy(
                syncStatus = "SYNCED",
                syncError = null,
                retryCount = 0,
                lastSyncAttempt = System.currentTimeMillis(),
                updatedAt = serverItem.updatedAt,
                price = serverItem.price,
                quantity = serverItem.quantity,
                status = serverItem.status
            )
            itemDao.insertItem(updatedEntity)
            Timber.tag(TAG).d("✅ Local item updated, syncStatus: SYNCED")

            Timber.tag(TAG).d("🏁 EditItemWorker.doWork() SUCCESS")
            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Worker failed with exception")
            updateItemStatus(itemId, "UPDATE_FAILED", e.message)

            when {
                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ||
                        e.message?.contains("502", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("🔁 Retryable error, will retry")
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
        private const val TAG = "EditItemWorker"
        const val KEY_ITEM_ID = "item_id"
        const val KEY_ADD_IMAGE_URIS = "add_image_uris"
        const val KEY_REMOVE_IMAGE_IDS = "remove_image_ids"

        fun createOneTimeRequest(
            itemId: String,
            addImageUris: List<Uri> = emptyList(),
            removeImageIds: List<Long> = emptyList()
        ): OneTimeWorkRequest {
            Timber.tag(TAG).d("📦 Creating EditItemWorker for: $itemId")
            Timber.tag(TAG).d("   Add images: ${addImageUris.size}")
            Timber.tag(TAG).d("   Remove image IDs: ${removeImageIds.size}")

            val inputData = androidx.work.Data.Builder()
                .putString(KEY_ITEM_ID, itemId)
                .putStringArray(
                    KEY_ADD_IMAGE_URIS,
                    addImageUris.map { it.toString() }.toTypedArray()
                )
                .putLongArray(
                    KEY_REMOVE_IMAGE_IDS,
                    removeImageIds.toLongArray()
                )
                .build()

            return OneTimeWorkRequest.Builder(EditItemWorker::class.java)
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
                .addTag("edit_item")
                .build()
        }
    }
}