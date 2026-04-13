package com.example.skoolswap.ui.uniform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentUniformBinding
import dagger.hilt.android.AndroidEntryPoint

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

    private fun observeViewModel() {
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