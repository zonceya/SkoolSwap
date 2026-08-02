// domain/model/RecommendationModels.kt
package za.co.skoolswap.domain.model.homefeed

import za.co.skoolswap.domain.model.Item

// ============ HOME FEED ============
data class HomeFeed(
    val success: Boolean,
    val schoolId: Int,
    val message: String?,
    val sections: List<Section>
)

sealed class Section {
    abstract val type: String

    data class Recommended(
        val title: String,
        override val type: String,
        val items: List<Item>
    ) : Section()

    data class Essentials(
        val title: String,
        override val type: String,
        val sections: EssentialsSections
    ) : Section()

    data class Trending(
        val title: String,
        override val type: String,
        val items: List<Item>
    ) : Section()

    data class Recent(
        val title: String,
        override val type: String,
        val items: List<Item>
    ) : Section()
}

data class EssentialsSections(
    val uniforms: List<Item>,
    val sports: List<Item>,
    val accessories: List<Item>,
    val stationery: List<Item> = emptyList()
) {
    companion object {
        fun empty() = EssentialsSections(
            uniforms = emptyList(),
            sports = emptyList(),
            accessories = emptyList(),
            stationery  = emptyList()
        )
    }
}

// ============ UNIFORM FEED ============
data class UniformFeed(
    val success: Boolean,
    val schoolId: Int,
    val gender: String?,
    val message: String?,
    val sections: List<UniformSection>
)

data class UniformSection(
    val title: String,
    val type: String, // summer, winter, pe_kit, accessories
    val items: List<Item>
)

// ============ SPORT FEED ============
data class SportFeed(
    val success: Boolean,
    val schoolId: Int,
    val message: String?,
    val sections: List<SportSection>
)

data class SportSection(
    val title: String,
    val type: String, // rugby, cricket, hockey, netball, soccer
    val items: List<Item>
)

// ============ RECENT FEED ============
data class RecentFeed(
    val success: Boolean,
    val schoolId: Int,
    val message: String?,
    val sections: List<RecentSection>,
    val items: List<Item>? // For single period response
)

data class RecentSection(
    val title: String,
    val period: String, // today, yesterday, week
    val items: List<Item>
)