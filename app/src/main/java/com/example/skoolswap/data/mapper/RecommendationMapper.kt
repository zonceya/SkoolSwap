// data/mapper/RecommendationMapper.kt
package com.example.skoolswap.data.mapper

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

// ============ RECOMMENDATION ITEM TO DOMAIN ============
fun RecommendationItemDto.toDomain(): Item {
    return Item(
        id = id,
        shopId = 0L, // Not provided in recommendations
        name = name,
        description = description ?: "",
        price = price,
        quantity = 1, // Default
        status = "active",
        meta = null,
        createdAt = createdAt,
        shop = null,
        images = if (!image.isNullOrEmpty()) {
            listOf(
                ItemImage(
                    id = 0L,
                    url = image,
                    filename = null,
                    contentType = null,
                    createdAt = null
                )
            )
        } else emptyList(),
        brandId = null,
        sizeId = null,
        schoolId = schoolId,
        itemConditionId = null,
        locationId = null,
        provinceId = null,
        genderId = null,
        label = null,
        reserved = 0
    )
}