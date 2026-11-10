package za.co.skoolswap.domain.model.chat

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * ✅ MESSAGE
 *
 * A single chat message.
 * Stored in Firestore: conversations/{conversationId}/messages/{messageId}
 */
data class Message(
    @DocumentId
    val id: String = "",

    val conversationId: String = "",
    val senderId: String = "",

    val content: String? = null,
    val imageUrl: String? = null,
    val imageMetadata: ImageMetadata? = null,

    val type: String = "text",   // "text", "image", "system"
    val status: String = "sent", // "sent", "delivered", "read"

    @ServerTimestamp
    val timestamp: Date? = null,

    @ServerTimestamp
    val readAt: Date? = null,

    @ServerTimestamp
    val deliveredAt: Date? = null,

    val deleted: Boolean = false
)