package com.example.skoolswap.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentItemDetailBinding
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.ui.detail.adapter.ImageSliderAdapter
import com.example.skoolswap.ui.detail.adapter.SimilarItemsAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.core.view.isVisible
import com.example.skoolswap.domain.model.Item

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

        // Never call clearState() — let ViewModel handle it
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

        binding.favBtn.setOnClickListener {
            Log.d(TAG, "Favorite button clicked")
            toggleFavorite()
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
    }

    private fun bindItem(item: com.example.skoolswap.domain.model.Item) {
        Log.d(TAG, "=== BINDING ITEM START ===")
        Log.d(TAG, "Item ID: ${item.id}")
        Log.d(TAG, "Item Name: ${item.name}")
        Log.d(TAG, "Item Price: ${item.price}")
        Log.d(TAG, "Item Status: ${item.status}")
        Log.d(TAG, "Item Quantity: ${item.quantity}")

        binding.productTitle.text = item.name
        binding.productPrice.text = "R${String.format("%.2f", item.price)}"

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

        // Log current adapter state
        val existingAdapter = binding.imageSlider.adapter as? ImageSliderAdapter
        if (existingAdapter != null) {
            Log.d(TAG, "Existing adapter found with ${existingAdapter.imageUrls.size} images")
            Log.d(TAG, "Existing URLs: ${existingAdapter.imageUrls}")
            Log.d(TAG, "New URLs: $imageUrls")
            Log.d(TAG, "URLs equal? ${existingAdapter.imageUrls == imageUrls}")
        } else {
            Log.d(TAG, "No existing adapter found")
        }

        // ALWAYS create a new adapter - don't try to reuse
        Log.d(TAG, "Creating new ImageSliderAdapter with ${imageUrls.size} images")
        val newAdapter = ImageSliderAdapter(imageUrls)
        binding.imageSlider.adapter = newAdapter
        binding.imageSlider.setCurrentItem(0, false)

        // Force ViewPager2 to refresh
        binding.imageSlider.post {
            binding.imageSlider.setCurrentItem(0, false)
            Log.d(TAG, "Force refreshed ViewPager2 to position 0")
        }

        Log.d(TAG, "=== SETUP IMAGE SLIDER END ===")
    }

    private fun shareItem() {
        Log.d(TAG, "shareItem called")
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, "${item.name}\nR${item.price}\nCheck it out!")
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share item"))
            Log.d(TAG, "Share intent launched")
        } else {
            Log.w(TAG, "Cannot share - no item loaded")
        }
    }

    private fun toggleFavorite() {
        Log.d(TAG, "toggleFavorite called (TODO)")
        // TODO: Implement favorite functionality
    }

    private fun contactSeller() {
        Log.d(TAG, "contactSeller called")
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            Log.d(TAG, "Contact seller for item: ${currentState.item.name}")
            // TODO: Navigate to chat with seller
        } else {
            Log.w(TAG, "Cannot contact seller - no item loaded")
        }
    }

    private fun onBuyClick() {
        Log.d(TAG, "onBuyClick called")
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            Log.d(TAG, "Buy item: ${currentState.item.name}")
            // TODO: Handle buy action
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