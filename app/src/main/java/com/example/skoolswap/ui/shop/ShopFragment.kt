package com.example.skoolswap.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentShopBinding
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemCategorySection
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShopViewModel by viewModels()

    private lateinit var productAdapter: ProductAdapter
    private lateinit var categorySectionAdapter: CategorySectionAdapter
    @Inject
    lateinit var authRepository: AuthRepositoryInterface
    private var isCategoriesView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabListeners()
        setupObservers()

        // Load data
        viewModel.loadMyShop()
        viewModel.loadMyShopItems()
    }

    private fun setupRecyclerViews() {
        // Adapter for All Items view (simple grid)
        productAdapter = ProductAdapter { itemId ->
            navigateToEditItem(itemId)
        }

        // Adapter for Categories view (sectioned)
        categorySectionAdapter = CategorySectionAdapter { itemId ->
            navigateToEditItem(itemId)
        }

        binding.productRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun setupTabListeners() {
        binding.allItemsTab.setOnClickListener {
            if (!isCategoriesView) return@setOnClickListener
            isCategoriesView = false
            updateTabStyles()
            showAllItemsView()
        }

        binding.categoriesTab.setOnClickListener {
            if (isCategoriesView) return@setOnClickListener
            isCategoriesView = true
            updateTabStyles()
            showCategoriesView()
        }
    }

    private fun updateTabStyles() {
        if (isCategoriesView) {
            // Categories tab selected
            binding.categoriesTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            binding.categoriesTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.allItemsTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.allItemsTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        } else {
            // All Items tab selected
            binding.allItemsTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            binding.allItemsTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.categoriesTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.categoriesTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun showAllItemsView() {
        val allItems = viewModel.allItems.value
        if (allItems.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.productRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE

            // For All Items, use GridLayoutManager
            binding.productRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

            productAdapter.submitList(allItems)
            binding.productRecyclerView.adapter = productAdapter
        }
    }
    private fun loadProfilePicture(url: String?) {
        try {
            Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .timeout(10000)
                .into(binding.storeProfileImage)
        } catch (e: Exception) {
            binding.storeProfileImage.setImageResource(R.drawable.ic_user)
        }
    }
    private fun showCategoriesView() {
        val allItems = viewModel.allItems.value
        if (allItems.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.productRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE

            // IMPORTANT: Use LinearLayoutManager for VERTICAL stacking of sections
            binding.productRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            val sections = groupItemsByCategory(allItems)
            categorySectionAdapter.submitSections(sections)
            binding.productRecyclerView.adapter = categorySectionAdapter
        }
    }

    private fun groupItemsByCategory(items: List<Item>): List<ItemCategorySection> {
        return items.groupBy { item ->
            viewModel.getCategoryFromTypeId(item.itemTypeId) ?: "Other"
        }.map { (categoryName, categoryItems) ->
            ItemCategorySection(
                categoryName = categoryName,
                items = categoryItems
            )
        }.sortedBy { it.categoryName }
    }

    private fun setupObservers() {
        // Observe all items
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allItems.collectLatest { items ->
                if (isCategoriesView) {
                    val sections = groupItemsByCategory(items)
                    categorySectionAdapter.submitSections(sections)
                } else {
                    productAdapter.submitList(items)
                }

                // Update stats
                viewModel.currentShop.value?.let { shop ->
                    binding.storeStats.text = "${items.size} items"
                }

                // Handle empty state
                if (items.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.productRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.productRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        // Observe shop info - UPDATED with profile picture loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentShop.collectLatest { shop ->
                shop?.let {
                    binding.storeName.text = it.displayName.ifEmpty { it.name }
                    binding.storeStats.text = "${viewModel.allItems.value.size} items"

                    // Load profile picture
                    if (it.profilePictureUrl.isNotEmpty()) {
                        loadProfilePicture(it.profilePictureUrl)
                    } else {
                        // Fallback to user profile picture
                        authRepository.getServerUser().collect { user ->
                            user?.profilePictureUrl?.let { url ->
                                if (url.isNotEmpty()) {
                                    loadProfilePicture(url)
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    private fun navigateToEditItem(itemId: String) {
        findNavController().navigate(R.id.editItemFragment, Bundle().apply {
            putString("itemId", itemId)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}