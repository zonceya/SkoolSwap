package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.Item
import kotlinx.coroutines.flow.Flow

interface FavoriteRepositoryInterface {
    fun getAllFavorites(userId: Int): Flow<List<Item>>
    suspend fun isFavorite(userId: Int, itemId: String): Boolean
    suspend fun addFavorite(userId: Int, itemId: String)
    suspend fun removeFavorite(userId: Int, itemId: String)
    suspend fun toggleFavorite(userId: Int, itemId: String): Boolean
    suspend fun getFavoritesCount(userId: Int): Int

}