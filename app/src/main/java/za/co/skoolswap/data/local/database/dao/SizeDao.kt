package za.co.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import za.co.skoolswap.data.local.database.entities.SizeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SizeDao {

    @Query("SELECT * FROM sizes ORDER BY name")
    fun getAll(): Flow<List<SizeEntity>>

    @Query("SELECT * FROM sizes WHERE id = :id")
    suspend fun getById(id: Int): SizeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sizes: List<SizeEntity>)

    @Query("DELETE FROM sizes")
    suspend fun clearAll()
}