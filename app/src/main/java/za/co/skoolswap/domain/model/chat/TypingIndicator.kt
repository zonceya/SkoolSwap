package za.co.skoolswap.domain.model.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * ✅ TYPING INDICATOR
 *
 * Stored in Firestore: conversations/{conversationId}/typing/{userId}
 * Auto-expires after 3 seconds (checked via timestamp).
 */
data class TypingIndicator(
    val userId: String = "",

    @ServerTimestamp
    val timestamp: Date? = null
)