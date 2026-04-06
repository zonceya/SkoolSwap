package com.example.skoolswap.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProductsBinding
import com.example.skoolswap.ui.products.adapter.ProductsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sectionType = arguments?.getString("SECTION_TYPE") ?: "all"
        val sectionTitle = arguments?.getString("SECTION_TITLE") ?: "All Items"
        val period = arguments?.getString("PERIOD")

        setupToolbar(sectionTitle)
        setupRecyclerView()
        setupSortFilterBar()
        setupDrawer()
        setupFilterResultListener()  // Add this
        setupPriceRangeListener()    // Add this
        observeViewModel()

        viewModel.loadProducts(sectionType, period)
    }

    private fun setupToolbar(title: String) {
        binding.sectionTitle.text = title

        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.searchBtn.setOnClickListener {
            // Handle search
        }
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { item ->
            // Navigate to item detail
            viewModel.trackClick(item.id, arguments?.getString("SECTION_TYPE") ?: "all", 0)
        }

        binding.productsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productsAdapter
        }
    }

    private fun setupSortFilterBar() {
        binding.sortBtn.setOnClickListener { showSortMenu() }

        binding.filterBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.sortBtn)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_recommended -> viewModel.sortBy("recommended")
                R.id.sort_newest -> viewModel.sortBy("newest")
                R.id.sort_price_low -> viewModel.sortBy("price_low")
                R.id.sort_price_high -> viewModel.sortBy("price_high")
            }
            binding.sortText.text = item.title
            true
        }

        popup.show()
    }

    private fun setupDrawer() {
        // Setup filter categories - FIXED: Use navigation instead of Intent
        binding.filterCategory.setOnClickListener {
            navigateToFilterOptions("category", viewModel.getCategories())
        }

        binding.filterSchool.setOnClickListener {
            navigateToFilterOptions("school", viewModel.getSchools())
        }

        binding.filterCondition.setOnClickListener {
            navigateToFilterOptions("condition", viewModel.getConditions())
        }

        binding.filterColour.setOnClickListener {
            navigateToFilterOptions("colour", viewModel.getColors())
        }

        binding.filterBrand.setOnClickListener {
            navigateToFilterOptions("brand", viewModel.getBrands())
        }

        binding.filterPriceRange.setOnClickListener {
            showPriceRangeDialog()
        }

        binding.resetFilters.setOnClickListener {
            viewModel.resetFilters()
            binding.drawerLayout.closeDrawers()
        }

        binding.applyFilters.setOnClickListener {
            viewModel.applyFilters()
            binding.drawerLayout.closeDrawers()
        }
    }
    private fun navigateToFilterOptions(filterType: String, options: List<String>) {
        // Convert List<String> to Array<String> for safe navigation
        val optionsArray = options.toTypedArray()

        val bundle = Bundle().apply {
            putString("FILTER_TYPE", filterType)
            putStringArray("OPTIONS", optionsArray)  // Use putStringArray instead of putStringArrayList
        }

        findNavController().navigate(R.id.filterOptionsFragment, bundle)
    }

    // Add this to receive filter results
    private fun setupFilterResultListener() {
        parentFragmentManager.setFragmentResultListener("filter_request", viewLifecycleOwner) { _, result ->
            val selectedOptions = result.getStringArrayList("SELECTED_OPTIONS") ?: arrayListOf()
            val filterType = result.getString("FILTER_TYPE") ?: return@setFragmentResultListener

            when (filterType) {
                "category" -> viewModel.updateFilter("category", selectedOptions)
                "school" -> viewModel.updateFilter("school", selectedOptions)
                "condition" -> viewModel.updateFilter("condition", selectedOptions)
                "colour" -> viewModel.updateFilter("colour", selectedOptions)
                "brand" -> viewModel.updateFilter("brand", selectedOptions)
            }
        }
    }

    // In ProductsFragment.kt
    private fun showPriceRangeDialog() {
        val dialog = PriceRangeDialogFragment.newInstance(
            min = 0f,
            max = 1000f,
            currentMin = viewModel.currentMinPrice,
            currentMax = viewModel.currentMaxPrice
        )

        dialog.setOnPriceRangeAppliedListener { min, max ->
            viewModel.updatePriceRange(min, max)
            // Show selected price
            binding.selectedPriceRange.visibility = View.VISIBLE
            binding.selectedPriceRange.text = "R${min.toInt()} - R${max.toInt()}"
        }

        dialog.show(parentFragmentManager, PriceRangeDialogFragment.TAG)
    }

    // Add this to receive price range results
    private fun setupPriceRangeListener() {
        parentFragmentManager.setFragmentResultListener("price_range_request", viewLifecycleOwner) { _, result ->
            val min = result.getFloat("min_price", 0f)
            val max = result.getFloat("max_price", 1000f)
            viewModel.updatePriceRange(min, max)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.products.collect { products ->
                productsAdapter.submitList(products)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Show/hide loading
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}