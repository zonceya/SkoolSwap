package za.co.skoolswap.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import za.co.skoolswap.domain.model.chat.Conversation
import za.co.skoolswap.domain.model.chat.ImageMetadata
import za.co.skoolswap.domain.model.chat.LastMessage
import za.co.skoolswap.domain.model.chat.Message
import za.co.skoolswap.domain.model.chat.TypingIndicator
import za.co.skoolswap.domain.repository.ChatRepositoryInterface
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ CHAT REPOSITORY (Implementation)
 *
 * The ONLY place in the app that talks to Firestore for chat.
 * Everything else (ViewModel, UI) goes through the interface.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepositoryInterface {

    companion object {
        private const val COL_CONVERSATIONS = "conversations"
        private const val SUB_MESSAGES = "messages"
        private const val SUB_TYPING = "typing"
        private const val TYPING_TIMEOUT_MS = 3_000L // 3 seconds
    }

    // ─────────────────────────────────────────
    // CONVERSATIONS
    // ─────────────────────────────────────────

    override suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String,
        itemId: String,
        itemName: String,
        itemPrice: Double,
        itemImageUrl: String
    ): Result<String> = runCatching {

        val sortedIds = listOf(currentUserId, otherUserId).sorted()
        val conversationId = "${sortedIds[0]}_${sortedIds[1]}_$itemId"

        val conversationRef = firestore
            .collection(COL_CONVERSATIONS)
            .document(conversationId)

        val existing = conversationRef.get().await()

        if (existing.exists()) {
            return@runCatching conversationId
        }

        val newConversation = Conversation(
            id = conversationId,
            participants = listOf(currentUserId, otherUserId),
            itemId = itemId,
            itemName = itemName,
            itemPrice = itemPrice,
            itemImageUrl = itemImageUrl,
            unreadCount = mapOf(currentUserId to 0, otherUserId to 0),
            status = "active"
        )

        conversationRef.set(newConversation).await()
        conversationId
    }

    override fun getUserConversations(userId: String): Flow<List<Conversation>> {
        return firestore.collection(COL_CONVERSATIONS)
            .whereArrayContains("participants", userId)
            .whereEqualTo("status", "active")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)
                }
            }
    }

    override suspend fun getConversation(conversationId: String): Result<Conversation?> =
        runCatching {
            val doc = firestore.collection(COL_CONVERSATIONS)
                .document(conversationId)
                .get()
                .await()

            if (!doc.exists()) null
            else doc.toObject(Conversation::class.java)
        }

    // ─────────────────────────────────────────
    // MESSAGES
    // ─────────────────────────────────────────

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)
            .collection(SUB_MESSAGES)
            .whereEqualTo("deleted", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                }
            }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String
    ): Result<Message> = runCatching {

        val messageId = UUID.randomUUID().toString()

        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            type = "text",
            status = "sent",
            timestamp = Date() // will be overwritten by @ServerTimestamp
        )

        // 1. Write message
        firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)
            .collection(SUB_MESSAGES)
            .document(messageId)
            .set(message)
            .await()

        // 2. Update conversation (lastMessage + updatedAt + unread counts)
        val conversationRef = firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)

        val conversationDoc = conversationRef.get().await()
        val conversation = conversationDoc.toObject(Conversation::class.java)

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to LastMessage(
                content = content,
                senderId = senderId,
                type = "text",
                timestamp = Date()
            ),
            "updatedAt" to Date()
        )

        // Increment unread for everyone except the sender
        conversation?.participants?.forEach { participantId ->
            if (participantId != senderId) {
                updates["unreadCount.$participantId"] =
                    FieldValue.increment(1)
            }
        }

        conversationRef.update(updates).await()

        message
    }

    override suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        imageUrl: String,
        metadata: ImageMetadata
    ): Result<Message> = runCatching {

        val messageId = UUID.randomUUID().toString()

        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            imageUrl = imageUrl,
            imageMetadata = metadata,
            type = "image",
            status = "sent",
            timestamp = Date()
        )

        firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)
            .collection(SUB_MESSAGES)
            .document(messageId)
            .set(message)
            .await()

        // Update conversation preview
        val conversationRef = firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)

        val conversationDoc = conversationRef.get().await()
        val conversation = conversationDoc.toObject(Conversation::class.java)

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to LastMessage(
                content = "📷 Photo",
                senderId = senderId,
                type = "image",
                timestamp = Date()
            ),
            "updatedAt" to Date()
        )

        conversation?.participants?.forEach { participantId ->
            if (participantId != senderId) {
                updates["unreadCount.$participantId"] =
                    FieldValue.increment(1)
            }
        }

        conversationRef.update(updates).await()

        message
    }

    override suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String
    ): Result<Unit> = runCatching {
        firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)
            .collection(SUB_MESSAGES)
            .document(messageId)
            .update(
                mapOf(
                    "status" to "read",
                    "readAt" to Date()
                )
            )
            .await()
    }

    override suspend fun markAllMessagesAsRead(
        conversationId: String,
        userId: String
    ): Result<Unit> = runCatching {

        val convRef = firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)

        // 1. Reset unread count for this user
        convRef.update("unreadCount.$userId", 0).await()

        // 2. Mark all messages from the OTHER user as read
        val unreadMessages = convRef.collection(SUB_MESSAGES)
            .whereNotEqualTo("senderId", userId)
            .whereEqualTo("status", "sent")
            .get()
            .await()

        if (unreadMessages.isEmpty) return@runCatching

        // Firestore batch: update many docs in one transaction
        val batch = firestore.batch()
        unreadMessages.documents.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf(
                    "status" to "read",
                    "readAt" to Date()
                )
            )
        }
        batch.commit().await()
    }

    override suspend fun deleteMessage(
        conversationId: String,
        messageId: String
    ): Result<Unit> = runCatching {
        firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)
            .collection(SUB_MESSAGES)
            .document(messageId)
            .update("deleted", true)
            .await()
    }

    // ─────────────────────────────────────────
    // TYPING INDICATOR
    // ─────────────────────────────────────────

    override fun getTypingUsers(conversationId: String): Flow<List<String>> {
        return firestore.collection(COL_CONVERSATIONS)
            .document(conversationId)
            .collection(SUB_TYPING)
            .snapshots()
            .map { snapshot ->
                val cutoff = System.currentTimeMillis() - TYPING_TIMEOUT_MS
                snapshot.documents
                    .mapNotNull { it.toObject(TypingIndicator::class.java) }
                    .filter { indicator ->
                        // Ignore stale entries older than 3 seconds
                        val ts = indicator.timestamp?.time ?: 0L
                        ts >= cutoff
                    }
                    .map { it.userId }
            }
    }

    override suspend fun sendTypingIndicator(
        conversationId: String,
        userId: String
    ) {
        try {
            firestore.collection(COL_CONVERSATIONS)
                .document(conversationId)
                .collection(SUB_TYPING)
                .document(userId)
                .set(
                    TypingIndicator(
                        userId = userId,
                        timestamp = Date()
                    )
                )
                .await()
        } catch (_: Exception) {
            // Typing indicators are best-effort — never crash the app
        }
    }

    override suspend fun stopTypingIndicator(
        conversationId: String,
        userId: String
    ) {
        try {
            firestore.collection(COL_CONVERSATIONS)
                .document(conversationId)
                .collection(SUB_TYPING)
                .document(userId)
                .delete()
                .await()
        } catch (_: Exception) {
            // Silent fail
        }
    }

    // ─────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────

    override fun cleanup(conversationId: String) {
        // Firestore snapshot listeners auto-clean when collected coroutines cancel.
        // Nothing to do here for now — hook is here for future use.
    }
}