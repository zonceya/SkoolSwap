package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.database.dao.ProductsCacheDao
import za.co.skoolswap.data.local.database.entities.ProductsCacheEntity
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsCacheRepository @Inject constructor(
    private val productsCacheDao: ProductsCacheDao,
    private val gson: Gson
) : ProductsCacheRepositoryInterface {

    private val CACHE_DURATION_MS = Long.MAX_VALUE // Effectively never expires

    private val _cacheValidMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override fun isCacheValid(cacheKey: String): Boolean {
        return _cacheValidMap.value[cacheKey] ?: false
    }

    override suspend fun cacheProducts(
        cacheKey: String,
        items: List<Item>,
        schoolId: Int?,
        sectionType: String?,
        period: String?,
        categoryId: Int?
    ) {
        try {
            val json = gson.toJson(items)
            val entity = ProductsCacheEntity(
                cacheKey = cacheKey,
                itemsJson = json,
                cachedAt = System.currentTimeMillis(),
                schoolId = schoolId,
                sectionType = sectionType,
                period = period,
                categoryId = categoryId
            )
            productsCacheDao.insertOrUpdate(entity)

            _cacheValidMap.value = _cacheValidMap.value + (cacheKey to true)

            Timber.tag(LogTags.REPOSITORY).d("✅ Cached ${items.size} items for key: $cacheKey")
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to cache products")
        }
    }

    override suspend fun preload() {
        try {
            Timber.tag(LogTags.REPOSITORY).d("🔄 Preloading products cache...")
            val cacheCount = productsCacheDao.getCacheCount()
            if (cacheCount > 0) {
                Timber.tag(LogTags.REPOSITORY).d("✅ Products cache is ready with $cacheCount entries")
            } else {
                Timber.tag(LogTags.REPOSITORY).d("✅ Products cache is empty but ready for use")
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Failed to preload products cache")
        }
    }

    // ✅ FIXED: Cache NEVER expires - always return data if it exists
    // The maxAgeMs parameter is ignored - it's only kept for interface compatibility
    override suspend fun getCachedProducts(
        cacheKey: String,
        schoolId: Int?,
        maxAgeMs: Long  // Kept for interface, but NOT used for expiry
    ): List<Item>? {
        val entity = if (schoolId != null) {
            productsCacheDao.getByKeyAndSchool(cacheKey, schoolId)
        } else {
            productsCacheDao.getByKey(cacheKey)
        }

        if (entity == null) {
            Timber.tag(LogTags.REPOSITORY).d("No cache found for key: $cacheKey")
            return null
        }

        val age = System.currentTimeMillis() - entity.cachedAt
        val ageMinutes = age / 1000 / 60
        val ageHours = ageMinutes / 60
        val ageDays = ageHours / 24

        // ✅ Log age for monitoring, but NEVER expire
        Timber.tag(LogTags.REPOSITORY).d(
            "📦 Cache found for key: $cacheKey (age: ${ageDays}d ${ageHours % 24}h ${ageMinutes % 60}m)"
        )

        // ✅ Always mark as valid - data is always usable
        _cacheValidMap.value = _cacheValidMap.value + (cacheKey to true)

        return try {
            val type = object : TypeToken<List<Item>>() {}.type
            val items: List<Item> = gson.fromJson(entity.itemsJson, type)
            Timber.tag(LogTags.REPOSITORY).d("📦 Loaded ${items.size} items from cache: $cacheKey (age: ${ageDays}d)")
            items
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to parse cached products")
            _cacheValidMap.value = _cacheValidMap.value + (cacheKey to false)
            null
        }
    }

    override suspend fun cacheHomeFeedItems(
        feedItems: List<Item>,
        schoolId: Int,
        sections: Map<String, List<Item>>
    ) {
        sections.forEach { (sectionType, items) ->
            val cacheKey = "${sectionType}_${schoolId}"
            cacheProducts(
                cacheKey = cacheKey,
                items = items,
                schoolId = schoolId,
                sectionType = sectionType
            )
        }

        cacheProducts(
            cacheKey = "home_feed_${schoolId}",
            items = feedItems,
            schoolId = schoolId,
            sectionType = "home_feed"
        )
    }

    override suspend fun getCachedSection(
        sectionType: String,
        schoolId: Int
    ): List<Item>? {
        val cacheKey = "${sectionType}_${schoolId}"
        return getCachedProducts(cacheKey, schoolId)
    }

    override suspend fun clearCache() {
        productsCacheDao.clearAll()
        _cacheValidMap.value = emptyMap()
        Timber.tag(LogTags.REPOSITORY).d("🗑️ All products cache cleared")
    }

    // ============ UNIFORM CACHE ============
    override suspend fun cacheUniforms(
        schoolId: Int,
        gender: String?,
        items: List<Item>
    ) {
        val cacheKey = "uniforms_${schoolId}_${gender ?: "all"}"
        cacheProducts(
            cacheKey = cacheKey,
            items = items,
            schoolId = schoolId,
            sectionType = "uniforms",
            period = gender
        )
    }

    override suspend fun getCachedUniforms(
        schoolId: Int,
        gender: String?
    ): List<Item>? {
        val cacheKey = "uniforms_${schoolId}_${gender ?: "all"}"
        return getCachedProducts(cacheKey, schoolId)
    }

    // ============ SPORT CACHE ============
    override suspend fun cacheSports(
        schoolId: Int,
        sportType: String?,
        items: List<Item>
    ) {
        val cacheKey = "sports_${schoolId}_${sportType ?: "all"}"
        cacheProducts(
            cacheKey = cacheKey,
            items = items,
            schoolId = schoolId,
            sectionType = "sports",
            period = sportType
        )
    }

    override suspend fun getCachedSports(
        schoolId: Int,
        sportType: String?
    ): List<Item>? {
        val cacheKey = "sports_${schoolId}_${sportType ?: "all"}"
        return getCachedProducts(cacheKey, schoolId)
    }

    // ============ RECENT CACHE ============
    override suspend fun cacheRecent(
        schoolId: Int,
        period: String?,
        items: List<Item>
    ) {
        val cacheKey = "recent_${schoolId}_${period ?: "all"}"
        cacheProducts(
            cacheKey = cacheKey,
            items = items,
            schoolId = schoolId,
            sectionType = "recent",
            period = period
        )
    }

    override suspend fun getCachedRecent(
        schoolId: Int,
        period: String?
    ): List<Item>? {
        val cacheKey = "recent_${schoolId}_${period ?: "all"}"
        return getCachedProducts(cacheKey, schoolId)
    }

    // ============ ITEM DETAIL CACHE ============
    override suspend fun cacheItemDetail(
        itemId: String,
        item: Item
    ) {
        val cacheKey = "item_${itemId}"
        cacheProducts(
            cacheKey = cacheKey,
            items = listOf(item),
            sectionType = "item_detail"
        )
    }

    override suspend fun getCachedItemDetail(
        itemId: String
    ): Item? {
        val cacheKey = "item_${itemId}"
        val items = getCachedProducts(cacheKey)
        return items?.firstOrNull()
    }

    // ============ SIMILAR ITEMS CACHE ============
    override suspend fun cacheSimilarItems(
        itemId: String,
        items: List<Item>
    ) {
        val cacheKey = "similar_${itemId}"
        cacheProducts(
            cacheKey = cacheKey,
            items = items,
            sectionType = "similar_items"
        )
    }

    override suspend fun getCachedSimilarItems(
        itemId: String
    ): List<Item>? {
        val cacheKey = "similar_${itemId}"
        return getCachedProducts(cacheKey)
    }
}