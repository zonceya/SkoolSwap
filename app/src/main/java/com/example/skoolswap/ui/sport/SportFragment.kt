package com.example.skoolswap.ui.sport

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentSportBinding
import com.example.skoolswap.domain.model.GearItem
import com.example.skoolswap.domain.model.SportItem
import com.example.skoolswap.ui.products.adapter.ProductsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SportFragment : Fragment() {

    private var _binding: FragmentSportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SportViewModel by viewModels()

    private lateinit var featuredSportsAdapter: FeaturedSportsAdapter
    private lateinit var moreSportsAdapter: MoreSportsAdapter
    private lateinit var gearAdapter: GearAdapter
    private lateinit var productsAdapter: ProductsAdapter  // ← ADD THIS

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
        Log.d("SportFragment", "✅ onViewCreated - START")
        setupRecyclerViews()
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

        // ← ADD PRODUCTS GRID (All Sport Items)
        productsAdapter = ProductsAdapter { item ->
            // Navigate to product detail
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

    private fun observeViewModel() {
        viewModel.featuredSports.observe(viewLifecycleOwner) { sports ->
            featuredSportsAdapter.submitList(sports)
        }

        viewModel.moreSports.observe(viewLifecycleOwner) { sports ->
            moreSportsAdapter.submitList(sports)
        }

        viewModel.gearItems.observe(viewLifecycleOwner) { gear ->
            gearAdapter.submitList(gear)
        }

        viewModel.allSportItems.observe(viewLifecycleOwner) { items ->
            Log.d("SportFragment", "All sport items received: ${items.size}")
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