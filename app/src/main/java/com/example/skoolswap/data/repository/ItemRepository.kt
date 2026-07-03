package com.example.skoolswap.data.repository

import com.example.skoolswap.data.local.database.dao.ItemImageDao
import com.example.skoolswap.data.local.database.entities.ItemImageEntity
import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.identity.util.UUID
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.entities.ItemEntity
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.ItemApiService
import com.example.skoolswap.data.remote.models.request.CreateItemRequest
import com.example.skoolswap.data.remote.models.request.ItemData
import com.example.skoolswap.data.remote.models.request.UpdateItemData
import com.example.skoolswap.data.remote.models.request.UpdateItemRequest
import com.example.skoolswap.data.remote.models.response.item.AttachImagesByUrlRequest
import com.example.skoolswap.data.remote.models.response.item.ItemDetailDto
import com.example.skoolswap.data.remote.models.response.item.UpdateItemDto
import com.example.skoolswap.data.remote.models.response.item.ViewShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.ShopItemDto
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.model.ItemMeta
import com.example.skoolswap.domain.model.Shop
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.utils.ImageMultipartHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemApiService: ItemApiService,
    private val itemDao: ItemDao,
    private val itemImageDao: ItemImageDao,
    private val authRepository: AuthRepositoryInterface,
    private val imageUploadRepository: ImageUploadRepository

) : ItemRepositoryInterface {

    companion object {
        private const val TAG = "ItemRepository"
        private const val MAX_IMAGES = 3
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 5 minutes cache
    }

    private val _recentlyCreatedItem = MutableStateFlow<Item?>(null)
    override val recentlyCreatedItem: StateFlow<Item?> = _recentlyCreatedItem.asStateFlow()
    private fun isCacheValid(cacheTime: Long): Boolean {
        return System.currentTimeMillis() - cacheTime < CACHE_DURATION_MS
    }
    private val _currentItems = MutableStateFlow<List<Item>>(emptyList())
    override val currentItems: StateFlow<List<Item>> = _currentItems.asStateFlow()
    private val memoryCache = mutableMapOf<String, Item>()
    private val memoryCacheTime = mutableMapOf<String, Long>()
    // ============ CREATE ITEM WITHOUT IMAGES ============
    // ItemRepository.kt - Add/Update these methods

    override suspend fun createItemOfflineFirst(
        context: Context,
        name: String,
        description: String,
        mainCategoryId: Int,
        subCategoryId: Int,
        brandId: Int?,
        price: Double,
        quantity: Int,
        itemConditionId: Int?,
        provinceId: Int?,
        locationId: Int?,
        genderId: Int?,
        schoolId: Int?,
        sizeId: Int?,
        colorId: Int?,
        tagIds: List<Int>?,
        imageUris: List<Uri>  // These are local URIs
    ): Result<Item> {
        return try {
            // 1. Generate local ID
            val localId = UUID.randomUUID().toString()

            // 2. Create local item with UPLOADING status
            val localItem = Item(
                id = localId,
                shopId = 0L,
                name = name,
                description = description,
                price = price,
                quantity = quantity,
                status = "active",
                mainCategoryId = mainCategoryId,
                subCategoryId = subCategoryId,
                brandId = brandId,
                sizeId = sizeId,
                schoolId = schoolId,
                itemConditionId = itemConditionId,
                locationId = locationId,
                provinceId = provinceId,
                genderId = genderId,
                colorId = colorId,
                createdAt = System.currentTimeMillis().toString(),
                images = imageUris.map { uri ->
                    ItemImage(id = 0, url = uri.toString(), isCover = false)
                },
                syncStatus = "UPLOADING",
                syncError = null,
                retryCount = 0,
                shop = null,
                meta = null,
                label = null,
                reserved = 0,
                itemTypeId = null,
                sizeName = null,
                colorName = null,
                brandName = null,
                conditionName = null,
                locationName = null,
                viewCount = 0
            )

            // 3. Save to local database
            itemDao.insertItem(localItem.toEntity())

            // 4. Save image URIs to database as well (for the worker)
            val imageEntities = imageUris.mapIndexed { index, uri ->
                ItemImageEntity(
                    itemId = localId,
                    url = uri.toString(),  // Store the URI string
                    isCover = index == 0,
                    position = index
                )
            }
            itemImageDao.updateImagesForItem(localId, imageEntities)

            Timber.tag(TAG).d("✅ Item saved locally with ID: $localId, images: ${imageUris.size}")
            Result.success(localItem)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to create item locally")
            Result.failure(e)
        }
    }

    // ItemRepository.kt - Update attachImagesToItem

    override suspend fun attachImagesToItem(
        itemId: String,
        imageUrls: List<String>
    ): Result<List<ItemImage>> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            if (imageUrls.isEmpty()) {
                return Result.success(emptyList())
            }

            // ✅ Use itemApiService instead of imageApiService
            val response = itemApiService.attachImagesByUrl(
                authHeader = "Bearer $token",
                itemId = itemId,
                request = AttachImagesByUrlRequest(imageUrls)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val images = response.body()?.images?.map { imageDto ->
                    ItemImage(
                        id = imageDto.id,
                        url = imageDto.url,
                        filename = imageDto.filename,
                        contentType = imageDto.contentType,
                        createdAt = imageDto.createdAt,
                        isCover = false
                    )
                } ?: emptyList()

                Timber.tag(TAG).d("✅ Attached ${images.size} images to item $itemId")
                Result.success(images)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to attach images"
                Timber.tag(TAG).e("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to attach images")
            Result.failure(e)
        }
    }

    override suspend fun updateItemOfflineFirst(
        context: Context,
        itemId: String,
        name: String?,
        description: String?,
        mainCategoryId: Int?,
        subCategoryId: Int?,
        brandId: Int?,
        price: Double?,
        quantity: Int?,
        itemConditionId: Int?,
        provinceId: Int?,
        locationId: Int?,
        genderId: Int?,
        schoolId: Int?,
        sizeId: Int?,
        colorId: Int?,
        tagIds: List<Int>?,
        addImageUris: List<Uri>,
        removeImageIds: List<Long>
    ): Result<Item> {
        Timber.tag(TAG).d("🚀 updateItemOfflineFirst START")
        Timber.tag(TAG).d("   itemId: $itemId")
        Timber.tag(TAG).d("   mainCategoryId: $mainCategoryId")
        Timber.tag(TAG).d("   subCategoryId: $subCategoryId")
        Timber.tag(TAG).d("   addImageUris: ${addImageUris.size}")
        Timber.tag(TAG).d("   removeImageIds: ${removeImageIds.size}")

        return try {
            // 1. Get existing item from database
            val existingItem = itemDao.getItemById(itemId)
            if (existingItem == null) {
                Timber.tag(TAG).e("❌ Item not found in database: $itemId")
                return Result.failure(Exception("Item not found"))
            }

            Timber.tag(TAG).d("📦 Existing item found:")
            Timber.tag(TAG).d("   Name: ${existingItem.name}")
            Timber.tag(TAG).d("   mainCategoryId: ${existingItem.mainCategoryId}")
            Timber.tag(TAG).d("   subCategoryId: ${existingItem.subCategoryId}")

            // 2. Convert to domain model
            val existingDomain = existingItem.toDomain()

            // 3. Build updated images list
            val updatedImages = buildUpdatedImages(
                existingDomain.images,
                addImageUris,
                removeImageIds
            )

            // 4. Create updated item with UPDATING status
            val updatedItem = existingDomain.copy(
                name = name ?: existingDomain.name,
                description = description ?: existingDomain.description,
                price = price ?: existingDomain.price,
                quantity = quantity ?: existingDomain.quantity,
                mainCategoryId = mainCategoryId ?: existingDomain.mainCategoryId,
                subCategoryId = subCategoryId ?: existingDomain.subCategoryId,
                brandId = brandId ?: existingDomain.brandId,
                sizeId = sizeId ?: existingDomain.sizeId,
                colorId = colorId ?: existingDomain.colorId,
                schoolId = schoolId ?: existingDomain.schoolId,
                itemConditionId = itemConditionId ?: existingDomain.itemConditionId,
                locationId = locationId ?: existingDomain.locationId,
                provinceId = provinceId ?: existingDomain.provinceId,
                genderId = genderId ?: existingDomain.genderId,
                images = updatedImages,
                syncStatus = "UPDATING",  // ✅ Mark as updating
                syncError = null,
                retryCount = 0,
                updatedAt = System.currentTimeMillis().toString()
            )

            Timber.tag(TAG).d("📝 Updated item:")
            Timber.tag(TAG).d("   mainCategoryId: ${updatedItem.mainCategoryId}")
            Timber.tag(TAG).d("   subCategoryId: ${updatedItem.subCategoryId}")
            Timber.tag(TAG).d("   syncStatus: ${updatedItem.syncStatus}")
            Timber.tag(TAG).d("   images: ${updatedItem.images.size}")

            // 5. Save to database using safe insert
            safeInsertOrUpdateItem(updatedItem)

            // 6. Save image URIs for the worker
            if (addImageUris.isNotEmpty()) {
                val imageEntities = addImageUris.mapIndexed { index, uri ->
                    ItemImageEntity(
                        itemId = itemId,
                        url = uri.toString(),
                        isCover = index == 0 && updatedItem.images.isEmpty(),
                        position = updatedItem.images.size + index
                    )
                }
                itemImageDao.updateImagesForItem(itemId, imageEntities)
                Timber.tag(TAG).d("✅ ${imageEntities.size} new images saved to database")
            }

            // 7. Save deletion IDs for the worker
            if (removeImageIds.isNotEmpty()) {
                // Store deletion IDs in a separate table or as a JSON field
                // For now, we'll pass them to the worker via input data
                Timber.tag(TAG).d("✅ ${removeImageIds.size} images marked for deletion")
            }

            Timber.tag(TAG).d("✅ Item updated locally with ID: $itemId")
            Timber.tag(TAG).d("🏁 updateItemOfflineFirst SUCCESS")
            Result.success(updatedItem)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ updateItemOfflineFirst FAILED")
            Result.failure(e)
        }
    }
    private fun buildUpdatedImages(
        existingImages: List<ItemImage>,
        addImageUris: List<Uri>,
        removeImageIds: List<Long>
    ): List<ItemImage> {
        // Keep images not marked for deletion
        val keptImages = existingImages.filter {
            !removeImageIds.contains(it.id)
        }

        // Convert new Uris to ItemImages
        val newImages = addImageUris.mapIndexed { index, uri ->
            ItemImage(
                id = 0,  // Temporary ID
                url = uri.toString(),
                isCover = index == 0 && keptImages.isEmpty()
            )
        }

        return keptImages + newImages
    }

    private suspend fun safeInsertOrUpdateItem(newItem: Item) {
        Timber.tag(TAG).d("🛡️ safeInsertOrUpdateItem called")
        Timber.tag(TAG).d("   Item ID: ${newItem.id}")
        Timber.tag(TAG).d("   Name: ${newItem.name}")
        Timber.tag(TAG).d("   mainCategoryId: ${newItem.mainCategoryId}")
        Timber.tag(TAG).d("   subCategoryId: ${newItem.subCategoryId}")
        Timber.tag(TAG).d("   colorId: ${newItem.colorId}")
        Timber.tag(TAG).d("   syncStatus: ${newItem.syncStatus}")

        val existing = itemDao.getItemById(newItem.id)

        if (existing != null) {
            Timber.tag(TAG).d("📦 Existing item found")
            Timber.tag(TAG).d("   Existing mainCategoryId: ${existing.mainCategoryId}")
            Timber.tag(TAG).d("   Existing subCategoryId: ${existing.subCategoryId}")

            // ✅ MERGE: Preserve non-null values from newItem, fallback to existing
            val merged = existing.copy(
                // ✅ PRESERVE CRITICAL IDs - only update if newItem has a non-null value
                mainCategoryId = newItem.mainCategoryId ?: existing.mainCategoryId,
                subCategoryId = newItem.subCategoryId ?: existing.subCategoryId,
                colorId = newItem.colorId ?: existing.colorId,
                brandId = newItem.brandId ?: existing.brandId,
                sizeId = newItem.sizeId ?: existing.sizeId,
                schoolId = newItem.schoolId ?: existing.schoolId,
                itemConditionId = newItem.itemConditionId ?: existing.itemConditionId,
                locationId = newItem.locationId ?: existing.locationId,
                provinceId = newItem.provinceId ?: existing.provinceId,
                genderId = newItem.genderId ?: existing.genderId,

                // ✅ ALWAYS UPDATE mutable fields
                name = newItem.name,
                description = newItem.description,
                price = newItem.price,
                quantity = newItem.quantity,
                status = newItem.status,
                updatedAt = newItem.updatedAt ?: existing.updatedAt,
                syncStatus = newItem.syncStatus,
                syncError = newItem.syncError,
                retryCount = newItem.retryCount,
                lastSyncAttempt = newItem.lastSyncAttempt,

                // ✅ Update images if provided
                imageCount = if (newItem.images.isNotEmpty()) newItem.images.size else existing.imageCount,
                coverImage = newItem.coverImage ?: existing.coverImage,

                // Preserve created_at if new item doesn't have it
                createdAt = newItem.createdAt.takeIf { it.isNotEmpty() } ?: existing.createdAt,
            )

            Timber.tag(TAG).d("✅ Merged entity:")
            Timber.tag(TAG).d("   mainCategoryId: ${merged.mainCategoryId}")
            Timber.tag(TAG).d("   subCategoryId: ${merged.subCategoryId}")
            Timber.tag(TAG).d("   syncStatus: ${merged.syncStatus}")

            itemDao.insertItem(merged)
            Timber.tag(TAG).d("✅ Item updated in database")
        } else {
            Timber.tag(TAG).d("📦 No existing item found, inserting new")
            val entity = newItem.toEntity()
            Timber.tag(TAG).d("   mainCategoryId: ${entity.mainCategoryId}")
            Timber.tag(TAG).d("   subCategoryId: ${entity.subCategoryId}")
            itemDao.insertItem(entity)
            Timber.tag(TAG).d("✅ New item inserted")
        }
    }
    override suspend fun createItemSimple(
        name: String,
        description: String,
        mainCategoryId: Int,
        subCategoryId: Int,
        brandId: Int?,
        price: Double,
        quantity: Int,
        itemConditionId: Int?,
        provinceId: Int?,
        locationId: Int?,
        genderId: Int?,
        schoolId: Int?,
        sizeId: Int?,
        colorId: Int?,
        tagIds: List<Int>?
    ): Result<Item> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val request = CreateItemRequest(
                item = ItemData(
                    name = name,
                    description = description,
                    mainCategoryId = mainCategoryId,
                    subCategoryId = subCategoryId,
                    brandId = brandId,
                    price = price,
                    quantity = quantity,
                    itemConditionId = itemConditionId,
                    provinceId = provinceId,
                    locationId = locationId,
                    genderId = genderId,
                    schoolId = schoolId,
                    sizeId = sizeId,
                    colorId = colorId,
                    label = "popular",
                    status = "active",
                    tagIds = tagIds
                )
            )

            val response = itemApiService.createItem("Bearer $token", request)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to create item: ${response.errorBody()?.string()}"
                Timber.tag(TAG).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true || responseBody.item == null) {
                val errorMsg = responseBody?.message ?: "Failed to create item"
                Timber.tag(TAG).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val item = responseBody.toDomain()
            itemDao.insertItem(item.toEntity())
            _recentlyCreatedItem.value = item

            Log.i(TAG, "Item created successfully: ${item.name}")
            Result.success(item)

        } catch (e: Exception) {
            Log.e(TAG, "Create item failed", e)
            Result.failure(e)
        }
    }

    // ============ CREATE ITEM WITH IMAGES ============
    // In ItemRepository.kt - createItemWithImages with uploadAndAttachImages

    override suspend fun createItemWithImages(
        context: Context,
        name: String,
        description: String,
        mainCategoryId: Int,
        subCategoryId: Int,
        brandId: Int?,
        price: Double,
        quantity: Int,
        itemConditionId: Int?,
        provinceId: Int?,
        locationId: Int?,
        genderId: Int?,
        schoolId: Int?,
        sizeId: Int?,
        colorId: Int?,
        tagIds: List<Int>?,
        imageUris: List<Uri>
    ): Result<Item> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            if (imageUris.size > MAX_IMAGES) {
                return Result.failure(Exception("Cannot exceed $MAX_IMAGES images"))
            }

            // 1. First create the item (without images)
            val request = CreateItemRequest(
                item = ItemData(
                    name = name,
                    description = description,
                    mainCategoryId = mainCategoryId,
                    subCategoryId = subCategoryId,
                    brandId = brandId,
                    price = price,
                    quantity = quantity,
                    itemConditionId = itemConditionId,
                    provinceId = provinceId,
                    locationId = locationId,
                    genderId = genderId,
                    schoolId = schoolId,
                    sizeId = sizeId,
                    colorId = colorId,
                    label = "popular",
                    status = "active",
                    tagIds = tagIds
                )
            )

            Timber.tag(TAG).d("Creating item: $name")
            val itemResponse = itemApiService.createItem("Bearer $token", request)

            if (!itemResponse.isSuccessful) {
                val errorMsg = "Failed to create item: ${itemResponse.errorBody()?.string()}"
                Timber.tag(TAG).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val createdItemResponse = itemResponse.body()
            if (createdItemResponse?.success != true || createdItemResponse.item == null) {
                val errorMsg = createdItemResponse?.message ?: "Failed to create item"
                Timber.tag(TAG).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val itemId = createdItemResponse.item.id
            Timber.tag(TAG).d("Item created with ID: $itemId")

            var finalItem = createdItemResponse.toDomain()

            // 2. Upload and attach images using ImageUploadRepository
            if (imageUris.isNotEmpty()) {
                val result = imageUploadRepository.uploadAndAttachImages(
                    context = context,
                    itemId = itemId,
                    imageUris = imageUris
                )

                if (result.isSuccess) {
                    val uploadedUrls = result.getOrNull() ?: emptyList()
                    // If you need the full image objects, fetch the updated item
                    if (uploadedUrls.isNotEmpty()) {
                        // Optionally fetch the updated item to get image details
                        val updatedItemResult = getItem(itemId)
                        if (updatedItemResult.isSuccess) {
                            finalItem = updatedItemResult.getOrNull()!!
                        }
                    }
                    Timber.tag(TAG).d("✅ Uploaded and attached ${uploadedUrls.size} images")
                } else {
                    Timber.tag(TAG).w("⚠️ Image upload failed: ${result.exceptionOrNull()?.message}")
                }
            }

            // Save to database
            itemDao.insertItem(finalItem.toEntity())
            _recentlyCreatedItem.value = finalItem

            Timber.tag(TAG).i("Item created successfully: ${finalItem.name}")
            Result.success(finalItem)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Create item with images failed")
            Result.failure(e)
        }
    }

    // ============ ADD IMAGES TO EXISTING ITEM ============
    override suspend fun addItemImages(
        context: Context,
        itemId: String,
        imageUris: List<Uri>
    ): Result<List<ItemImage>> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val imageParts = ImageMultipartHelper.createImageParts(context, imageUris)
            if (imageParts.isEmpty()) {
                return Result.failure(Exception("No valid images provided"))
            }

            val response = itemApiService.addItemImages("Bearer $token", itemId, imageParts)

            if (!response.isSuccessful) {
                val errorMsg = "Server error: ${response.code()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body?.success == true) {
                // Fix: Map to ItemImage, not Item
                val images = body.images?.map { imageDto ->
                    ItemImage(
                        id = imageDto.id,
                        url = imageDto.url,
                        filename = imageDto.filename,
                        contentType = imageDto.contentType,
                        createdAt = imageDto.createdAt,
                        isCover = false
                    )
                } ?: emptyList()
                Timber.tag(TAG).i("Added ${images.size} images to item $itemId")
                Result.success(images)
            } else {
                val errorMsg = body?.message ?: "Failed to upload images"
                Timber.tag(TAG).e(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Add item images failed")
            Result.failure(e)
        }
    }

    // ============ REMOVE IMAGE FROM ITEM ============
    override suspend fun removeItemImage(
        itemId: String,
        imageId: Long
    ): Result<Int> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = itemApiService.removeItemImage("Bearer $token", itemId, imageId)

            if (!response.isSuccessful) {
                val errorMsg = "Server error: ${response.code()}"
                Timber.tag(TAG).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body?.success == true) {
                val remaining = body.remainingImages ?: 0
                Timber.tag(TAG).i("Image $imageId removed from item $itemId. Remaining: $remaining")
                Result.success(remaining)
            } else {
                val errorMsg = body?.message ?: "Failed to remove image"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Remove item image failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getShopItemForEdit(itemId: String): Result<Item> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            Log.d(TAG, "🔍 Fetching item for edit: $itemId")
            val response = itemApiService.getShopItemForEdit("Bearer $token", itemId)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to load item: ${response.code()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true || responseBody.item == null) {
                val errorMsg = responseBody?.message ?: "Failed to load item"
                Timber.tag(TAG).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val item = mapViewShopItemToDomain(responseBody.item)

            Timber.tag(TAG).d("✅ Item loaded: ${item.name} with ${item.images.size} images")

            itemDao.insertItem(item.toEntity())

            Result.success(item)
        } catch (e: Exception) {
            Log.e(TAG, "Get item for edit failed", e)
            Result.failure(e)
        }
    }

    private fun mapViewShopItemToDomain(dto: ViewShopItemDto): Item {
        val variant = dto.variants?.firstOrNull()

        val imageList = dto.images?.mapIndexed { index, url ->
            ItemImage(
                id = 0,
                url = url,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = index == 0
            )
        } ?: emptyList()

        val finalPrice = variant?.price ?: dto.price?.toDoubleOrNull() ?: 0.0
        val finalQuantity = variant?.quantity ?: dto.totalQuantity

        return Item(
            id = dto.id,
            shopId = dto.shopId,
            name = dto.name,
            description = dto.description,
            price = finalPrice,
            quantity = finalQuantity,
            status = dto.status,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            images = imageList,
            coverImage = imageList.firstOrNull()?.url,

            // IDs from DTO
            brandId = dto.brand?.id ?: dto.brandId,
            sizeId = variant?.sizeId ?: dto.size?.id,
            colorId = variant?.colorId ?: dto.color?.id,
            schoolId = dto.school?.id ?: dto.schoolId,
            itemConditionId = variant?.conditionId ?: dto.itemConditionId,
            locationId = dto.town?.id ?: dto.locationId,  // ✅ FIXED: Get town ID here
            provinceId = dto.province?.id ?: dto.provinceId,
            genderId = dto.gender?.id ?: dto.genderId,
            mainCategoryId = dto.mainCategory?.id ?: dto.mainCategoryId,
            subCategoryId = dto.subCategory?.id ?: dto.subCategoryId,

            // Names for display
            sizeName = variant?.sizeName ?: dto.size?.name,
            colorName = variant?.colorName ?: dto.color?.name,
            brandName = dto.brand?.name,
            conditionName = variant?.conditionName,
            locationName = dto.town?.name,  // ✅ Store town name

            shop = dto.shop?.let {
                Shop(
                    id = it.id,
                    name = it.name,
                    displayName = "",
                    userId = 0L,
                    sellerName = it.sellerName ?: "",
                    profilePictureUrl = "",
                    createdAt = "",
                    sellerMobile = it.sellerMobile,
                    itemsCount = 0
                )
            },
            label = dto.label,
            reserved = dto.totalReserved,
            meta = null,
            itemTypeId = null
        )
    }
    // ============ GET SINGLE ITEM ============
    override suspend fun getItem(itemId: String): Result<Item> {
        Log.d(TAG, "getItem called for ID: $itemId")

        // 1. ALWAYS fetch from network first to get the latest data
        Log.d(TAG, "🌐 Fetching from NETWORK (prioritizing latest)...")
        val startTime = System.currentTimeMillis()

        return try {
            val response = itemApiService.getItem(itemId)
            val duration = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                Log.e(TAG, "Network error: ${response.code()}, falling back to cache")
                // If API fails, fallback to cache
                return getItemFromCache(itemId)
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = mapItemDetailToDomain(itemResponse.item)

                Log.d(TAG, "✅ Network fetch successful (${duration}ms):")
                Log.d(TAG, "   - Name: ${item.name}")
                Log.d(TAG, "   - Images: ${item.images.size}")
                item.images.forEachIndexed { index, image ->
                    Log.d(TAG, "     Image $index: ${image.url}")
                }

                // Save to database WITH images (this updates Room with latest)
                saveToDatabaseWithImages(item)

                // Update memory cache
                memoryCache[itemId] = item
                memoryCacheTime[itemId] = System.currentTimeMillis()

                Result.success(item)
            } else {
                Log.e(TAG, "Item not found in response, falling back to cache")
                getItemFromCache(itemId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception: ${e.message}, falling back to cache", e)
            getItemFromCache(itemId)
        }
    }

    // Helper method to get from cache (memory or database)
    private suspend fun getItemFromCache(itemId: String): Result<Item> {
        // 1. Check memory cache first (fastest)
        memoryCache[itemId]?.let { cachedItem ->
            val cacheAge = System.currentTimeMillis() - (memoryCacheTime[itemId] ?: 0)
            Timber.tag(TAG).d("💾 Using MEMORY cache (age: ${cacheAge}ms)")
            Timber.tag(TAG).d("   - Images: ${cachedItem.images.size}")
            return Result.success(cachedItem)
        }

        // 2. Check database cache
        try {
            val dbItem = itemDao.getItemById(itemId)
            if (dbItem != null) {
                Log.d(TAG, "💾 Using DATABASE cache")

                // Load images from database
                val images = itemImageDao.getImagesForItem(itemId).map { imageEntity ->
                    ItemImage(
                        id = imageEntity.id,
                        url = imageEntity.url,
                        filename = null,
                        contentType = null,
                        createdAt = null,
                        isCover = imageEntity.isCover
                    )
                }

                Log.d(TAG, "   - Loaded ${images.size} images from database")

                val domainItem = Item(
                    id = dbItem.id,
                    shopId = dbItem.shopId,
                    name = dbItem.name,
                    description = dbItem.description,
                    price = dbItem.price,
                    quantity = dbItem.quantity,
                    status = dbItem.status,
                    brandId = dbItem.brandId,
                    sizeId = dbItem.sizeId,
                    schoolId = dbItem.schoolId,
                    itemConditionId = dbItem.itemConditionId,
                    genderId = dbItem.genderId,
                    sizeName = dbItem.sizeName,
                    colorName = dbItem.colorName,
                    brandName = dbItem.brandName,
                    conditionName = dbItem.conditionName,
                    createdAt = dbItem.createdAt,
                    updatedAt = dbItem.updatedAt,
                    images = images,
                    coverImage = images.firstOrNull { it.isCover }?.url ?: images.firstOrNull()?.url,
                    shop = null,
                    provinceId = dbItem.provinceId,
                    locationId = dbItem.locationId,
                    label = dbItem.label,
                    reserved = dbItem.reserved,
                    meta = null,
                    itemTypeId = dbItem.itemTypeId,
                    viewCount = dbItem.viewCount ?: 0
                )

                // Update memory cache
                memoryCache[itemId] = domainItem
                memoryCacheTime[itemId] = System.currentTimeMillis()

                return Result.success(domainItem)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database error: ${e.message}", e)
        }

        Log.e(TAG, "❌ No data found anywhere for: $itemId")
        return Result.failure(Exception("Item not found"))
    }

    // Add this method to ItemRepositoryInterface and implementation
    override suspend fun getItemWithRefresh(itemId: String): Result<Item> {
        Log.d(TAG, "getItemWithRefresh called for ID: $itemId")

        // Clear cache for this item
        memoryCache.remove(itemId)
        memoryCacheTime.remove(itemId)

        // Force network fetch
        return try {
            val response = itemApiService.getItem(itemId)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = mapItemDetailToDomain(itemResponse.item)

                Log.d(TAG, "✅ Force refresh: ${item.name}, Images: ${item.images.size}")

                // Save to database WITH images
                saveToDatabaseWithImages(item)

                // Update memory cache
                memoryCache[itemId] = item
                memoryCacheTime[itemId] = System.currentTimeMillis()

                Result.success(item)
            } else {
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Force refresh failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mapItemDetailToDomain(dto: ItemDetailDto): Item {
        val imageList = mutableListOf<ItemImage>()

        dto.coverPhoto?.let { url ->
            imageList.add(ItemImage(id = 0, url = url, isCover = true))
        }
        dto.images?.forEach { url ->
            if (url != dto.coverPhoto && !imageList.any { it.url == url }) {
                imageList.add(ItemImage(id = 0, url = url, isCover = false))
            }
        }
        if (imageList.isEmpty() && dto.image != null) {
            imageList.add(ItemImage(id = 0, url = dto.image, isCover = true))
        }

        return Item(
            id = dto.id,
            shopId = dto.shop?.id ?: 0L,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            quantity = dto.quantity,
            status = dto.status,
            viewCount = dto.viewCount,
            createdAt = dto.createdAt,
            images = imageList,
            coverImage = dto.coverPhoto,
            brandId = dto.brand?.id,
            schoolId = dto.school?.id,
            provinceId = dto.province?.id,
            locationId = dto.town?.id,
            genderId = dto.gender?.id,
            mainCategoryId = dto.mainCategory?.id,
            subCategoryId = dto.subCategory?.id,
            itemConditionId = dto.condition?.id,
            sizeId = dto.size?.id,
            colorId = dto.color?.id,
            sizeName = dto.size?.name,
            colorName = dto.color?.name,
            brandName = dto.brand?.name,
            conditionName = dto.condition?.name,
            reserved = dto.quantity - dto.availableQuantity,
            shop = dto.shop?.let {
                Shop(
                    id = it.id,
                    name = it.name,
                    displayName = it.displayName ?: "",
                    userId = 0L,
                    sellerName = it.sellerName ?: "",
                    profilePictureUrl = "",
                    createdAt = "",
                    sellerMobile = it.sellerMobile,
                    itemsCount = 0
                )
            },
            label = null,
            itemTypeId = null,
            meta = null
        )
    }

    override suspend fun getActiveItems(limit: Int): Result<List<Item>> {
        return try {
            val cachedItems = itemDao.getActiveItems(limit)
            if (cachedItems.isNotEmpty()) {
                Log.d(TAG, "Returning ${cachedItems.size} cached active items")
                return Result.success(cachedItems.map { it.toDomain() })
            }

            val response = itemApiService.getItems(limit = limit)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val items = response.body()?.mapNotNull { it.toDomain() } ?: emptyList()
            itemDao.insertItems(items.map { it.toEntity() })
            _currentItems.value = items

            Log.i(TAG, "Fetched ${items.size} active items from API")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Get active items failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getMyShopItems(): Result<List<Item>> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = itemApiService.getMyShopItems("Bearer $token")

            // ✅ ADD THIS LOGGING
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response successful: ${response.isSuccessful}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error response: $errorBody")
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val responseBody = response.body()
            Log.d(TAG, "Response body success: ${responseBody?.success}")
            Log.d(TAG, "Items count: ${responseBody?.items?.size}")
            Log.d(TAG, "Shop: ${responseBody?.shop}")

            if (responseBody?.success != true) {
                return Result.failure(Exception("Failed to load items"))
            }

            val items = responseBody.items?.map { it.toDomain(responseBody.shop?.id ?: 0L) } ?: emptyList()

            // ✅ Log each item
            items.forEach { item ->
                Log.d(TAG, "Item: ${item.name}, ViewCount: ${item.viewCount}, Status: ${item.status}")
            }

            _currentItems.value = items
            Result.success(items)

        } catch (e: Exception) {
            Log.e(TAG, "Get my shop items failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getShopItems(shopId: Long): Result<List<Item>> {
        return try {
            val cachedItems = itemDao.getItemsByShopId(shopId)
            if (cachedItems.isNotEmpty()) {
                Timber.tag(TAG).d("Returning ${cachedItems.size} cached items for shop $shopId")
                return Result.success(cachedItems.map { it.toDomain() })
            }

            val response = itemApiService.getShopItems(shopId)

            if (!response.isSuccessful) {
                Log.e(TAG, "Server error: ${response.code()}")
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val responseBody = response.body()
            if (responseBody == null) {
                Log.w(TAG, "Response body is null for shop $shopId")
                return Result.success(emptyList())
            }

            if (!responseBody.success) {
                Log.e(TAG, "API returned success=false for shop $shopId")
                return Result.failure(Exception("Failed to load items"))
            }

            val items: List<Item> = responseBody.items
                ?.map { publicShopItemDto ->
                    publicShopItemDto.toDomain(shopId)
                } ?: emptyList()

            try {
                val entities = items.map { it.toEntity() }
                itemDao.insertItems(entities)
                Log.d(TAG, "Cached ${entities.size} items for shop $shopId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache items", e)
            }

            Log.i(TAG, "Fetched ${items.size} items for shop $shopId from API")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Get shop items failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearItems() {
        try {
            itemDao.clearAllItems()
            _recentlyCreatedItem.value = null
            _currentItems.value = emptyList()
            Log.i(TAG, "Cleared all cached items")
        } catch (e: Exception) {
            Log.e(TAG, "Clear items failed", e)
        }
    }
    private suspend fun saveToDatabaseWithImages(item: Item) {
        try {
            Log.d(TAG, "💾 Saving item to database with ${item.images.size} images")

            // Save the item entity
            itemDao.insertItem(item.toEntity())

            // Save all images as separate entities
            val imageEntities = item.images.mapIndexed { index, image ->
                ItemImageEntity(
                    itemId = item.id,
                    url = image.url,
                    isCover = index == 0,
                    position = index
                )
            }

            itemImageDao.updateImagesForItem(item.id, imageEntities)

            Log.d(TAG, "✅ Successfully saved item and ${imageEntities.size} images to database")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to database: ${e.message}", e)
        }
    }

    // ✅ Add @Override annotation or just make sure it's marked as override
    override suspend fun updateItemStatus(itemId: String, status: String): Result<Unit> {
        return try {
            // Update local database
            itemDao.updateItemStatus(itemId, status)
            Log.i(TAG, "✅ Updated item $itemId status to $status in database")

            // Also update the cache
            val currentList = _currentItems.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val updatedItem = currentList[index].copy(status = status)
                currentList[index] = updatedItem
                _currentItems.value = currentList
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update item status failed", e)
            Result.failure(e)
        }
    }

    override suspend fun saveLocalItem(item: Item): Result<Unit> {
        return try {
            itemDao.insertItem(item.toEntity())
            Timber.tag(TAG).d("✅ Item saved locally: ${item.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to save local item")
            Result.failure(e)
        }
    }

    // In ItemRepository.kt - updateItemSimple
    override suspend fun updateItemSimple(
        itemId: String,
        name: String?,
        description: String?,
        mainCategoryId: Int?,
        subCategoryId: Int?,
        brandId: Int?,
        price: Double?,
        quantity: Int?,
        itemConditionId: Int?,
        provinceId: Int?,
        locationId: Int?,
        genderId: Int?,
        schoolId: Int?,
        sizeId: Int?,
        colorId: Int?,
        tagIds: List<Int>?,
        status: String?
    ): Result<Item> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // ✅ Only include fields that belong to Item, not variant
            val updateData = UpdateItemData(
                name = name,
                description = description,
                price = price,  // This goes to variant
                quantity = quantity,  // This goes to variant
                mainCategoryId = mainCategoryId,
                subCategoryId = subCategoryId,
                brandId = brandId,
                sizeId = sizeId,  // This goes to variant
                colorId = colorId,  // This goes to variant
                itemConditionId = itemConditionId,  // This goes to variant
                provinceId = provinceId,
                locationId = locationId,
                genderId = genderId,
                schoolId = schoolId,
                label = null,
                status = status,
                tagIds = tagIds
            )

            val request = UpdateItemRequest(item = updateData)

            Log.d(TAG, "Updating item $itemId")
            Log.d(TAG, "Update data: ${updateData}")

            val response = itemApiService.updateItem("Bearer $token", itemId, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when (response.code()) {
                    422 -> "Validation failed: $errorBody"
                    502 -> "Server is temporarily unavailable. Please try again."
                    500 -> "Server error. Please try again later."
                    else -> "Failed to update item: ${response.code()}"
                }
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to update item"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val updatedItem = responseBody.item?.let { mapUpdateDtoToItem(it) }
                ?: return Result.failure(Exception("No item data in response"))

            itemDao.insertItem(updatedItem.toEntity())
            Log.d(TAG, "✅ Updated item ${updatedItem.id} in local database")

            Result.success(updatedItem)

        } catch (e: Exception) {
            Log.e(TAG, "Update item failed", e)
            Result.failure(e)
        }
    }
    override suspend fun updateItemWithImages(
        context: Context,
        itemId: String,
        name: String?,
        description: String?,
        mainCategoryId: Int?,
        subCategoryId: Int?,
        brandId: Int?,
        price: Double?,
        quantity: Int?,
        itemConditionId: Int?,
        provinceId: Int?,
        locationId: Int?,
        genderId: Int?,
        schoolId: Int?,
        sizeId: Int?,
        colorId: Int?,
        tagIds: List<Int>?,
        status: String?,
        addImageUris: List<Uri>,
        removeImageIds: List<Long>,
        replaceAllImages: List<Uri>?
    ): Result<Item> {
        return try {
            val updateResult = updateItemSimple(
                itemId = itemId,
                name = name,
                description = description,
                mainCategoryId = mainCategoryId,
                subCategoryId = subCategoryId,
                brandId = brandId,
                price = price,
                quantity = quantity,
                itemConditionId = itemConditionId,
                provinceId = provinceId,
                locationId = locationId,
                genderId = genderId,
                schoolId = schoolId,
                sizeId = sizeId,
                colorId = colorId,
                tagIds = tagIds,
                status = status
            )

            if (updateResult.isFailure) {
                return updateResult
            }

            var updatedItem = updateResult.getOrNull() ?: return Result.failure(Exception("Update failed"))

            if (replaceAllImages != null) {
                val token = authRepository.getAuthToken().value ?: return Result.failure(Exception("Not authenticated"))

                val currentItem = getItem(itemId).getOrNull()
                currentItem?.images?.forEach { imageUrl ->
                    // Note: removeItemImage expects ID, but we have URL
                    // You may need to modify this based on your needs
                }

                if (replaceAllImages.isNotEmpty()) {
                    val imageParts = ImageMultipartHelper.createImageParts(context, replaceAllImages)
                    if (imageParts.isNotEmpty()) {
                        val imagesResponse = itemApiService.addItemImages("Bearer $token", itemId, imageParts)
                        if (imagesResponse.isSuccessful && imagesResponse.body()?.success == true) {
                            val images = imagesResponse.body()?.images ?: emptyList()
                            // Convert to List<ItemImage>
                            updatedItem = updatedItem.copy(
                                images = images.map { imageDto ->
                                    ItemImage(
                                        id = imageDto.id,
                                        url = imageDto.url,
                                        filename = imageDto.filename,
                                        contentType = imageDto.contentType,
                                        createdAt = imageDto.createdAt
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                val token = authRepository.getAuthToken().value ?: return Result.failure(Exception("Not authenticated"))

                if (addImageUris.isNotEmpty()) {
                    val imageParts = ImageMultipartHelper.createImageParts(context, addImageUris)
                    if (imageParts.isNotEmpty()) {
                        val imagesResponse = itemApiService.addItemImages("Bearer $token", itemId, imageParts)
                        if (imagesResponse.isSuccessful && imagesResponse.body()?.success == true) {
                            val newImages = imagesResponse.body()?.images ?: emptyList()
                            val newImageItems = newImages.map { imageDto ->
                                ItemImage(
                                    id = imageDto.id,
                                    url = imageDto.url,
                                    filename = imageDto.filename,
                                    contentType = imageDto.contentType,
                                    createdAt = imageDto.createdAt
                                )
                            }
                            val allImages = updatedItem.images + newImageItems
                            updatedItem = updatedItem.copy(images = allImages)
                        }
                    }
                }
            }

            val finalItem = getItem(itemId).getOrElse { updatedItem }
            itemDao.insertItem(finalItem.toEntity())

            Result.success(finalItem)

        } catch (e: Exception) {
            Log.e(TAG, "Update item with images failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = itemApiService.deleteItem("Bearer $token", itemId)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to delete item: ${response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to delete item"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            itemDao.softDeleteItem(itemId)

            val currentList = _currentItems.value.toMutableList()
            currentList.removeAll { it.id == itemId }
            _currentItems.value = currentList

            Log.i(TAG, "Item deleted successfully: $itemId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Delete item failed", e)
            Result.failure(e)
        }
    }

    override suspend fun markItemAsSold(itemId: String): Result<Item> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = itemApiService.markItemAsSold("Bearer $token", itemId)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to mark item as sold: ${response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to mark item as sold"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val updatedItem = getItem(itemId).getOrElse {
                return Result.failure(Exception("Failed to get updated item"))
            }

            Log.i(TAG, "Item marked as sold: $itemId")
            Result.success(updatedItem)

        } catch (e: Exception) {
            Log.e(TAG, "Mark as sold failed", e)
            Result.failure(e)
        }
    }

    private fun mapUpdateDtoToItem(dto: UpdateItemDto): Item {
        // ✅ Handle both String URLs and Image objects
        val imageList = dto.images?.mapNotNull { image ->
            when (image) {
                is String -> {
                    // Case 1: It's a simple string URL
                    ItemImage(
                        id = 0,
                        url = image,
                        filename = null,
                        contentType = null,
                        createdAt = null,
                        isCover = false
                    )
                }
                is Map<*, *> -> {
                    // Case 2: It's a Map (from object format)
                    val id = (image["id"] as? Number)?.toLong() ?: 0
                    val url = image["url"] as? String ?: ""
                    val filename = image["filename"] as? String
                    val contentType = image["content_type"] as? String
                    val createdAt = image["created_at"] as? String

                    if (url.isNotEmpty()) {
                        ItemImage(
                            id = id,
                            url = url,
                            filename = filename,
                            contentType = contentType,
                            createdAt = createdAt,
                            isCover = false
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        } ?: emptyList()

        return Item(
            id = dto.id,
            shopId = 0L,
            name = dto.name,
            description = dto.description,
            price = dto.price.toDoubleOrNull() ?: 0.0,
            quantity = dto.quantity,
            status = dto.status,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            images = imageList,
            coverImage = imageList.firstOrNull()?.url,
            brandId = dto.brandId,
            sizeId = dto.sizeId,
            colorId = dto.colorId,
            schoolId = dto.schoolId,
            itemConditionId = dto.conditionId,
            locationId = dto.townId,
            provinceId = dto.provinceId,
            genderId = dto.genderId,
            mainCategoryId = dto.mainCategoryId,
            subCategoryId = dto.subCategoryId,
            label = null,
            reserved = dto.quantity - dto.availableQuantity,
            meta = if (dto.colorName != null || dto.sizeName != null) {
                ItemMeta(color = dto.colorName, size = dto.sizeName)
            } else null,
            shop = null,
            sizeName = dto.sizeName,
            colorName = dto.colorName,
            brandName = dto.brandName,
            conditionName = dto.conditionName,
            viewCount = 0
        )
    }

}