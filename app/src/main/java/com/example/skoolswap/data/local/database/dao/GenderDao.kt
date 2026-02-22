package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.GenderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenderDao {

    @Query("SELECT * FROM genders ORDER BY name")
    fun getAll(): Flow<List<GenderEntity>>

    @Query("SELECT * FROM genders WHERE id = :id")
    suspend fun getById(id: Int): GenderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(genders: List<GenderEntity>)

    @Query("DELETE FROM genders")
    suspend fun clearAll()
}