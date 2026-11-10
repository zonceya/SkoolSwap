package za.co.skoolswap.ui.chatlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import dagger.hilt.android.AndroidEntryPoint
import za.co.skoolswap.R
import za.co.skoolswap.ui.chat.ChatFragment
import za.co.skoolswap.ui.theme.SkoolSwapTheme

/**
 * ✅ CONVERSATION LIST FRAGMENT
 *
 * Thin wrapper hosting ConversationListScreen in Compose.
 * Navigates to ChatFragment when user taps a row.
 */
@AndroidEntryPoint
class ConversationListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {

            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                SkoolSwapTheme {
                    ConversationListScreen(
                        onConversationClick = { conversationId, otherUserId, itemName ->
                            // Navigate to chat
                            val bundle = bundleOf(
                                ChatFragment.ARG_CONVERSATION_ID to conversationId,
                                ChatFragment.ARG_SELLER_ID to otherUserId,
                                ChatFragment.ARG_ITEM_NAME to itemName
                            )
                            findNavController().navigate(
                                R.id.action_conversationListFragment_to_chatFragment,
                                bundle
                            )
                        }
                    )
                }
            }
        }
    }
}