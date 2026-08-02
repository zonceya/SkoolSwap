package za.skoolswap.app.ui.sport

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import za.co.skoolswap.R
import za.co.skoolswap.databinding.FragmentSportBinding
import za.co.skoolswap.domain.model.GearItem
import za.co.skoolswap.domain.model.SportItem
import za.co.skoolswap.ui.products.adapter.ProductsAdapter
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
        setupSwipeRefresh()
        observeViewModel()
        viewModel.loadSportData()
    }

    private fun setupRecyclerViews() {
        featuredSportsAdapter = FeaturedSportsAdapter { sport ->
            navigateToSportProducts(sport)
        }
        binding.featuredSportsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = featuredSportsAdapter
        }

        moreSportsAdapter = MoreSportsAdapter { sport ->
            navigateToSportProducts(sport)
        }
        binding.moreSportsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = moreSportsAdapter
        }

        gearAdapter = GearAdapter { gear ->
            navigateToGearProducts(gear)
        }
        binding.gearRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = gearAdapter
        }

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
        // Loading state - LiveData is already view-lifecycle-safe
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && isFirstLoad) {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.scrollView.visibility = View.GONE
            } else if (!isLoading) {
                binding.shimmerLayout.visibility = View.GONE
            }
        }

        // Error state - StateFlow MUST use repeatOnLifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }

        viewModel.featuredSports.observe(viewLifecycleOwner) { sports ->
            if (sports.isNotEmpty()) {
                featuredSportsAdapter.submitList(sports)
            }
        }

        viewModel.moreSports.observe(viewLifecycleOwner) { sports ->
            if (sports.isNotEmpty()) {
                moreSportsAdapter.submitList(sports)
            }
        }

        viewModel.gearItems.observe(viewLifecycleOwner) { gear ->
            if (gear.isNotEmpty()) {
                gearAdapter.submitList(gear)
            }
        }

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
            putInt("CATEGORY_ID", 2)                    // Sports main category
            putInt("SUB_CATEGORY_ID", sport.id)         // ← This is key
            putString("SUB_CATEGORY_NAME", sport.name)  // For banner
            putString("MAIN_CATEGORY_NAME", "Sports")   // For banner
        }
        findNavController().navigate(R.id.action_sportFragment_to_productsFragment, bundle)
    }

    private fun navigateToGearProducts(gear: GearItem) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", "sport")
            putString("SECTION_TITLE", gear.name)
            putInt("CATEGORY_ID", 2)
            putString("GEAR_TYPE", gear.type)
            putInt("SUB_CATEGORY_ID", -1)               // Not a sub-category
            putString("SUB_CATEGORY_NAME", gear.name)
            putString("MAIN_CATEGORY_NAME", "Sports")
        }
        findNavController().navigate(R.id.action_sportFragment_to_productsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}