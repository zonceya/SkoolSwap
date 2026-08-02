package za.co.skoolswap.domain.model

import android.net.Uri

sealed class EditImage {
    // Image that already exists on server
    data class Existing(
        val id: Long,
        val url: String,
        val isMarkedForDeletion: Boolean = false
    ) : EditImage()

    // New image just added (not uploaded yet)
    data class New(
        val uri: Uri,
        val isUploading: Boolean = false
    ) : EditImage()

    // Empty slot for adding new images
    object Empty : EditImage()

    val isDeletionMarked: Boolean
        get() = this is Existing && isMarkedForDeletion
}