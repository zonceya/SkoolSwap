package za.co.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import za.co.skoolswap.data.local.database.entities.TownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TownDao {

    @Query("SELECT * FROM towns ORDER BY name")
    fun getAll(): Flow<List<TownEntity>>

    @Query("SELECT * FROM towns WHERE provinceId = :provinceId ORDER BY name")
    fun getByProvinceId(provinceId: Int): Flow<List<TownEntity>>

    @Query("SELECT * FROM towns WHERE id = :id")
    suspend fun getById(id: Int): TownEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(towns: List<TownEntity>)

    @Query("DELETE FROM towns WHERE provinceId = :provinceId")
    suspend fun deleteByProvinceId(provinceId: Int)

    @Query("DELETE FROM towns")
    suspend fun clearAll()
}