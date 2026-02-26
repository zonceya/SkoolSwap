package com.example.skoolswap.ui.shop

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentShopBinding
import com.example.skoolswap.databinding.ItemProductBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        findNavController().currentBackStackEntry?.savedStateHandle?.let { handle ->
            handle.getLiveData<Boolean>("item_updated").observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    handle.remove<Boolean>("item_updated")

                    lifecycleScope.launch {
                        Log.d("ShopFragment", "🔄 Item updated, waiting 500ms before refresh")
                        delay(500)
                        viewModel.loadMyShopItems()
                        Toast.makeText(requireContext(), "Item updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
        //viewModel.refresh()
        lifecycleScope.launch {
            delay(500) // 500ms delay
            viewModel.loadMyShopItems()
            Log.d("ShopFragment", "🔄 Refreshing shop items after delay")
        }
    }

    private fun setupRecyclerView() {
        // Initialize adapter with click listener
        productAdapter = ProductAdapter { itemId ->
            // Navigate to edit screen with the item ID using bundle
            val bundle = Bundle().apply {
                putString("itemId", itemId)
            }
            findNavController().navigate(R.id.editItemFragment, bundle)
        }

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
        // Only show samples if there are no real items
        if (productAdapter.itemCount == 0) {
            productAdapter.submitList(sampleItems)
        }
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
                    binding.storeStats.text = "0 items • 0 sold • 0 current"
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
            viewModel.shopItems.collectLatest { items ->
                if (items.isNotEmpty()) {
                    // Pass the domain items directly - they contain IDs!
                    productAdapter.submitList(items)  // Now passing List<Item>, not List<Product>
                } else {
                    // Show empty state or keep sample data
                    if (viewModel.isLoadingItems.value) {
                        // Show loading
                    } else {
                        // Maybe show empty state or keep samples for now
                        showSampleProducts()
                    }
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
            val displayName = shop.displayName.ifEmpty {
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


