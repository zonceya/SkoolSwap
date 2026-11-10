package za.co.skoolswap.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import za.co.skoolswap.domain.model.chat.ChatUiState
import za.co.skoolswap.domain.model.chat.ConnectionStatus
import za.co.skoolswap.domain.model.chat.ImageMetadata
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.ChatRepositoryInterface
import javax.inject.Inject

/**
 * ✅ CHAT VIEW MODEL
 *
 * Bridges Firestore data (via ChatRepository) to Compose UI.
 *
 * RESPONSIBILITIES:
 * 1. Load conversation + messages (real-time)
 * 2. Send messages (text + image)
 * 3. Mark messages as read
 * 4. Handle typing indicators (debounced)
 * 5. Expose a single ChatUiState to UI
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val chatRepository: ChatRepositoryInterface
) : ViewModel() {

    // ─────────────────────────────────────────
    // UI STATE (single source of truth for Compose)
    // ─────────────────────────────────────────
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────
    // INTERNAL STATE
    // ─────────────────────────────────────────
    private var conversationId: String = ""
    private var currentUserId: String = ""

    // Jobs — so we can cancel listeners when fragment closes
    private var messagesJob: Job? = null
    private var typingJob: Job? = null

    // Typing debounce — prevents firing typing indicators on every keystroke
    private var typingDebounceJob: Job? = null

    // ─────────────────────────────────────────
    // INITIALIZE
    // ─────────────────────────────────────────

    /**
     * Call this ONCE when the chat screen opens.
     * Pass in the params from your navigation arguments.
     */
    fun initialize(
        conversationId: String,
        currentUserId: String
    ) {
        // Guard — don't re-initialize if same conversation
        if (this.conversationId == conversationId && messagesJob != null) {
            return
        }

        this.conversationId = conversationId
        this.currentUserId = currentUserId

        _uiState.update {
            it.copy(
                currentUserId = currentUserId,
                isLoading = true,
                connectionStatus = ConnectionStatus.Connecting
            )
        }

        loadConversation()
        loadMessages()
        loadTypingIndicators()
        markAllAsRead()
    }

    // ─────────────────────────────────────────
    // LOAD CONVERSATION
    // ─────────────────────────────────────────

    private fun loadConversation() {
        viewModelScope.launch {
            val result = chatRepository.getConversation(conversationId)

            result
                .onSuccess { conversation ->
                    _uiState.update { it.copy(conversation = conversation) }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load conversation")
                    _uiState.update {
                        it.copy(
                            error = "Failed to load conversation",
                            connectionStatus = ConnectionStatus.Disconnected
                        )
                    }
                }
        }
    }

    // ─────────────────────────────────────────
    // LOAD MESSAGES (real-time)
    // ─────────────────────────────────────────

    private fun loadMessages() {
        messagesJob?.cancel() // cancel any previous listener

        messagesJob = viewModelScope.launch {
            try {
                chatRepository.getMessages(conversationId).collect { messages ->
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoading = false,
                            connectionStatus = ConnectionStatus.Connected,
                            error = null
                        )
                    }

                    // Auto-mark unread messages from the OTHER user as read
                    val unreadFromOthers = messages.filter {
                        it.senderId != currentUserId && it.status != "read"
                    }
                    unreadFromOthers.forEach { message ->
                        markMessageAsRead(message.id)
                    }

                }
            } catch (e: Exception) {
                Timber.e(e, "Messages listener failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectionStatus = ConnectionStatus.Disconnected,
                        error = "Connection lost"
                    )
                }
            }
        }
    }
    // ─────────────────────────────────────────
    // LOAD TYPING INDICATORS (real-time)
    // ─────────────────────────────────────────

    private fun loadTypingIndicators() {
        typingJob?.cancel()

        typingJob = viewModelScope.launch {
            try {
                chatRepository.getTypingUsers(conversationId).collect { typingUsers ->
                    val otherIsTyping = typingUsers.any { it != currentUserId }
                    _uiState.update { it.copy(isTyping = otherIsTyping) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Typing listener failed")
            }
        }
    }

    // ─────────────────────────────────────────
    // SEND MESSAGE
    // ─────────────────────────────────────────

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        // Optimistically clear input
        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            // Stop typing indicator
            chatRepository.stopTypingIndicator(conversationId, currentUserId)
            typingDebounceJob?.cancel()

            // 1. Send message to Firestore
            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                content = text
            )

            result.onSuccess { message ->
                // 🆕 2. Tell Rails to notify recipient
                notifyRecipient(text)
            }.onFailure { error ->
                Timber.e(error, "Failed to send message")
                _uiState.update {
                    it.copy(
                        error = "Failed to send message",
                        inputText = text // restore text so user can retry
                    )
                }
            }
        }
    }
    private suspend fun notifyRecipient(messageText: String) {
        try {
            // Get recipient ID from conversation
            val conversation = chatRepository.getConversation(conversationId).getOrNull()
            val recipientId = conversation?.otherParticipantId(currentUserId)

            if (recipientId.isNullOrBlank()) {
                Timber.w("⚠️ No recipient found — skipping notification")
                return
            }

            authRepository.sendChatNotification(
                recipientId = recipientId,
                messageBody = messageText,
                conversationId = conversationId
            ).onSuccess {
                Timber.d("✅ Notification sent via Rails to $recipientId")
            }.onFailure { error ->
                Timber.w("⚠️ Notification failed: ${error.message}")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to send notification")
        }
    }
    fun sendImageMessage(imageUrl: String, metadata: ImageMetadata) {
        viewModelScope.launch {
            val result = chatRepository.sendImageMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                imageUrl = imageUrl,
                metadata = metadata
            )

            result.onSuccess {
                // 🆕 Notify recipient
                notifyRecipient("📷 Photo")
            }.onFailure { error ->
                Timber.e(error, "Failed to send image")
                _uiState.update { it.copy(error = "Failed to send image") }
            }
        }
    }

    // ─────────────────────────────────────────
    // INPUT TEXT (with typing indicator debounce)
    // ─────────────────────────────────────────

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }

        // Cancel previous debounce timer
        typingDebounceJob?.cancel()

        if (newText.isBlank()) {
            // Input emptied → stop typing indicator immediately
            viewModelScope.launch {
                chatRepository.stopTypingIndicator(conversationId, currentUserId)
            }
            return
        }

        // Send typing indicator immediately (first keystroke)
        viewModelScope.launch {
            chatRepository.sendTypingIndicator(conversationId, currentUserId)
        }

        // Then debounce: if user stops typing for 2s → stop indicator
        typingDebounceJob = viewModelScope.launch {
            delay(2_000L)
            chatRepository.stopTypingIndicator(conversationId, currentUserId)
        }
    }

    // ─────────────────────────────────────────
    // MARK AS READ
    // ─────────────────────────────────────────

    private fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            chatRepository.markMessageAsRead(conversationId, messageId)
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            chatRepository.markAllMessagesAsRead(conversationId, currentUserId)
        }
    }

    // ─────────────────────────────────────────
    // DELETE MESSAGE
    // ─────────────────────────────────────────

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(conversationId, messageId)
        }
    }

    // ─────────────────────────────────────────
    // ERROR / RETRY
    // ─────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retry() {
        _uiState.update { it.copy(error = null, isLoading = true) }
        loadConversation()
        loadMessages()
    }

    // ─────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        typingJob?.cancel()
        typingDebounceJob?.cancel()
        chatRepository.cleanup(conversationId)
    }
}