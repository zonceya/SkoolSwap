package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.MainCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MainCategoryDao {

    @Query("SELECT * FROM main_categories ORDER BY displayOrder")
    fun getAll(): Flow<List<MainCategoryEntity>>

    @Query("SELECT * FROM main_categories WHERE id = :id")
    suspend fun getById(id: Int): MainCategoryEntity?
    @Query("SELECT COUNT(*) FROM main_categories")
    suspend fun getCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<MainCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: MainCategoryEntity)

    @Query("DELETE FROM main_categories")
    suspend fun clearAll()
}