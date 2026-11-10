package za.co.skoolswap.domain.model.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * ✅ LAST MESSAGE
 *
 * Embedded inside Conversation for list preview.
 * We only store the minimal fields needed (Firestore reads are cheaper this way).
 */
data class LastMessage(
    val content: String = "",
    val senderId: String = "",
    val type: String = "text", // "text", "image", "system"

    @ServerTimestamp
    val timestamp: Date? = null
)