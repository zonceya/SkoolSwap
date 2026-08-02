package za.co.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import za.co.skoolswap.data.local.database.entities.ItemEntity

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Query("SELECT * FROM items WHERE shopId = :shopId AND deleted = 0")
    suspend fun getItemsByShopId(shopId: Long): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE status = 'active' AND deleted = 0 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getActiveItems(limit: Int = 50): List<ItemEntity>

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET status = :status WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: String, status: String)

    @Query("UPDATE items SET deleted = 1 WHERE id = :itemId")
    suspend fun softDeleteItem(itemId: String)

    @Query("UPDATE items SET imageCount = :count WHERE id = :itemId")
    suspend fun updateImageCount(itemId: String, count: Int)

    @Query("DELETE FROM items")
    suspend fun clearAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(item: ItemEntity)

    @Query("DELETE FROM items WHERE lastCacheTime < :expiryTime")
    suspend fun deleteExpiredItems(expiryTime: Long)

    @Query("SELECT * FROM items WHERE id = :itemId AND lastCacheTime > :expiryTime")
    suspend fun getValidItemById(itemId: String, expiryTime: Long): ItemEntity?
    // ItemDao.kt - Add these methods

    @Query("SELECT * FROM items WHERE syncStatus = 'UPLOADING'")
    suspend fun getUploadingItems(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE syncStatus = 'FAILED'")
    suspend fun getFailedItems(): List<ItemEntity>
    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("UPDATE items SET syncStatus = :status, syncError = :error WHERE id = :itemId")
    suspend fun updateSyncStatus(itemId: String, status: String, error: String? = null)
}
