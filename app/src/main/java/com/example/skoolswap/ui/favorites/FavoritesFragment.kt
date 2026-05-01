package com.example.skoolswap.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentFavoritesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: FavoritesAdapter  // Use FavoritesAdapter instead

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
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter { itemId ->
            val bundle = bundleOf("itemId" to itemId)
            findNavController().navigate(R.id.itemDetailFragment, bundle)
        }
        binding.recyclerView.apply {
            // This creates 2 columns side by side
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

                // Log for debugging
                if (items.isNotEmpty()) {
                    android.util.Log.d("FavoritesFragment", "First item: ${items[0].name}")
                    android.util.Log.d("FavoritesFragment", "Images count: ${items[0].images.size}")
                    items[0].images.forEachIndexed { index, image ->
                        android.util.Log.d("FavoritesFragment", "Image $index: ${image.url}")
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