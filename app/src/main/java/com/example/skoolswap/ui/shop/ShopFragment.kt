package com.example.skoolswap.ui.shop

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentShopBinding
import com.example.skoolswap.databinding.ItemProductBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShopViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    // Track if we're currently editing to prevent observer updates during edit
    private var isEditing = false

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

        // Set initial rating
        binding.storeRating.rating = 2.5f

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Show any cached data immediately
        viewModel.currentShop.value?.let { shop ->
            updateShopUI(shop)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to fragment
        viewModel.refresh()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()

        binding.productRecyclerView.apply {
            adapter = productAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
            clipToPadding = false
            setPadding(0, 0, 0, 32.dpToPx())
        }

        showSampleProducts()
    }

    private fun showSampleProducts() {
        val products = listOf(
            Product(
                name = "Nike Air Max 270",
                price = "R1,899",
                imageUrl = "https://static.nike.com/a/images/t_PDP_1280_v1/f_auto,q_auto:eco/8416e8a6-3e6e-4b1e-9b5a-2b5f5b5b5b5b/air-max-270-mens-shoe.jpg"
            ),
            Product(
                name = "Adidas Ultraboost 22",
                price = "R2,299",
                imageUrl = "https://assets.adidas.com/images/w_600,f_auto,q_auto/6b5b5b5b5b5b5b5b5b5b5b5b/ultraboost-22-running-shoes.jpg"
            ),
            Product(
                name = "Apple iPhone 14 Pro",
                price = "R24,999",
                imageUrl = "https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-14-pro-finish-select-202209-6-7inch?wid=5120&hei=2880&fmt=webp&qlt=80&.v=1660753974991"
            ),
            Product(
                name = "Samsung Galaxy S23",
                price = "R18,999",
                imageUrl = "https://images.samsung.com/is/image/samsung/p6pim/za/2302/gallery/za-galaxy-s23-s911-sm-s911bzkdafa-thumb-534852526"
            ),
            Product(
                name = "Sony WH-1000XM5",
                price = "R5,999",
                imageUrl = "https://www.sony.co.za/image/5c9b5b5b5b5b5b5b5b5b5b5b?fmt=png-alpha&wid=1200"
            ),
            Product(
                name = "MacBook Pro 14\"",
                price = "R34,999",
                imageUrl = "https://www.apple.com/v/macbook-pro-14-and-16/b/images/overview/hero/hero_intro_endframe__e6khcva4hkeq_large.jpg"
            ),
            Product(
                name = "Nike Jordan 1 Retro",
                price = "R3,499",
                imageUrl = "https://static.nike.com/a/images/t_PDP_1280_v1/f_auto,q_auto:eco/5b5b5b5b5b5b5b5b5b5b5b5b/air-jordan-1-retro-high-og-shoes.jpg"
            ),
            Product(
                name = "PlayStation 5",
                price = "R12,999",
                imageUrl = "https://gmedia.playstation.com/is/image/SIEPDC/ps5-product-thumbnail-01-en-14sep21?$1600px$"
            ),
            Product(
                name = "Canon EOS R6",
                price = "R32,999",
                imageUrl = "https://www.canon.co.za/media/5b5b5b5b5b5b5b5b5b5b5b5b/eos-r6-front.png"
            ),
            Product(
                name = "Bose QuietComfort 45",
                price = "R4,799",
                imageUrl = "https://assets.bose.com/content/dam/cloudassets/Bose_DAM/Web/consumer_electronics/global/products/headphones/qc45/product_silo_images/qc45_black_EC_hero.psd/_jcr_content/renditions/cq5dam.web.1280.1280.png"
            ),
            Product(
                name = "iPad Pro 12.9\"",
                price = "R22,999",
                imageUrl = "https://www.apple.com/v/ipad-pro/al/images/overview/hero/hero__d5b5b5b5b5b5_large.jpg"
            ),
            Product(
                name = "Dyson V15 Detect",
                price = "R8,999",
                imageUrl = "https://www.dyson.com/dam/jcr:5b5b5b5b5b5b5b5b5b5b5b5b/v15-detect-hero.png"
            ),
            Product(
                name = "Nike Dunk Low",
                price = "R1,499",
                imageUrl = "https://static.nike.com/a/images/t_PDP_1280_v1/f_auto,q_auto:eco/5b5b5b5b5b5b5b5b5b5b5b5b/dunk-low-retro-mens-shoe.jpg"
            ),
            Product(
                name = "Xbox Series X",
                price = "R11,999",
                imageUrl = "https://img-prod-cms-rt-microsoft-com.akamaized.net/cms/api/am/imageFileData/RE4mRni?ver=8362"
            ),
            Product(
                name = "GoPro Hero 11",
                price = "R6,499",
                imageUrl = "https://gopro.com/content/dam/gopro/product-plus/hero11/update-2022/hero11-black-product-plus.png"
            ),
            Product(
                name = "AirPods Pro (2nd Gen)",
                price = "R4,299",
                imageUrl = "https://www.apple.com/v/airpods-pro/b/images/overview/hero/hero__d5b5b5b5b5b5_large.jpg"
            )
        )
        productAdapter.submitList(products)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.currentShop.collectLatest { shop ->
                // Don't update UI if we're currently editing (to prevent flicker)
                if (!isEditing && shop != null) {
                    updateShopUI(shop)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { isLoading ->
                //binding.progressBar.isVisible = isLoading

                // Only show loading text if there's no shop data yet
                if (isLoading && viewModel.currentShop.value == null) {
                    binding.storeName.text = "Loading..."
                    binding.storeStats.text = "Loading shop..."
                } else if (!isLoading && viewModel.currentShop.value == null) {
                    // If loading finished but no shop data, show default
                    binding.storeName.text = "My Shop"
                    binding.storeStats.text = "0 items • 0 sold • 0 followers"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    // Force immediate UI update after successful update
                    viewModel.currentShop.value?.let { shop ->
                        if (!isEditing) {
                            updateShopUI(shop)
                        }
                    }
                    Toast.makeText(requireContext(), "Display name updated!", Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // 1. Click shop name to start editing
        binding.storeName.setOnClickListener {
            startEditingShopName()
        }

        // 2. Save when "Done" is pressed on keyboard
        binding.storeNameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveShopNameIfChanged()
                true
            } else {
                false
            }
        }

        // 3. Save when field loses focus
        binding.storeNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.storeNameEditText.isVisible) {
                saveShopNameIfChanged()
            }
        }

        // 4. Profile image click
        binding.storeProfileImage.setOnClickListener {
            Toast.makeText(requireContext(), "Shop profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startEditingShopName() {
        isEditing = true

        // Switch to EditText
        binding.storeName.isVisible = false
        binding.storeNameEditText.isVisible = true

        // Set current name and prepare for editing
        val currentName = viewModel.currentShop.value?.let { shop ->
            if (shop.displayName.isNotEmpty()) shop.displayName else shop.name
        } ?: ""

        binding.storeNameEditText.setText(currentName)
        binding.storeNameEditText.requestFocus()
        binding.storeNameEditText.selectAll()

        // Show keyboard
        showKeyboard()
    }

    private fun saveShopNameIfChanged() {
        val newName = binding.storeNameEditText.text.toString().trim()

        if (newName.isEmpty()) {
            // If empty, just exit edit mode
            exitEditMode()
            return
        }

        // Get current display name from shop
        val currentDisplayName = viewModel.currentShop.value?.displayName ?: ""
        // Get current shop name
        val currentShopName = viewModel.currentShop.value?.name ?: ""
        // Determine what's currently displayed
        val currentNameDisplayed = if (currentDisplayName.isNotEmpty()) currentDisplayName else currentShopName

        // Update UI IMMEDIATELY before API call
        if (newName != currentNameDisplayed) {
            // Show the new name immediately
            binding.storeName.text = newName

            // Update the shop object locally
            viewModel.currentShop.value?.let { currentShop ->
                val updatedShop = currentShop.copy(displayName = newName)
                // Update the flow immediately (temporary until server confirms)
                // This is a hack - ideally your ViewModel should have a method for this
                // We'll handle this in the updateDisplayName function instead
            }

            // Then make the API call
            updateDisplayName(newName)
        }

        // Always exit edit mode
        exitEditMode()
    }

    private fun exitEditMode() {
        isEditing = false

        // Switch back to TextView
        binding.storeName.isVisible = true
        binding.storeNameEditText.isVisible = false

        // Hide keyboard
        hideKeyboard()
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.storeNameEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.storeNameEditText.windowToken, 0)
    }

    private fun updateShopUI(shop: com.example.skoolswap.domain.model.Shop) {
        binding.apply {
            // Shop name - show display name if available, otherwise default name
            val displayName = if (shop.displayName.isNotEmpty()) {
                shop.displayName
            } else {
                shop.name
            }

            // Only update if different to prevent unnecessary redraws
            if (storeName.text.toString() != displayName) {
                storeName.text = displayName
            }

            // Only update EditText if we're not currently editing
            if (!isEditing && storeNameEditText.text.toString() != displayName) {
                storeNameEditText.setText(displayName)
            }

            // Update stats with real item count from shop
            val statsText = "${shop.itemsCount} items • 0 sold • 0 followers"
            if (storeStats.text.toString() != statsText) {
                storeStats.text = statsText
            }

            // Load profile picture from user
            if (shop.profilePictureUrl.isNotEmpty() && shop.profilePictureUrl.isNotBlank()) {
                try {
                    Glide.with(requireContext())
                        .load(shop.profilePictureUrl)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .transform(CircleCrop())
                        .into(storeProfileImage)
                } catch (e: Exception) {
                    storeProfileImage.setImageResource(R.drawable.ic_user)
                }
            } else {
                storeProfileImage.setImageResource(R.drawable.ic_user)
            }
        }
    }

    private fun updateDisplayName(displayName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Show loading state
        //    binding.progressBar.isVisible = true

            val result = viewModel.updateShopDisplayName(displayName)

        //    binding.progressBar.isVisible = false

            if (result.isSuccess) {
                // Success - UI will update automatically via observers
                // Show immediate feedback
                binding.storeName.text = displayName

                // Toast will be shown via updateSuccess observer
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to update"
                Snackbar.make(
                    binding.root,
                    errorMessage,
                    Snackbar.LENGTH_LONG
                ).show()

                // Revert to original name if update failed
                viewModel.currentShop.value?.let { shop ->
                    val originalName = if (shop.displayName.isNotEmpty()) shop.displayName else shop.name
                    binding.storeName.text = originalName
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}

// Data class for products with online images
data class Product(
    val name: String,
    val price: String,
    val imageUrl: String
)

// Adapter for products with online images
class ProductAdapter : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<Product>()

    fun submitList(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                productName.text = product.name
                productPrice.text = product.price

                // Load image from URL using Glide
                Glide.with(itemView.context)
                    .load(product.imageUrl)
                    .thumbnail(0.25f)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(productImage)

                root.setOnClickListener {
                    Toast.makeText(
                        root.context,
                        "Clicked: ${product.name} - ${product.price}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}