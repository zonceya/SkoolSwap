package za.co.skoolswap.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.FragmentFavoritesBinding
import za.co.skoolswap.data.local.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: FavoritesAdapter
    private var isFirstLoad = true

    @Inject
    lateinit var appPreferences: AppPreferences

    companion object {
        private const val MY_FAVORITES = "My Favorites"
        private const val DEFAULT_NAME = "My"
        private const val FAVORITES_FORMAT = "%s's Favorites"
        private const val GRID_SPAN_COUNT = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupRetryButton()
        observeViewModel()
        setUserName()
    }

    private fun setUserName() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userName = appPreferences.userName.first() ?: DEFAULT_NAME
            binding.favoritesTitle.text = if (userName == DEFAULT_NAME) {
                MY_FAVORITES
            } else {
                String.format(FAVORITES_FORMAT, userName)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter { itemId ->
            val bundle = bundleOf(AppConstants.ARG_PRODUCT_ID to itemId)
            findNavController().navigate(R.id.itemDetailFragment, bundle)
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
            adapter = this@FavoritesFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.teal_200)
            )
            setOnRefreshListener {
                viewModel.refreshFavorites()
                isRefreshing = false
            }
        }
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            viewModel.refreshFavorites()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading && isFirstLoad && viewModel.favorites.value.isEmpty()) {
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyView.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                        } else {
                            binding.shimmerLayout.visibility = View.GONE
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        if (errorMsg != null && viewModel.favorites.value.isEmpty()) {
                            binding.errorLayout.visibility = View.VISIBLE
                            binding.errorMessage.text = errorMsg
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyView.visibility = View.GONE
                            binding.shimmerLayout.visibility = View.GONE
                            binding.swipeRefreshLayout.isRefreshing = false
                        } else {
                            binding.errorLayout.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.favorites.collect { items ->
                        isFirstLoad = false
                        binding.shimmerLayout.visibility = View.GONE
                        binding.recyclerView.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.errorLayout.visibility = View.GONE
                        adapter.submitList(items)
                        Timber.tag(LogTags.UI).d("Loaded ${items.size} favorites")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}