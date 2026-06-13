package com.example.skoolswap.data.local.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skoolswap.data.local.database.entities.HomeFeedEntity

@Dao
interface HomeFeedDao {
    @Query("SELECT * FROM home_feed LIMIT 1")
    suspend fun getHomeFeed(): HomeFeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeFeed(feed: HomeFeedEntity)

    @Query("DELETE FROM home_feed")
    suspend fun clearHomeFeed()
}