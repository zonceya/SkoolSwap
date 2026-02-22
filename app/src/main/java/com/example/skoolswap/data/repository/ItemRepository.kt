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
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.utils.ImageMultipartHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                Log.d(TAG, "Uploading ${imageUris.size} images")
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
                Log.i(TAG, "Added ${images.size} images to item $itemId")
                Result.success(images)
            } else {
                val errorMsg = body?.message ?: "Failed to upload images"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Add item images failed", e)
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
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body?.success == true) {
                val remaining = body.remainingImages ?: 0
                Log.i(TAG, "Image $imageId removed from item $itemId. Remaining: $remaining")
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

    // ============ GET SINGLE ITEM ============
    override suspend fun getItem(itemId: String): Result<Item> {
        return try {
            val cachedItem = itemDao.getItemById(itemId)
            if (cachedItem != null) {
                Log.d(TAG, "Returning cached item: $itemId")
                return Result.success(cachedItem.toDomain())
            }

            val response = itemApiService.getItem(null, itemId)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = itemResponse.toDomain()
                itemDao.insertItem(item.toEntity())
                Log.i(TAG, "Fetched item from API: ${item.name}")
                Result.success(item)
            } else {
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get item failed", e)
            Result.failure(e)
        }
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

    // ============ GET SHOP ITEMS ============
    override suspend fun getShopItems(shopId: Long): Result<List<Item>> {
        return try {
            val cachedItems = itemDao.getItemsByShopId(shopId)
            if (cachedItems.isNotEmpty()) {
                Log.d(TAG, "Returning ${cachedItems.size} cached items for shop $shopId")
                return Result.success(cachedItems.map { it.toDomain() })
            }

            val response = itemApiService.getShopItems(shopId)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val items = response.body()?.mapNotNull { it.toDomain() } ?: emptyList()
            itemDao.insertItems(items.map { it.toEntity() })

            Log.i(TAG, "Fetched ${items.size} items for shop $shopId from API")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Get shop items failed", e)
            Result.failure(e)
        }
    }

    // ============ CLEAR CACHED ITEMS ============
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

    // ============ SOFT DELETE ITEM ============
    suspend fun softDeleteItem(itemId: String): Result<Unit> {
        return try {
            itemDao.softDeleteItem(itemId)
            Log.i(TAG, "Soft deleted item $itemId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Soft delete item failed", e)
            Result.failure(e)
        }
    }
}