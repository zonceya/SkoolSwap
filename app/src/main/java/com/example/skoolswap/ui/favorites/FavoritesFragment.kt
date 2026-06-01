package com.example.skoolswap.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.remote.creation.dsl.first
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
        observeViewModel()
        setUserName()
    }

    private fun setUserName() {
        lifecycleScope.launch {
            val userName = appPreferences.userName.first() ?: "My"
            binding.favoritesTitle.text = "$userName's Favorites"
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

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.favorites.collect { items ->
                adapter.submitList(items)
                binding.emptyView.isVisible = items.isEmpty()
                binding.recyclerView.isVisible = items.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}