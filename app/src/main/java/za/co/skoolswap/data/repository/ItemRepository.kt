package za.co.skoolswap.data.repository

import za.co.skoolswap.data.local.database.dao.ItemImageDao
import za.co.skoolswap.data.local.database.entities.ItemImageEntity
import android.content.Context
import android.net.Uri
import java.util.UUID
import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.database.dao.ItemDao
import za.co.skoolswap.data.mapper.toDomain
import za.co.skoolswap.data.mapper.toEntity
import za.co.skoolswap.data.remote.api.ItemApiService
import za.co.skoolswap.data.remote.models.request.CreateItemRequest
import za.co.skoolswap.data.remote.models.request.ItemData
import za.co.skoolswap.data.remote.models.request.UpdateItemData
import za.co.skoolswap.data.remote.models.request.UpdateItemRequest
import za.co.skoolswap.data.remote.models.response.item.AttachImagesByUrlRequest
import za.co.skoolswap.data.remote.models.response.item.ItemDetailDto
import za.co.skoolswap.data.remote.models.response.item.UpdateItemDto
import za.co.skoolswap.data.remote.models.response.item.ViewShopItemDto
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.ItemImage
import za.co.skoolswap.domain.model.ItemMeta
import za.co.skoolswap.domain.model.Shop
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.ItemRepositoryInterface
import za.co.skoolswap.utils.ImageMultipartHelper
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

    // Constants - internal use only
    private val MAX_IMAGES = 3
    private val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L

    private val _recentlyCreatedItem = MutableStateFlow<Item?>(null)
    override val recentlyCreatedItem: StateFlow<Item?> = _recentlyCreatedItem.asStateFlow()

    private fun isCacheValid(cacheTime: Long): Boolean {
        return System.currentTimeMillis() - cacheTime < CACHE_DURATION_MS
    }

    private val _currentItems = MutableStateFlow<List<Item>>(emptyList())
    override val currentItems: StateFlow<List<Item>> = _currentItems.asStateFlow()
    private val memoryCache = mutableMapOf<String, Item>()
    private val memoryCacheTime = mutableMapOf<String, Long>()

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
        imageUris: List<Uri>
    ): Result<Item> {
        return try {
            val localId = UUID.randomUUID().toString()

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
                syncStatus = AppConstants.SyncStatus.UPLOADING,
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

            itemDao.insertItem(localItem.toEntity())

            val imageEntities = imageUris.mapIndexed { index, uri ->
                ItemImageEntity(
                    itemId = localId,
                    url = uri.toString(),
                    isCover = index == 0,
                    position = index
                )
            }
            itemImageDao.updateImagesForItem(localId, imageEntities)

            Timber.tag(LogTags.REPOSITORY).d("✅ Item saved locally with ID: $localId, images: ${imageUris.size}")
            Result.success(localItem)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Failed to create item locally")
            Result.failure(e)
        }
    }

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

                Timber.tag(LogTags.REPOSITORY).d("✅ Attached ${images.size} images to item $itemId")
                Result.success(images)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to attach images"
                Timber.tag(LogTags.REPOSITORY).e("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to attach images")
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
        Timber.tag(LogTags.REPOSITORY).d("🚀 updateItemOfflineFirst START")
        Timber.tag(LogTags.REPOSITORY).d("   itemId: $itemId")

        return try {
            val existingItem = itemDao.getItemById(itemId)
            if (existingItem == null) {
                Timber.tag(LogTags.REPOSITORY).e("❌ Item not found in database: $itemId")
                return Result.failure(Exception("Item not found"))
            }

            val existingDomain = existingItem.toDomain()

            val updatedImages = buildUpdatedImages(
                existingDomain.images,
                addImageUris,
                removeImageIds
            )

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
                syncStatus = AppConstants.SyncStatus.UPDATING,
                syncError = null,
                retryCount = 0,
                updatedAt = System.currentTimeMillis().toString()
            )

            safeInsertOrUpdateItem(updatedItem)

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
                Timber.tag(LogTags.REPOSITORY).d("✅ ${imageEntities.size} new images saved to database")
            }

            if (removeImageIds.isNotEmpty()) {
                Timber.tag(LogTags.REPOSITORY).d("✅ ${removeImageIds.size} images marked for deletion")
            }

            Timber.tag(LogTags.REPOSITORY).d("✅ Item updated locally with ID: $itemId")
            Result.success(updatedItem)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ updateItemOfflineFirst FAILED")
            Result.failure(e)
        }
    }

    private fun buildUpdatedImages(
        existingImages: List<ItemImage>,
        addImageUris: List<Uri>,
        removeImageIds: List<Long>
    ): List<ItemImage> {
        val keptImages = existingImages.filter {
            !removeImageIds.contains(it.id)
        }

        val newImages = addImageUris.mapIndexed { index, uri ->
            ItemImage(
                id = 0,
                url = uri.toString(),
                isCover = index == 0 && keptImages.isEmpty()
            )
        }

        return keptImages + newImages
    }

    private suspend fun safeInsertOrUpdateItem(newItem: Item) {
        Timber.tag(LogTags.REPOSITORY).d("🛡️ safeInsertOrUpdateItem called")
        Timber.tag(LogTags.REPOSITORY).d("   Item ID: ${newItem.id}")

        val existing = itemDao.getItemById(newItem.id)

        if (existing != null) {
            val merged = existing.copy(
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
                name = newItem.name,
                description = newItem.description,
                price = newItem.price,
                quantity = newItem.quantity,
                status = newItem.status as String,
                updatedAt = newItem.updatedAt ?: existing.updatedAt,
                syncStatus = newItem.syncStatus,
                syncError = newItem.syncError,
                retryCount = newItem.retryCount,
                lastSyncAttempt = newItem.lastSyncAttempt,
                imageCount = if (newItem.images.isNotEmpty()) newItem.images.size else existing.imageCount,
                coverImage = newItem.coverImage ?: existing.coverImage,
                createdAt = newItem.createdAt.takeIf { it.isNotEmpty() } ?: existing.createdAt,
            )

            itemDao.insertItem(merged)
            Timber.tag(LogTags.REPOSITORY).d("✅ Item updated in database")
        } else {
            Timber.tag(LogTags.REPOSITORY).d("📦 No existing item found, inserting new")
            val entity = newItem.toEntity()
            itemDao.insertItem(entity)
            Timber.tag(LogTags.REPOSITORY).d("✅ New item inserted")
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
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true || responseBody.item == null) {
                val errorMsg = responseBody?.message ?: "Failed to create item"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val item = responseBody.toDomain()
            itemDao.insertItem(item.toEntity())
            _recentlyCreatedItem.value = item

            Timber.tag(LogTags.REPOSITORY).i("Item created successfully: ${item.name}")
            Result.success(item)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Create item failed")
            Result.failure(e)
        }
    }

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

            Timber.tag(LogTags.REPOSITORY).d("Creating item: $name")
            val itemResponse = itemApiService.createItem("Bearer $token", request)

            if (!itemResponse.isSuccessful) {
                val errorMsg = "Failed to create item: ${itemResponse.errorBody()?.string()}"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val createdItemResponse = itemResponse.body()
            if (createdItemResponse?.success != true || createdItemResponse.item == null) {
                val errorMsg = createdItemResponse?.message ?: "Failed to create item"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val itemId = createdItemResponse.item.id
            Timber.tag(LogTags.REPOSITORY).d("Item created with ID: $itemId")

            var finalItem = createdItemResponse.toDomain()

            if (imageUris.isNotEmpty()) {
                val result = imageUploadRepository.uploadAndAttachImages(
                    context = context,
                    itemId = itemId,
                    imageUris = imageUris
                )

                if (result.isSuccess) {
                    val uploadedUrls = result.getOrNull() ?: emptyList()
                    if (uploadedUrls.isNotEmpty()) {
                        val updatedItemResult = getItem(itemId)
                        if (updatedItemResult.isSuccess) {
                            finalItem = updatedItemResult.getOrNull()!!
                        }
                    }
                    Timber.tag(LogTags.REPOSITORY).d("✅ Uploaded and attached ${uploadedUrls.size} images")
                } else {
                    Timber.tag(LogTags.REPOSITORY).w("⚠️ Image upload failed: ${result.exceptionOrNull()?.message}")
                }
            }

            itemDao.insertItem(finalItem.toEntity())
            _recentlyCreatedItem.value = finalItem

            Timber.tag(LogTags.REPOSITORY).i("Item created successfully: ${finalItem.name}")
            Result.success(finalItem)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Create item with images failed")
            Result.failure(e)
        }
    }

    override suspend fun addItemImages(
        context: Context,
        itemId: String,
        imageUris: List<Uri>
    ): Result<List<ItemImage>> {
        return try {
            val token = authRepository.getAuthToken().value
                ?: return Result.failure(Exception("Not authenticated"))

            // ✅ Create multipart parts from URIs
            val imageParts = ImageMultipartHelper.createImageParts(context, imageUris)

            if (imageParts.isEmpty()) {
                return Result.failure(Exception("No valid images provided"))
            }

            // ✅ Call the fixed endpoint
            val response = itemApiService.addItemImages(
                authHeader = "Bearer $token",
                itemId = itemId,
                images = imageParts  // ← List of MultipartBody.Part
            )

            if (!response.isSuccessful) {
                val errorMsg = "Server error: ${response.code()}"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body?.success == true) {
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

                Timber.tag(LogTags.REPOSITORY).i("Added ${images.size} images to item $itemId")
                Result.success(images)
            } else {
                val errorMsg = body?.message ?: "Failed to upload images"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Add item images failed")
            Result.failure(e)
        }
    }

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
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body?.success == true) {
                val remaining = body.remainingImages ?: 0
                Timber.tag(LogTags.REPOSITORY).i("Image $imageId removed from item $itemId. Remaining: $remaining")
                Result.success(remaining)
            } else {
                val errorMsg = body?.message ?: "Failed to remove image"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Remove item image failed")
            Result.failure(e)
        }
    }

    override suspend fun getShopItemForEdit(itemId: String): Result<Item> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            Timber.tag(LogTags.REPOSITORY).d("🔍 Fetching item for edit: $itemId")
            val response = itemApiService.getShopItemForEdit("Bearer $token", itemId)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to load item: ${response.code()}"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true || responseBody.item == null) {
                val errorMsg = responseBody?.message ?: "Failed to load item"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val item = mapViewShopItemToDomain(responseBody.item)

            Timber.tag(LogTags.REPOSITORY).d("✅ Item loaded: ${item.name} with ${item.images.size} images")

            itemDao.insertItem(item.toEntity())

            Result.success(item)
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get item for edit failed")
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
            brandId = dto.brand?.id ?: dto.brandId,
            sizeId = variant?.sizeId ?: dto.size?.id,
            colorId = variant?.colorId ?: dto.color?.id,
            schoolId = dto.school?.id ?: dto.schoolId,
            itemConditionId = variant?.conditionId ?: dto.itemConditionId,
            locationId = dto.town?.id ?: dto.locationId,
            provinceId = dto.province?.id ?: dto.provinceId,
            genderId = dto.gender?.id ?: dto.genderId,
            mainCategoryId = dto.mainCategory?.id ?: dto.mainCategoryId,
            subCategoryId = dto.subCategory?.id ?: dto.subCategoryId,
            sizeName = variant?.sizeName ?: dto.size?.name,
            colorName = variant?.colorName ?: dto.color?.name,
            brandName = dto.brand?.name,
            conditionName = variant?.conditionName,
            locationName = dto.town?.name,
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

    override suspend fun getItem(itemId: String): Result<Item> {
        Timber.tag(LogTags.REPOSITORY).d("getItem called for ID: $itemId")

        Timber.tag(LogTags.REPOSITORY).d("🌐 Fetching from NETWORK (prioritizing latest)...")
        val startTime = System.currentTimeMillis()

        return try {
            val response = itemApiService.getItem(itemId)
            val duration = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                Timber.tag(LogTags.REPOSITORY).e("Network error: ${response.code()}, falling back to cache")
                return getItemFromCache(itemId)
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = mapItemDetailToDomain(itemResponse.item)

                Timber.tag(LogTags.REPOSITORY).d("✅ Network fetch successful (${duration}ms):")
                Timber.tag(LogTags.REPOSITORY).d("   - Name: ${item.name}")
                Timber.tag(LogTags.REPOSITORY).d("   - Images: ${item.images.size}")

                saveToDatabaseWithImages(item)

                memoryCache[itemId] = item
                memoryCacheTime[itemId] = System.currentTimeMillis()

                Result.success(item)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("Item not found in response, falling back to cache")
                getItemFromCache(itemId)
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Network exception, falling back to cache")
            getItemFromCache(itemId)
        }
    }

    private suspend fun getItemFromCache(itemId: String): Result<Item> {
        memoryCache[itemId]?.let { cachedItem ->
            val cacheAge = System.currentTimeMillis() - (memoryCacheTime[itemId] ?: 0)
            Timber.tag(LogTags.REPOSITORY).d("💾 Using MEMORY cache (age: ${cacheAge}ms)")
            return Result.success(cachedItem)
        }

        try {
            val dbItem = itemDao.getItemById(itemId)
            if (dbItem != null) {
                Timber.tag(LogTags.REPOSITORY).d("💾 Using DATABASE cache")

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

                memoryCache[itemId] = domainItem
                memoryCacheTime[itemId] = System.currentTimeMillis()

                return Result.success(domainItem)
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Database error")
        }

        Timber.tag(LogTags.REPOSITORY).e("❌ No data found anywhere for: $itemId")
        return Result.failure(Exception("Item not found"))
    }

    override suspend fun getItemWithRefresh(itemId: String): Result<Item> {
        Timber.tag(LogTags.REPOSITORY).d("getItemWithRefresh called for ID: $itemId")

        memoryCache.remove(itemId)
        memoryCacheTime.remove(itemId)

        return try {
            val response = itemApiService.getItem(itemId)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = mapItemDetailToDomain(itemResponse.item)

                Timber.tag(LogTags.REPOSITORY).d("✅ Force refresh: ${item.name}, Images: ${item.images.size}")

                saveToDatabaseWithImages(item)

                memoryCache[itemId] = item
                memoryCacheTime[itemId] = System.currentTimeMillis()

                Result.success(item)
            } else {
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Force refresh failed")
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
            schoolLogoUrl = dto.school?.logoUrl,
            schoolName = dto.school?.name,
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
                Timber.tag(LogTags.REPOSITORY).d("Returning ${cachedItems.size} cached active items")
                return Result.success(cachedItems.map { it.toDomain() })
            }

            val response = itemApiService.getItems(limit = limit)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val items = response.body()?.mapNotNull { it.toDomain() } ?: emptyList()
            itemDao.insertItems(items.map { it.toEntity() })
            _currentItems.value = items

            Timber.tag(LogTags.REPOSITORY).i("Fetched ${items.size} active items from API")
            Result.success(items)
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get active items failed")
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

            Timber.tag(LogTags.REPOSITORY).d("Response code: ${response.code()}")
            Timber.tag(LogTags.REPOSITORY).d("Response successful: ${response.isSuccessful}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Timber.tag(LogTags.REPOSITORY).e("Error response: $errorBody")
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val responseBody = response.body()
            Timber.tag(LogTags.REPOSITORY).d("Response body success: ${responseBody?.success}")
            Timber.tag(LogTags.REPOSITORY).d("Items count: ${responseBody?.items?.size}")

            if (responseBody?.success != true) {
                return Result.failure(Exception("Failed to load items"))
            }

            val items = responseBody.items?.map { it.toDomain(responseBody.shop?.id ?: 0L) } ?: emptyList()

            items.forEach { item ->
                Timber.tag(LogTags.REPOSITORY).d("Item: ${item.name}, ViewCount: ${item.viewCount}, Status: ${item.status}")
            }

            _currentItems.value = items
            Result.success(items)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get my shop items failed")
            Result.failure(e)
        }
    }

    override suspend fun getShopItems(shopId: Long): Result<List<Item>> {
        return try {
            val cachedItems = itemDao.getItemsByShopId(shopId)
            if (cachedItems.isNotEmpty()) {
                Timber.tag(LogTags.REPOSITORY).d("Returning ${cachedItems.size} cached items for shop $shopId")
                return Result.success(cachedItems.map { it.toDomain() })
            }

            val response = itemApiService.getShopItems(shopId)

            if (!response.isSuccessful) {
                Timber.tag(LogTags.REPOSITORY).e("Server error: ${response.code()}")
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val responseBody = response.body()
            if (responseBody == null) {
                Timber.tag(LogTags.REPOSITORY).w("Response body is null for shop $shopId")
                return Result.success(emptyList())
            }

            if (!responseBody.success) {
                Timber.tag(LogTags.REPOSITORY).e("API returned success=false for shop $shopId")
                return Result.failure(Exception("Failed to load items"))
            }

            val items = responseBody.items?.map { publicShopItemDto ->
                publicShopItemDto.toDomain(shopId)
            } ?: emptyList()

            try {
                val entities = items.map { it.toEntity() }
                itemDao.insertItems(entities)
                Timber.tag(LogTags.REPOSITORY).d("Cached ${entities.size} items for shop $shopId")
            } catch (e: Exception) {
                Timber.tag(LogTags.REPOSITORY).w(e, "Failed to cache items")
            }

            Timber.tag(LogTags.REPOSITORY).i("Fetched ${items.size} items for shop $shopId from API")
            Result.success(items)
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get shop items failed")
            Result.failure(e)
        }
    }

    override suspend fun clearItems() {
        try {
            itemDao.clearAllItems()
            _recentlyCreatedItem.value = null
            _currentItems.value = emptyList()
            Timber.tag(LogTags.REPOSITORY).i("Cleared all cached items")
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Clear items failed")
        }
    }

    private suspend fun saveToDatabaseWithImages(item: Item) {
        try {
            Timber.tag(LogTags.REPOSITORY).d("💾 Saving item to database with ${item.images.size} images")

            itemDao.insertItem(item.toEntity())

            val imageEntities = item.images.mapIndexed { index, image ->
                ItemImageEntity(
                    itemId = item.id,
                    url = image.url,
                    isCover = index == 0,
                    position = index
                )
            }

            itemImageDao.updateImagesForItem(item.id, imageEntities)

            Timber.tag(LogTags.REPOSITORY).d("✅ Successfully saved item and ${imageEntities.size} images to database")
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to save to database")
        }
    }

    override suspend fun updateItemStatus(itemId: String, status: String): Result<Unit> {
        return try {
            itemDao.updateItemStatus(itemId, status)
            Timber.tag(LogTags.REPOSITORY).i("✅ Updated item $itemId status to $status in database")

            val currentList = _currentItems.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val updatedItem = currentList[index].copy(status = status)
                currentList[index] = updatedItem
                _currentItems.value = currentList
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Update item status failed")
            Result.failure(e)
        }
    }

    override suspend fun saveLocalItem(item: Item): Result<Unit> {
        return try {
            itemDao.insertItem(item.toEntity())
            Timber.tag(LogTags.REPOSITORY).d("✅ Item saved locally: ${item.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Failed to save local item")
            Result.failure(e)
        }
    }

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

            val updateData = UpdateItemData(
                name = name,
                description = description,
                price = price,
                quantity = quantity,
                mainCategoryId = mainCategoryId,
                subCategoryId = subCategoryId,
                brandId = brandId,
                sizeId = sizeId,
                colorId = colorId,
                itemConditionId = itemConditionId,
                provinceId = provinceId,
                locationId = locationId,
                genderId = genderId,
                schoolId = schoolId,
                label = null,
                status = status,
                tagIds = tagIds
            )

            val request = UpdateItemRequest(item = updateData)

            Timber.tag(LogTags.REPOSITORY).d("Updating item $itemId")

            val response = itemApiService.updateItem("Bearer $token", itemId, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when (response.code()) {
                    422 -> "Validation failed: $errorBody"
                    502 -> "Server is temporarily unavailable. Please try again."
                    500 -> "Server error. Please try again later."
                    else -> "Failed to update item: ${response.code()}"
                }
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to update item"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val updatedItem = responseBody.item?.let { mapUpdateDtoToItem(it) }
                ?: return Result.failure(Exception("No item data in response"))

            itemDao.insertItem(updatedItem.toEntity())
            Timber.tag(LogTags.REPOSITORY).d("✅ Updated item ${updatedItem.id} in local database")

            Result.success(updatedItem)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Update item failed")
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

                if (replaceAllImages.isNotEmpty()) {
                    val imageParts = ImageMultipartHelper.createImageParts(context, replaceAllImages)
                    if (imageParts.isNotEmpty()) {
                        val imagesResponse = itemApiService.addItemImages("Bearer $token", itemId, imageParts)
                        if (imagesResponse.isSuccessful && imagesResponse.body()?.success == true) {
                            val images = imagesResponse.body()?.images ?: emptyList()
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
            Timber.tag(LogTags.REPOSITORY).e(e, "Update item with images failed")
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
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to delete item"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            itemDao.softDeleteItem(itemId)

            val currentList = _currentItems.value.toMutableList()
            currentList.removeAll { it.id == itemId }
            _currentItems.value = currentList

            Timber.tag(LogTags.REPOSITORY).i("Item deleted successfully: $itemId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Delete item failed")
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
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to mark item as sold"
                Timber.tag(LogTags.REPOSITORY).e(errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val updatedItem = getItem(itemId).getOrElse {
                return Result.failure(Exception("Failed to get updated item"))
            }

            Timber.tag(LogTags.REPOSITORY).i("Item marked as sold: $itemId")
            Result.success(updatedItem)

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Mark as sold failed")
            Result.failure(e)
        }
    }

    private fun mapUpdateDtoToItem(dto: UpdateItemDto): Item {
        val imageList = dto.images?.mapNotNull { image ->
            when (image) {
                is String -> {
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