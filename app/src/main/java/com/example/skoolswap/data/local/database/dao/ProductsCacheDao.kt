package com.example.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.ProductsCacheEntity

@Dao
interface ProductsCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: ProductsCacheEntity)

    @Query("SELECT * FROM products_cache WHERE cacheKey = :cacheKey")
    suspend fun getByKey(cacheKey: String): ProductsCacheEntity?

    @Query("SELECT * FROM products_cache WHERE cacheKey = :cacheKey AND schoolId = :schoolId")
    suspend fun getByKeyAndSchool(cacheKey: String, schoolId: Int): ProductsCacheEntity?

    @Query("SELECT * FROM products_cache WHERE sectionType = :sectionType AND schoolId = :schoolId")
    suspend fun getBySectionType(sectionType: String, schoolId: Int): List<ProductsCacheEntity>

    @Query("DELETE FROM products_cache WHERE cachedAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM products_cache")
    suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM products_cache")
    suspend fun getCacheCount(): Int
}