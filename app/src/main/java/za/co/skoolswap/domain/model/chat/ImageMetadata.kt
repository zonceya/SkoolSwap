package za.co.skoolswap.domain.model.chat


/**
 * ✅ IMAGE METADATA
 *
 * Extra info about image messages.
 * Used to reserve correct space in UI BEFORE image loads
 * (prevents layout jumping).
 */
data class ImageMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0 // bytes
)