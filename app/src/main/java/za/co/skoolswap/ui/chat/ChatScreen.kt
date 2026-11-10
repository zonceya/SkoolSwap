package za.co.skoolswap.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import za.co.skoolswap.domain.model.chat.ChatUiState
import za.co.skoolswap.domain.model.chat.ConnectionStatus
import za.co.skoolswap.domain.model.chat.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ✅ CHAT SCREEN (Entry Point)
 *
 * The top-level composable.
 * It:
 *   1. Connects to the ViewModel (via Hilt)
 *   2. Initializes chat with navigation args
 *   3. Shows loading/error/content
 */
@Composable
fun ChatScreen(
    conversationId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    // Initialize chat once when the screen appears
    LaunchedEffect(conversationId, currentUserId) {
        viewModel.initialize(conversationId, currentUserId)
    }

    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Route to the correct UI based on state
    when {
        uiState.isLoading && uiState.messages.isEmpty() -> {
            LoadingState()
        }
        uiState.error != null && uiState.messages.isEmpty() -> {
            ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::retry
            )
        }
        else -> {
            ChatContent(
                uiState = uiState,
                onBackClick = onBackClick,
                onSendMessage = viewModel::sendMessage,
                onInputChange = viewModel::onInputTextChanged
            )
        }
    }

    // Error snackbar (non-blocking)
    uiState.error?.let { error ->
        if (uiState.messages.isNotEmpty()) {
            ErrorSnackbar(
                message = error,
                onDismiss = viewModel::clearError
            )
        }
    }
}

// ─────────────────────────────────────────
// SUB-COMPOSABLES
// ─────────────────────────────────────────

/**
 * ✅ CHAT CONTENT
 * The main chat layout: header + messages + input
 */
@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onBackClick: () -> Unit,
    onSendMessage: () -> Unit,
    onInputChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ChatHeader(
            itemName = uiState.conversation?.itemName ?: "Chat",
            isTyping = uiState.isTyping,
            connectionStatus = uiState.connectionStatus,
            onBackClick = onBackClick
        )

        MessageList(
            messages = uiState.messages,
            currentUserId = uiState.currentUserId,
            modifier = Modifier.weight(1f)
        )

        MessageInput(
            text = uiState.inputText,
            onTextChange = onInputChange,
            onSendClick = onSendMessage
        )
    }
}

/**
 * ✅ CHAT HEADER
 * Shows item context + typing/connection status + back button
 */
@Composable
private fun ChatHeader(
    itemName: String,
    isTyping: Boolean,
    connectionStatus: ConnectionStatus,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Title + status
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Show typing OR connection status
                val statusText = when {
                    isTyping -> "typing..."
                    connectionStatus == ConnectionStatus.Connected -> "Online"
                    connectionStatus == ConnectionStatus.Connecting -> "Connecting..."
                    else -> "Offline"
                }
                val statusColor = when {
                    isTyping -> MaterialTheme.colorScheme.primary
                    connectionStatus == ConnectionStatus.Connected -> Color(0xFF4CAF50)
                    connectionStatus == ConnectionStatus.Connecting -> Color(0xFFFFA726)
                    else -> Color(0xFF9E9E9E)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * ✅ MESSAGE LIST
 * Scrollable list of messages, newest at the bottom
 */
@Composable
private fun MessageList(
    messages: List<Message>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to newest when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Because we use reverseLayout, index 0 is newest
            listState.animateScrollToItem(0)
        }
    }

    // Empty state
    if (messages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👋",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Say hello to start the conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,  // ⚡ New messages appear at bottom automatically
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = messages,
            key = { it.id }   // Stable key for efficient recomposition
        ) { message ->
            MessageBubble(
                message = message,
                isMine = message.senderId == currentUserId
            )
        }
    }
}

/**
 * ✅ MESSAGE BUBBLE
 * A single message, aligned right (mine) or left (theirs)
 */
@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = if (isMine) {
                // Tail on the right for my messages
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            } else {
                // Tail on the left for their messages
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            },
            color = if (isMine) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {
                // Text content
                message.content?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Image content
                message.imageUrl?.let { url ->
                    Spacer(modifier = Modifier.height(4.dp))
                    AsyncImage(
                        model = url,
                        contentDescription = "Message image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                // Timestamp + read status
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = if (isMine) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )

                    // Read receipt (only on my messages)
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (message.status) {
                                "read" -> "✓✓"
                                "delivered" -> "✓✓"
                                else -> "✓"
                            },
                            fontSize = 10.sp,
                            color = if (message.status == "read") {
                                Color(0xFF4FC3F7) // Light blue when read
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ MESSAGE INPUT
 * Bottom bar with text field + send button
 */
@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .navigationBarsPadding(), // ✅ Avoid nav bar overlap
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) onSendClick()
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onSendClick,
                shape = CircleShape,
                containerColor = if (text.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * ✅ LOADING STATE
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * ✅ ERROR STATE (full screen, with retry)
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 48.sp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * ✅ ERROR SNACKBAR (non-blocking, when messages already loaded)
 */
@Composable
private fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        // Auto-dismiss after 3 seconds
        kotlinx.coroutines.delay(3_000L)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(message)
        }
    }
}

/**
 * ✅ HELPER: Format timestamp
 */
private fun formatTime(date: Date?): String {
    if (date == null) return ""
    val now = Date()
    val diffMs = now.time - date.time
    val diffMinutes = diffMs / 1000 / 60

    return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1440 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(date)
    }
}