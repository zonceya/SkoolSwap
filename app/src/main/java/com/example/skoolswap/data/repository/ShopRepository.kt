package com.example.skoolswap.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.database.dao.ShopDao
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.mapper.toDomain
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.data.remote.api.ShopApiService
import com.example.skoolswap.data.remote.models.request.UpdateShopData
import com.example.skoolswap.data.remote.models.request.UpdateShopRequest
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.Shop
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ShopRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class ShopRepository @Inject constructor(
    private val shopApiService: ShopApiService,
    private val shopDao: ShopDao,
    private val authRepository: AuthRepositoryInterface,
    private val itemRepository: ItemRepositoryInterface,
    private val appPreferences: AppPreferences
) : ShopRepositoryInterface {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _currentShop = MutableStateFlow<Shop?>(null)
    override val currentShop: StateFlow<Shop?> = _currentShop.asStateFlow()

    private val _shopItems = MutableStateFlow<List<Item>>(emptyList())
    override val shopItems: StateFlow<List<Item>> = _shopItems.asStateFlow()

    init {
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
                Timber.tag(LogTags.REPOSITORY).d("Loaded cached shop: ${it.name}")
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Error loading cached shop")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getMyShop(): Result<Shop> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val currentUser = authRepository.getServerUser().value
            Timber.tag(LogTags.REPOSITORY).d("Current user in ShopRepository: ${currentUser?.profilePictureUrl}")

            if (currentUser == null) {
                return Result.failure(Exception("User not found"))
            }

            val response = shopApiService.getMyShop("Bearer $token")

            if (response.isSuccessful) {
                val shopResponse = response.body()
                if (shopResponse?.success == true && shopResponse.shop != null) {
                    val shop = shopResponse.shop.toDomain(
                        profilePictureUrl = currentUser.profilePictureUrl ?: ""
                    )

                    Timber.tag(LogTags.REPOSITORY).d("Created shop with profilePic: ${shop.profilePictureUrl}")

                    shopDao.insertShop(shop.toEntity())
                    _currentShop.value = shop

                    Timber.tag(LogTags.REPOSITORY).i("Shop loaded successfully: ${shop.name}")
                    Result.success(shop)
                } else {
                    val errorMsg = shopResponse?.error ?: "Failed to load shop"
                    Timber.tag(LogTags.REPOSITORY).e("API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Timber.tag(LogTags.REPOSITORY).e("Network error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get shop failed")
            Result.failure(e)
        }
    }

    override suspend fun getMyShopItems(): Result<List<Item>> {
        return try {
            val result = itemRepository.getMyShopItems()

            result.onSuccess { items ->
                _shopItems.value = items
                Timber.tag(LogTags.REPOSITORY).d("Loaded ${items.size} shop items")
            }

            result
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get my shop items failed")
            Result.failure(e)
        }
    }

    override suspend fun updateShopDisplayName(displayName: String): Result<Shop> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val existingShop = _currentShop.value
            if (existingShop == null) {
                val loadResult = getMyShop()
                if (loadResult.isFailure) {
                    return Result.failure(Exception("Shop not loaded: ${loadResult.exceptionOrNull()?.message}"))
                }
            }

            val currentShop = _currentShop.value ?: return Result.failure(Exception("Shop not available"))

            val request = UpdateShopRequest(
                shop = UpdateShopData(display_name = displayName)
            )

            val response = shopApiService.updateShop("Bearer $token", request)

            if (response.isSuccessful) {
                val updateResponse = response.body()
                if (updateResponse?.success == true && updateResponse.shop != null) {
                    val updatedShop = currentShop.copy(
                        displayName = displayName
                    )

                    shopDao.updateShopDisplayName(
                        shopId = updatedShop.id,
                        displayName = displayName,
                        updatedAt = java.time.Instant.now().toString()
                    )

                    _currentShop.value = updatedShop

                    Timber.tag(LogTags.REPOSITORY).i("Shop display name updated to: $displayName")
                    Result.success(updatedShop)
                } else {
                    val errorMsg = updateResponse?.error ?:
                    updateResponse?.errors?.joinToString(", ") ?:
                    "Failed to update shop"
                    Timber.tag(LogTags.REPOSITORY).e("Update API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Timber.tag(LogTags.REPOSITORY).e("Network error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Update shop failed")
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getPublicShop(shopId: Long): Result<Shop> {
        return try {
            val cachedShop = shopDao.getShopById(shopId)
            if (cachedShop != null) {
                Timber.tag(LogTags.REPOSITORY).d("Returning cached public shop: ${cachedShop.name}")
                return Result.success(cachedShop.toDomain())
            }

            val response = shopApiService.getPublicShop(shopId)

            if (response.isSuccessful) {
                val publicResponse = response.body()
                if (publicResponse?.success == true && publicResponse.shop != null) {
                    val shop = Shop(
                        id = publicResponse.shop.id,
                        name = publicResponse.shop.name,
                        displayName = "",
                        userId = publicResponse.shop.seller.id,
                        sellerName = publicResponse.shop.seller.name,
                        sellerMobile = null,
                        profilePictureUrl = "",
                        createdAt = publicResponse.shop.createdAt,
                        itemsCount = publicResponse.shop.stats.totalItems
                    )

                    shopDao.insertShop(shop.toEntity())

                    Timber.tag(LogTags.REPOSITORY).i("Public shop loaded: ${shop.name}")
                    Result.success(shop)
                } else {
                    val errorMsg = publicResponse?.error ?: "Failed to load public shop"
                    Timber.tag(LogTags.REPOSITORY).e("API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Timber.tag(LogTags.REPOSITORY).e("Network error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Get public shop failed")
            Result.failure(e)
        }
    }

    override suspend fun clearShopData() {
        try {
            shopDao.clearAllShops()
            _currentShop.value = null
            _shopItems.value = emptyList()
            Timber.tag(LogTags.REPOSITORY).i("Shop data cleared")
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Error clearing shop data")
        }
    }
}