package za.co.skoolswap.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.mapper.toDomain
import za.co.skoolswap.domain.repository.EditItemWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

// Private constants - internal to this file only
private const val BACKOFF_DELAY_MINUTES = 30L
private const val WORKER_TAG = "edit_item"

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
            Timber.tag(LogTags.SERVICE).e("❌ No item ID provided")
            return ListenableWorker.Result.failure()
        }

        val imageUriStrings = inputData.getStringArray(KEY_ADD_IMAGE_URIS)
        val removeImageIds = inputData.getLongArray(KEY_REMOVE_IMAGE_IDS)?.toList() ?: emptyList()

        val addImageUris = imageUriStrings?.mapNotNull {
            try { Uri.parse(it) } catch (e: Exception) { null }
        } ?: emptyList()

        Timber.tag(LogTags.SERVICE).d("🚀 EditItemWorker.doWork() START")
        Timber.tag(LogTags.SERVICE).d("   Item ID: $itemId")
        Timber.tag(LogTags.SERVICE).d("   Add images: ${addImageUris.size}")
        Timber.tag(LogTags.SERVICE).d("   Remove image IDs: ${removeImageIds.size}")

        return try {
            Timber.tag(LogTags.SERVICE).d("🔍 Step 1: Fetching local item...")
            val localItem = itemDao.getItemById(itemId)
            if (localItem == null) {
                Timber.tag(LogTags.SERVICE).e("❌ Item not found")
                return ListenableWorker.Result.failure()
            }

            Timber.tag(LogTags.SERVICE).d("✅ Local item found:")
            Timber.tag(LogTags.SERVICE).d("   Name: ${localItem.name}")
            Timber.tag(LogTags.SERVICE).d("   mainCategoryId: ${localItem.mainCategoryId}")
            Timber.tag(LogTags.SERVICE).d("   subCategoryId: ${localItem.subCategoryId}")
            Timber.tag(LogTags.SERVICE).d("   syncStatus: ${localItem.syncStatus}")

            var uploadedUrls = emptyList<String>()
            if (addImageUris.isNotEmpty()) {
                Timber.tag(LogTags.SERVICE).d("📤 Step 2: Uploading ${addImageUris.size} images...")
                try {
                    uploadedUrls = imageUploadRepository.uploadImages(
                        context = applicationContext,
                        imageUris = addImageUris
                    )
                    Timber.tag(LogTags.SERVICE).d("✅ Uploaded ${uploadedUrls.size}/${addImageUris.size} images")
                } catch (e: Exception) {
                    Timber.tag(LogTags.SERVICE).e(e, "❌ Image upload failed")
                }
            } else {
                Timber.tag(LogTags.SERVICE).d("📤 Step 2: No images to upload")
            }

            if (removeImageIds.isNotEmpty()) {
                Timber.tag(LogTags.SERVICE).d("🗑️ Removing ${removeImageIds.size} images from server...")
                removeImageIds.forEach { imageId ->
                    try {
                        val result = itemRepository.removeItemImage(itemId, imageId)
                        if (result.isSuccess) {
                            Timber.tag(LogTags.SERVICE).d("✅ Removed image ID: $imageId")
                        } else {
                            Timber.tag(LogTags.SERVICE).w("⚠️ Failed to remove image $imageId: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Timber.tag(LogTags.SERVICE).e(e, "❌ Failed to remove image $imageId")
                    }
                }
            }

            if (uploadedUrls.isNotEmpty()) {
                Timber.tag(LogTags.SERVICE).d("🔄 Attaching ${uploadedUrls.size} images to item...")
                val attachResult = itemRepository.attachImagesToItem(itemId, uploadedUrls)
                if (attachResult.isFailure) {
                    Timber.tag(LogTags.SERVICE).w("⚠️ Failed to attach images: ${attachResult.exceptionOrNull()?.message}")
                } else {
                    Timber.tag(LogTags.SERVICE).d("✅ Images attached")
                }
            }

            Timber.tag(LogTags.SERVICE).d("🔄 Step 3: Updating item on server...")

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
                status = domainItem.status as String?
            )

            if (updateResult.isFailure) {
                val error = updateResult.exceptionOrNull()?.message ?: "Unknown error"
                Timber.tag(LogTags.SERVICE).e("❌ API update failed: $error")
                updateItemStatus(itemId, AppConstants.SyncStatus.UPDATE_FAILED, error)

                if (error.contains("network", ignoreCase = true) ||
                    error.contains("timeout", ignoreCase = true) ||
                    error.contains("502", ignoreCase = true)) {
                    Timber.tag(LogTags.SERVICE).d("🔁 Retryable error")
                    return ListenableWorker.Result.retry()
                }
                return ListenableWorker.Result.failure()
            }

            val serverItem = updateResult.getOrNull()!!
            Timber.tag(LogTags.SERVICE).d("✅ Item updated on server:")
            Timber.tag(LogTags.SERVICE).d("   Server ID: ${serverItem.id}")
            Timber.tag(LogTags.SERVICE).d("   Status: ${serverItem.status}")

            Timber.tag(LogTags.SERVICE).d("🔄 Step 4: Updating local item...")
            val updatedEntity = localItem.copy(
                syncStatus = AppConstants.SyncStatus.SYNCED,
                syncError = null,
                retryCount = 0,
                lastSyncAttempt = System.currentTimeMillis(),
                updatedAt = serverItem.updatedAt,
                price = serverItem.price,
                quantity = serverItem.quantity,
                status = serverItem.status as String
            )
            itemDao.insertItem(updatedEntity)
            Timber.tag(LogTags.SERVICE).d("✅ Local item updated, syncStatus: ${AppConstants.SyncStatus.SYNCED}")

            Timber.tag(LogTags.SERVICE).d("🏁 EditItemWorker.doWork() SUCCESS")
            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(LogTags.SERVICE).e(e, "❌ Worker failed with exception")
            updateItemStatus(itemId, AppConstants.SyncStatus.UPDATE_FAILED, e.message)

            when {
                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ||
                        e.message?.contains("502", ignoreCase = true) == true -> {
                    Timber.tag(LogTags.SERVICE).d("🔁 Retryable error, will retry")
                    ListenableWorker.Result.retry()
                }
                else -> {
                    Timber.tag(LogTags.SERVICE).d("❌ Non-retryable error")
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
                    Timber.tag(LogTags.SERVICE).d("📝 Updated item status: $status (retry: ${updated.retryCount})")
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.SERVICE).e(e, "Failed to update status")
            }
        }
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val KEY_ADD_IMAGE_URIS = "add_image_uris"
        const val KEY_REMOVE_IMAGE_IDS = "remove_image_ids"

        fun createOneTimeRequest(
            itemId: String,
            addImageUris: List<Uri> = emptyList(),
            removeImageIds: List<Long> = emptyList()
        ): OneTimeWorkRequest {
            Timber.tag(LogTags.SERVICE).d("📦 Creating EditItemWorker for: $itemId")
            Timber.tag(LogTags.SERVICE).d("   Add images: ${addImageUris.size}")
            Timber.tag(LogTags.SERVICE).d("   Remove image IDs: ${removeImageIds.size}")

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
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.SECONDS
                )
                .addTag(WORKER_TAG)
                .build()
        }
    }
}