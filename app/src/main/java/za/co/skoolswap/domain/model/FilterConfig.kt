// domain/model/FilterConfig.kt
package za.co.skoolswap.domain.model

data class FilterConfig(
    val categoryId: Int? = null,
    val categoryName: String? = null,  // Make nullable for global filter
    val filterGroups: List<FilterGroup>
)

data class FilterGroup(
    val id: String,
    val name: String,
    val filterType: String,
    val options: List<FilterOption> = emptyList(),
    val min: Float? = null,
    val max: Float? = null,
    val step: Int? = null
)

data class FilterOption(
    val id: Int,
    val name: String
)

data class AppliedFilters(
    val condition: Int? = null,
    val gender: Int? = null,
    val size: Int? = null,
    val type: List<Int>? = null,
    val brand: List<Int>? = null,
    val color: List<Int>? = null,
    val sportType: Int? = null,
    val grade: Int? = null,
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
    val categoryId: Int? = null
)