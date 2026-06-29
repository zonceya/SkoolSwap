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

    // ✅ ADD THESE METHODS FOR IMAGE CLEANUP

    // Time-based cleanup: Delete images older than cutoff time
    @Query("DELETE FROM item_images WHERE createdAt < :cutoffTime")
    suspend fun deleteImagesOlderThan(cutoffTime: Long): Int

    // Cap-based cleanup: Get all images sorted by age (oldest first)
    @Query("SELECT * FROM item_images ORDER BY createdAt ASC")
    suspend fun getAllImagesSortedByAge(): List<ItemImageEntity>

    // Get total size of all images
    @Query("SELECT SUM(fileSize) FROM item_images")
    suspend fun getTotalImageSize(): Long?

    // Delete a specific image by ID
    @Query("DELETE FROM item_images WHERE id = :imageId")
    suspend fun deleteImage(imageId: Long)

    // Get total count of images
    @Query("SELECT COUNT(*) FROM item_images")
    suspend fun getImageCount(): Int
}