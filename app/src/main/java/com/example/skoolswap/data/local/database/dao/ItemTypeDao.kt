// data/local/database/dao/ItemTypeDao.kt
package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.ItemTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemType: ItemTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(itemTypes: List<ItemTypeEntity>)

    // ✅ Return Flow for real-time updates
    @Query("SELECT * FROM item_types ORDER BY name")
    fun getAll(): Flow<List<ItemTypeEntity>>

    @Query("SELECT * FROM item_types WHERE id = :id")
    suspend fun getById(id: Int): ItemTypeEntity?

    @Query("DELETE FROM item_types")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM item_types")
    suspend fun count(): Int
}