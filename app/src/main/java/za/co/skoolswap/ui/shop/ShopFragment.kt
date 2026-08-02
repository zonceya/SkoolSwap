package za.co.skoolswap.ui.shop

import android.os.Bundle
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
import com.bumptech.glide.Glide
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.FragmentShopBinding
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.ItemCategorySection
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@AndroidEntryPoint
class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShopViewModel by viewModels()

    private lateinit var shopProductAdapter: ShopProductAdapter
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
                    Timber.tag(LogTags.UI).d("🔄 Item updated, refreshing immediately")
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
        shopProductAdapter = ShopProductAdapter { itemId ->
            Timber.tag(LogTags.UI).d("All items clicked: $itemId")
            navigateToEditItem(itemId)
        }

        categoryGridAdapter = CategoryGridAdapter(
            onItemClick = { itemId ->
                Timber.tag(LogTags.UI).d("Category grid item clicked: $itemId")
                navigateToEditItem(itemId)
            },
            onSoldToggle = { itemId, markAsSold ->
                Timber.tag(LogTags.UI).d("🔄 Sold toggle: $itemId -> $markAsSold")
                viewModel.toggleItemSoldStatus(itemId, markAsSold)
            },
            isShopMode = true
        )

        categorySectionAdapter = CategorySectionAdapter(
            onItemClick = { itemId ->
                Timber.tag(LogTags.UI).d("Category section item clicked: $itemId")
                navigateToEditItem(itemId)
            },
            onSoldToggle = { itemId, markAsSold ->
                Timber.tag(LogTags.UI).d("🔄 Sold toggle: $itemId -> $markAsSold")
                viewModel.toggleItemSoldStatus(itemId, markAsSold)
            },
            isShopMode = true
        )

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
        val blackColor = ContextCompat.getColor(requireContext(), android.R.color.black)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        if (isCategoriesView) {
            binding.categoriesTab.setBackgroundColor(blackColor)
            binding.categoriesTab.setTextColor(whiteColor)
            binding.allItemsTab.setBackgroundColor(whiteColor)
            binding.allItemsTab.setTextColor(blackColor)
        } else {
            binding.allItemsTab.setBackgroundColor(blackColor)
            binding.allItemsTab.setTextColor(whiteColor)
            binding.categoriesTab.setBackgroundColor(whiteColor)
            binding.categoriesTab.setTextColor(blackColor)
        }
    }

    private fun showAllItemsView() {
        val allItems = viewModel.allItems.value
        Timber.tag(LogTags.UI).d("showAllItemsView: ${allItems.size} items")

        if (allItems.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.emptyStateText.text = getString(R.string.shop_empty_items)
            binding.productRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE

            binding.productRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
            shopProductAdapter.submitList(allItems)
            binding.productRecyclerView.adapter = shopProductAdapter
        }
    }

    private fun showCategoriesView() {
        val allItems = viewModel.allItems.value
        Timber.tag(LogTags.UI).d("showCategoriesView: ${allItems.size} items")

        if (allItems.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.emptyStateText.text = getString(R.string.shop_empty_items)
            binding.productRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE

            val sections = groupItemsByCategory(allItems)
            categorySectionAdapter.submitList(sections)

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
                .override(216, 216)
                .timeout(10000)
                .into(binding.storeProfileImage)
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Failed to load profile picture")
            binding.storeProfileImage.setImageResource(R.drawable.ic_user)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                Timber.tag(LogTags.UI).d("Loading state: $isLoading")
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allItems.collectLatest { items ->
                Timber.tag(LogTags.UI).d("=== ALL ITEMS RECEIVED ===")
                Timber.tag(LogTags.UI).d("Total items count: ${items.size}")
                items.forEachIndexed { i, item ->
                    Timber.tag(LogTags.UI).d("Item[$i]: ${item.name}, viewCount: ${item.viewCount}, status: ${item.status}, id: ${item.id}")
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
                    val sections = groupItemsByCategory(items)
                    categorySectionAdapter.submitList(sections)
                    binding.productRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.productRecyclerView.adapter = categorySectionAdapter
                } else {
                    shopProductAdapter.submitList(items)
                    binding.productRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
                    binding.productRecyclerView.adapter = shopProductAdapter
                }

                binding.storeStats.text = "${items.size} items"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {  // ← COROUTINE SCOPE
            viewModel.currentShop.collectLatest { shop ->  // ← Called INSIDE the coroutine
                shop?.let {
                    binding.storeName.text = it.displayName.ifEmpty { it.name }

                    val picUrl = it.profilePictureUrl.takeIf { p -> p.isNotEmpty() }
                        ?: withTimeoutOrNull(2000) {
                            authRepository.getServerUser().filterNotNull().first()
                        }?.profilePictureUrl

                    if (!picUrl.isNullOrEmpty()) {
                        loadProfilePicture(picUrl)
                    } else {
                        binding.storeProfileImage.setImageResource(R.drawable.ic_user)
                    }
                }
            }
        }
    }

    private fun navigateToEditItem(itemId: String) {
        Timber.tag(LogTags.UI).d("🔍 Navigating to edit item with ID: $itemId")
        try {
            val action = R.id.action_shopFragment_to_editItemFragment
            val bundle = Bundle().apply {
                putString("itemId", itemId)
            }
            findNavController().navigate(action, bundle)
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Failed to navigate to edit item")
            try {
                findNavController().navigate(R.id.editItemFragment, Bundle().apply {
                    putString("itemId", itemId)
                })
            } catch (e2: Exception) {
                Timber.tag(LogTags.UI).e(e2, "Fallback navigation also failed")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}