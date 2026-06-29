// data/mapper/RecommendationMapper.kt
package com.example.skoolswap.data.mapper

import android.util.Log
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.dao.ItemImageDao
import com.example.skoolswap.data.local.database.entities.HomeFeedEntity
import com.example.skoolswap.data.local.database.entities.ItemImageEntity
import com.example.skoolswap.data.remote.models.response.home.EssentialsSectionsDto
import com.example.skoolswap.data.remote.models.response.home.HomeRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.HomeSectionDto
import com.example.skoolswap.data.remote.models.response.home.RecentRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.RecentSectionDto
import com.example.skoolswap.data.remote.models.response.home.RecommendationItemDto
import com.example.skoolswap.data.remote.models.response.home.SportRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.SportSectionDto
import com.example.skoolswap.data.remote.models.response.home.UniformRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.UniformSectionDto
import com.example.skoolswap.domain.model.*
import com.example.skoolswap.domain.model.homefeed.EssentialsSections
import com.example.skoolswap.domain.model.homefeed.HomeFeed
import com.example.skoolswap.domain.model.homefeed.RecentFeed
import com.example.skoolswap.domain.model.homefeed.RecentSection
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.domain.model.homefeed.SportFeed
import com.example.skoolswap.domain.model.homefeed.SportSection
import com.example.skoolswap.domain.model.homefeed.UniformFeed
import com.example.skoolswap.domain.model.homefeed.UniformSection
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import timber.log.Timber
import java.lang.reflect.Type


// Replace the existing `private val gson = ...` at the top of RecommendationMapper.kt

private val gson = GsonBuilder()
    .registerTypeAdapter(Section::class.java, object : JsonDeserializer<Section>,
        JsonSerializer<Section> {

        override fun serialize(
            src: Section,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            // Serialize the concrete subtype, then make sure "type" field is present
            val element = when (src) {
                is Section.Recommended -> context.serialize(src, Section.Recommended::class.java)
                is Section.Essentials  -> context.serialize(src, Section.Essentials::class.java)
                is Section.Trending    -> context.serialize(src, Section.Trending::class.java)
                is Section.Recent      -> context.serialize(src, Section.Recent::class.java)
            }.asJsonObject
            // Ensure the "type" discriminator survives the round-trip
            if (!element.has("type")) element.addProperty("type", src.type)
            return element
        }

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Section {
            val obj  = json.asJsonObject
            val type = obj.get("type")?.asString ?: "recommended"
            return when (type) {
                "recommended", "nearby"                   -> context.deserialize(json, Section.Recommended::class.java)
                "essentials", "nearby_essentials"         -> context.deserialize(json, Section.Essentials::class.java)
                "trending", "nearby_trending"             -> context.deserialize(json, Section.Trending::class.java)
                "recent", "nearby_recent"                 -> context.deserialize(json, Section.Recent::class.java)
                else                                      -> context.deserialize(json, Section.Recommended::class.java)
            }
        }
    })
    .create()
// ============ HOME RESPONSE TO DOMAIN ============
fun HomeRecommendationResponse.toDomain(): HomeFeed {
    return HomeFeed(
        success = success,
        schoolId = schoolId,
        message = message,
        sections = sections.map { it.toDomain() }
    )
}

fun HomeSectionDto.toDomain(): Section {
    return when (type) {
        "recommended", "nearby" -> {
            Section.Recommended(
                title = title,
                type = type,
                items = items?.map { it.toDomain() } ?: emptyList()
            )
        }
        "essentials", "nearby_essentials" -> {
            Section.Essentials(
                title = title,
                type = type,
                sections = sections?.toDomain() ?: EssentialsSections(
                    uniforms = emptyList(),
                    sports = emptyList(),
                    accessories = emptyList()
                )
            )
        }
        "trending", "nearby_trending" -> {
            Section.Trending(
                title = title,
                type = type,
                items = items?.map { it.toDomain() } ?: emptyList()
            )
        }
        "recent", "nearby_recent" -> {
            Section.Recent(
                title = title,
                type = type,
                items = items?.map { it.toDomain() } ?: emptyList()
            )
        }
        else -> {
            // Default to recommended
            Section.Recommended(
                title = title,
                type = type,
                items = items?.map { it.toDomain() } ?: emptyList()
            )
        }
    }
}

fun EssentialsSectionsDto.toDomain(): EssentialsSections {
    return EssentialsSections(
        uniforms = uniforms.map { it.toDomain() },
        sports = sports.map { it.toDomain() },
        accessories = accessories.map { it.toDomain() }
    )
}

// ============ UNIFORM RESPONSE TO DOMAIN ============
fun UniformRecommendationResponse.toDomain(): UniformFeed {
    return UniformFeed(
        success = success,
        schoolId = schoolId,
        gender = gender,
        message = message,
        sections = sections.map { it.toDomain() }
    )
}

fun UniformSectionDto.toDomain(): UniformSection {
    return UniformSection(
        title = title,
        type = type,
        items = items.map { it.toDomain() }
    )
}

// ============ SPORT RESPONSE TO DOMAIN ============
fun SportRecommendationResponse.toDomain(): SportFeed {
    return SportFeed(
        success = success,
        schoolId = schoolId,
        message = message,
        sections = sections.map { it.toDomain() }
    )
}

fun SportSectionDto.toDomain(): SportSection {
    return SportSection(
        title = title,
        type = type,
        items = items.map { it.toDomain() }
    )
}

// ============ RECENT RESPONSE TO DOMAIN ============
fun RecentRecommendationResponse.toDomain(): RecentFeed {
    return RecentFeed(
        success = success,
        schoolId = schoolId,
        message = message,
        sections = sections?.map { it.toDomain() } ?: emptyList(),
        items = items?.map { it.toDomain() }
    )
}

fun RecentSectionDto.toDomain(): RecentSection {
    return RecentSection(
        title = title,
        period = period,
        items = items.map { it.toDomain() }
    )
}
// ============ LOCAL CACHE ============
fun HomeFeed.toEntity(): HomeFeedEntity {
    return try {
        val json = gson.toJson(this)   // ← was Gson().toJson(this)
        Timber.tag("Mapper").d("Converting HomeFeed to JSON, sections: ${sections.size}")
        HomeFeedEntity(
            id = "home_feed",
            sectionsJson = json,
            cachedAt = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        Timber.tag("Mapper").e(e, "Failed to convert HomeFeed to JSON: ${e.message}")
        HomeFeedEntity(id = "home_feed", sectionsJson = "{}", cachedAt = System.currentTimeMillis())
    }
}

fun HomeFeedEntity.toDomain(): HomeFeed {
    return try {
        Timber.tag("Mapper").d("Parsing JSON, length: ${sectionsJson.length}")
        val feed = gson.fromJson(sectionsJson, HomeFeed::class.java)  // ← was Gson().fromJson(...)
        Timber.tag("Mapper").d("Parsed feed, sections: ${feed.sections.size}")
        feed
    } catch (e: Exception) {
        Timber.tag("Mapper").e(e, "Failed to parse HomeFeed from JSON: ${e.message}")
        Timber.tag("Mapper").e("JSON: ${sectionsJson.take(500)}")
        HomeFeed(success = false, schoolId = 0, message = null, sections = emptyList())
    }
}
fun HomeFeed.getAllItems(): List<Item> {
    val allItems = mutableListOf<Item>()

    sections.forEach { section ->
        when (section) {
            is Section.Recommended -> allItems.addAll(section.items)
            is Section.Trending -> allItems.addAll(section.items)
            is Section.Recent -> allItems.addAll(section.items)
            is Section.Essentials -> {
                allItems.addAll(section.sections.uniforms)
                allItems.addAll(section.sections.sports)
                allItems.addAll(section.sections.accessories)
            }
        }
    }

    return allItems.distinctBy { it.id }
}
suspend fun HomeFeed.saveItemsToCache(itemDao: ItemDao, itemImageDao: ItemImageDao) {
    val allItems = getAllItems()

    allItems.forEach { item ->
        try {
            // Save item entity
            val itemEntity = item.toEntity()
            itemDao.insertOrReplace(itemEntity)

            // Save images
            val imageEntities = item.images.mapIndexed { index, image ->
                ItemImageEntity(
                    itemId = item.id,
                    url = image.url,
                    isCover = index == 0,
                    position = index
                )
            }
            if (imageEntities.isNotEmpty()) {
                itemImageDao.updateImagesForItem(item.id, imageEntities)
            }
        } catch (e: Exception) {
            Timber.tag("HomeFeed").e("Failed to cache item ${item.id}: ${e.message}")
        }
    }

    Timber.tag("HomeFeed").d("✅ Cached ${allItems.size} individual items to Room")
}
