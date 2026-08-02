package za.co.skoolswap.domain.model.homefeed

data class RankedItem(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String?,
    val mainCategory: String?,
    val mainCategoryId: Int?,
    val subCategory: String?,
    val subCategoryId: Int?,
    val gender: String?,
    val genderId: Int?,
    val condition: String?,
    val brand: String?,
    val schoolName: String?,
    val schoolId: Int,
    val relevance: String,
    val images: List<String>?,
    val coverPhoto: String?,
    val createdAt: String,
    val viewCount: Int = 0
)