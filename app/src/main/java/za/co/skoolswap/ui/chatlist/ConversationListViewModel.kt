package za.co.skoolswap.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.model.chat.Conversation
import za.co.skoolswap.domain.repository.ChatRepositoryInterface
import javax.inject.Inject

/**
 * ✅ CONVERSATION LIST VIEW MODEL
 *
 * Loads all conversations for the current user.
 * Real-time — new messages re-order the list automatically.
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepositoryInterface,
    private val appPreferences: AppPreferences
) : ViewModel() {

    // ─────────────────────────────────────────
    // UI STATE
    // ─────────────────────────────────────────
    data class UiState(
        val conversations: List<Conversation> = emptyList(),
        val currentUserId: String = "",
        val isLoading: Boolean = true,
        val error: String? = null,
        val isEmpty: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────
    fun initialize() {
        viewModelScope.launch {
            try {
                val userId = appPreferences.getUserId()?.toString() ?: ""
                if (userId.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Please log in to see your messages"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(currentUserId = userId) }
                listenToConversations(userId)

            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize conversation list")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load conversations"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // REAL-TIME LISTENER
    // ─────────────────────────────────────────
    private fun listenToConversations(userId: String) {
        viewModelScope.launch {
            try {
                chatRepository.getUserConversations(userId).collect { conversations ->
                    _uiState.update {
                        it.copy(
                            conversations = conversations,
                            isLoading = false,
                            isEmpty = conversations.isEmpty(),
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Conversation list listener failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Connection lost"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // REFRESH
    // ─────────────────────────────────────────
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        initialize()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}