package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.database.dao.FavoriteDao
import com.example.skoolswap.data.local.database.entities.FavoriteEntity
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FavoriteRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val itemRepository: ItemRepository
) : FavoriteRepositoryInterface {

    override fun getAllFavorites(userId: Int): Flow<List<Item>> {
        return favoriteDao.getFavoritesForUser(userId).map { favorites ->
            Timber.d("Loading ${favorites.size} favorites from database for user $userId")

            favorites.mapNotNull { favorite ->
                val result = itemRepository.getItem(favorite.itemId)
                val item = result.getOrNull()

                if (item != null) {
                    Timber.d("Loaded favorite: ${item.name}, Images: ${item.images.size}")
                }
                item
            }
        }
    }

    override suspend fun isFavorite(userId: Int, itemId: String): Boolean {
        return favoriteDao.isFavorite(userId, itemId)
    }

    override suspend fun addFavorite(userId: Int, itemId: String) {
        favoriteDao.addFavorite(FavoriteEntity(userId, itemId))
        Timber.d("Added favorite for user $userId: $itemId")
    }

    override suspend fun removeFavorite(userId: Int, itemId: String) {
        favoriteDao.removeFavorite(userId, itemId)
        Timber.d("Removed favorite for user $userId: $itemId")
    }

    override suspend fun toggleFavorite(userId: Int, itemId: String): Boolean {
        return if (isFavorite(userId, itemId)) {
            removeFavorite(userId, itemId)
            false
        } else {
            addFavorite(userId, itemId)
            true
        }
    }

    override suspend fun getFavoritesCount(userId: Int): Int {
        return favoriteDao.getFavoritesCount(userId)
    }
}