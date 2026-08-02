package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_feed")
data class HomeFeedEntity(
    @PrimaryKey val id: String = "home_feed",
    val sectionsJson: String,  // Store as JSON
    val cachedAt: Long = System.currentTimeMillis()
)