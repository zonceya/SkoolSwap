package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.SubCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubCategoryDao {

    @Query("SELECT * FROM sub_categories ORDER BY displayOrder")
    fun getAll(): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE mainCategoryId = :mainCategoryId ORDER BY displayOrder")
    fun getByMainCategoryId(mainCategoryId: Int): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE id = :id")
    suspend fun getById(id: Int): SubCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subCategories: List<SubCategoryEntity>)

    @Query("DELETE FROM sub_categories WHERE mainCategoryId = :mainCategoryId")
    suspend fun deleteByMainCategoryId(mainCategoryId: Int)

    @Query("DELETE FROM sub_categories")
    suspend fun clearAll()
}