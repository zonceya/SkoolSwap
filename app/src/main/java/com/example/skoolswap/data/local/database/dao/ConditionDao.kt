package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.ConditionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConditionDao {

    @Query("SELECT * FROM conditions ORDER BY id")
    fun getAll(): Flow<List<ConditionEntity>>

    @Query("SELECT * FROM conditions WHERE id = :id")
    suspend fun getById(id: Int): ConditionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conditions: List<ConditionEntity>)

    @Query("DELETE FROM conditions")
    suspend fun clearAll()
}