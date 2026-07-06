package com.example.skoolswap.data.repository

import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.database.dao.ProductsCacheDao
import com.example.skoolswap.data.local.database.entities.ProductsCacheEntity
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsCacheRepository @Inject constructor(
    private val productsCacheDao: ProductsCacheDao,
    private val gson: Gson
) : ProductsCacheRepositoryInterface {

    // Constants - internal use only
    private val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours

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

    override suspend fun getCachedProducts(
        cacheKey: String,
        schoolId: Int?,
        maxAgeMs: Long
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
        if (age > maxAgeMs) {
            Timber.tag(LogTags.REPOSITORY).d("Cache expired for key: $cacheKey (age: ${age / 1000 / 60}min)")
            _cacheValidMap.value = _cacheValidMap.value + (cacheKey to false)
            return null
        }

        return try {
            val type = object : TypeToken<List<Item>>() {}.type
            val items: List<Item> = gson.fromJson(entity.itemsJson, type)
            _cacheValidMap.value = _cacheValidMap.value + (cacheKey to true)
            Timber.tag(LogTags.REPOSITORY).d("📦 Loaded ${items.size} items from cache: $cacheKey")
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