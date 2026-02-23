package com.example.skoolswap.domain.repository

import android.content.Context
import android.net.Uri
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import kotlinx.coroutines.flow.StateFlow

interface ItemRepositoryInterface {

    val recentlyCreatedItem: StateFlow<Item?>
    val currentItems: StateFlow<List<Item>>

    // Create item without images - UPDATED parameter names
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

    // Create item with images - UPDATED parameter names
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
    suspend fun getMyShopItems(): Result<List<Item>>
    // Get active items
    suspend fun getActiveItems(limit: Int = 50): Result<List<Item>>
    // Get shop items
    suspend fun getShopItems(shopId: Long): Result<List<Item>>
    // Clear cached items
    suspend fun clearItems()
}