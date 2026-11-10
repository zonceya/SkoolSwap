package za.co.skoolswap.domain.model.chat
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * ✅ CONVERSATION
 *
 * Represents a chat thread between a buyer and a seller about ONE item.
 * Stored in Firestore: conversations/{conversationId}
 */
data class Conversation(
    @DocumentId
    val id: String = "",

    val participants: List<String> = emptyList(),

    val itemId: String = "",
    val itemName: String = "",
    val itemPrice: Double = 0.0,
    val itemImageUrl: String = "",

    val orderId: String? = null,

    val lastMessage: LastMessage? = null,
    val unreadCount: Map<String, Int> = emptyMap(),

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val updatedAt: Date? = null,

    val status: String = "active" // "active", "archived", "blocked"
) {
    /**
     * Get the OTHER participant's ID (the one that's NOT me)
     */
    fun otherParticipantId(currentUserId: String): String? {
        return participants.firstOrNull { it != currentUserId }
    }

    /**
     * Get unread count for a specific user
     */
    fun unreadFor(userId: String): Int {
        return unreadCount[userId] ?: 0
    }
}