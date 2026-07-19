package com.example.skoolswap.ui.uniform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentUniformBinding
import com.example.skoolswap.domain.model.UniformCategory
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
        setupSwipeRefresh()
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
        // LiveData observers are already view-lifecycle-aware, these are fine as-is
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && viewModel.categories.value.isNullOrEmpty()) {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.uniformTabRecycler.visibility = View.GONE
                binding.errorLayout.visibility = View.GONE
            } else {
                binding.shimmerLayout.visibility = View.GONE
            }
        }

        // Single observer — removed the duplicate
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (!categories.isNullOrEmpty()) {
                binding.shimmerLayout.visibility = View.GONE
                binding.uniformTabRecycler.visibility = View.VISIBLE
                binding.errorLayout.visibility = View.GONE
            }
            (binding.uniformTabRecycler.adapter as? UniformCategoryAdapter)?.submitList(categories)
        }

        // StateFlow collector — must use repeatOnLifecycle to be view-lifecycle-safe
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }
    }

    private fun navigateToProducts(category: UniformCategory) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", when (category.categoryId) {
                1 -> "uniform"
                2 -> "sports"
                3 -> "accessories"
                else -> "uniform"
            })
            putString("SECTION_TITLE", category.name)
            putInt("CATEGORY_ID", category.categoryId)
            putInt("SUB_CATEGORY_ID", category.id)
        }
        findNavController().navigate(R.id.action_uniformFragment_to_productsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}