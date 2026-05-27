package com.example.skoolswap.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.utils.extensions.formatViewCount

private const val TAG = "ItemDetailFragment"

@AndroidEntryPoint
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemDetailViewModel by viewModels()

    private lateinit var similarItemsAdapter: SimilarItemsAdapter
    private var currentItemId: String? = null
    private var currentImageUrls: List<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        val itemId = arguments?.getString("itemId")
        val source = arguments?.getString("source") ?: "unknown"

        Log.d(TAG, "Arguments - itemId: $itemId, source: $source")

        if (itemId == null) {
            Log.e(TAG, "itemId is null, popping backstack")
            findNavController().popBackStack()
            return
        }

        currentItemId = itemId
        setupViews()
        hideFab()
        setupListeners()
        observeViewModel()
        viewModel.trackView(itemId, source)
        Log.d(TAG, "Loading item: $itemId from source: $source")
        viewModel.loadItem(itemId, source)
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
        Log.d(TAG, "FAB hidden")
    }

    private fun setupViews() {
        Log.d(TAG, "setupViews called")
        similarItemsAdapter = SimilarItemsAdapter { item ->
            Log.d(TAG, "Similar item clicked: ${item.id}")
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
        Log.d(TAG, "Views setup complete")
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners called")
        binding.backBtn.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            findNavController().popBackStack()
        }

        binding.shareBtn.setOnClickListener {
            Log.d(TAG, "Share button clicked")
            shareItem()
        }

        // FIXED: Only ONE favorite button listener
        binding.favBtn.setOnClickListener {
            Log.d(TAG, "Favorite button clicked")
            viewModel.toggleFavorite()  // Call ViewModel method directly
        }

        binding.contactSeller.setOnClickListener {
            Log.d(TAG, "Contact seller button clicked")
            contactSeller()
        }

        binding.buyButton.setOnClickListener {
            Log.d(TAG, "Buy button clicked")
            onBuyClick()
        }

        binding.shippingHeader.setOnClickListener {
            Log.d(TAG, "Shipping header clicked")
            toggleShippingSection()
        }
    }

    private fun toggleShippingSection() {
        val isVisible = binding.shippingExpandableContent.isVisible
        Log.d(TAG, "Toggle shipping section, currently visible: $isVisible")

        if (isVisible) {
            binding.shippingExpandableContent.visibility = View.GONE
            binding.shippingToggle.text = "+"
        } else {
            binding.shippingExpandableContent.visibility = View.VISIBLE
            binding.shippingToggle.text = "-"
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel called")

        lifecycleScope.launch {
            Log.d(TAG, "Starting itemState collection")
            viewModel.itemState.collect { state ->
                Log.d(TAG, "ItemState received: $state")
                when (state) {
                    is ItemDetailViewModel.ItemDetailState.Loading -> {
                        Log.d(TAG, "State: Loading...")
                        showLoading(true)
                    }
                    is ItemDetailViewModel.ItemDetailState.Success -> {
                        Log.d(TAG, "State: Success! Item: ${state.item.name}, ID: ${state.item.id}")
                        Log.d(TAG, "Item images count: ${state.item.images.size}")
                        state.item.images.forEachIndexed { index, image ->
                            Log.d(TAG, "  Image $index: ${image.url}")
                        }
                        showLoading(false)
                        bindItem(state.item)
                    }
                    is ItemDetailViewModel.ItemDetailState.Error -> {
                        Log.e(TAG, "State: Error: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.similarItems.collect { items ->
                Log.d(TAG, "Similar items updated: ${items.size} items")
                similarItemsAdapter.submitList(items)
            }
        }

        lifecycleScope.launch {
            viewModel.sizeName.collect { sizeName ->
                Log.d(TAG, "Size name updated: $sizeName")
                if (!sizeName.isNullOrEmpty()) {
                    val displaySize = sizeName.replace("Adult", "UK")
                    binding.productSize.text = "$displaySize"
                    binding.productSize.visibility = View.VISIBLE
                    Log.d(TAG, "Size displayed: $displaySize")
                } else {
                    binding.productSize.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.schoolName.collect { schoolName ->
                Log.d(TAG, "School name updated: $schoolName")
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
                Log.d(TAG, "Color name updated: $colorName")
                if (!colorName.isNullOrEmpty()) {
                    binding.productColor.text = "Color: $colorName"
                    binding.productColor.visibility = View.VISIBLE
                } else {
                    binding.productColor.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.conditionName.collect { conditionName ->
                Log.d(TAG, "Condition name updated: $conditionName")
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
                Log.d(TAG, "Brand name updated: $brandName")
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
                Log.d(TAG, "===== IMAGE URLS FROM VIEWMODEL =====")
                Log.d(TAG, "Received ${urls.size} image URLs")
                urls.forEachIndexed { index, url ->
                    Log.d(TAG, "  URL $index: $url")
                }
                Log.d(TAG, "=====================================")
                currentImageUrls = urls
            }
        }

        lifecycleScope.launch {
            viewModel.isFavorite.collect { isFavorite ->
                Log.d(TAG, "Favorite status changed: $isFavorite")
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
        Log.d(TAG, "=== BINDING ITEM START ===")
        Log.d(TAG, "Item ID: ${item.id}")
        Log.d(TAG, "Item Name: ${item.name}")
        Log.d(TAG, "Item Price: ${item.price}")
        Log.d(TAG, "Item Status: ${item.status}")
        Log.d(TAG, "Item Quantity: ${item.quantity}")

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
            Log.d(TAG, "Description set: ${item.description.take(50)}...")
        } else {
            binding.productDescription.visibility = View.GONE
            Log.d(TAG, "No description")
        }

        if (item.status == "sold" || item.quantity <= 0) {
            binding.soldBadge.visibility = View.VISIBLE
            Log.d(TAG, "Sold badge visible")
        } else {
            binding.soldBadge.visibility = View.GONE
            Log.d(TAG, "Sold badge hidden")
        }

        setupImageSlider(item)
        Log.d(TAG, "=== BINDING ITEM END ===")
    }

    private fun setupImageSlider(item: Item) {
        Log.d(TAG, "=== SETUP IMAGE SLIDER START ===")

        val imageUrls = mutableListOf<String>()

        if (!item.coverImage.isNullOrBlank()) {
            imageUrls.add(item.coverImage)
            Log.d(TAG, "Added cover image: ${item.coverImage}")
        } else {
            Log.d(TAG, "No cover image")
        }

        Log.d(TAG, "Processing ${item.images.size} images from item.images")
        item.images
            .mapNotNull { it.url }
            .filter { it.startsWith("http") && !imageUrls.contains(it) }
            .forEach {
                imageUrls.add(it)
                Log.d(TAG, "Added image: $it")
            }

        Log.d(TAG, "Total image URLs collected: ${imageUrls.size}")
        imageUrls.forEachIndexed { index, url ->
            Log.d(TAG, "  Final URL $index: $url")
        }

        if (imageUrls.isEmpty()) {
            Log.w(TAG, "No images to display, hiding slider")
            binding.imageSlider.visibility = View.GONE
            return
        }

        binding.imageSlider.visibility = View.VISIBLE
        Log.d(TAG, "Image slider visible")

        Log.d(TAG, "Creating new ImageSliderAdapter with ${imageUrls.size} images")
        val newAdapter = ImageSliderAdapter(imageUrls)
        binding.imageSlider.adapter = newAdapter
        binding.imageSlider.setCurrentItem(0, false)

        binding.imageSlider.post {
            binding.imageSlider.setCurrentItem(0, false)
            Log.d(TAG, "Force refreshed ViewPager2 to position 0")
        }

        Log.d(TAG, "=== SETUP IMAGE SLIDER END ===")
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

    // REMOVED the empty toggleFavorite() method since we're using viewModel.toggleFavorite()

    private fun contactSeller() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item
            val sellerMobile = item.shop?.sellerMobile

            if (!sellerMobile.isNullOrBlank()) {
                Log.d(TAG, "Contacting seller via mobile: $sellerMobile")
                // Open WhatsApp or dialer
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://wa.me/${sellerMobile.replace(Regex("[^0-9]"), "")}")
                }
                startActivity(intent)
            } else {
                Log.w(TAG, "No seller mobile available")
                // Show dialog or fallback to chat
            }
        }
    }

    private fun onBuyClick() {
        Log.d(TAG, "onBuyClick called")
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item
            val sellerMobile = viewModel.sellerMobile.value  // Get from ViewModel

            if (sellerMobile.isNullOrBlank()) {
                Toast.makeText(requireContext(),
                    "Seller contact information not available",
                    Toast.LENGTH_SHORT).show()
                return
            }

            val bottomSheet = ContactOptionsBottomSheet(item, sellerMobile)  // Pass mobile number
            bottomSheet.show(parentFragmentManager, "contact_options")
        } else {
            Log.w(TAG, "Cannot buy - no item loaded")
        }
    }

    private fun navigateToDetail(itemId: String) {
        Log.d(TAG, "Navigating to item detail: $itemId")
        val bundle = bundleOf("itemId" to itemId)
        findNavController().navigate(R.id.itemDetailFragment, bundle)
        Log.d(TAG, "Navigation called")
    }

    private fun showLoading(show: Boolean) {
        Log.d(TAG, "showLoading: $show")
        binding.progressBar?.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView?.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Log.e(TAG, "showError: $message")
        // TODO: Show error dialog or snackbar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called - currentItemId: $currentItemId")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }
}