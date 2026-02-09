// data/repository/ItemTypeRepository.kt - CORRECTED VERSION
package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.database.dao.ItemTypeDao
import com.example.skoolswap.data.mapper.ItemTypeMapper
import com.example.skoolswap.data.remote.api.ItemApiService
import com.example.skoolswap.domain.model.ItemType
import com.example.skoolswap.domain.repository.ItemTypeRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemTypeRepository @Inject constructor(
    private val itemApiService: ItemApiService,
    private val authRepository: AuthRepository,
    private val itemTypeDao: ItemTypeDao
) : ItemTypeRepositoryInterface {

    // Add mutex to prevent duplicate API calls
    private val refreshMutex = Mutex()
    private var isRefreshing = false

    override fun getItemTypes(): Flow<List<ItemType>> {
        return itemTypeDao.getAll()
            .map { entities ->
                Log.d("ItemTypeRepository", "DAO returned ${entities.size} entities")
                entities.map { entity ->
                    ItemTypeMapper.entityToDomain(entity)
                }
            }
    }

    override suspend fun refreshItemTypes(): Result<Unit> {
        Log.d("ItemTypeRepository", "refreshItemTypes() called")

        // Prevent duplicate API calls
        return refreshMutex.withLock {
            if (isRefreshing) {
                Log.d("ItemTypeRepository", "Already refreshing, skipping duplicate call")
                return@withLock Result.success(Unit)
            }

            isRefreshing = true
            try {
                val result = performRefresh()
                isRefreshing = false
                result
            } catch (e: Exception) {
                isRefreshing = false
                throw e
            }
        }
    }

    // Private method for actual refresh logic
    private suspend fun performRefresh(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // 1. Get authentication token - CORRECTED: Access StateFlow value
                val authToken = getAuthTokenFromRepository()

                Log.d("ItemTypeRepository", "Auth token retrieved: ${authToken?.take(10)}...")

                if (authToken == null) {
                    Log.e("ItemTypeRepository", "No auth token available")
                    return@withContext Result.failure(
                        Exception("User not authenticated. Please sign in.")
                    )
                }

                // 2. Fetch from API with token
                Log.d("ItemTypeRepository", "Calling API with auth token")
                val response = itemApiService.getItemTypes("Bearer $authToken")

                Log.d("ItemTypeRepository", "API response code: ${response.code()}")

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("ItemTypeRepository", "API call failed: ${response.code()} - $errorBody")
                    return@withContext Result.failure(
                        Exception("Failed to fetch item types: ${response.code()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    Log.e("ItemTypeRepository", "API returned null body")
                    return@withContext Result.failure(
                        Exception("API returned empty response")
                    )
                }

                // Check if success is true
                if (!body.success) {
                    Log.e("ItemTypeRepository", "API success=false")
                    return@withContext Result.failure(
                        Exception("API request was not successful")
                    )
                }

                val itemTypesDto = body.itemTypes ?: emptyList()
                Log.d("ItemTypeRepository", "Got ${itemTypesDto.size} item types from API")

                // 3. Convert to entities
                val entities = itemTypesDto.map { dto ->
                    ItemTypeMapper.dtoToEntity(dto)
                }

                // 4. Save to database
                Log.d("ItemTypeRepository", "Saving ${entities.size} entities to DB")
                itemTypeDao.clearAll()
                itemTypeDao.insertAll(entities)

                Log.d("ItemTypeRepository", "Successfully refreshed ${entities.size} item types")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("ItemTypeRepository", "Exception in refreshItemTypes", e)
            Result.failure(e)
        }
    }

    // CORRECTED: Get token from StateFlow
    private fun getAuthTokenFromRepository(): String? {
        try {
            // Get the StateFlow from auth repository
            val authTokenFlow = authRepository.getAuthToken()

            // Access the .value property to get current token
            val authToken = authTokenFlow.value

            Log.d("ItemTypeRepository", "StateFlow token value: $authToken")

            return authToken
        } catch (e: Exception) {
            Log.e("ItemTypeRepository", "Error getting auth token from StateFlow: ${e.message}")
            return null
        }
    }

    override suspend fun getItemTypeById(id: Int): ItemType? {
        return withContext(Dispatchers.IO) {
            itemTypeDao.getById(id)?.let { ItemTypeMapper.entityToDomain(it) }
        }
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            itemTypeDao.clearAll()
        }
    }
}