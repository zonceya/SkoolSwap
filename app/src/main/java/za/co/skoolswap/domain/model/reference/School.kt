package za.co.skoolswap.domain.model.reference

data class School(
    val id: Int,
    val name: String,
    val schoolType: String?,
    val provinceId: Int?
)