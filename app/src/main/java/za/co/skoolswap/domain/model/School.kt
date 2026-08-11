package za.co.skoolswap.domain.model

data class School(
    val id: Int,
    val name: String,
    val provinceId: Int?,
    val provinceName: String?,
    val locationId: Int?,
    val logoUrl: String? = null,
    val schoolType: String?
) {
    val displayName: String
        get() = buildString {
            append(name)
            if (locationId != null) {
                append(" (ID: $locationId)")
            }
        }
}