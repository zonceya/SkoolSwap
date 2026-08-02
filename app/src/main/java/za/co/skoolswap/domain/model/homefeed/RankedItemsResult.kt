package za.co.skoolswap.domain.model.homefeed

data class RankedItemsResult(
    val items: List<RankedItem>,
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int
)