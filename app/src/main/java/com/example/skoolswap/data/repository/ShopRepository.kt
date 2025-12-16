package com.example.skoolswap.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.skoolswap.data.local.database.dao.ShopDao
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.ShopApiService
import com.example.skoolswap.data.remote.models.request.UpdateShopData
import com.example.skoolswap.data.remote.models.request.UpdateShopRequest
import com.example.skoolswap.domain.model.Shop
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ShopRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class ShopRepository @Inject constructor(
    private val shopApiService: ShopApiService,
    private val shopDao: ShopDao,
    private val authRepository: AuthRepositoryInterface
) : ShopRepositoryInterface {

    companion object {
        private const val TAG = "ShopRepository"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _currentShop = MutableStateFlow<Shop?>(null)
    override val currentShop: StateFlow<Shop?> = _currentShop.asStateFlow()

    init {
        // Load cached shop on initialization
        coroutineScope.launch {
            loadCachedShop()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadCachedShop() {
        try {
            val cachedShop = shopDao.getCurrentShop()
            cachedShop?.let {
                _currentShop.value = it.toDomain()
                Log.d(TAG, "Loaded cached shop: ${it.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached shop", e)
        }
    }

    // In ShopRepository.kt, check the getMyShop() function:
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getMyShop(): Result<Shop> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // Get current user for profile picture - IMPORTANT!
            val currentUser = authRepository.getServerUser().value
            println("DEBUG: Current user in ShopRepository: ${currentUser?.profilePictureUrl}")

            if (currentUser == null) {
                return Result.failure(Exception("User not found"))
            }

            // Fetch from API
            val response = shopApiService.getMyShop("Bearer $token")

            if (response.isSuccessful) {
                val shopResponse = response.body()
                if (shopResponse?.success == true && shopResponse.shop != null) {
                    // Create shop domain with user's profile picture - FIX HERE
                    val shop = shopResponse.shop.toDomain(
                        profilePictureUrl = currentUser.profilePictureUrl ?: "" // Pass the URL
                    )

                    println("DEBUG: Created shop with profilePic: ${shop.profilePictureUrl}")

                    // Cache in database
                    shopDao.insertShop(shop.toEntity())

                    // Update state
                    _currentShop.value = shop

                    Log.i(TAG, "Shop loaded successfully: ${shop.name}")
                    Result.success(shop)
                } else {
                    val errorMsg = shopResponse?.error ?: "Failed to load shop"
                    Log.e(TAG, "API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Log.e(TAG, "Network error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get shop failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updateShopDisplayName(displayName: String): Result<Shop> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // Get current shop
            val existingShop = _currentShop.value
            if (existingShop == null) {
                // Try to load shop first
                val loadResult = getMyShop()
                if (loadResult.isFailure) {
                    return Result.failure(Exception("Shop not loaded: ${loadResult.exceptionOrNull()?.message}"))
                }
            }

            val currentShop = _currentShop.value ?: return Result.failure(Exception("Shop not available"))

            // Prepare request
            val request = UpdateShopRequest(
                shop = UpdateShopData(display_name = displayName)
            )

            // Send to API
            val response = shopApiService.updateShop("Bearer $token", request)

            if (response.isSuccessful) {
                val updateResponse = response.body()
                if (updateResponse?.success == true && updateResponse.shop != null) {
                    // Create updated shop
                    val updatedShop = currentShop.copy(
                        displayName = displayName
                    )

                    // Update database
                    shopDao.updateShopDisplayName(
                        shopId = updatedShop.id,
                        displayName = displayName,
                        updatedAt = java.time.Instant.now().toString()
                    )

                    // Update state
                    _currentShop.value = updatedShop

                    Log.i(TAG, "Shop display name updated to: $displayName")
                    Result.success(updatedShop)
                } else {
                    val errorMsg = updateResponse?.error ?:
                    updateResponse?.errors?.joinToString(", ") ?:
                    "Failed to update shop"
                    Log.e(TAG, "Update API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Log.e(TAG, "Network error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update shop failed", e)
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getPublicShop(shopId: Long): Result<Shop> {
        return try {
            // Try cache first
            val cachedShop = shopDao.getShopById(shopId)
            if (cachedShop != null) {
                Log.d(TAG, "Returning cached public shop: ${cachedShop.name}")
                return Result.success(cachedShop.toDomain())
            }

            // Fetch from API
            val response = shopApiService.getPublicShop(shopId)

            if (response.isSuccessful) {
                val publicResponse = response.body()
                if (publicResponse?.success == true && publicResponse.shop != null) {
                    // Create shop (public API doesn't provide profile picture)
                    val shop = Shop(
                        id = publicResponse.shop.id,
                        name = publicResponse.shop.name,
                        displayName = "", // Not in public response
                        userId = publicResponse.shop.seller.id,
                        sellerName = publicResponse.shop.seller.name,
                        profilePictureUrl = "", // Will be fetched separately
                        createdAt = publicResponse.shop.created_at,
                        itemsCount = publicResponse.shop.stats.total_items
                    )

                    // Cache in database
                    shopDao.insertShop(shop.toEntity())

                    Log.i(TAG, "Public shop loaded: ${shop.name}")
                    Result.success(shop)
                } else {
                    val errorMsg = publicResponse?.error ?: "Failed to load public shop"
                    Log.e(TAG, "API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Log.e(TAG, "Network error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get public shop failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearShopData() {
        try {
            shopDao.clearAllShops()
            _currentShop.value = null
            Log.i(TAG, "Shop data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing shop data", e)
        }
    }
}