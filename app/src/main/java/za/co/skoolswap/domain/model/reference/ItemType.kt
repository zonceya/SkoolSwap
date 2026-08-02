package za.co.skoolswap.domain.model.reference

data class ItemType(
    val id: Int,
    val name: String,
    val description: String?,
    val groupId: Int?
)
