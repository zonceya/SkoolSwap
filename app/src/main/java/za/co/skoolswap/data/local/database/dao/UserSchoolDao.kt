package za.co.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import za.co.skoolswap.data.local.database.entities.UserSchoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSchoolDao {

    @Query("SELECT * FROM user_schools WHERE userId = :userId LIMIT 1")
    fun getCurrentForUser(userId: Int): Flow<UserSchoolEntity?>

    @Query("SELECT * FROM user_schools WHERE userId = :userId LIMIT 1")
    suspend fun getCurrentForUserSync(userId: Int): UserSchoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userSchool: UserSchoolEntity)

    @Query("DELETE FROM user_schools WHERE userId = :userId")
    suspend fun deleteForUser(userId: Int)

    @Query("DELETE FROM user_schools")
    suspend fun clearAll()
}