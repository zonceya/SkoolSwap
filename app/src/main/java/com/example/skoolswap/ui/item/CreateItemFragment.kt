package com.example.skoolswap.ui.item

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.ItemConstants
import com.example.skoolswap.data.remote.models.response.item.ItemTypeDto
import com.example.skoolswap.databinding.FragmentCreateItemBinding
import com.example.skoolswap.domain.repository.ItemTypeRepositoryInterface
import com.example.skoolswap.ui.profile.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateItemFragment : Fragment() {

    private var _binding: FragmentCreateItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateItemViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    // Store selected values
    private var selectedCategoryId: Int = 2 // Default: Men
    private var selectedSubcategoryId: Int = 201 // Default: Tops for men
    private var selectedConditionId: Int = 2 // Default: Used - Like New
    private var selectedSizeId: Int = 3 // Default: M
    private var selectedBrandId: Int = 1 // Default: Brand A
    private var selectedColor: String? = null
    private var selectedMaterialId: Int = 1 // Default: Cotton
    private var selectedProvinceId: Int = 6 // Default: Gauteng
    private var selectedLocationId: Int = 101 // Default: Johannesburg
    private var selectedGenderId: Int = 1 // Default: Men
    private var selectedSchoolId: Int = 1 // Default: University of Cape Town
    private var selectedQuantity: Int = 1
    private var selectedItemTypeId: Int = 9 // Default: Tennis Shoes (ID 9)

    // For camera photo capture
    private var currentPhotoUri: Uri? = null
    private var cameraPhotoFile: File? = null
    @Inject
    lateinit var itemTypeRepository: ItemTypeRepositoryInterface
    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        lifecycleScope.launch {
            if (isSuccess && currentPhotoUri != null) {
                // Add the captured photo
                if (viewModel.images.value.size < 3) {
                    viewModel.addImage(currentPhotoUri!!)
                    showToast("Photo added")
                } else {
                    showToast("Maximum 3 images allowed")
                }
            } else {
                showToast("Failed to capture image")
            }

            // Clear the URI to prevent permission errors
            currentPhotoUri = null
            cameraPhotoFile?.delete()
            cameraPhotoFile = null
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (uris.isNullOrEmpty()) {
            return@registerForActivityResult
        }

        lifecycleScope.launch {
            var imagesAdded = 0
            uris.forEach { uri ->
                if (viewModel.images.value.size < 3) {
                    viewModel.addImage(uri)
                    imagesAdded++
                }
            }

            if (imagesAdded > 0) {
                showToast("$imagesAdded image(s) added")
            }
        }
    }
    private fun loadItemTypes() {
        Log.d(TAG, "loadItemTypes() called")

        lifecycleScope.launch {
            try {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    Log.d(TAG, "Collecting item types from repository...")

                    // Collect from repository Flow
                    itemTypeRepository.getItemTypes().collect { types ->
                        Log.d(TAG, "Repository returned ${types.size} item types")

                        if (!isAdded || _binding == null) {
                            Log.w(TAG, "Fragment not attached, skipping UI update")
                            return@collect
                        }

                        try {
                            if (types.isNotEmpty()) {
                                // Convert to DTOs for the spinner
                                val dtoTypes = types.map { itemType ->
                                    ItemTypeDto(
                                        id = itemType.id,
                                        name = itemType.name,
                                        groupId = itemType.groupId,
                                        description = itemType.description
                                    )
                                }
                                Log.d(TAG, "Updating spinner with ${dtoTypes.size} types from DB")
                                updateItemTypeSpinner(dtoTypes)
                            } else {
                                // Database is empty, fetch from API
                                Log.d(TAG, "DB empty, fetching from API...")
                                refreshItemTypesFromApi()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing item types: ${e.message}", e)
                            showConstantsInSpinner()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadItemTypes coroutine: ${e.message}", e)
                showConstantsInSpinner()
            }
        }
    }

    private suspend fun refreshItemTypesFromApi() {
        Log.d(TAG, "refreshItemTypesFromApi called")
        try {
            val result = itemTypeRepository.refreshItemTypes()
            result.onSuccess {
                Log.d(TAG, "Successfully refreshed item types from API")
                // The Flow will automatically emit new data
            }.onFailure { error ->
                Log.e(TAG, "Failed to refresh item types: ${error.message}", error)
                // Fallback to constants
                withContext(Dispatchers.Main) {
                    showConstantsInSpinner()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in refreshItemTypesFromApi: ${e.message}", e)
            withContext(Dispatchers.Main) {
                showConstantsInSpinner()
            }
        }
    }

    // In CreateItemFragment.kt - RE-ENABLE API CALLS
 /*   private suspend fun refreshItemTypesFromApi() {
        Log.d(TAG, "refreshItemTypesFromApi called")
        try {
            val result = itemTypeRepository.refreshItemTypes()
            result.onSuccess {
                Log.d(TAG, "Successfully refreshed item types from API")
                // The Flow will automatically emit the new data when DB is updated
            }.onFailure { error ->
                Log.e(TAG, "Failed to refresh item types: ${error.message}", error)
                // Fallback to constants
                showConstantsInSpinner()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in refreshItemTypesFromApi: ${e.message}", e)
            showConstantsInSpinner()
        }
    }*/
    private fun showConstantsInSpinner() {
        try {
            Log.d(TAG, "showConstantsInSpinner() called")
            val constantTypes = ItemConstants.getRailsItemTypes()
            val convertedTypes = constantTypes.map { constant ->
                ItemTypeDto(
                    id = constant.id,
                    name = constant.displayName,
                    groupId = 0,
                    description = null
                )
            }
            updateItemTypeSpinner(convertedTypes)
        } catch (e: Exception) {
            Log.e(TAG, "Error in showConstantsInSpinner: ${e.message}", e)
        }
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            showToast("Camera permission is required to take photos")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupObservers()
        setupClickListeners()
        Log.d(TAG, "onViewCreated: Testing basic functionality")

        // Test 1: Basic view access
        try {
            binding.itemName.setText("Test Item")
            Log.d(TAG, "Test 1: View access - PASSED")
        } catch (e: Exception) {
            Log.e(TAG, "Test 1: View access - FAILED: ${e.message}")
        }

        // Test 2: Repository injection
        try {
            Log.d(TAG, "Test 2: Repository injected: ${itemTypeRepository != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Test 2: Repository injection - FAILED: ${e.message}")
        }

        // Test 3: Setup spinners (no network)
        try {
            setupSpinners()
            Log.d(TAG, "Test 3: Setup spinners - PASSED")
        } catch (e: Exception) {
            Log.e(TAG, "Test 3: Setup spinners - FAILED: ${e.message}")
        }

        // Only then try to load item types
        binding.root.post {
            loadItemTypes()
        }
    }

    private fun setupSpinners() {
        // Main Category Spinner
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ItemConstants.MAIN_CATEGORIES.map { it.displayName }
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mainCategorySpinner.adapter = categoryAdapter

        binding.mainCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = ItemConstants.MAIN_CATEGORIES[position]
                selectedCategoryId = selectedCategory.id
                updateSubcategorySpinner(selectedCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Item Type Spinner (NEW - for Rails item_types)
        val railsItemTypes = ItemConstants.getRailsItemTypes()
        val itemTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            railsItemTypes.map { it.displayName }
        )
        binding.itemTypeSpinner.adapter = itemTypeAdapter

        binding.itemTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedItemTypeId = railsItemTypes[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set default selection to Tennis Shoes (position 8 = ID 9)
        binding.itemTypeSpinner.setSelection(8)

        // Condition Spinner
        setupSimpleSpinner(
            spinner = binding.conditionSpinner,
            items = ItemConstants.CONDITIONS,
            onSelected = { selectedConditionId = it.id }
        )

        // Size Spinner
        setupSimpleSpinner(
            spinner = binding.sizeSpinner,
            items = ItemConstants.SIZES,
            onSelected = { selectedSizeId = it.id }
        )

        // Brand Spinner
        setupSimpleSpinner(
            spinner = binding.brandSpinner,
            items = ItemConstants.BRANDS,
            onSelected = { selectedBrandId = it.id }
        )

        // Color Spinner
        setupSimpleSpinner(
            spinner = binding.colourSpinner,
            items = ItemConstants.COLORS,
            onSelected = { selectedColor = it.value }
        )

        // Province Spinner
        binding.provinceSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ItemConstants.PROVINCES.map { it.displayName }
        )
        binding.provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedProvinceId = ItemConstants.PROVINCES[position].id
                updateLocationSpinner(selectedProvinceId)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // School Spinner
        setupSimpleSpinner(
            spinner = binding.schoolSpinner,
            items = ItemConstants.SCHOOLS,
            onSelected = { selectedSchoolId = it.id }
        )

        // Gender Spinner
        setupSimpleSpinner(
            spinner = binding.genderSpinner,
            items = ItemConstants.GENDERS,
            onSelected = { selectedGenderId = it.id }
        )

        // Material Spinner
        setupSimpleSpinner(
            spinner = binding.materialSpinner,
            items = ItemConstants.MATERIALS,
            onSelected = { selectedMaterialId = it.id }
        )

        // Quantity Spinner
        val quantities = listOf("1", "2", "3", "4", "5")
        binding.quantitySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            quantities
        )
        binding.quantitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedQuantity = (position + 1)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun <T> setupSimpleSpinner(
        spinner: Spinner,
        items: List<T>,
        onSelected: (T) -> Unit
    ) {
        val displayNames = when (items.firstOrNull()) {
            is ItemConstants.Condition -> items.map { (it as ItemConstants.Condition).displayName }
            is ItemConstants.Size -> items.map { (it as ItemConstants.Size).displayName }
            is ItemConstants.Brand -> items.map { (it as ItemConstants.Brand).displayName }
            is ItemConstants.Color -> items.map { (it as ItemConstants.Color).displayName }
            is ItemConstants.Gender -> items.map { (it as ItemConstants.Gender).displayName }
            is ItemConstants.Material -> items.map { (it as ItemConstants.Material).displayName }
            is ItemConstants.School -> items.map { (it as ItemConstants.School).displayName }
            else -> items.map { it.toString() }
        }

        // FIX: Use proper layout
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,  // Your custom layout
            displayNames
        )

        // Set dropdown view for when spinner is clicked
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                onSelected(items[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    private fun updateItemTypeSpinner(types: List<ItemTypeDto>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            types.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.itemTypeSpinner.adapter = adapter
    }
    private fun updateSubcategorySpinner(category: ItemConstants.Category) {
        try {
            val subcategoryNames = category.subcategories.map { it.displayName }

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item,
                subcategoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.subcategorySpinner.adapter = adapter

            binding.subcategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (category.subcategories.isNotEmpty()) {
                        selectedSubcategoryId = category.subcategories[position].id
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            if (subcategoryNames.isNotEmpty()) {
                binding.subcategorySpinner.setSelection(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating subcategory spinner: ${e.message}", e)
        }
    }
    private fun updateLocationSpinner(provinceId: Int) {
        val locations = ItemConstants.getLocationsByProvince(provinceId)
        val locationNames = locations.map { it.displayName }
        val locationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            locationNames
        )
        binding.locationSpinner.adapter = locationAdapter

        binding.locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (locations.isNotEmpty()) {
                    selectedLocationId = locations[position].id
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (locationNames.isNotEmpty()) {
            binding.locationSpinner.setSelection(0)
        }
    }

    private fun setupObservers() {
        // Collect images StateFlow
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { uris ->
                    updateImagePreview(uris)
                }
            }
        }

        // Collect uiState StateFlow
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "UI State changed: $state")

                    when (state) {
                        is CreateItemUiState.Loading -> showLoading()

                        is CreateItemUiState.Success -> {
                            hideLoading()
                            val message = state.message ?: "Item created successfully"
                            showSuccess(message)
                            navigateAfterSuccess()
                        }

                        is CreateItemUiState.Error -> {
                            hideLoading()
                            val errorMessage = state.message ?: "An unknown error occurred"
                            Log.e(TAG, "Error state: $errorMessage")
                            showError(errorMessage)
                        }

                        CreateItemUiState.Idle -> hideLoading()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.coverPhoto.setOnClickListener {
            pickImagesIfAllowed()
        }

        binding.differentAngle.setOnClickListener {
            pickImagesIfAllowed()
        }

        binding.labelPhoto.setOnClickListener {
            pickImagesIfAllowed()
        }

        binding.submitButton.setOnClickListener {
            if (validateForm()) {
                createItem()
            }
        }
    }

    private fun pickImagesIfAllowed() {
        if (viewModel.images.value.size >= 3) {
            showToast("Maximum 3 images allowed")
            return
        }

        showImageSourceOptions()
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission() // Camera
                    1 -> launchGallery() // Gallery
                    // 2 is Cancel
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionExplanation()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionExplanation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera Permission Needed")
            .setMessage("SkoolSwap needs camera permission to let you take photos of items you want to list.")
            .setPositiveButton("Allow") { _, _ ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchCamera() {
        try {
            // Create a temporary file to store the photo
            cameraPhotoFile = createImageFile()
            if (cameraPhotoFile != null) {
                // Get the URI using FileProvider for security
                currentPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    cameraPhotoFile!!
                )

                // Launch camera with the URI
                cameraLauncher.launch(currentPhotoUri!!)
            } else {
                showToast("Failed to create image file")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Camera is not available")
        }
    }

    private fun createImageFile(): File? {
        return try {
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"

            // Get the storage directory
            val storageDir = requireContext().getExternalFilesDir("SkoolSwap_Images")

            // Create the directory if it doesn't exist
            storageDir?.mkdirs()

            // Create the file
            File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",          /* suffix */
                storageDir       /* directory */
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun launchGallery() {
        // Try to launch gallery
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            e.printStackTrace()
            handleGalleryFailure()
        }
    }

    private fun handleGalleryFailure() {
        AlertDialog.Builder(requireContext())
            .setTitle("Gallery Access Issue")
            .setMessage(
                "There's a temporary issue accessing your gallery (Android system bug).\n\n" +
                        "You can:\n" +
                        "• Use 'Take Photo' to capture new images (recommended)\n" +
                        "• Restart your device and try again\n" +
                        "• Clear data for Google Photos app in Settings"
            )
            .setPositiveButton("Take Photo") { _, _ ->
                checkCameraPermission()
            }
            .setNeutralButton("Try Gallery Again") { _, _ ->
                launchGallery()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createItem() {
        val name = binding.itemName.text.toString()
        val description = binding.description.text.toString()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        viewModel.createItem(
            context = requireContext(),
            name = name,
            description = description,
            price = price,
            quantity = selectedQuantity,
            itemTypeId = selectedItemTypeId,  // Use the selected item type ID (1-9)
            brandId = selectedBrandId,
            sizeId = selectedSizeId,
            schoolId = selectedSchoolId,
            conditionId = selectedConditionId,
            locationId = selectedLocationId,
            provinceId = selectedProvinceId,
            genderId = selectedGenderId,
            color = selectedColor,
            sizeMeta = ItemConstants.SIZES.find { it.id == selectedSizeId }?.displayName
        )
    }

    private fun updateImagePreview(uris: List<Uri>) {
        // Update image count
        binding.imagesSubheading.text = "${uris.size}/3 images selected"

        // Clear all images first
        binding.coverPhoto.setImageResource(R.drawable.ic_create)
        binding.differentAngle.setImageResource(R.drawable.ic_create)
        binding.labelPhoto.setImageResource(R.drawable.ic_create)

        // Load images into ImageViews using Glide
        lifecycleScope.launch {
            uris.forEachIndexed { index, uri ->
                when (index) {
                    0 -> loadImageWithGlide(uri, binding.coverPhoto)
                    1 -> loadImageWithGlide(uri, binding.differentAngle)
                    2 -> loadImageWithGlide(uri, binding.labelPhoto)
                }
            }
        }
    }

    private fun loadImageWithGlide(uri: Uri, imageView: android.widget.ImageView) {
        try {
            Glide.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.ic_create)
                .error(R.drawable.ic_create)
                .centerCrop()
                .into(imageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun validateForm(): Boolean {
        if (binding.itemName.text.isNullOrEmpty()) {
            showToast("Please enter item name")
            return false
        }

        if (binding.description.text.isNullOrEmpty()) {
            showToast("Please enter description")
            return false
        }

        if (binding.price.text.isNullOrEmpty()) {
            showToast("Please enter price")
            return false
        }

        val price = binding.price.text.toString().toDoubleOrNull()
        if (price == null || price <= 0) {
            showToast("Please enter a valid price")
            return false
        }

        return true
    }

    private fun showLoading() {
        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Creating..."
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.submitButton.isEnabled = true
        binding.submitButton.text = "Create Item"
        binding.progressBar.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        clearForm()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        binding.itemName.text?.clear()
        binding.description.text?.clear()
        binding.price.text?.clear()
        viewModel.clearImages()

        // Clear image previews
        binding.coverPhoto.setImageResource(R.drawable.ic_create)
        binding.differentAngle.setImageResource(R.drawable.ic_create)
        binding.labelPhoto.setImageResource(R.drawable.ic_create)

        updateImagePreview(emptyList())

        // Reset spinners to defaults
        binding.mainCategorySpinner.setSelection(1) // Men
        binding.itemTypeSpinner.setSelection(8) // Tennis Shoes (ID 9)
        binding.conditionSpinner.setSelection(1) // Used - Like New
        binding.sizeSpinner.setSelection(2) // M
        binding.brandSpinner.setSelection(0) // Brand A
        binding.colourSpinner.setSelection(0) // Red
        binding.provinceSpinner.setSelection(5) // Gauteng
        binding.materialSpinner.setSelection(0) // Cotton
        binding.quantitySpinner.setSelection(0) // 1
        binding.genderSpinner.setSelection(0) // Men
        binding.schoolSpinner.setSelection(0) // UCT
    }

    private fun navigateAfterSuccess() {
        lifecycleScope.launch {
            delay(2000)

            try {
                val actionId = R.id.action_createItemFragment_to_mainFragment
                findNavController().navigate(actionId)
            } catch (e: Exception) {
                try {
                    findNavController().navigate(R.id.nav_home)
                } catch (e2: Exception) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearImages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Clear any pending camera URIs
        currentPhotoUri = null
        cameraPhotoFile = null
    }

    override fun onPause() {
        super.onPause()
        // Clean up temporary files
        cameraPhotoFile?.delete()
        cameraPhotoFile = null
    }
}