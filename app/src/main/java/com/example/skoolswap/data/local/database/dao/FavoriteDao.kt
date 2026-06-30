package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY addedAt DESC")
    fun getFavoritesForUser(userId: Int): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND itemId = :itemId)")
    suspend fun isFavorite(userId: Int, itemId: String): Boolean

    @Insert
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND itemId = :itemId")
    suspend fun removeFavorite(userId: Int, itemId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId")
    suspend fun getFavoritesCount(userId: Int): Int
}