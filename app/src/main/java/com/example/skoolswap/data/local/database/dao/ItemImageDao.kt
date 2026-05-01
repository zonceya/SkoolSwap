package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.skoolswap.data.local.database.entities.ItemImageEntity

@Dao
interface ItemImageDao {

    @Query("SELECT * FROM item_images WHERE itemId = :itemId ORDER BY position ASC")
    suspend fun getImagesForItem(itemId: String): List<ItemImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ItemImageEntity>)

    @Query("DELETE FROM item_images WHERE itemId = :itemId")
    suspend fun deleteImagesForItem(itemId: String)

    @Transaction
    suspend fun updateImagesForItem(itemId: String, images: List<ItemImageEntity>) {
        deleteImagesForItem(itemId)
        insertImages(images)
    }

    @Query("DELETE FROM item_images")
    suspend fun clearAllImages()
}