package za.co.skoolswap.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import za.co.skoolswap.data.local.database.entities.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingActionEntity): Long

    @Update
    suspend fun updatePendingAction(action: PendingActionEntity)

    @Query("SELECT * FROM pending_actions WHERE status = 'pending' ORDER BY createdAt ASC")
    fun getPendingActions(): Flow<List<PendingActionEntity>>

    @Query("SELECT * FROM pending_actions WHERE status = 'pending' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingActionsSync(limit: Int = 50): List<PendingActionEntity>

    @Query("UPDATE pending_actions SET status = 'completed' WHERE id = :actionId")
    suspend fun markAsCompleted(actionId: Long)

    @Query("UPDATE pending_actions SET retryCount = retryCount + 1, status = 'pending' WHERE id = :actionId")
    suspend fun incrementRetry(actionId: Long)

    @Query("UPDATE pending_actions SET status = 'failed' WHERE id = :actionId")
    suspend fun markAsFailed(actionId: Long)

    @Query("DELETE FROM pending_actions WHERE status = 'completed' AND createdAt < :cutoffTime")
    suspend fun deleteCompletedActionsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM pending_actions WHERE status = 'failed' AND retryCount >= maxRetries")
    suspend fun deleteFailedActions(): Int

    @Query("SELECT COUNT(*) FROM pending_actions WHERE status = 'pending'")
    suspend fun getPendingCount(): Int
}