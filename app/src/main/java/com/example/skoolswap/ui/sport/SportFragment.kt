package com.example.skoolswap.ui.sport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var moreSportsAdapter: MoreSportsAdapter  // ✅ Fixed name
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
        setupSwipeRefresh()
        observeViewModel()
        viewModel.loadSportData()
    }

    private fun setupRecyclerViews() {
        // ✅ Featured Sports - Always images
        featuredSportsAdapter = FeaturedSportsAdapter { sport ->
            navigateToSportProducts(sport)
        }
        binding.featuredSportsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = featuredSportsAdapter
        }

        // ✅ More Sports - Always text chips
        moreSportsAdapter = MoreSportsAdapter { sport ->  // ✅ Fixed
            navigateToSportProducts(sport)
        }
        binding.moreSportsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = moreSportsAdapter
        }

        // Gear adapter
        gearAdapter = GearAdapter { gear ->
            navigateToGearProducts(gear)
        }
        binding.gearRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = gearAdapter
        }

        // Products Grid
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

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshSports()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            viewModel.loadSportData()
        }
    }

    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
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

        // ✅ Featured Sports - Images that change randomly
        viewModel.featuredSports.observe(viewLifecycleOwner) { sports ->
            if (sports.isNotEmpty()) {
                featuredSportsAdapter.submitList(sports)
            }
        }

        // ✅ More Sports - Text chips that change randomly
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

        // All Sport Items
        viewModel.allSportItems.observe(viewLifecycleOwner) { items ->
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