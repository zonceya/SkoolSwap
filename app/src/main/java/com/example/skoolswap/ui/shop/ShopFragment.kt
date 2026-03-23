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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentShopBinding
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

    private var isEditing = false
    private var hasLoadedRealItems = false
    private var hasAttemptedLoad = false  // Track if we've tried loading

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

        binding.storeRating.rating = 2.5f

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Load data
        viewModel.loadMyShop(showLoading = true)
        viewModel.loadMyShopItems()

        // Show any cached data immediately
        viewModel.currentShop.value?.let { shop ->
            updateShopUI(shop)
        }

        // Show fallback samples immediately while loading
        showFallbackSamples()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(500)
            viewModel.loadMyShopItems()
            Log.d("ShopFragment", "🔄 Refreshing shop items after delay")
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { itemId ->
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
    }

    // ✅ NEW: Show fallback samples immediately for better UX
    private fun showFallbackSamples() {
        if (!hasLoadedRealItems && !hasAttemptedLoad) {
            Log.d("ShopFragment", "📱 Showing fallback sample items while loading")
            productAdapter.submitList(sampleItems)
        }
    }

    // ✅ NEW: Show error state with samples
    private fun showErrorStateWithSamples(errorMessage: String) {
        Log.d("ShopFragment", "⚠️ Error loading items: $errorMessage - showing samples")
        productAdapter.submitList(sampleItems)
        binding.emptyStateText?.visibility = View.GONE  // Hide empty state, show samples instead
        Snackbar.make(binding.root, "Using sample items: $errorMessage", Snackbar.LENGTH_SHORT).show()
    }

    // ✅ NEW: Show empty state (no items and no error)
    private fun showEmptyState() {
        Log.d("ShopFragment", "📭 No items found, showing empty state")
        productAdapter.submitList(emptyList())
        binding.emptyStateText?.visibility = View.VISIBLE
        binding.emptyStateText?.text = "No items yet. Tap + to add your first item!"
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.currentShop.collectLatest { shop ->
                if (!isEditing && shop != null) {
                    updateShopUI(shop)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading && viewModel.currentShop.value == null) {
                    binding.storeName.text = "Loading..."
                    binding.storeStats.text = "Loading shop..."
                } else if (!isLoading && viewModel.currentShop.value == null) {
                    binding.storeName.text = "My Shop"
                    binding.storeStats.text = "0 items • 0 sold • 0 current"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { error ->
                error?.let {
                    // If there's an error loading items, show samples
                    if (it.contains("items", ignoreCase = true)) {
                        showErrorStateWithSamples(it)
                    } else {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    }
                    viewModel.clearError()
                }
            }
        }

        // ✅ UPDATED: Handle shop items with fallback logic
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.shopItems.collectLatest { items ->
                hasAttemptedLoad = true
                Log.d("ShopFragment", "📦 Received ${items.size} items from ViewModel")

                if (items.isNotEmpty()) {
                    // We have real items!
                    hasLoadedRealItems = true
                    productAdapter.submitList(items)
                    binding.emptyStateText?.visibility = View.GONE
                    Log.d("ShopFragment", "✅ Displaying ${items.size} real items")
                } else {
                    // No real items
                    if (viewModel.isLoadingItems.value) {
                        // Still loading, keep showing samples
                        Log.d("ShopFragment", "⏳ Still loading, keeping samples visible")
                    } else {
                        // Loading complete but no items - show empty state
                        Log.d("ShopFragment", "⚠️ No items found after load")
                        showEmptyState()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
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
        binding.storeName.setOnClickListener {
            startEditingShopName()
        }

        binding.storeNameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveShopNameIfChanged()
                true
            } else {
                false
            }
        }

        binding.storeNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.storeNameEditText.isVisible) {
                saveShopNameIfChanged()
            }
        }

        binding.storeProfileImage.setOnClickListener {
            Toast.makeText(requireContext(), "Shop profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startEditingShopName() {
        isEditing = true

        binding.storeName.isVisible = false
        binding.storeNameEditText.isVisible = true

        val currentName = viewModel.currentShop.value?.let { shop ->
            if (shop.displayName.isNotEmpty()) shop.displayName else shop.name
        } ?: ""

        binding.storeNameEditText.setText(currentName)
        binding.storeNameEditText.requestFocus()
        binding.storeNameEditText.selectAll()

        showKeyboard()
    }

    private fun saveShopNameIfChanged() {
        val newName = binding.storeNameEditText.text.toString().trim()

        if (newName.isEmpty()) {
            exitEditMode()
            return
        }

        val currentDisplayName = viewModel.currentShop.value?.displayName ?: ""
        val currentShopName = viewModel.currentShop.value?.name ?: ""
        val currentNameDisplayed = if (currentDisplayName.isNotEmpty()) currentDisplayName else currentShopName

        if (newName != currentNameDisplayed) {
            binding.storeName.text = newName
            updateDisplayName(newName)
        }

        exitEditMode()
    }

    private fun exitEditMode() {
        isEditing = false
        binding.storeName.isVisible = true
        binding.storeNameEditText.isVisible = false
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
            val displayName = shop.displayName.ifEmpty { shop.name }

            if (storeName.text.toString() != displayName) {
                storeName.text = displayName
            }

            if (!isEditing && storeNameEditText.text.toString() != displayName) {
                storeNameEditText.setText(displayName)
            }

            val statsText = "${shop.itemsCount} items • 0 sold • 0 followers"
            if (storeStats.text.toString() != statsText) {
                storeStats.text = statsText
            }

            // Load profile picture
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
            val result = viewModel.updateShopDisplayName(displayName)

            if (result.isSuccess) {
                binding.storeName.text = displayName
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to update"
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()

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