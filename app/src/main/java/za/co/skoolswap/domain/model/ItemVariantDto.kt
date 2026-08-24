package za.co.skoolswap.domain.model

// data/remote/models/response/item/ItemVariantDto.kt

data class ItemVariantDto(
    val id: String,
    val sizeId: Int?,
    val sizeName: String?,
    val colorId: Int?,
    val colorName: String?,
    val conditionId: Int?,
    val conditionName: String?,
    val price: Double?,
    val quantity: Int?,
    val isActive: Boolean?
)