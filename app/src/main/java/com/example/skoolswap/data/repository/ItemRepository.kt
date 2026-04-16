package com.example.skoolswap.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.ItemApiService
import com.example.skoolswap.data.remote.models.request.CreateItemRequest
import com.example.skoolswap.data.remote.models.request.ItemData
import com.example.skoolswap.data.remote.models.request.UpdateItemData
import com.example.skoolswap.data.remote.models.request.UpdateItemRequest
import com.example.skoolswap.data.remote.models.response.item.ItemDetailDto
import com.example.skoolswap.data.remote.models.response.item.UpdateItemDto
import com.example.skoolswap.data.remote.models.response.item.ViewShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.PublicShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.ShopItemDto
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.model.ItemMeta
import com.example.skoolswap.domain.model.Shop
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.utils.ImageMultipartHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemApiService: ItemApiService,
    private val itemDao: ItemDao,
    private val authRepository: AuthRepositoryInterface
) : ItemRepositoryInterface {

    companion object {
        private const val TAG = "ItemRepository"
        private const val MAX_IMAGES = 3
    }

    private val _recentlyCreatedItem = MutableStateFlow<Item?>(null)
    override val recentlyCreatedItem: StateFlow<Item?> = _recentlyCreatedItem.asStateFlow()

    private val _currentItems = MutableStateFlow<List<Item>>(emptyList())
    override val currentItems: StateFlow<List<Item>> = _currentItems.asStateFlow()

    // ============ CREATE ITEM WITHOUT IMAGES (NEW VERSION) ============
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
            // 1. Get auth token
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // 2. Prepare item data
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

            // 3. Create item
            val response = itemApiService.createItem("Bearer $token", request)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to create item: ${response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true || responseBody.item == null) {
                val errorMsg = responseBody?.message ?: "Failed to create item"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            // 4. Convert to domain and cache
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

    // ============ CREATE ITEM WITH IMAGES (NEW VERSION) ============
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
            // 1. Get auth token
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // 2. Validate image count
            if (imageUris.size > MAX_IMAGES) {
                return Result.failure(Exception("Cannot exceed $MAX_IMAGES images"))
            }

            // 3. Prepare item data
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

            // 4. Create item first
            Log.d(TAG, "Creating item: $name")
            val itemResponse = itemApiService.createItem("Bearer $token", request)

            if (!itemResponse.isSuccessful) {
                val errorMsg = "Failed to create item: ${itemResponse.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val createdItemResponse = itemResponse.body()
            if (createdItemResponse?.success != true || createdItemResponse.item == null) {
                val errorMsg = createdItemResponse?.message ?: "Failed to create item"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val itemId = createdItemResponse.item.id
            Log.d(TAG, "Item created with ID: $itemId")

            // 5. Upload images if provided
            var finalItem = createdItemResponse.toDomain()

            if (imageUris.isNotEmpty()) {
                Timber.tag(TAG).d("Uploading ${imageUris.size} images")
                val imageParts = ImageMultipartHelper.createImageParts(context, imageUris)

                if (imageParts.isNotEmpty()) {
                    val imagesResponse = itemApiService.addItemImages(
                        "Bearer $token",
                        itemId,
                        imageParts
                    )

                    if (imagesResponse.isSuccessful && imagesResponse.body()?.success == true) {
                        val images = imagesResponse.body()?.images ?: emptyList()
                        finalItem = finalItem.copy(images = images.map { it.toDomain() })
                        Log.d(TAG, "Images uploaded successfully: ${images.size} images")
                    } else {
                        Log.w(TAG, "Image upload failed, but item was created")
                    }
                }
            }

            // 6. Cache in database
            itemDao.insertItem(finalItem.toEntity())

            // 7. Update state
            _recentlyCreatedItem.value = finalItem

            Log.i(TAG, "Item with images created successfully: ${finalItem.name}")
            Result.success(finalItem)

        } catch (e: Exception) {
            Log.e(TAG, "Create item with images failed", e)
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
                val images = body.images?.map { it.toDomain() } ?: emptyList()
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

            // Convert the response to your domain Item model
            val item = mapViewShopItemToDomain(responseBody.item)

            Timber.tag(TAG).d("✅ Item loaded: ${item.name} with ${item.images.size} images")

            // Cache in database
            itemDao.insertItem(item.toEntity())

            Result.success(item)
        } catch (e: Exception) {
            Log.e(TAG, "Get item for edit failed", e)
            Result.failure(e)
        }
    }

    // Helper mapping function
    private fun mapViewShopItemToDomain(dto: ViewShopItemDto): Item {
        return Item(
            id = dto.id,
            shopId = dto.shopId,
            name = dto.name,
            description = dto.description,
            price = dto.price?.toDoubleOrNull() ?: 0.0,
            quantity = dto.totalQuantity,
            status = dto.status,
            mainCategoryId = dto.mainCategoryId,
            subCategoryId = dto.subCategoryId,
            brandId = dto.brandId,
            sizeId = null, // You might need to map these from variants
            schoolId = dto.schoolId,
            itemConditionId = dto.itemConditionId,
            locationId = dto.locationId,
            provinceId = dto.provinceId,
            genderId = dto.genderId,
            colorId = null, // You might need to map these from variants
            label = dto.label,
            reserved = dto.totalReserved,
            createdAt = dto.createdAt,
            images = dto.images?.map { imageDto ->
                ItemImage(
                    id = imageDto.id,
                    url = imageDto.url,
                    filename = imageDto.filename,
                    contentType = imageDto.contentType,
                    createdAt = imageDto.createdAt
                )
            } ?: emptyList(),
            shop = dto.shop?.let {
                Shop(
                    id = it.id,
                    name = it.name,
                    displayName = "",
                    userId = 0L,
                    sellerName = "",
                    profilePictureUrl = "",
                    createdAt = "",
                    itemsCount = 0
                )
            },
            meta = null,
            itemTypeId = null
        )
    }
    // ============ GET SINGLE ITEM ============
    override suspend fun getItem(itemId: String): Result<Item> {
        return try {
            val cachedItem = itemDao.getItemById(itemId)
            if (cachedItem != null) {
                Timber.tag(TAG).d("Returning cached item: $itemId")
                return Result.success(cachedItem.toDomain())
            }

            val response = itemApiService.getItem(itemId)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = mapItemDetailToDomain(itemResponse.item)
                itemDao.insertItem(item.toEntity())
                Timber.tag(TAG).i("Fetched item from API: ${item.name} with ${item.images.size} images")
                Result.success(item)
            } else {
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Get item failed")
            Result.failure(e)
        }
    }
    private fun mapItemDetailToDomain(dto: ItemDetailDto): Item {
        val imageList = mutableListOf<ItemImage>()

        // Add cover photo if exists (as first/primary image)
        dto.coverPhoto?.let { url ->
            imageList.add(ItemImage(
                id = 0,  // CDN images don't have IDs
                url = url,
                filename = null,
                contentType = null,
                createdAt = null
            ))
        }

        // Add additional images from images array (avoid duplicate with cover)
        dto.images?.forEach { url ->
            if (url != dto.coverPhoto) {  // Don't add duplicate
                imageList.add(ItemImage(
                    id = 0,
                    url = url,
                    filename = null,
                    contentType = null,
                    createdAt = null
                ))
            }
        }

        // If no cover photo but there's an image field, use that
        if (imageList.isEmpty() && dto.image != null) {
            imageList.add(ItemImage(
                id = 0,
                url = dto.image,
                filename = null,
                contentType = null,
                createdAt = null
            ))
        }

        return Item(
            id = dto.id,
            shopId = dto.shop?.id ?: 0L,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            quantity = dto.quantity,
            status = dto.status,
            createdAt = dto.createdAt,
            images = imageList,
            brandId = dto.brand?.id,
            schoolId = dto.school?.id,
            provinceId = dto.province?.id,
            locationId = dto.town?.id,
            genderId = dto.gender?.id,
            mainCategoryId = dto.mainCategory?.id,
            subCategoryId = dto.subCategory?.id,
            itemConditionId = dto.condition?.id,
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
                    itemsCount = 0
                )
            },
            label = null,
            itemTypeId = null,
            meta = null,
            image = dto.image,
            coverImage = dto.coverPhoto
        )
    }
    // ============ GET ACTIVE ITEMS ============
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

    // In your repository
    override suspend fun getMyShopItems(): Result<List<Item>> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = itemApiService.getMyShopItems("Bearer $token")

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                return Result.failure(Exception("Failed to load items"))
            }

            // ✅ Get shopId from the response
            val shopId = responseBody.shop?.id ?: 0L

            // ✅ Pass shopId to toDomain()
            val items: List<Item> = when (val itemsList = responseBody.items) {
                is List<*> -> {
                    itemsList.mapNotNull { item ->
                        when (item) {
                            is ShopItemDto -> item.toDomain(shopId)  // ✅ Pass shopId here!
                            else -> {
                                Log.w(TAG, "Unexpected item type: ${item?.javaClass?.simpleName}")
                                null
                            }
                        }
                    }
                }
                else -> emptyList()
            }

            _currentItems.value = items

            // Cache items
            try {
                val entities = items.map { it.toEntity() }
                itemDao.insertItems(entities)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache items", e)
            }

            Log.i(TAG, "Fetched ${items.size} shop items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Get my shop items failed", e)
            Result.failure(e)
        }
    }
    // ============ GET SHOP ITEMS ============
    override suspend fun getShopItems(shopId: Long): Result<List<Item>> {
        return try {
            // Try cache first
            val cachedItems = itemDao.getItemsByShopId(shopId)
            if (cachedItems.isNotEmpty()) {
                Log.d(TAG, "Returning ${cachedItems.size} cached items for shop $shopId")
                return Result.success(cachedItems.map { it.toDomain() })
            }

            // Make API call
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

            // ✅ FIXED: Explicitly tell the compiler which mapper to use
            val items: List<Item> = responseBody.items
                ?.map { publicShopItemDto ->
                    publicShopItemDto.toDomain(shopId)  // Now it knows the type
                } ?: emptyList()

            // Cache items
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

    // ============ UPDATE ITEM STATUS ============
    suspend fun updateItemStatus(itemId: String, status: String): Result<Unit> {
        return try {
            itemDao.updateItemStatus(itemId, status)
            Log.i(TAG, "Updated item $itemId status to $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update item status failed", e)
            Result.failure(e)
        }
    }
// Add to ItemRepository.kt - inside the ItemRepository class

    // ============ UPDATE ITEM WITHOUT IMAGES ============
    // ============ UPDATE ITEM WITHOUT IMAGES ============
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
            // 1. Get auth token
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // 2. Prepare update data
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

            // 3. Make API call
            Log.d(TAG, "Updating item $itemId")
            val response = itemApiService.updateItem("Bearer $token", itemId, request)

            if (!response.isSuccessful) {
                val errorMsg = "Failed to update item: ${response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                val errorMsg = responseBody?.message ?: "Failed to update item"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            // 4. Convert response to domain model
            val updatedItem = responseBody.item?.let { mapUpdateDtoToItem(it) }
                ?: return Result.failure(Exception("No item data in response"))

            // 🔥 IMPORTANT: Update in database IMMEDIATELY
            itemDao.insertItem(updatedItem.toEntity())
            Log.d(TAG, "✅ Updated item ${updatedItem.id} in local database with price: ${updatedItem.price}")

            // 5. Update current items list if needed
            val currentList = _currentItems.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == updatedItem.id }
            if (index >= 0) {
                currentList[index] = updatedItem
                _currentItems.value = currentList
                Log.d(TAG, "✅ Updated item in currentItems StateFlow")
            }

            Log.i(TAG, "Item updated successfully: ${updatedItem.name}")
            Result.success(updatedItem)

        } catch (e: Exception) {
            Log.e(TAG, "Update item failed", e)
            Result.failure(e)
        }
    }

    // ============ UPDATE ITEM WITH IMAGE OPERATIONS ============
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
            // 1. First update the item details
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

            // 2. Handle image replacement (complete replace)
            if (replaceAllImages != null) {
                // First remove all existing images
                val token = authRepository.getAuthToken().value ?: return Result.failure(Exception("Not authenticated"))

                // Get current images
                val currentItem = getItem(itemId).getOrNull()
                currentItem?.images?.forEach { image ->
                    try {
                        removeItemImage(itemId, image.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to remove image ${image.id}", e)
                    }
                }

                // Add new images
                if (replaceAllImages.isNotEmpty()) {
                    val imageParts = ImageMultipartHelper.createImageParts(context, replaceAllImages)
                    if (imageParts.isNotEmpty()) {
                        val imagesResponse = itemApiService.addItemImages("Bearer $token", itemId, imageParts)
                        if (imagesResponse.isSuccessful && imagesResponse.body()?.success == true) {
                            val images = imagesResponse.body()?.images ?: emptyList()
                            updatedItem = updatedItem.copy(images = images.map { it.toDomain() })
                        }
                    }
                }
            } else {
                // Handle individual image operations
                val token = authRepository.getAuthToken().value ?: return Result.failure(Exception("Not authenticated"))

                // Remove specified images
                if (removeImageIds.isNotEmpty()) {
                    removeImageIds.forEach { imageId ->
                        try {
                            removeItemImage(itemId, imageId)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to remove image $imageId", e)
                        }
                    }
                }

                // Add new images
                if (addImageUris.isNotEmpty()) {
                    val imageParts = ImageMultipartHelper.createImageParts(context, addImageUris)
                    if (imageParts.isNotEmpty()) {
                        val imagesResponse = itemApiService.addItemImages("Bearer $token", itemId, imageParts)
                        if (imagesResponse.isSuccessful && imagesResponse.body()?.success == true) {
                            val newImages = imagesResponse.body()?.images ?: emptyList()
                            val allImages = updatedItem.images + newImages.map { it.toDomain() }
                            updatedItem = updatedItem.copy(images = allImages)
                        }
                    }
                }
            }

            // 3. Get fresh copy of item
            val finalItem = getItem(itemId).getOrElse { updatedItem }

            // 4. Update in database
            itemDao.insertItem(finalItem.toEntity())

            Result.success(finalItem)

        } catch (e: Exception) {
            Log.e(TAG, "Update item with images failed", e)
            Result.failure(e)
        }
    }

    // ============ DELETE ITEM (SOFT DELETE) ============
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

            // Remove from database (soft delete by updating status or actually remove)
            itemDao.softDeleteItem(itemId)

            // Update current items list
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

    // ============ MARK ITEM AS SOLD ============
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

            // Get updated item
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

    // Helper function to map UpdateItemDto to Item
    private fun mapUpdateDtoToItem(dto: UpdateItemDto): Item {
        return Item(
            id = dto.id,
            shopId = 0L, // This might need to be fetched separately
            name = dto.name,
            description = dto.description,
            price = dto.price.toDoubleOrNull() ?: 0.0,
            quantity = dto.quantity,
            status = dto.status,
            createdAt = dto.createdAt,
            images = dto.images?.map { it.toDomain() } ?: emptyList(),
            brandId = dto.brandId,
            sizeId = dto.sizeId,
            schoolId = dto.schoolId,
            itemConditionId = dto.conditionId,
            locationId = dto.townId,
            provinceId = dto.provinceId,
            genderId = dto.genderId,
            label = null,
            reserved = dto.quantity - dto.availableQuantity,
            meta = if (dto.colorName != null || dto.sizeName != null) {
                ItemMeta(color = dto.colorName, size = dto.sizeName)
            } else null,
            shop = null
        )
    }

}