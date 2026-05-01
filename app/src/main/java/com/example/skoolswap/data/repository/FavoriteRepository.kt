package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.database.dao.FavoriteDao
import com.example.skoolswap.data.local.database.entities.FavoriteEntity
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FavoriteRepositoryInterface
import com.example.skoolswap.ui.products.PriceRangeDialogFragment.Companion.TAG
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

    override fun getAllFavorites(): Flow<List<Item>> {
        return favoriteDao.getAllFavorites().map { favorites ->
            Timber.tag("PriceRangeDialogFragmen").d("Loading ${favorites.size} favorites from database")

            favorites.mapNotNull { favorite ->
                // This will now use the cache which includes images from database
                val result = itemRepository.getItem(favorite.itemId)
                val item = result.getOrNull()

                if (item != null) {
                    Timber.tag("PriceRangeDialogFragmen").d("Loaded favorite: ${item.name}, Images: ${item.images.size}")
                    if (item.images.isEmpty()) {
                        Log.w(TAG, "⚠️ Item ${item.name} has NO images in cache")
                    } else {
                        Log.d(TAG, "✅ First image: ${item.images.first().url}")
                    }
                }
                item
            }
        }
    }

    override suspend fun isFavorite(itemId: String): Boolean {
        return favoriteDao.isFavorite(itemId)
    }

    override suspend fun addFavorite(itemId: String) {
        favoriteDao.addFavorite(FavoriteEntity(itemId))
    }

    override suspend fun removeFavorite(itemId: String) {
        favoriteDao.removeFavoriteById(itemId)
    }

    override suspend fun toggleFavorite(itemId: String): Boolean {
        return if (isFavorite(itemId)) {
            removeFavorite(itemId)
            false
        } else {
            addFavorite(itemId)
            true
        }
    }

    override suspend fun getFavoritesCount(): Int {
        return favoriteDao.getFavoritesCount()
    }
}