package za.co.skoolswap.domain.model.chat


/**
 * ✅ CHAT UI STATE
 *
 * NOT stored in Firestore.
 * This is what our Compose UI consumes — combines Firestore data with UI flags.
 */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val conversation: Conversation? = null,
    val currentUserId: String = "",
    val isTyping: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connected
)