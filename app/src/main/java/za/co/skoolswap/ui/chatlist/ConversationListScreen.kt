package za.co.skoolswap.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import za.co.skoolswap.domain.model.chat.Conversation
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ CONVERSATION LIST SCREEN
 *
 * Displays all chat threads for the current user.
 */
@Composable
fun ConversationListScreen(
    onConversationClick: (conversationId: String, sellerId: String, itemName: String) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Top app bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬  Messages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Content
        when {
            uiState.isLoading -> LoadingState()

            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::refresh
            )

            uiState.isEmpty -> EmptyState()

            else -> ConversationList(
                conversations = uiState.conversations,
                currentUserId = uiState.currentUserId,
                onClick = onConversationClick
            )
        }
    }
}

// ─────────────────────────────────────────
// SUB-COMPOSABLES
// ─────────────────────────────────────────

@Composable
private fun ConversationList(
    conversations: List<Conversation>,
    currentUserId: String,
    onClick: (String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            ConversationRow(
                conversation = conversation,
                currentUserId = currentUserId,
                onClick = {
                    val otherUserId = conversation.otherParticipantId(currentUserId) ?: ""
                    onClick(conversation.id, otherUserId, conversation.itemName)
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val unreadCount = conversation.unreadFor(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar — item image for now
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.itemImageUrl.isNotBlank()) {
                AsyncImage(
                    model = conversation.itemImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(text = "📦", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle column — item name + last message
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.itemName.ifBlank { "Chat" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val preview = when {
                conversation.lastMessage == null -> "Tap to start chatting"
                conversation.lastMessage!!.type == "image" -> "📷 Photo"
                conversation.lastMessage!!.content.isBlank() -> "Empty message"
                else -> conversation.lastMessage!!.content
            }

            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unreadCount > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right column — timestamp + unread badge
        Column(horizontalAlignment = Alignment.End) {
            val timeText = formatConversationTime(conversation.lastMessage?.timestamp ?: conversation.updatedAt)
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unreadCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.coerceAtMost(99).toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start chatting with sellers by tapping \"Contact Seller\" on any item.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// ─────────────────────────────────────────
// HELPER
// ─────────────────────────────────────────
private fun formatConversationTime(date: Date?): String {
    if (date == null) return ""
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = date }

    val isToday = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
            && now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    val isYesterday = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
            && now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) + 1

    return when {
        isToday -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        isYesterday -> "Yesterday"
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) ->
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(date)
    }
}