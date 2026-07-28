// data/local/database/dao/SchoolDao.kt
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

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getByIdSync(id: Int): SchoolEntity?

    // ✅ Get schools in same province
    @Query("""
        SELECT * FROM schools 
        WHERE provinceId = :provinceId AND id != :excludeId
        LIMIT 10
    """)
    suspend fun getByProvinceId(provinceId: Int, excludeId: Int): List<SchoolEntity>

    // ✅ Get schools in same location
    @Query("""
        SELECT * FROM schools 
        WHERE locationId = :locationId AND id != :excludeId
        LIMIT 10
    """)
    suspend fun getByLocationId(locationId: Int, excludeId: Int): List<SchoolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schools: List<SchoolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(school: SchoolEntity)

    @Query("DELETE FROM schools")
    suspend fun clearAll()
}