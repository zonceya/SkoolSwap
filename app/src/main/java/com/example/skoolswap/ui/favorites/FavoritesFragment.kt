package com.example.skoolswap.ui.favorites

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
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentFavoritesBinding
import com.example.skoolswap.data.local.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        // viewLifecycleOwner.lifecycleScope so it cancels when the view is destroyed
        viewLifecycleOwner.lifecycleScope.launch {
            val userName = appPreferences.userName.first() ?: "My"
            binding.favoritesTitle.text =
                if (userName == "My") "My Favorites" else "$userName's Favorites"
        }
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter { itemId ->
            val bundle = bundleOf("itemId" to itemId)
            findNavController().navigate(R.id.itemDetailFragment, bundle)
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
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
        // Single coroutine, all collectors inside repeatOnLifecycle.
        // repeatOnLifecycle cancels everything inside it when the view
        // drops below STARTED (i.e. on back press / destroy), so _binding
        // is guaranteed to be non-null whenever a collector runs.
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
                        binding.recyclerView.visibility =
                            if (items.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.emptyView.visibility =
                            if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.errorLayout.visibility = View.GONE
                        adapter.submitList(items)
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