package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.Item
import kotlinx.coroutines.flow.Flow

interface FavoriteRepositoryInterface {
    fun getAllFavorites(): Flow<List<Item>>
    suspend fun isFavorite(itemId: String): Boolean
    suspend fun addFavorite(itemId: String)
    suspend fun removeFavorite(itemId: String)
    suspend fun toggleFavorite(itemId: String): Boolean
    suspend fun getFavoritesCount(): Int
}