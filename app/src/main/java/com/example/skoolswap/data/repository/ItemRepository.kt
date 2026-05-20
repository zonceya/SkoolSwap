package com.example.skoolswap.data.repository

import com.example.skoolswap.data.local.database.dao.ItemImageDao
import com.example.skoolswap.data.local.database.entities.ItemImageEntity
import android.content.Context
import android.net.Uri
import android.util.Log
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
    private val appPreferences: AppPreferences

) : ItemRepositoryInterface {

    companion object {
        private const val TAG = "ItemRepository"
        private const val MAX_IMAGES = 3
        private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes cache
    }

    private val _recentlyCreatedItem = MutableStateFlow<Item?>(null)
    override val recentlyCreatedItem: StateFlow<Item?> = _recentlyCreatedItem.asStateFlow()

    private val _currentItems = MutableStateFlow<List<Item>>(emptyList())
    override val currentItems: StateFlow<List<Item>> = _currentItems.asStateFlow()
    private val memoryCache = mutableMapOf<String, Item>()
    private val memoryCacheTime = mutableMapOf<String, Long>()
    // ============ CREATE ITEM WITHOUT IMAGES ============
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
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body()
            if (responseBody?.success != true || responseBody.item == null) {
                val errorMsg = responseBody?.message ?: "Failed to create item"
                Log.e(TAG, errorMsg)
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
                        // Convert to List<ItemImage> correctly
                        val imageItems: List<ItemImage> = images.map { imageDto ->
                            ItemImage(
                                id = imageDto.id,
                                url = imageDto.url,
                                filename = imageDto.filename,
                                contentType = imageDto.contentType,
                                createdAt = imageDto.createdAt,
                                isCover = false
                            )
                        }
                        finalItem = finalItem.copy(images = imageItems)
                        Log.d(TAG, "Images uploaded successfully: ${imageItems.size} images")
                    } else {
                        Log.w(TAG, "Image upload failed, but item was created")
                    }
                }
            }

            itemDao.insertItem(finalItem.toEntity())
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
        // Convert images to List<ItemImage>
        val imageList = dto.images?.map { imageDto ->
            ItemImage(
                id = imageDto.id,
                url = imageDto.url,
                filename = imageDto.filename,
                contentType = imageDto.contentType,
                createdAt = imageDto.createdAt,
                isCover = false
            )
        } ?: emptyList()

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
            schoolId = dto.schoolId,
            itemConditionId = dto.itemConditionId,
            locationId = dto.locationId,
            provinceId = dto.provinceId,
            genderId = dto.genderId,
            sizeId = dto.size?.id,
            colorId = dto.color?.id,
            label = dto.label,
            reserved = dto.totalReserved,
            createdAt = dto.createdAt,
            images = imageList,  // ← Now List<ItemImage>, not List<String>
            shop = dto.shop?.let {
                Shop(
                    id = it.id,
                    name = it.name,
                    displayName = "",
                    userId = 0L,
                    sellerName = "",
                    profilePictureUrl = "",
                    createdAt = "",
                    sellerMobile = it.sellerMobile,
                    itemsCount = 0
                )
            },
            meta = null,
            itemTypeId = null
        )
    }

    // ============ GET SINGLE ITEM ============
    override suspend fun getItem(itemId: String): Result<Item> {
        Log.d(TAG, "getItem called for ID: $itemId")

        // 1. Check memory cache first (fastest)
        memoryCache[itemId]?.let { cachedItem ->
            val cacheAge = System.currentTimeMillis() - (memoryCacheTime[itemId] ?: 0)
            if (cacheAge < CACHE_DURATION_MS && cachedItem.images.isNotEmpty()) {
                Log.d(TAG, "✅ Using MEMORY cache (age: ${cacheAge}ms)")
                Log.d(TAG, "   - Images: ${cachedItem.images.size}")
                return Result.success(cachedItem)
            } else {
                Log.d(TAG, "⚠️ Memory cache expired or has no images")
                memoryCache.remove(itemId)
                memoryCacheTime.remove(itemId)
            }
        }

        // 2. Check database cache with images
        try {
            val dbItem = itemDao.getItemById(itemId)
            if (dbItem != null) {
                Log.d(TAG, "💾 Using database cache")

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
                    images = images,  // Now with actual images!
                    coverImage = images.firstOrNull { it.isCover }?.url ?: images.firstOrNull()?.url,
                    shop = null,
                    provinceId = dbItem.provinceId,
                    locationId = dbItem.locationId,
                    label = dbItem.label,
                    reserved = dbItem.reserved,
                    meta = null,
                    itemTypeId = dbItem.itemTypeId
                )

                // Update memory cache
                memoryCache[itemId] = domainItem
                memoryCacheTime[itemId] = System.currentTimeMillis()

                return Result.success(domainItem)
            } else {
                Log.d(TAG, "💾 No database cache found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database error: ${e.message}", e)
        }

        // 3. Fetch from network (slowest but most reliable)
        Log.d(TAG, "🌐 Fetching from NETWORK...")
        val startTime = System.currentTimeMillis()

        return try {
            val response = itemApiService.getItem(itemId)
            val duration = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                Log.e(TAG, "Network error: ${response.code()}")
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val itemResponse = response.body()
            if (itemResponse?.success == true && itemResponse.item != null) {
                val item = mapItemDetailToDomain(itemResponse.item)

                Log.d(TAG, "✅ Network fetch successful (${duration}ms):")
                Log.d(TAG, "   - Name: ${item.name}")
                Log.d(TAG, "   - Images: ${item.images.size}")

                // Save to database WITH images
                saveToDatabaseWithImages(item)

                // Update memory cache
                memoryCache[itemId] = item
                memoryCacheTime[itemId] = System.currentTimeMillis()

                Result.success(item)
            } else {
                Log.e(TAG, "Item not found in response")
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception: ${e.message}", e)
            Result.failure(e)
        }
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
    // Extension function to convert Domain Item to ItemEntity
    fun Item.toEntity(): ItemEntity {
        return ItemEntity(
            id = id,
            shopId = shopId,
            name = name,
            description = description,
            price = price,
            quantity = quantity,
            status = status,
            itemTypeId = null, // Add if you have this in your Item domain model
            brandId = brandId,
            sizeId = sizeId,
            schoolId = schoolId,
            sizeName = sizeName,
            colorName = colorName,
            brandName = brandName,
            conditionName = conditionName,
            itemConditionId = itemConditionId,
            locationId = null, // Add if you have this in your Item domain model
            provinceId = provinceId,
            genderId = genderId,
            metaColor = null, // Add if you have this in your Item domain model
            metaSize = null, // Add if you have this in your Item domain model
            label = null, // Add if you have this in your Item domain model
            reserved = 0, // Add if you have this in your Item domain model
            createdAt = createdAt,
            updatedAt = updatedAt,
            deleted = false,
            imageCount = images.size,
            lastCacheTime = System.currentTimeMillis()
        )
    }
    private fun mapItemDetailToDomain(dto: ItemDetailDto): Item {
        val imageList = mutableListOf<ItemImage>()

        // 1. Add cover photo as FIRST image (primary)
        dto.coverPhoto?.let { url ->
            imageList.add(ItemImage(
                id = 0,
                url = url,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = true
            ))
        }

        // 2. Add additional images from images array (avoid duplicate cover)
        dto.images?.forEach { url ->
            if (url != dto.coverPhoto && !imageList.any { it.url == url }) {
                imageList.add(ItemImage(
                    id = 0,
                    url = url,
                    filename = null,
                    contentType = null,
                    createdAt = null,
                    isCover = false
                ))
            }
        }

        // 3. Fallback: If no cover photo but there's an image field, use that
        if (imageList.isEmpty() && dto.image != null) {
            imageList.add(ItemImage(
                id = 0,
                url = dto.image,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = true
            ))
        }

        Log.d("Mapper", "Mapped ${imageList.size} images")

        return Item(
            id = dto.id,
            shopId = dto.shop?.id ?: 0L,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            quantity = dto.quantity,
            status = dto.status,
            createdAt = dto.createdAt,
            images = imageList,  // ← List<ItemImage>
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
            meta = null,
            coverImage = dto.coverPhoto
            // Remove: image = dto.image,
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

            if (!response.isSuccessful) {
                return Result.failure(Exception("Server error: ${response.code()}"))
            }

            val responseBody = response.body()
            if (responseBody?.success != true) {
                return Result.failure(Exception("Failed to load items"))
            }

            val shopId = responseBody.shop?.id ?: 0L

            val items: List<Item> = when (val itemsList = responseBody.items) {
                is List<*> -> {
                    itemsList.mapNotNull { item ->
                        when (item) {
                            is ShopItemDto -> item.toDomain(shopId)
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

            try {
                val entities = items.map { it.toEntity() }
                itemDao.insertItems(entities)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to cache items")
            }

            Timber.tag(TAG).i("Fetched ${items.size} shop items")
            Result.success(items)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Get my shop items failed")
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

            val updatedItem = responseBody.item?.let { mapUpdateDtoToItem(it) }
                ?: return Result.failure(Exception("No item data in response"))

            itemDao.insertItem(updatedItem.toEntity())
            Log.d(TAG, "✅ Updated item ${updatedItem.id} in local database")

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
        // Convert images to List<ItemImage>
        val imageList = dto.images?.map { imageDto ->
            ItemImage(
                id = imageDto.id,
                url = imageDto.url,
                filename = imageDto.filename,
                contentType = imageDto.contentType,
                createdAt = imageDto.createdAt
            )
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
            images = imageList,  // ← Now List<ItemImage>
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