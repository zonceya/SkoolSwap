package com.example.skoolswap.domain.repository

import android.content.Context
import android.net.Uri
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import kotlinx.coroutines.flow.StateFlow

interface ItemRepositoryInterface {

    val recentlyCreatedItem: StateFlow<Item?>
    val currentItems: StateFlow<List<Item>>

    // Create item without images
    suspend fun createItemSimple(
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
        tagIds: List<Int>? = null
    ): Result<Item>

    // Create item with images
    suspend fun createItemWithImages(
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
        tagIds: List<Int>? = null,
        imageUris: List<Uri> = emptyList()
    ): Result<Item>

    // NEW: Update item without images
    suspend fun updateItemSimple(
        itemId: String,
        name: String? = null,
        description: String? = null,
        mainCategoryId: Int? = null,
        subCategoryId: Int? = null,
        brandId: Int? = null,
        price: Double? = null,
        quantity: Int? = null,
        itemConditionId: Int? = null,
        provinceId: Int? = null,
        locationId: Int? = null,
        genderId: Int? = null,
        schoolId: Int? = null,
        sizeId: Int? = null,
        colorId: Int? = null,
        tagIds: List<Int>? = null,
        status: String? = null
    ): Result<Item>

    // NEW: Update item with image operations
    suspend fun updateItemWithImages(
        context: Context,
        itemId: String,
        name: String? = null,
        description: String? = null,
        mainCategoryId: Int? = null,
        subCategoryId: Int? = null,
        brandId: Int? = null,
        price: Double? = null,
        quantity: Int? = null,
        itemConditionId: Int? = null,
        provinceId: Int? = null,
        locationId: Int? = null,
        genderId: Int? = null,
        schoolId: Int? = null,
        sizeId: Int? = null,
        colorId: Int? = null,
        tagIds: List<Int>? = null,
        status: String? = null,
        addImageUris: List<Uri> = emptyList(),
        removeImageIds: List<Long> = emptyList(),
        replaceAllImages: List<Uri>? = null // If provided, replaces all images
    ): Result<Item>

    // Add images to existing item
    suspend fun addItemImages(
        context: Context,
        itemId: String,
        imageUris: List<Uri>
    ): Result<List<ItemImage>>

    // Remove image from item
    suspend fun removeItemImage(
        itemId: String,
        imageId: Long
    ): Result<Int>

    // Get single item
    suspend fun getItem(itemId: String): Result<Item>

    // Get my shop items
    suspend fun getMyShopItems(): Result<List<Item>>
    suspend fun getShopItemForEdit(itemId: String): Result<Item>
    // Get active items
    suspend fun getActiveItems(limit: Int = 50): Result<List<Item>>
    suspend fun getItemWithRefresh(itemId: String): Result<Item>
    // Get shop items
    suspend fun getShopItems(shopId: Long): Result<List<Item>>

    // NEW: Delete item (soft delete)
    suspend fun deleteItem(itemId: String): Result<Unit>

    // NEW: Mark item as sold
    suspend fun markItemAsSold(itemId: String): Result<Item>

    // Clear cached items
    suspend fun clearItems()

}