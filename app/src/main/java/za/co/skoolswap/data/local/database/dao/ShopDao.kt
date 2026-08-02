package za.co.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import za.co.skoolswap.data.local.database.entities.ShopEntity

@Dao
interface ShopDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("SELECT * FROM shops WHERE id = :shopId")
    suspend fun getShopById(shopId: Long): ShopEntity?

    @Query("SELECT * FROM shops WHERE userId = :userId")
    suspend fun getShopByUserId(userId: Long): ShopEntity?

    @Query("SELECT * FROM shops LIMIT 1")
    suspend fun getCurrentShop(): ShopEntity?

    @Query("UPDATE shops SET displayName = :displayName, updatedAt = :updatedAt WHERE id = :shopId")
    suspend fun updateShopDisplayName(shopId: Long, displayName: String, updatedAt: String)

    @Query("UPDATE shops SET profilePictureUrl = :profilePictureUrl, sellerName = :sellerName WHERE userId = :userId")
    suspend fun updateShopWithUserProfile(userId: Long, profilePictureUrl: String, sellerName: String)

    @Query("DELETE FROM shops")
    suspend fun clearAllShops()
}