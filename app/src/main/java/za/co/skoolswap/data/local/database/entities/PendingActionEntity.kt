package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,           // "like", "save", "follow", "report", etc.
    val itemId: String? = null,
    val userId: Int? = null,
    val data: String? = null,         // JSON payload
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: String = "pending"    // "pending", "processing", "completed", "failed"
)