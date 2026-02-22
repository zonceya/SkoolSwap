package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.BrandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandDao {

    @Query("SELECT * FROM brands ORDER BY name")
    fun getAll(): Flow<List<BrandEntity>>

    @Query("SELECT * FROM brands WHERE id = :id")
    suspend fun getById(id: Int): BrandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(brands: List<BrandEntity>)

    @Query("DELETE FROM brands")
    suspend fun clearAll()
}