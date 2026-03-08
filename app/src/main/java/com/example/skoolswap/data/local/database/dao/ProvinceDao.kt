package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.ProvinceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProvinceDao {

    @Query("SELECT * FROM provinces ORDER BY name")
    fun getAll(): Flow<List<ProvinceEntity>>

    @Query("SELECT COUNT(*) FROM provinces")
    suspend fun getCount(): Int
    @Query("SELECT * FROM provinces ORDER BY name")
    suspend fun getAllSync(): List<ProvinceEntity>

    @Query("SELECT * FROM provinces WHERE id = :id")
    suspend fun getById(id: Int): ProvinceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(provinces: List<ProvinceEntity>)

    @Query("DELETE FROM provinces")
    suspend fun clearAll()
}