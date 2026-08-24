package za.co.skoolswap.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.FragmentItemDetailBinding
import za.co.skoolswap.ui.detail.adapter.ImageSliderAdapter  // ✅ IMPORT
import za.co.skoolswap.ui.detail.adapter.SimilarItemsAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.utils.extensions.formatViewCount
import za.co.skoolswap.utils.ColorUtils
import za.co.skoolswap.workers.WorkerManager
import jakarta.inject.Inject

// Private constants
private const val NAVIGATION_ITEM_ID = "itemId"
private const val NAVIGATION_SOURCE = "source"
private const val SHIPPING_TOGGLE_EXPANDED = "−"
private const val SHIPPING_TOGGLE_COLLAPSED = "+"
private const val VIEW_COUNT_ZERO = 0

@AndroidEntryPoint
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemDetailViewModel by viewModels()

    private lateinit var similarItemsAdapter: SimilarItemsAdapter
    private var currentItemId: String? = null
    private var currentImageUrls: List<String>? = null

    @Inject
    lateinit var workerManager: WorkerManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.tag(LogTags.FRAGMENT).d("onCreateView called")
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag(LogTags.FRAGMENT).d("onViewCreated called")

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )

        binding.shippingExpandableContent.visibility = View.GONE
        binding.shippingToggle.text = SHIPPING_TOGGLE_COLLAPSED
        binding.shippingAnimation.pauseAnimation()

        val itemId = arguments?.getString(NAVIGATION_ITEM_ID)
        val source = arguments?.getString(NAVIGATION_SOURCE) ?: "unknown"

        Timber.tag(LogTags.FRAGMENT).d("Arguments - itemId: $itemId, source: $source")

        if (itemId == null) {
            Timber.tag(LogTags.FRAGMENT).e("itemId is null, popping backstack")
            findNavController().popBackStack()
            return
        }

        lifecycleScope.launch {
            workerManager.cacheItemNow(itemId)
        }

        currentItemId = itemId
        setupViews()
        hideFab()
        setupListeners()
        observeViewModel()
        viewModel.trackView(itemId, source)
        Timber.tag(LogTags.FRAGMENT).d("Loading item: $itemId from source: $source")
        viewModel.loadItem(itemId, source)
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
        Timber.tag(LogTags.UI).d("FAB hidden")
    }

    private fun setupViews() {
        Timber.tag(LogTags.UI).d("setupViews called")
        similarItemsAdapter = SimilarItemsAdapter { item ->
            Timber.tag(LogTags.UI).d("Similar item clicked: ${item.id}")
            navigateToDetail(item.id)
        }
        binding.similarRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = similarItemsAdapter
        }
        binding.imageSlider.apply {
            offscreenPageLimit = 2
            setCurrentItem(0, false)
        }
        Timber.tag(LogTags.UI).d("Views setup complete")
    }

    private fun setupListeners() {
        Timber.tag(LogTags.UI).d("setupListeners called")
        binding.backBtn.setOnClickListener {
            Timber.tag(LogTags.UI).d("Back button clicked")
            findNavController().popBackStack()
        }

        binding.shareBtn.setOnClickListener {
            Timber.tag(LogTags.UI).d("Share button clicked")
            shareItem()
        }

        binding.shippingHeader.setOnClickListener {
            Timber.tag(LogTags.UI).d("Shipping header clicked")
            toggleShippingSection()
        }

        binding.favBtn.setOnClickListener {
            Timber.tag(LogTags.UI).d("Favorite button clicked")
            viewModel.toggleFavorite()
        }

        binding.buyButton.setOnClickListener {
            Timber.tag(LogTags.UI).d("Buy button clicked")
            onBuyClick()
        }
    }

    private fun toggleShippingSection() {
        val content = binding.shippingExpandableContent
        val toggle = binding.shippingToggle

        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE
            toggle.text = SHIPPING_TOGGLE_COLLAPSED
            binding.shippingAnimation.pauseAnimation()
        } else {
            content.visibility = View.VISIBLE
            toggle.text = SHIPPING_TOGGLE_EXPANDED
            binding.shippingAnimation.playAnimation()
        }
    }

// ItemDetailFragment.kt

    private fun observeViewModel() {
        Timber.tag(LogTags.UI).d("observeViewModel called")

        // ================================================================
        // 1. MAIN ITEM STATE
        // ================================================================
        lifecycleScope.launch {
            viewModel.itemState.collect { state ->
                Timber.tag(LogTags.UI).d("ItemState received: $state")
                when (state) {
                    is ItemDetailState.Loading -> {
                        Timber.tag(LogTags.UI).d("State: Loading...")
                        showLoading(true)
                    }
                    is ItemDetailState.Success -> {
                        Timber.tag(LogTags.UI).d("State: Success! Item: ${state.item.name}, ID: ${state.item.id}")
                        showLoading(false)
                        bindItem(state.item)
                    }
                    is ItemDetailState.Error -> {
                        Timber.tag(LogTags.UI).e("State: Error: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        // ================================================================
        // 2. SIMILAR ITEMS SHIMMER - Single source of truth
        // ================================================================
        lifecycleScope.launch {
            viewModel.similarItemsShimmer.collect { isShimmering ->
                val items = viewModel.similarItems.value
                val isLoading = viewModel.itemState.value is ItemDetailState.Loading

                Timber.tag(LogTags.UI).d("Similar shimmer: $isShimmering, items: ${items.size}, loading: $isLoading")

                if (isShimmering) {
                    // Show shimmer
                    binding.similarShimmer.visibility = View.VISIBLE
                    startShimmer(binding.similarShimmer)
                    binding.similarRecycler.visibility = View.GONE
                    binding.similarSectionTitle.visibility = View.GONE
                    binding.similarEmptyState.visibility = View.GONE
                } else {
                    // Hide shimmer
                    stopShimmer(binding.similarShimmer)
                    binding.similarShimmer.visibility = View.GONE

                    // Show content ONLY if not loading main item
                    if (!isLoading) {
                        if (items.isEmpty()) {
                            binding.similarRecycler.visibility = View.GONE
                            binding.similarSectionTitle.visibility = View.GONE
                            binding.similarEmptyState.visibility = View.VISIBLE
                            binding.similarEmptyState.text = "No similar items available"
                        } else {
                            binding.similarRecycler.visibility = View.VISIBLE
                            binding.similarSectionTitle.visibility = View.VISIBLE
                            binding.similarEmptyState.visibility = View.GONE
                            similarItemsAdapter.submitList(items)
                        }
                    }
                }
            }
        }

        // ================================================================
        // 3. SIMILAR ITEMS - Only update when shimmer is done
        // ================================================================
        lifecycleScope.launch {
            viewModel.similarItems.collect { items ->
                val isShimmering = viewModel.similarItemsShimmer.value
                val isLoading = viewModel.itemState.value is ItemDetailState.Loading

                Timber.tag(LogTags.UI).d("Similar items updated: ${items.size} items, shimmer: $isShimmering, loading: $isLoading")

                // Skip if shimmer is active - let shimmer collector handle it
                if (isShimmering) {
                    return@collect
                }

                // Skip if main item is still loading
                if (isLoading) {
                    return@collect
                }

                // Update UI when shimmer is done
                stopShimmer(binding.similarShimmer)
                binding.similarShimmer.visibility = View.GONE

                if (items.isEmpty()) {
                    binding.similarRecycler.visibility = View.GONE
                    binding.similarSectionTitle.visibility = View.GONE
                    binding.similarEmptyState.visibility = View.VISIBLE
                    binding.similarEmptyState.text = "No similar items available"
                } else {
                    binding.similarRecycler.visibility = View.VISIBLE
                    binding.similarSectionTitle.visibility = View.VISIBLE
                    binding.similarEmptyState.visibility = View.GONE
                    similarItemsAdapter.submitList(items)
                }
            }
        }

        // ================================================================
        // 4. SIZE
        // ================================================================
        lifecycleScope.launch {
            viewModel.sizeName.collect { sizeName ->
                if (!sizeName.isNullOrEmpty()) {
                    val displaySize = sizeName.replace("Adult", "UK")
                    binding.productSize.text = displaySize
                    binding.productSize.visibility = View.VISIBLE
                } else {
                    binding.productSize.visibility = View.GONE
                }
            }
        }

        // ================================================================
        // 5. SCHOOL NAME
        // ================================================================
        lifecycleScope.launch {
            viewModel.schoolName.collect { schoolName ->
                if (!schoolName.isNullOrEmpty()) {
                    binding.productSchool.text = schoolName
                    binding.productSchool.visibility = View.VISIBLE
                } else {
                    binding.productSchool.visibility = View.GONE
                }
            }
        }

        // ================================================================
        // 6. SCHOOL LOGO
        // ================================================================
        lifecycleScope.launch {
            viewModel.schoolLogoUrl.collect { logoUrl ->
                if (!logoUrl.isNullOrEmpty()) {
                    Glide.with(this@ItemDetailFragment)
                        .load(logoUrl)
                        .placeholder(R.drawable.ic_school)
                        .error(R.drawable.ic_school)
                        .circleCrop()
                        .into(binding.schoolLogo)
                    binding.schoolLogo.visibility = View.VISIBLE
                } else {
                    binding.schoolLogo.visibility = View.GONE
                }
            }
        }

        // ================================================================
        // 7. COLOR
        // ================================================================
        lifecycleScope.launch {
            viewModel.colorName.collect { colorName ->
                if (!colorName.isNullOrEmpty()) {
                    binding.productColor.text = "Color: $colorName"
                    val colorInt = ColorUtils.getColorInt(colorName)
                    binding.productColor.setTextColor(colorInt)
                    binding.productColor.visibility = View.VISIBLE
                } else {
                    binding.productColor.visibility = View.GONE
                }
            }
        }

        // ================================================================
        // 8. CONDITION
        // ================================================================
        lifecycleScope.launch {
            viewModel.conditionName.collect { conditionName ->
                if (!conditionName.isNullOrEmpty()) {
                    binding.productCondition.text = "Condition: $conditionName"
                    binding.productCondition.visibility = View.VISIBLE
                } else {
                    binding.productCondition.visibility = View.GONE
                }
            }
        }

        // ================================================================
        // 9. BRAND
        // ================================================================
        lifecycleScope.launch {
            viewModel.brandName.collect { brandName ->
                if (!brandName.isNullOrEmpty()) {
                    binding.productBrand.text = "Brand: $brandName"
                    binding.productBrand.visibility = View.VISIBLE
                } else {
                    binding.productBrand.visibility = View.GONE
                }
            }
        }

        // ================================================================
        // 10. IMAGE URLS
        // ================================================================
        lifecycleScope.launch {
            viewModel.imageUrls.collect { urls ->
                currentImageUrls = urls
            }
        }

        // ================================================================
        // 11. FAVORITE STATUS
        // ================================================================
        lifecycleScope.launch {
            viewModel.isFavorite.collect { isFavorite ->
                updateFavoriteButtonIcon(isFavorite)
            }
        }
    }

    private fun updateFavoriteButtonIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.favBtn.setImageResource(R.drawable.ic_favorite_filled)
            binding.favBtn.setColorFilter(android.graphics.Color.BLACK)
        } else {
            binding.favBtn.setImageResource(R.drawable.ic_favorite)
            binding.favBtn.setColorFilter(android.graphics.Color.BLACK)
        }
    }

    private fun bindItem(item: Item) {
        binding.productTitle.text = item.name
        binding.productPrice.text = "R${String.format("%.2f", item.price)}"
        // ✅ Show raw data for debugging
        val priceDisplay = if (item.price > 0) {
            "R${String.format("%.2f", item.price)}"
        } else {
            "Price: ${item.price}"  // Shows the actual value
        }
        binding.productPrice.text = priceDisplay
        if (item.viewCount > VIEW_COUNT_ZERO) {
            binding.viewCountContainer.visibility = View.VISIBLE
            binding.viewCount.text = item.viewCount.formatViewCount()
        } else {
            binding.viewCountContainer.visibility = View.GONE
        }

        if (item.description.isNotEmpty()) {
            binding.productDescription.text = item.description
            binding.productDescription.visibility = View.VISIBLE
        } else {
            binding.productDescription.visibility = View.GONE
        }

        if (item.status == "sold" || item.quantity <= 0) {
            binding.soldBadge.visibility = View.VISIBLE
        } else {
            binding.soldBadge.visibility = View.GONE
        }

        setupImageSlider(item)
    }

    private fun setupImageSlider(item: Item) {
        val imageUrls = item.resolveAllImageUrls()

        if (imageUrls.isEmpty()) {
            binding.imageSlider.visibility = View.GONE
            return
        }

        binding.imageSlider.visibility = View.VISIBLE
        // ✅ Now ImageSliderAdapter is resolved with import
        binding.imageSlider.adapter = ImageSliderAdapter(imageUrls)
        binding.imageSlider.post { binding.imageSlider.setCurrentItem(0, false) }
    }

    private fun shareItem() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailState.Success) {
            val item = currentState.item
            val shareText = buildString {
                appendLine("📦 ${item.name}")
                appendLine("💰 Price: R${String.format("%.2f", item.price)}")
                if (!item.brandName.isNullOrBlank()) {
                    appendLine("🏷️ Brand: ${item.brandName}")
                }
                if (!item.sizeName.isNullOrBlank()) {
                    appendLine("📏 Size: ${item.sizeName.replace("Adult", "UK")}")
                }
                if (!item.conditionName.isNullOrBlank()) {
                    appendLine("✅ Condition: ${item.conditionName}")
                }
                appendLine()
                appendLine("Check it out on SkoolSwap app!")
            }

            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun onBuyClick() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailState.Success) {
            val item = currentState.item
            val sellerMobile = viewModel.sellerMobile.value

            if (sellerMobile.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Seller contact information not available",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // ✅ Fixed: Use constructor directly
            val bottomSheet = ContactOptionsBottomSheet(item, sellerMobile)
            bottomSheet.show(parentFragmentManager, "contact_options")
        }
    }

    private fun navigateToDetail(itemId: String) {
        val bundle = bundleOf(NAVIGATION_ITEM_ID to itemId)
        findNavController().navigate(R.id.itemDetailFragment, bundle)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            // Main item shimmer
            binding.shimmerLayout.visibility = View.VISIBLE
            startShimmer(binding.shimmerLayout)
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
            binding.errorLayout.visibility = View.GONE

            // Similar items shimmer - ONLY if ViewModel says so
            if (viewModel.similarItemsShimmer.value) {
                binding.similarShimmer.visibility = View.VISIBLE
                startShimmer(binding.similarShimmer)
                binding.similarRecycler.visibility = View.GONE
                binding.similarSectionTitle.visibility = View.GONE
                binding.similarEmptyState.visibility = View.GONE
            }
        } else {
            // Hide main shimmer
            stopShimmer(binding.shimmerLayout)
            binding.shimmerLayout.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
            binding.errorLayout.visibility = View.GONE

            // Let the similarItemsShimmer collector handle hiding similar shimmer
        }
    }

    private fun showError(message: String) {
        binding.similarShimmer.visibility = View.GONE
        binding.shimmerLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.scrollView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun startShimmer(shimmerLayout: View) {
        try {
            when (shimmerLayout) {
                is com.facebook.shimmer.ShimmerFrameLayout -> shimmerLayout.startShimmer()
                else -> shimmerLayout.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e("Error starting shimmer: ${e.message}")
            shimmerLayout.visibility = View.VISIBLE
        }
    }

    private fun stopShimmer(shimmerLayout: View) {
        try {
            when (shimmerLayout) {
                is com.facebook.shimmer.ShimmerFrameLayout -> shimmerLayout.stopShimmer()
                else -> shimmerLayout.visibility = View.GONE
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e("Error stopping shimmer: ${e.message}")
            shimmerLayout.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.tag(LogTags.FRAGMENT).d("onDestroyView called")
        _binding = null
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val fullScreenDialog = parentFragmentManager
                .findFragmentByTag("full_screen_viewer") as? DialogFragment

            if (fullScreenDialog != null && fullScreenDialog.isAdded && !fullScreenDialog.isRemoving) {
                fullScreenDialog.dismiss()
                return
            }

            isEnabled = false
            findNavController().popBackStack()
        }
    }
}