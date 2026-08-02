package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.Shop
import kotlinx.coroutines.flow.StateFlow

interface ShopRepositoryInterface {
    // Flow for current user's shop
    val currentShop: StateFlow<Shop?>

    // Get current user's shop
    suspend fun getMyShop(): Result<Shop>
    val shopItems: StateFlow<List<Item>>
    suspend fun getMyShopItems(): Result<List<Item>>
    // Update shop display name
    suspend fun updateShopDisplayName(displayName: String): Result<Shop>

    // Get public shop by ID
    suspend fun getPublicShop(shopId: Long): Result<Shop>

    // Clear shop data (on logout)
    suspend fun clearShopData()
}