package com.example.skoolswap.ui.sport

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentSportBinding
import com.example.skoolswap.domain.model.GearItem
import com.example.skoolswap.domain.model.SportItem
import com.example.skoolswap.ui.products.adapter.ProductsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SportFragment : Fragment() {

    private var _binding: FragmentSportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SportViewModel by viewModels()

    private lateinit var featuredSportsAdapter: FeaturedSportsAdapter
    private lateinit var moreSportsAdapter: MoreSportsAdapter
    private lateinit var gearAdapter: GearAdapter
    private lateinit var productsAdapter: ProductsAdapter
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag("SportFragment").d("✅ onViewCreated - START")

        setupRecyclerViews()
        setupRetryButton()
        observeViewModel()
        viewModel.loadSportData()
    }

    private fun setupRecyclerViews() {
        // Featured Sports Grid (2 columns)
        featuredSportsAdapter = FeaturedSportsAdapter { sport ->
            navigateToSportProducts(sport)
        }
        binding.featuredSportsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = featuredSportsAdapter
        }

        // More Sports (Horizontal)
        moreSportsAdapter = MoreSportsAdapter { sport ->
            navigateToSportProducts(sport)
        }
        binding.moreSportsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = moreSportsAdapter
        }

        // Shop by Gear (Horizontal)
        gearAdapter = GearAdapter { gear ->
            navigateToGearProducts(gear)
        }
        binding.gearRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = gearAdapter
        }

        // Products Grid (All Sport Items)
        productsAdapter = ProductsAdapter { item ->
            val bundle = Bundle().apply {
                putString("itemId", item.id)
            }
            findNavController().navigate(R.id.itemDetailFragment, bundle)
        }
        binding.productsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productsAdapter
        }
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            viewModel.loadSportData()
        }
    }

    private fun observeViewModel() {
        // Loading state - show shimmer
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Timber.tag("SportFragment")
                .d("isLoading: $isLoading, isFirstLoad: $isFirstLoad, items empty: ${viewModel.allSportItems.value.isNullOrEmpty()}")
            if (isLoading && isFirstLoad && viewModel.allSportItems.value.isNullOrEmpty()) {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.scrollView.visibility = View.GONE
                binding.errorLayout.visibility = View.GONE
            } else {
                binding.shimmerLayout.visibility = View.GONE
            }
        }

        // Error state
        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null && viewModel.allSportItems.value.isNullOrEmpty()) {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.errorMessage.text = errorMsg
                    binding.scrollView.visibility = View.GONE
                    binding.shimmerLayout.visibility = View.GONE
                } else {
                    binding.errorLayout.visibility = View.GONE
                }
            }
        }

        // Featured Sports
        viewModel.featuredSports.observe(viewLifecycleOwner) { sports ->
            if (sports.isNotEmpty()) {
                featuredSportsAdapter.submitList(sports)
            }
        }

        // More Sports
        viewModel.moreSports.observe(viewLifecycleOwner) { sports ->
            if (sports.isNotEmpty()) {
                moreSportsAdapter.submitList(sports)
            }
        }

        // Gear Items
        viewModel.gearItems.observe(viewLifecycleOwner) { gear ->
            if (gear.isNotEmpty()) {
                gearAdapter.submitList(gear)
            }
        }

        // All Sport Items - hide shimmer when data arrives
        viewModel.allSportItems.observe(viewLifecycleOwner) { items ->
            Timber.tag("SportFragment")
                .d("All sport items received: ${items.size}, isFirstLoad: $isFirstLoad")
            isFirstLoad = false
            binding.shimmerLayout.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
            binding.errorLayout.visibility = View.GONE
            productsAdapter.submitList(items)
        }

    }

    private fun navigateToSportProducts(sport: SportItem) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", "sport")
            putString("SECTION_TITLE", sport.name)
            putInt("CATEGORY_ID", 2)
            putInt("SPORT_TYPE_ID", sport.id)
        }
        findNavController().navigate(R.id.action_sportFragment_to_productsFragment, bundle)
    }

    private fun navigateToGearProducts(gear: GearItem) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", "sport")
            putString("SECTION_TITLE", gear.name)
            putInt("CATEGORY_ID", 2)
            putString("GEAR_TYPE", gear.type)
        }
        findNavController().navigate(R.id.action_sportFragment_to_productsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}