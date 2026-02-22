package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.SchoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    @Query("SELECT * FROM schools ORDER BY name")
    fun getAll(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getById(id: Int): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schools: List<SchoolEntity>)

    @Query("DELETE FROM schools")
    suspend fun clearAll()
}