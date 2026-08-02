package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemTypeRepositoryInterface {

    // Get all item types
    fun getItemTypes(): Flow<List<ItemType>>

    // Get item type by ID
    suspend fun getItemTypeById(id: Int): ItemType?

    // Refresh from API
    suspend fun refreshItemTypes(): Result<Unit>

    // Clear local cache
    suspend fun clearCache()
}