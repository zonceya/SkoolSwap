package za.co.skoolswap.domain.repository



import kotlinx.coroutines.flow.Flow
import za.co.skoolswap.domain.model.chat.Conversation
import za.co.skoolswap.domain.model.chat.ImageMetadata
import za.co.skoolswap.domain.model.chat.Message

/**
 * ✅ CHAT REPOSITORY INTERFACE
 *
 * The "contract" — WHAT chat features exist.
 * ViewModels depend on THIS, not on Firebase directly.
 */
interface ChatRepositoryInterface {

    // ─────────────────────────────────────────
    // CONVERSATIONS
    // ─────────────────────────────────────────

    /**
     * Get an existing conversation or create a new one.
     * Returns the conversation ID.
     */
    suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String,
        itemId: String,
        itemName: String,
        itemPrice: Double,
        itemImageUrl: String
    ): Result<String>

    /**
     * Watch all conversations for a user (real-time).
     */
    fun getUserConversations(userId: String): Flow<List<Conversation>>

    /**
     * Get a single conversation once.
     */
    suspend fun getConversation(conversationId: String): Result<Conversation?>

    // ─────────────────────────────────────────
    // MESSAGES
    // ─────────────────────────────────────────

    /**
     * Watch all messages in a conversation (real-time).
     */
    fun getMessages(conversationId: String): Flow<List<Message>>

    /**
     * Send a text message.
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String
    ): Result<Message>

    /**
     * Send an image message (URL already uploaded to R2).
     */
    suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        imageUrl: String,
        metadata: ImageMetadata
    ): Result<Message>

    /**
     * Mark a single message as read.
     */
    suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String
    ): Result<Unit>

    /**
     * Mark ALL messages in a conversation as read for this user.
     * Also resets unread count.
     */
    suspend fun markAllMessagesAsRead(
        conversationId: String,
        userId: String
    ): Result<Unit>

    /**
     * Soft-delete a message.
     */
    suspend fun deleteMessage(
        conversationId: String,
        messageId: String
    ): Result<Unit>

    // ─────────────────────────────────────────
    // TYPING INDICATOR
    // ─────────────────────────────────────────

    /**
     * Watch who is currently typing.
     */
    fun getTypingUsers(conversationId: String): Flow<List<String>>

    /**
     * Send a typing heartbeat (auto-expires after 3s).
     */
    suspend fun sendTypingIndicator(
        conversationId: String,
        userId: String
    )

    /**
     * Manually clear typing indicator.
     */
    suspend fun stopTypingIndicator(
        conversationId: String,
        userId: String
    )

    // ─────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────

    /**
     * Clean up any listeners/subscriptions for a conversation.
     */
    fun cleanup(conversationId: String)
}