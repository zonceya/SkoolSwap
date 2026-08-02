package za.co.skoolswap.domain.model

data class BannerItem(
    val imageUrl: String,
    val title: String? = null  // Optional title per banner
)