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

@AndroidEntryPoint
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemDetailViewModel by viewModels()

    private lateinit var similarItemsAdapter: SimilarItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getString("itemId")
        val source = arguments?.getString("source") ?: "unknown"

        if (itemId == null) {
            findNavController().popBackStack()
            return
        }

        setupViews()
        hideFab()
        setupListeners()
        observeViewModel()

        viewModel.loadItem(itemId, source)
    }
    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
    }
    private fun setupViews() {
        similarItemsAdapter = SimilarItemsAdapter { item ->
            navigateToDetail(item.id)
        }
        binding.similarRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = similarItemsAdapter
        }
        binding.imageSlider.apply {
            offscreenPageLimit = 2          // Keep 2 pages in memory
            setCurrentItem(0, false)        // Force start at position 0
        }
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.shareBtn.setOnClickListener {
            shareItem()
        }

        binding.favBtn.setOnClickListener {
            toggleFavorite()
        }

        binding.contactSeller.setOnClickListener {
            contactSeller()
        }

        binding.buyButton.setOnClickListener {
            onBuyClick()
        }

        binding.shippingHeader.setOnClickListener {
            toggleShippingSection()
        }
    }
    private fun toggleShippingSection() {
        val isVisible = binding.shippingExpandableContent.visibility == View.VISIBLE

        if (isVisible) {
            binding.shippingExpandableContent.visibility = View.GONE
            binding.shippingToggle.text = "+"
        } else {
            binding.shippingExpandableContent.visibility = View.VISIBLE
            binding.shippingToggle.text = "-"
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.itemState.collect { state ->
                Timber.tag("ItemDetail").d("State received: $state")
                when (state) {
                    is ItemDetailViewModel.ItemDetailState.Loading -> {
                        android.util.Log.d("ItemDetail", "Loading...")
                        showLoading(true)
                    }
                    is ItemDetailViewModel.ItemDetailState.Success -> {
                        android.util.Log.d("ItemDetail", "Success! Item: ${state.item.name}")
                        showLoading(false)
                        bindItem(state.item)
                    }
                    is ItemDetailViewModel.ItemDetailState.Error -> {
                        android.util.Log.d("ItemDetail", "Error: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.similarItems.collect { items ->
                similarItemsAdapter.submitList(items)
            }
        }

        lifecycleScope.launch {
            viewModel.sizeName.collect { sizeName ->
                if (!sizeName.isNullOrEmpty()) {
                    // Convert "Adult 8" to "UK 8"
                    val displaySize = sizeName.replace("Adult", "UK")
                    binding.productSize.text = "$displaySize"
                    binding.productSize.visibility = View.VISIBLE
                } else {
                    binding.productSize.visibility = View.GONE
                }
            }
        }

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
        lifecycleScope.launch {
            viewModel.colorName.collect { colorName ->
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
                if (!brandName.isNullOrEmpty()) {
                    binding.productBrand.text = "Brand: $brandName"
                    binding.productBrand.visibility = View.VISIBLE
                } else {
                    binding.productBrand.visibility = View.GONE
                }
            }
        }
    }

    private fun bindItem(item: com.example.skoolswap.domain.model.Item) {
        binding.productTitle.text = item.name
        android.util.Log.d("ItemDetail", "=== BINDING ITEM ===")
        android.util.Log.d("ItemDetail", "Name: ${item.name}")
        android.util.Log.d("ItemDetail", "Price: ${item.price}")
        binding.productPrice.text = "R${String.format("%.2f", item.price)}"

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
    private fun setupImageSlider(item: com.example.skoolswap.domain.model.Item) {
        val imageUrls = mutableListOf<String>()

        // Cover image first
        if (!item.coverImage.isNullOrBlank()) {
            imageUrls.add(item.coverImage)
        }

        // Add all images from the list
        item.images.forEach { imageItem ->
            if (!imageItem.url.isNullOrBlank() && imageItem.url.startsWith("http")) {
                if (!imageUrls.contains(imageItem.url)) {
                    imageUrls.add(imageItem.url)
                }
            }
        }

        Timber.tag("ItemDetail").d("Final image list size: ${imageUrls.size}")
        imageUrls.forEachIndexed { index, url ->
            Timber.tag("ItemDetail").d("Image $index: $url")
        }

        if (imageUrls.isEmpty()) {
            binding.imageSlider.visibility = View.GONE
            return
        }

        binding.imageSlider.visibility = View.VISIBLE
        binding.imageSlider.adapter = ImageSliderAdapter(imageUrls)
        binding.imageSlider.setCurrentItem(0, false)   // Start at first image
    }
    private fun shareItem() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            val item = currentState.item
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, "${item.name}\nR${item.price}\nCheck it out!")
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share item"))
        }
    }

    private fun toggleFavorite() {
        // TODO: Implement favorite functionality
    }

    private fun contactSeller() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            // TODO: Navigate to chat with seller
        }
    }

    private fun onBuyClick() {
        val currentState = viewModel.itemState.value
        if (currentState is ItemDetailViewModel.ItemDetailState.Success) {
            // TODO: Handle buy action
        }
    }

    private fun navigateToDetail(itemId: String) {
        val bundle = bundleOf("itemId" to itemId)
        findNavController().navigate(R.id.itemDetailFragment, bundle)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar?.visibility = if (show) View.VISIBLE else View.GONE
        // Fix: Use the correct ID - your ScrollView needs android:id="@+id/scrollView"
        binding.scrollView?.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        // TODO: Show error dialog or snackbar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        _binding = null
    }
}