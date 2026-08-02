package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_cache")
data class ProductsCacheEntity(
    @PrimaryKey
    val cacheKey: String, // "recommended", "trending", "recent", "essentials_uniforms", etc.
    val itemsJson: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val schoolId: Int? = null,
    val sectionType: String? = null, // "recommended", "trending", "recent", "essentials"
    val period: String? = null, // "today", "week", "all"
    val categoryId: Int? = null
)