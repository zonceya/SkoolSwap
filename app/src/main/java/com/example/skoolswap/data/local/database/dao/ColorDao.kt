package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.ColorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColorDao {

    @Query("SELECT * FROM colors ORDER BY name")
    fun getAll(): Flow<List<ColorEntity>>

    @Query("SELECT * FROM colors WHERE id = :id")
    suspend fun getById(id: Int): ColorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colors: List<ColorEntity>)

    @Query("DELETE FROM colors")
    suspend fun clearAll()
}