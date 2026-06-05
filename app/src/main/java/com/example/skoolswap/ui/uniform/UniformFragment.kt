package com.example.skoolswap.ui.uniform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentUniformBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UniformFragment : Fragment() {

    private var _binding: FragmentUniformBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UniformViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUniformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.retryButton.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            viewModel.loadUniformCategories()
        }
        viewModel.loadUniformCategories()
    }

    private fun setupRecyclerView() {
        val adapter = UniformCategoryAdapter { category ->
            navigateToProducts(category)
        }
        binding.uniformTabRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUniformCategories()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    private fun observeViewModel() {
        // Loading state - show shimmer
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && viewModel.categories.value.isNullOrEmpty()) {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.uniformTabRecycler.visibility = View.GONE
                binding.errorLayout.visibility = View.GONE
            } else {
                binding.shimmerLayout.visibility = View.GONE
            }
        }

        // Error state
        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null && viewModel.categories.value.isNullOrEmpty()) {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.errorMessage.text = errorMsg
                    binding.uniformTabRecycler.visibility = View.GONE
                    binding.shimmerLayout.visibility = View.GONE
                } else {
                    binding.errorLayout.visibility = View.GONE
                }
            }
        }

        // Categories data
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isNotEmpty()) {
                binding.shimmerLayout.visibility = View.GONE
                binding.uniformTabRecycler.visibility = View.VISIBLE
                binding.errorLayout.visibility = View.GONE
                (binding.uniformTabRecycler.adapter as? UniformCategoryAdapter)?.submitList(categories)
            }
        }
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            (binding.uniformTabRecycler.adapter as? UniformCategoryAdapter)?.submitList(categories)
        }
    }





    private fun navigateToProducts(category: UniformCategory) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", "uniform")
            putString("SECTION_TITLE", category.name)
            putInt("CATEGORY_ID", 1)
            putInt("SUB_CATEGORY_ID", category.id)
        }
        findNavController().navigate(R.id.action_uniformFragment_to_productsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}