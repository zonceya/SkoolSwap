package za.co.skoolswap.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import za.co.skoolswap.domain.repository.ItemCreationWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ItemCreationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val entryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        ItemCreationWorkerEntryPoint::class.java
    )

    private val itemDao = entryPoint.itemDao()
    private val itemImageDao = entryPoint.itemImageDao()
    private val itemRepository = entryPoint.itemRepository()

    override suspend fun doWork(): ListenableWorker.Result {
        Timber.tag(TAG).d("🚀 ItemCreationWorker.doWork() START")
        Timber.tag(TAG).d("   Thread: ${Thread.currentThread().name}")

        val itemId = inputData.getString(KEY_ITEM_ID) ?: run {
            Timber.tag(TAG).e("❌ No item ID provided in input data")
            Timber.tag(TAG).d("   Input data keys: ${inputData.keyValueMap.keys}")
            return ListenableWorker.Result.failure()
        }
        Timber.tag(TAG).d("📦 Item ID from input: $itemId")

        // ✅ Get image URIs from input data
        val imageUriStrings = inputData.getStringArray(KEY_IMAGE_URIS)
        Timber.tag(TAG).d("📸 Image URI strings from input: ${imageUriStrings?.size ?: 0}")

        val imageUris = imageUriStrings?.mapNotNull {
            try {
                val uri = Uri.parse(it)
                Timber.tag(TAG).d("   Parsed URI: $uri")
                uri
            } catch (e: Exception) {
                Timber.tag(TAG).w("   Failed to parse URI: $it - ${e.message}")
                null
            }
        } ?: emptyList()

        Timber.tag(TAG).d("📸 Valid image URIs count: ${imageUris.size}")
        if (imageUris.isNotEmpty()) {
            imageUris.forEachIndexed { index, uri ->
                Timber.tag(TAG).d("   Image[$index]: $uri")
            }
        }

        Timber.tag(TAG).d("🔄 Starting background creation for item: $itemId")
        Timber.tag(TAG).d("   Images: ${imageUris.size}")

        return try {
            // 1. Get local item
            Timber.tag(TAG).d("🔍 Step 1: Fetching local item from database...")
            val localItem = itemDao.getItemById(itemId)

            if (localItem == null) {
                Timber.tag(TAG).e("❌ Item not found in database: $itemId")
                Timber.tag(TAG).d("   Item may have been deleted or never saved")
                return ListenableWorker.Result.failure()
            }

            Timber.tag(TAG).d("✅ Local item found:")
            Timber.tag(TAG).d("   ID: ${localItem.id}")
            Timber.tag(TAG).d("   Name: ${localItem.name}")
            Timber.tag(TAG).d("   Description: ${localItem.description}")
            Timber.tag(TAG).d("   Price: ${localItem.price}")
            Timber.tag(TAG).d("   Quantity: ${localItem.quantity}")
            Timber.tag(TAG).d("   Status: ${localItem.status}")
            Timber.tag(TAG).d("   MainCategoryId: ${localItem.mainCategoryId}")
            Timber.tag(TAG).d("   SubCategoryId: ${localItem.subCategoryId}")
            Timber.tag(TAG).d("   BrandId: ${localItem.brandId}")
            Timber.tag(TAG).d("   SizeId: ${localItem.sizeId}")
            Timber.tag(TAG).d("   SchoolId: ${localItem.schoolId}")
            Timber.tag(TAG).d("   ConditionId: ${localItem.itemConditionId}")
            Timber.tag(TAG).d("   ProvinceId: ${localItem.provinceId}")
            Timber.tag(TAG).d("   LocationId: ${localItem.locationId}")
            Timber.tag(TAG).d("   GenderId: ${localItem.genderId}")
            Timber.tag(TAG).d("   ColorId: ${localItem.colorId}")
            Timber.tag(TAG).d("   SyncStatus: ${localItem.syncStatus}")
            Timber.tag(TAG).d("   RetryCount: ${localItem.retryCount}")

            // 2. Create item on server (without images)
            Timber.tag(TAG).d("🔄 Step 2: Creating item on server...")
            Timber.tag(TAG).d("   Request parameters:")
            Timber.tag(TAG).d("   name: ${localItem.name}")
            Timber.tag(TAG).d("   description: ${localItem.description}")
            Timber.tag(TAG).d("   mainCategoryId: ${localItem.mainCategoryId ?: 0}")
            Timber.tag(TAG).d("   subCategoryId: ${localItem.subCategoryId ?: 0}")
            Timber.tag(TAG).d("   brandId: ${localItem.brandId}")
            Timber.tag(TAG).d("   price: ${localItem.price}")
            Timber.tag(TAG).d("   quantity: ${localItem.quantity}")
            Timber.tag(TAG).d("   conditionId: ${localItem.itemConditionId}")
            Timber.tag(TAG).d("   provinceId: ${localItem.provinceId}")
            Timber.tag(TAG).d("   locationId: ${localItem.locationId}")
            Timber.tag(TAG).d("   genderId: ${localItem.genderId}")
            Timber.tag(TAG).d("   schoolId: ${localItem.schoolId}")
            Timber.tag(TAG).d("   sizeId: ${localItem.sizeId}")
            Timber.tag(TAG).d("   colorId: ${localItem.colorId}")

            val createStartTime = System.currentTimeMillis()
            val createResult = itemRepository.createItemSimple(
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
            val createDuration = System.currentTimeMillis() - createStartTime
            Timber.tag(TAG).d("   API call completed in ${createDuration}ms")

            if (createResult.isFailure) {
                val error = createResult.exceptionOrNull()?.message ?: "Unknown error"
                Timber.tag(TAG).e("❌ API creation failed after ${createDuration}ms")
                Timber.tag(TAG).d("   Error: $error")
                updateItemStatus(itemId, "FAILED", error)
                return ListenableWorker.Result.retry()
            }

            val serverItem = createResult.getOrNull()!!
            Timber.tag(TAG).d("✅ Item created on server successfully!")
            Timber.tag(TAG).d("   Server ID: ${serverItem.id}")
            Timber.tag(TAG).d("   Server Name: ${serverItem.name}")
            Timber.tag(TAG).d("   Server Price: ${serverItem.price}")
            Timber.tag(TAG).d("   Server Status: ${serverItem.status}")
            Timber.tag(TAG).d("   Server ShopId: ${serverItem.shopId}")
            Timber.tag(TAG).d("   Server CreatedAt: ${serverItem.createdAt}")
            Timber.tag(TAG).d("   Server UpdatedAt: ${serverItem.updatedAt}")

            // 3. ✅ UPLOAD IMAGES DIRECTLY USING addItemImages
            if (imageUris.isNotEmpty()) {
                Timber.tag(TAG).d("📤 Step 3: Uploading ${imageUris.size} images directly to item...")
                val uploadStartTime = System.currentTimeMillis()

                try {
                    val imageResult = itemRepository.addItemImages(
                        context = applicationContext,
                        itemId = serverItem.id,  // ← Use server item ID
                        imageUris = imageUris
                    )

                    val uploadDuration = System.currentTimeMillis() - uploadStartTime
                    Timber.tag(TAG).d("   Upload completed in ${uploadDuration}ms")

                    if (imageResult.isSuccess) {
                        val images = imageResult.getOrNull() ?: emptyList()
                        Timber.tag(TAG).d("✅ Uploaded ${images.size} images successfully!")
                        images.forEachIndexed { index, image ->
                            Timber.tag(TAG).d("   Image[$index]: ${image.url}")
                        }
                    } else {
                        val error = imageResult.exceptionOrNull()?.message ?: "Unknown error"
                        Timber.tag(TAG).w("⚠️ Image upload failed: $error")
                        Timber.tag(TAG).d("   This is a warning - item was created but images may not be visible")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "❌ Image upload exception")
                    Timber.tag(TAG).d("   Item was created, images can be added later")
                    // Don't fail - item was created successfully
                }
            } else {
                Timber.tag(TAG).d("📤 Step 3: No images to upload, skipping")
            }

            // 4. Update local item with server data
            Timber.tag(TAG).d("🔄 Step 4: Updating local item with server data...")
            val updatedEntity = localItem.copy(
                id = serverItem.id,
                shopId = serverItem.shopId,
                syncStatus = "ACTIVE",
                syncError = null,
                retryCount = 0,
                lastSyncAttempt = System.currentTimeMillis(),
                price = serverItem.price,
                quantity = serverItem.quantity,
                status = serverItem.status as String,
                createdAt = serverItem.createdAt,
                updatedAt = serverItem.updatedAt
            )
            itemDao.insertItem(updatedEntity)
            Timber.tag(TAG).d("✅ Local item updated:")
            Timber.tag(TAG).d("   ID: ${updatedEntity.id} (was: $itemId)")
            Timber.tag(TAG).d("   SyncStatus: ${updatedEntity.syncStatus}")
            Timber.tag(TAG).d("   RetryCount: ${updatedEntity.retryCount}")
            Timber.tag(TAG).d("   LastSyncAttempt: ${updatedEntity.lastSyncAttempt}")

            // 5. Delete placeholder
            if (itemId != serverItem.id) {
                Timber.tag(TAG).d("🔄 Step 5: Deleting placeholder item (ID: $itemId)...")
                itemDao.deleteItemById(itemId)
                itemImageDao.deleteImagesForItem(itemId)
                Timber.tag(TAG).d("✅ Placeholder deleted")
            } else {
                Timber.tag(TAG).d("🔄 Step 5: No placeholder to delete (IDs match)")
            }

            Timber.tag(TAG).d("✅ Item creation completed: ${serverItem.id}")
            Timber.tag(TAG).d("🏁 ItemCreationWorker.doWork() SUCCESS")
            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Worker failed with exception")
            Timber.tag(TAG).d("   Exception type: ${e.javaClass.simpleName}")
            Timber.tag(TAG).d("   Exception message: ${e.message}")
            if (e.stackTrace.isNotEmpty()) {
                Timber.tag(TAG).d("   Stack trace (first 3 lines):")
                e.stackTrace.take(3).forEachIndexed { index, element ->
                    Timber.tag(TAG).d("      [$index] $element")
                }
            }

            updateItemStatus(itemId, "FAILED", e.message)

            when {
                e.message?.contains("network", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("🔁 Network error detected - will retry")
                    ListenableWorker.Result.retry()
                }
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("🔁 Timeout detected - will retry")
                    ListenableWorker.Result.retry()
                }
                e.message?.contains("401", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("🔑 Authentication error (401) - may need new token")
                    ListenableWorker.Result.retry()
                }
                e.message?.contains("422", ignoreCase = true) == true -> {
                    Timber.tag(TAG).d("❌ Validation error (422) - non-retryable")
                    ListenableWorker.Result.failure()
                }
                else -> {
                    Timber.tag(TAG).d("❌ Non-retryable error")
                    ListenableWorker.Result.failure()
                }
            }
        }
    }

    private suspend fun updateItemStatus(itemId: String, status: String, error: String? = null) {
        Timber.tag(TAG).d("📝 updateItemStatus called: itemId=$itemId, status=$status")
        Timber.tag(TAG).d("   Error: $error")

        withContext(Dispatchers.IO) {
            try {
                val item = itemDao.getItemById(itemId)
                if (item != null) {
                    val newRetryCount = item.retryCount + 1
                    val updated = item.copy(
                        syncStatus = status,
                        syncError = error,
                        lastSyncAttempt = System.currentTimeMillis(),
                        retryCount = newRetryCount
                    )
                    itemDao.insertItem(updated)
                    Timber.tag(TAG).d("📝 Updated item status: $status (retry: $newRetryCount)")
                    if (error != null) {
                        Timber.tag(TAG).d("   Error message: $error")
                    }
                } else {
                    Timber.tag(TAG).w("⚠️ Cannot update status - item not found: $itemId")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Failed to update status for item $itemId")
            }
        }
    }

    companion object {
        private const val TAG = "ItemCreationWorker"
        const val KEY_ITEM_ID = "item_id"
        const val KEY_IMAGE_URIS = "image_uris"

        // ✅ Single parameter version (for retries or items without images)
        fun createOneTimeRequest(itemId: String): OneTimeWorkRequest {
            Timber.tag(TAG).d("📦 createOneTimeRequest called (single param)")
            Timber.tag(TAG).d("   Item ID: $itemId")

            val inputData = androidx.work.Data.Builder()
                .putString(KEY_ITEM_ID, itemId)
                .build()

            val request = OneTimeWorkRequest.Builder(ItemCreationWorker::class.java)
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

            Timber.tag(TAG).d("✅ Worker created with ID: ${request.id}")
            Timber.tag(TAG).d("   Tags: ${request.tags}")
            return request
        }

        // ✅ Version with image URIs
        fun createOneTimeRequest(itemId: String, imageUris: List<Uri>): OneTimeWorkRequest {
            Timber.tag(TAG).d("📦 createOneTimeRequest called (with images)")
            Timber.tag(TAG).d("   Item ID: $itemId")
            Timber.tag(TAG).d("   Image URIs: ${imageUris.size}")

            val uriStrings = imageUris.map {
                Timber.tag(TAG).d("   URI: $it")
                it.toString()
            }.toTypedArray()

            val inputData = androidx.work.Data.Builder()
                .putString(KEY_ITEM_ID, itemId)
                .putStringArray(KEY_IMAGE_URIS, uriStrings as Array<String?>)
                .build()

            val request = OneTimeWorkRequest.Builder(ItemCreationWorker::class.java)
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

            Timber.tag(TAG).d("✅ Worker created with ID: ${request.id}")
            Timber.tag(TAG).d("   Tags: ${request.tags}")

            val imageCount = inputData.getStringArray(KEY_IMAGE_URIS)?.size ?: 0
            Timber.tag(TAG).d("   Image URIs in data: $imageCount")

            return request
        }
    }
}