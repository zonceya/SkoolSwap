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
import com.example.skoolswap.utils.extensions.formatViewCount
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShopViewModel by viewModels()

    // ✅ Use ShopProductAdapter for All Items (view count, no toggle)
    private lateinit var shopProductAdapter: ShopProductAdapter

    // ✅ Use CategoryGridAdapter for Categories (view count + toggle)
    private lateinit var categoryGridAdapter: CategoryGridAdapter
    private lateinit var categorySectionAdapter: CategorySectionAdapter

    @Inject
    lateinit var authRepository: AuthRepositoryInterface
    private var isCategoriesView = false
    private var isFirstLoad = true

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
        updateTabStyles()
        binding.retryButton.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            viewModel.refresh()
        }
        viewModel.loadMyShop()
        viewModel.loadMyShopItems()

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("item_updated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    Timber.d("🔄 Item updated, refreshing immediately")
                    val updatedItemId = findNavController().currentBackStackEntry
                        ?.savedStateHandle?.get<String>("updated_item_id")

                    if (updatedItemId != null) {
                        viewModel.loadMyShopItems()
                        findNavController().currentBackStackEntry
                            ?.savedStateHandle?.remove<String>("updated_item_id")
                    } else {
                        viewModel.loadMyShopItems()
                    }

                    findNavController().currentBackStackEntry
                        ?.savedStateHandle?.remove<Boolean>("item_updated")
                }
            }
    }

    private fun setupRecyclerViews() {
        // ✅ All Items - ShopProductAdapter (view count, no toggle)
        shopProductAdapter = ShopProductAdapter { itemId ->
            Timber.d("All items clicked: $itemId")
            navigateToEditItem(itemId)
        }

        // ✅ Categories - CategoryGridAdapter (view count + toggle)
        categoryGridAdapter = CategoryGridAdapter(
            onItemClick = { itemId ->
                Timber.d("Category grid item clicked: $itemId")
                navigateToEditItem(itemId)
            },
            onSoldToggle = { itemId, markAsSold ->
                Timber.d("🔄 Sold toggle: $itemId -> $markAsSold")
                viewModel.toggleItemSoldStatus(itemId, markAsSold)
            },
            isShopMode = true
        )

        // ✅ Categories Section - CategorySectionAdapter (grouped with toggle)
        categorySectionAdapter = CategorySectionAdapter(
            onItemClick = { itemId ->
                Timber.d("Category section item clicked: $itemId")
                navigateToEditItem(itemId)
            },
            onSoldToggle = { itemId, markAsSold ->
                Timber.d("🔄 Sold toggle: $itemId -> $markAsSold")
                viewModel.toggleItemSoldStatus(itemId, markAsSold)
            },
            isShopMode = true
        )

        // Default to GridLayoutManager for All Items
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
            binding.categoriesTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            binding.categoriesTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.allItemsTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.allItemsTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        } else {
            binding.allItemsTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            binding.allItemsTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.categoriesTab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.categoriesTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun showAllItemsView() {
        val allItems = viewModel.allItems.value
        Timber.d("showAllItemsView: ${allItems.size} items")

        if (allItems.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.productRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE

            // ✅ All Items - ShopProductAdapter (view count, no toggle)
            binding.productRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
            shopProductAdapter.submitList(allItems)
            binding.productRecyclerView.adapter = shopProductAdapter
        }
    }

    private fun showCategoriesView() {
        val allItems = viewModel.allItems.value
        Timber.d("showCategoriesView: ${allItems.size} items")

        if (allItems.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.productRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE

            // ✅ Categories - grouped by category with toggle
            val sections = groupItemsByCategory(allItems)
            categorySectionAdapter.submitSections(sections)

            // ✅ Use LinearLayoutManager for sections (vertical scrolling)
            binding.productRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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
            Timber.e(e, "Failed to load profile picture")
            binding.storeProfileImage.setImageResource(R.drawable.ic_user)
        }
    }

    private fun setupObservers() {
        // Loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                Timber.d("Loading state: $isLoading")
                if (isLoading && isFirstLoad && viewModel.allItems.value.isEmpty()) {
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.productRecyclerView.visibility = View.GONE
                    binding.emptyStateText.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                    binding.loadingProgress.visibility = View.GONE
                } else {
                    binding.shimmerLayout.visibility = View.GONE
                }
            }
        }

        // Error state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null && viewModel.allItems.value.isEmpty()) {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.errorMessage.text = errorMsg
                    binding.productRecyclerView.visibility = View.GONE
                    binding.emptyStateText.visibility = View.GONE
                    binding.shimmerLayout.visibility = View.GONE
                    binding.loadingProgress.visibility = View.GONE
                } else {
                    binding.errorLayout.visibility = View.GONE
                }
            }
        }

        // All items
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allItems.collectLatest { items ->
                Timber.d("=== ALL ITEMS RECEIVED ===")
                Timber.d("Total items count: ${items.size}")
                items.forEachIndexed { i, item ->
                    Timber.d("Item[$i]: ${item.name}, viewCount: ${item.viewCount}, status: ${item.status}, id: ${item.id}")
                }

                if (items.isEmpty()) {
                    binding.loadingProgress.visibility = View.VISIBLE
                    binding.productRecyclerView.visibility = View.GONE
                    binding.emptyStateText.visibility = View.GONE
                    return@collectLatest
                }

                binding.loadingProgress.visibility = View.GONE
                binding.emptyStateText.visibility = View.GONE
                binding.productRecyclerView.visibility = View.VISIBLE

                if (isCategoriesView) {
                    // ✅ Categories view - grouped with toggle
                    val sections = groupItemsByCategory(items)
                    categorySectionAdapter.submitSections(sections)
                    binding.productRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.productRecyclerView.adapter = categorySectionAdapter
                } else {
                    // ✅ All Items view - flat grid with view count (no toggle)
                    shopProductAdapter.submitList(items)
                    binding.productRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
                    binding.productRecyclerView.adapter = shopProductAdapter
                }

                binding.storeStats.text = "${items.size} items"
            }
        }

        // Shop info
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentShop.collectLatest { shop ->
                shop?.let {
                    Timber.d("Shop loaded: ${it.name}")
                    binding.storeName.text = it.displayName.ifEmpty { it.name }
                    if (it.profilePictureUrl.isNotEmpty()) {
                        loadProfilePicture(it.profilePictureUrl)
                    } else {
                        authRepository.getServerUser().collect { user ->
                            user?.profilePictureUrl?.let { url ->
                                if (url.isNotEmpty()) loadProfilePicture(url)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToEditItem(itemId: String) {
        Timber.d("🔍 Navigating to edit item with ID: $itemId")
        try {
            val action = R.id.action_shopFragment_to_editItemFragment
            val bundle = Bundle().apply {
                putString("itemId", itemId)
            }
            findNavController().navigate(action, bundle)
        } catch (e: Exception) {
            Timber.e(e, "Failed to navigate to edit item")
            try {
                findNavController().navigate(R.id.editItemFragment, Bundle().apply {
                    putString("itemId", itemId)
                })
            } catch (e2: Exception) {
                Timber.e(e2, "Fallback navigation also failed")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}