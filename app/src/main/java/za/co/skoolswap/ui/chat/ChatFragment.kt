package za.co.skoolswap.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import za.co.skoolswap.R
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.ui.theme.SkoolSwapTheme
import javax.inject.Inject

/**
 * ✅ CHAT FRAGMENT
 *
 * A thin wrapper that hosts the Compose ChatScreen.
 * Works with your existing Fragment-based navigation graph.
 */
@AndroidEntryPoint
class ChatFragment : Fragment() {

    // ViewModel — same as always (Hilt injects it)
    private val viewModel: ChatViewModel by viewModels()

    // AppPreferences — to read current user ID
    @Inject
    lateinit var appPreferences: AppPreferences

    // Cached user ID — loaded once in onCreate
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read user ID from DataStore asynchronously, once
        lifecycleScope.launch {
            try {
                val userId = appPreferences.getUserId() // adjust if your method returns Flow
                currentUserId = userId?.toString() ?: ""
                Timber.d("ChatFragment: currentUserId=$currentUserId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load currentUserId")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ComposeView is a regular Android View that hosts Compose content
        return ComposeView(requireContext()).apply {

            // Recommended — dispose composition when view is destroyed
            // (prevents memory leaks when fragment is in back stack)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                // Wrap in your app theme
                SkoolSwapTheme {

                    // Read navigation args
                    val conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: ""
                    val currentUserId = currentUserId

                    // Render Compose screen
                    ChatScreen(
                        conversationId = conversationId,
                        currentUserId = currentUserId,
                        onBackClick = {
                            // Handle back → pop navigation
                            findNavController().popBackStack()
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_CONVERSATION_ID = "conversationId"
        const val ARG_SELLER_ID = "sellerId"
        const val ARG_ITEM_NAME = "itemName"
    }
}