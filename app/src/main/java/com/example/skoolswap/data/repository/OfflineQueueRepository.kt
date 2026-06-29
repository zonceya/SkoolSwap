package com.example.skoolswap.data.repository

import com.example.skoolswap.data.local.database.dao.PendingActionDao
import com.example.skoolswap.data.local.database.entities.PendingActionEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineQueueRepository @Inject constructor(
    private val pendingActionDao: PendingActionDao,
    private val gson: Gson
) {

    suspend fun queueAction(
        actionType: String,
        itemId: String? = null,
        userId: Int? = null,
        data: Any? = null
    ): Long {
        val jsonData = data?.let { gson.toJson(it) }
        val action = PendingActionEntity(
            actionType = actionType,
            itemId = itemId,
            userId = userId,
            data = jsonData,
            createdAt = System.currentTimeMillis()
        )
        return pendingActionDao.insertPendingAction(action)
    }

    fun getPendingActions(): Flow<List<PendingActionEntity>> {
        return pendingActionDao.getPendingActions()
    }

    suspend fun getPendingActionsSync(limit: Int = 50): List<PendingActionEntity> {
        return pendingActionDao.getPendingActionsSync(limit)
    }

    suspend fun markAsCompleted(actionId: Long) {
        pendingActionDao.markAsCompleted(actionId)
    }

    suspend fun incrementRetry(actionId: Long) {
        pendingActionDao.incrementRetry(actionId)
    }

    suspend fun markAsFailed(actionId: Long) {
        pendingActionDao.markAsFailed(actionId)
    }

    suspend fun getPendingCount(): Int {
        return pendingActionDao.getPendingCount()
    }

    suspend fun cleanupCompletedActions() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days
        pendingActionDao.deleteCompletedActionsOlderThan(cutoffTime)
        pendingActionDao.deleteFailedActions()
    }

    suspend fun <T> getData(action: PendingActionEntity, clazz: Class<T>): T? {
        return action.data?.let { gson.fromJson(it, clazz) }
    }
}