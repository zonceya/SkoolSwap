package com.example.skoolswap.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentItemDetailBinding
import com.example.skoolswap.ui.detail.adapter.ImageSliderAdapter
import com.example.skoolswap.ui.detail.adapter.SimilarItemsAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount
import com.example.skoolswap.utils.ColorUtils
import com.example.skoolswap.workers.WorkerManager
import jakarta.inject.Inject

private const val TAG = "ItemDetailFragment"

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
        Timber.tag(TAG).d("onCreateView called")
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag(TAG).d("onViewCreated called")
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
        binding.shippingExpandableContent.visibility = View.GONE
        binding.shippingToggle.text = "+"
        binding.shippingAnimation.pauseAnimation()
        val itemId = arguments?.getString("itemId")
        val source = arguments?.getString("source") ?: "unknown"

        Timber.tag(TAG).d("Arguments - itemId: $itemId, source: $source")

        if (itemId == null) {
            Timber.tag(TAG).e("itemId is null, popping backstack")
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
        Timber.tag(TAG).d("Loading item: $itemId from source: $source")
        viewModel.loadItem(itemId, source)
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
        Timber.tag(TAG).d("FAB hidden")
    }

    private fun setupViews() {
        Timber.tag(TAG).d("setupViews called")
        similarItemsAdapter = SimilarItemsAdapter { item ->
            Timber.tag(TAG).d("Similar item clicked: ${item.id}")
            navigateToDetail(item.id)
        }
        binding.similarRecycler.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = similarItemsAdapter
        }
        binding.imageSlider.apply {
            offscreenPageLimit = 2
            setCurrentItem(0, false)
        }
        Timber.tag(TAG).d("Views setup complete")
    }

    private fun setupListeners() {
        Timber.tag(TAG).d("setupListeners called")
        binding.backBtn.setOnClickListener {
            Timber.tag(TAG).d("Back button clicked")
            findNavController().popBackStack()
        }

        binding.shareBtn.setOnClickListener {
            Timber.tag(TAG).d("Share button clicked")
            shareItem()
        }
        binding.shippingHeader.setOnClickListener {
            Timber.tag(TAG).d("Shipping header clicked")
            toggleShippingSection()
        }
        binding.favBtn.setOnClickListener {
            Timber.tag(TAG).d("Favorite button clicked")
            viewModel.toggleFavorite()  // Call ViewModel method directly
        }

        /*  binding.contactSeller.setOnClickListener {
            Timber.tag(TAG).d("Contact seller button clicked")
            contactSeller()
        }*/

        binding.buyButton.setOnClickListener {
            Timber.tag(TAG).d("Buy button clicked")
            onBuyClick()
        }

        binding.shippingHeader.setOnClickListener {
            Timber.tag(TAG).d("Shipping header clicked")
            toggleShippingSection()
        }
    }

    private fun toggleShippingSection() {
        val content = binding.shippingExpandableContent
        val toggle = binding.shippingToggle

        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE      // Collapse
            toggle.text = "+"                   // Show +
            // Optional: pause animation when collapsed
            binding.shippingAnimation.pauseAnimation()
        } else {
            content.visibility = View.VISIBLE   // Expand
            toggle.text = "−"                   // Show -
            // Optional: play animation when expanded
            binding.shippingAnimation.playAnimation()
        }
    }

    private fun observeViewModel() {
        Timber.tag(TAG).d("observeViewModel called")

        lifecycleScope.launch {
            Timber.tag(TAG).d("Starting itemState collection")
            viewModel.itemState.collect { state ->
                Timber.tag(TAG).d("ItemState received: $state")
                when (state) {
                    is ItemDetailViewModel.ItemDetailState.Loading -> {
                        Timber.tag(TAG).d("State: Loading...")
                        showLoading(true)
                    }

                    is ItemDetailViewModel.ItemDetailState.Success -> {
                        Timber.tag(TAG)
                            .d("State: Success! Item: ${state.item.name}, ID: ${state.item.id}")
                        Timber.tag(TAG).d("Item images count: ${state.item.images.size}")
                        state.item.images.forEachIndexed { index, image ->
                            Timber.tag(TAG).d("  Image $index: ${image.url}")
                        }
                        showLoading(false)
                        bindItem(state.item)
                    }

                    is ItemDetailViewModel.ItemDetailState.Error -> {
                        Timber.tag(TAG).e("State: Error: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.similarItemsShimmer.collect { isShimmering ->
                Timber.tag(TAG).d("Similar shimmer state: $isShimmering")
                if (isShimmering) {
                    // Show shimmer with animation
                    binding.similarShimmer.visibility = View.VISIBLE
                    startShimmer(binding.similarShimmer)  // ✅ Safe call
                    binding.similarRecycler.visibility = View.GONE
                    binding.similarSectionTitle.visibility = View.GONE
                } else {
                    // ✅ Stop shimmer and render items
                    stopShimmer(binding.similarShimmer)  // ✅ Safe call
                    binding.similarShimmer.visibility = View.GONE

                    val items = viewModel.similarItems.value
                    val isLoading = viewModel.itemState.value is ItemDetailViewModel.ItemDetailState.Loading

                    Timber.tag(TAG).d("Shimmer done - rendering ${items.size} items, isLoading=$isLoading")

                    if (!isLoading) {
                        if (items.isEmpty()) {
                            binding.similarRecycler.visibility = View.GONE
                            binding.similarSectionTitle.visibility = View.GONE
                            Timber.tag(TAG).d("No similar items - hiding section")
                        } else {
                            binding.similarRecycler.visibility = View.VISIBLE
                            binding.similarSectionTitle.visibility = View.VISIBLE
                            similarItemsAdapter.submitList(items)
                            Timber.tag(TAG).d("Showing ${items.size} similar items")
                        }
                    }
                }
            }
        }

        // ✅ FIXED: Similar items data
        lifecycleScope.launch {
            viewModel.similarItems.collect { items ->
                Timber.tag(TAG).d("Similar items received: ${items.size} items")

                // Check if we're still loading the main item
                val isLoading = viewModel.itemState.value is ItemDetailViewModel.ItemDetailState.Loading
                if (isLoading) {
                    // Still loading main item - keep shimmer
                    binding.similarShimmer.visibility = View.VISIBLE
                    startShimmer(binding.similarShimmer)  // ✅ Safe call
                    binding.similarRecycler.visibility = View.GONE
                    binding.similarSectionTitle.visibility = View.GONE
                    return@collect
                }

                // If shimmer is still active, let the shimmer collector handle rendering
                if (viewModel.similarItemsShimmer.value) {
                    Timber.tag(TAG).d("Shimmer still active - deferring to shimmer collector")
                    return@collect
                }

                // Shimmer is done, render items
                stopShimmer(binding.similarShimmer)  // ✅ Safe call
                binding.similarShimmer.visibility = View.GONE

                if (items.isEmpty()) {
                    binding.similarRecycler.visibility = View.GONE
                    binding.similarSectionTitle.visibility = View.GONE
                    Timber.tag(TAG).d("No similar items - hiding section")
                } else {
                    binding.similarRecycler.visibility = View.VISIBLE
                    binding.similarSectionTitle.visibility = View.VISIBLE
                    similarItemsAdapter.submitList(items)
                    Timber.tag(TAG).d("Showing ${items.size} similar items")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.sizeName.collect { sizeName ->
                Timber.tag(TAG).d("Size name updated: $sizeName")
                if (!sizeName.isNullOrEmpty()) {
                    val displaySize = sizeName.replace("Adult", "UK")
                    binding.productSize.text = displaySize
                    binding.productSize.visibility = View.VISIBLE
                    Timber.tag(TAG).d("Size displayed: $displaySize")
                } else {
                    binding.productSize.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.schoolName.collect { schoolName ->
                Timber.tag(TAG).d("School name updated: $schoolName")
                if (!schoolName.isNullOrEmpty()) {
                    binding.productSchool.text = schoolName
                    binding.productSchool.visibility = View.VISIBLE
                } else {
                    binding.productSchool.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.colorName.collect { colorName ->
                Timber.tag(TAG).d("Color name updated: $colorName")
                if (!colorName.isNullOrEmpty()) {
                    binding.productColor.text = "Color: $colorName"
                    val colorInt = ColorUtils.getColorInt(colorName)

                    // Make sure to apply the color correctly
                    binding.productColor.setTextColor(colorInt)
                    // Also add a small color circle indicator if you want
                    binding.productColor.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        null,
                        null,
                        null
                    )

                    binding.productColor.visibility = View.VISIBLE
                    Log.d(TAG, "Color set to: $colorName with color int: $colorInt")
                } else {
                    binding.productColor.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.conditionName.collect { conditionName ->
                Timber.tag(TAG).d("Condition name updated: $conditionName")
                if (!conditionName.isNullOrEmpty()) {
                    binding.productCondition.text = "Condition: $conditionName"
                    binding.productCondition.visibility = View.VISIBLE
                } else {
                    binding.productCondition.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.brandName.collect { brandName ->
                Timber.tag(TAG).d("Brand name updated: $brandName")
                if (!brandName.isNullOrEmpty()) {
                    binding.productBrand.text = "Brand: $brandName"
                    binding.productBrand.visibility = View.VISIBLE
                } else {
                    binding.productBrand.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.imageUrls.collect { urls ->
                Timber.tag(TAG).d("===== IMAGE URLS FROM VIEWMODEL =====")
                Timber.tag(TAG).d("Received ${urls.size} image URLs")
                urls.forEachIndexed { index, url ->
                    Timber.tag(TAG).d("  URL $index: $url")
                }
                Timber.tag(TAG).d("=====================================")
                currentImageUrls = urls
            }
        }

        lifecycleScope.launch {
            viewModel.isFavorite.collect { isFavorite ->
                Timber.tag(TAG).d("Favorite status changed: $isFavorite")
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
        Log.d(TAG, "Favorite button icon updated: ${if (isFavorite) "filled" else "outline"}")
    }

    private fun bindItem(item: Item) {
        Timber.tag(TAG).d("=== BINDING ITEM START ===")
        Timber.tag(TAG).d("Item ID: ${item.id}")
        Timber.tag(TAG).d("Item Name: ${item.name}")
        Timber.tag(TAG).d("Item Price: ${item.price}")
        Timber.tag(TAG).d("Item Status: ${item.status}")
        Timber.tag(TAG).d("Item Quantity: ${item.quantity}")

        binding.productTitle.text = item.name
        binding.productPrice.text = "R${String.format("%.2f", item.price)}"

        // ADD THIS
        if (item.viewCount > 0) {
            binding.viewCountContainer.visibility = View.VISIBLE
            binding.viewCount.text = item.viewCount.formatViewCount()
        } else {
            binding.viewCountContainer.visibility = View.GONE
        }
        if (item.description.isNotEmpty()) {
            binding.productDescription.text = item.description
            binding.productDescription.visibility = View.VISIBLE
            Timber.tag(TAG).d("Description set: ${item.description.take(50)}...")
        } else {
            binding.productDescription.visibility = View.GONE
            Timber.tag(TAG).d("No description")
        }

        if (item.status == "sold" || item.quantity <= 0) {
            binding.soldBadge.visibility = View.VISIBLE
            Timber.tag(TAG).d("Sold badge visible")
        } else {
            binding.soldBadge.visibility = View.GONE
            Timber.tag(TAG).d("Sold badge hidden")
        }

        setupImageSlider(item)
        Timber.tag(TAG).d("=== BINDING ITEM END ===")
    }

    private fun setupImageSlider(item: Item) {
        val imageUrls = item.resolveAllImageUrls()

        if (imageUrls.isEmpty()) {
            binding.imageSlider.visibility = View.GONE
            return
        }

        binding.imageSlider.visibility = View.VISIBLE
        binding.imageSlider.adapter = ImageSliderAdapter(imageUrls)
        binding.imageSlider.post { binding.imageSlider.setCurrentItem(0, false) }
    }

    private fun shareItem() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item

            // Build richer share message
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

    private fun contactSeller() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item
            val sellerMobile = item.shop?.sellerMobile

            if (!sellerMobile.isNullOrBlank()) {
                Timber.tag(TAG).d("Contacting seller via mobile: $sellerMobile")
                // Open WhatsApp or dialer
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(
                        "https://wa.me/${
                            sellerMobile.replace(
                                Regex("[^0-9]"),
                                ""
                            )
                        }"
                    )
                }
                startActivity(intent)
            } else {
                Timber.tag(TAG).w("No seller mobile available")
                // Show dialog or fallback to chat
            }
        }
    }

    private fun onBuyClick() {
        Timber.tag(TAG).d("onBuyClick called")
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item
            val sellerMobile = viewModel.sellerMobile.value  // Get from ViewModel

            if (sellerMobile.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Seller contact information not available",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val bottomSheet = ContactOptionsBottomSheet(item, sellerMobile)  // Pass mobile number
            bottomSheet.show(parentFragmentManager, "contact_options")
        } else {
            Timber.tag(TAG).w("Cannot buy - no item loaded")
        }
    }

    private fun navigateToDetail(itemId: String) {
        Timber.tag(TAG).d("Navigating to item detail: $itemId")
        val bundle = bundleOf("itemId" to itemId)
        findNavController().navigate(R.id.itemDetailFragment, bundle)
        Timber.tag(TAG).d("Navigation called")
    }

    private fun showLoading(show: Boolean) {
        Timber.tag(TAG).d("showLoading: $show")
        if (show) {
            // Show main shimmer
            binding.shimmerLayout.visibility = View.VISIBLE
            startShimmer(binding.shimmerLayout)  // ✅ Safe call
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
            binding.errorLayout.visibility = View.GONE

            // Show similar items shimmer with animation
            binding.similarShimmer.visibility = View.VISIBLE
            startShimmer(binding.similarShimmer)  // ✅ Safe call
            binding.similarRecycler.visibility = View.GONE
            binding.similarSectionTitle.visibility = View.GONE
        } else {
            // Hide main shimmer
            stopShimmer(binding.shimmerLayout)  // ✅ Safe call
            binding.shimmerLayout.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
            binding.errorLayout.visibility = View.GONE

            // Let the similarItems collector handle showing/hiding similar items
        }
    }

    private fun showError(message: String) {
        Timber.tag(TAG).e("showError: $message")
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
            Timber.tag(TAG).e("Error starting shimmer: ${e.message}")
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
            Timber.tag(TAG).e("Error stopping shimmer: ${e.message}")
            shimmerLayout.visibility = View.GONE
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Do NOT show action bar here — it triggers a synchronous layout pass
        // during fragment teardown, causing 30+ frame skips. Show it in the
        // destination fragment's onResume instead, or use a shared ViewModel flag.
        Timber.tag(TAG).d("onDestroyView called")
        _binding = null
        // backPressedCallback auto-removes via viewLifecycleOwner — explicit remove not needed
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).d("onResume called - currentItemId: $currentItemId")
        // Hide action bar here instead of onCreateView (called twice currently)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(TAG).d("onPause called")
    }

    override fun onStop() {
        super.onStop()
        // Show action bar here — fires after the transition is complete,
        // not during the layout pass that causes jank
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val fullScreenDialog = parentFragmentManager
                .findFragmentByTag("full_screen_viewer") as? DialogFragment

            // ✅ Only intercept if the dialog is actually showing — not mid-dismissal
            if (fullScreenDialog != null && fullScreenDialog.isAdded && !fullScreenDialog.isRemoving) {
                fullScreenDialog.dismiss()
                return
            }

            isEnabled = false
            findNavController().popBackStack()
        }
    }

}