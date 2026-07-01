package com.example.skoolswap.ui.item

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.example.skoolswap.databinding.FragmentCreateItemBinding
import com.example.skoolswap.domain.model.reference.*
import com.example.skoolswap.ui.component.ColorPickerBottomSheet
import com.example.skoolswap.ui.component.OptionsPickerBottomSheet
import com.example.skoolswap.utils.DialogAction
import com.example.skoolswap.utils.DialogHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateItemFragment : Fragment() {

    companion object {
        private const val TAG = "CreateItemFragment"
    }

    private var _binding: FragmentCreateItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateItemViewModel by viewModels()

    // Store selected values
    private var selectedMainCategoryId: Int? = null
    private var selectedSubCategoryId: Int? = null
    private var selectedConditionId: Int? = null
    private var selectedSizeId: Int? = null
    private var selectedBrandId: Int? = null
    private var selectedColorId: Int? = null
    private var selectedProvinceId: Int? = null
    private var selectedTownId: Int? = null
    private var selectedSchoolId: Int? = null
    private var selectedGenderId: Int? = null
    private var selectedQuantity: Int = 1

    // Flag to prevent multiple camera launches
    private var isCameraLaunched = false

    // Simple camera launcher
    private val simpleCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        Log.d(TAG, "📸 Camera callback received, bitmap = ${if (bitmap != null) "not null" else "null"}")
        isCameraLaunched = false

        if (bitmap != null) {
            Log.d(TAG, "Bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            lifecycleScope.launch {
                if (viewModel.images.value.size < 3) {
                    Log.d(TAG, "Attempting to save bitmap to file...")
                    val uri = saveBitmapToFile(bitmap)
                    if (uri != null) {
                        Log.d(TAG, "✅ Image saved successfully: $uri")
                        viewModel.addImage(uri)
                        Toast.makeText(requireContext(), "Photo added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "❌ Failed to save bitmap to file")
                        Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Max images reached (${viewModel.images.value.size}/3)")
                    Toast.makeText(requireContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e(TAG, "❌ Bitmap is null - camera may have been cancelled or failed")
            Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        Log.d(TAG, "📷 Gallery callback, uris = ${uris?.size ?: 0} images")
        if (uris.isNullOrEmpty()) {
            Log.w(TAG, "No images selected from gallery")
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            var imagesAdded = 0
            uris.forEach { uri ->
                if (viewModel.images.value.size < 3) {
                    Log.d(TAG, "Adding image from gallery: $uri")
                    viewModel.addImage(uri)
                    imagesAdded++
                }
            }
            if (imagesAdded > 0) {
                Log.d(TAG, "✅ Added $imagesAdded image(s) from gallery")
                Toast.makeText(requireContext(), "$imagesAdded image(s) added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Camera permission result: $isGranted")
        if (isGranted) {
            Log.d(TAG, "Camera permission granted, launching camera")
            launchCameraSafely()
        } else {
            Log.e(TAG, "Camera permission denied")
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentCreateItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        hideFab()
        setupObservers()
        setupClickListeners()
        setupDefaultSelections()
        restoreState(savedInstanceState)
        binding.deleteCover.visibility = View.GONE
        binding.deleteAngle2.visibility = View.GONE
        binding.deleteAngle3.visibility = View.GONE
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
        Log.d(TAG, "FAB hidden")
    }

    private fun setupObservers() {
        Log.d(TAG, "Setting up observers")

        // Collect Main Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainCategories.collect { categories ->
                    Log.d(TAG, "📦 MAIN CATEGORIES received: ${categories.size}")
                    showSubcategoryLoading(false)
                    setupMainCategoryPicker(categories)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    Log.d(TAG, "Loading state: $isLoading")
                    showSubcategoryLoading(isLoading)
                }
            }
        }

        // Collect Sub Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    Log.d(TAG, "📦 SUBCATEGORIES received: ${subCategories.size}")
                    setupSubCategoryPicker(subCategories)
                }
            }
        }

        // Collect Conditions
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditions.collect { conditions ->
                    Log.d(TAG, "Conditions received: ${conditions.size}")
                    setupConditionPicker(conditions)
                }
            }
        }

        // Collect Sizes
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sizes.collect { sizes ->
                    Log.d(TAG, "Sizes received: ${sizes.size}")
                    setupSizePicker(sizes)
                }
            }
        }

        // Collect Brands
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.brands.collect { brands ->
                    Log.d(TAG, "Brands received: ${brands.size}")
                    setupBrandPicker(brands)
                }
            }
        }

        // Collect Colors
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colors.collect { colors ->
                    Log.d(TAG, "Colors received: ${colors.size}")
                    setupColorPicker(colors)
                }
            }
        }

        // Collect Provinces
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.provinces.collect { provinces ->
                    Log.d(TAG, "Provinces received: ${provinces.size}")
                    setupProvincePicker(provinces)
                }
            }
        }

        // Collect Towns
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.towns.collect { towns ->
                    Log.d(TAG, "Towns received: ${towns.size}")
                    setupTownPicker(towns)
                }
            }
        }

        // Collect Images
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { uris ->
                    Log.d(TAG, "📸 Images updated: ${uris.size} images")
                    updateImagePreview(uris)
                }
            }
        }

        // Collect Schools
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.schools.collect { schools ->
                    Log.d(TAG, "Schools received: ${schools.size}")
                    setupSchoolPicker(schools)
                }
            }
        }

        // Collect Genders
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genders.collect { genders ->
                    Log.d(TAG, "Genders received: ${genders.size}")
                    setupGenderPicker(genders)
                }
            }
        }

        // Observe UI State for navigation
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Log.d(TAG, "UI State: $uiState")
                    when (uiState) {
                        is CreateItemUiState.Success -> {
                            Log.d(TAG, "✅ Success state: ${uiState.message}")
                            hideLoading()
                            showSuccess(uiState.message)
                            navigateAfterSuccess()
                        }
                        is CreateItemUiState.Error -> {
                            Log.e(TAG, "❌ Error state: ${uiState.message}")
                            hideLoading()
                            showError(uiState.message)
                        }
                        is CreateItemUiState.Loading -> {
                            Log.d(TAG, "Loading state")
                            showLoading()
                        }
                        is CreateItemUiState.Idle -> {
                            Log.d(TAG, "Idle state")
                            hideLoading()
                        }
                    }
                }
            }
        }
    }

    // ============ BOTTOM SHEET PICKER METHODS ============

    private fun setupMainCategoryPicker(categories: List<MainCategory>) {
        Log.d(TAG, "🎯 setupMainCategoryPicker called with ${categories.size} categories")

        // Log each category for debugging
        categories.forEachIndexed { index, category ->
            Log.d(TAG, "  Category $index: ${category.name} (ID: ${category.id})")
        }

        if (categories.isEmpty()) {
            Log.w(TAG, "Categories list is empty, hiding picker")
            binding.mainCategoryInput.visibility = View.GONE
            binding.mainCategoryLabel.visibility = View.GONE
            return
        }

        // Make sure views are visible
        binding.mainCategoryInput.visibility = View.VISIBLE
        binding.mainCategoryLabel.visibility = View.VISIBLE

        // Clear any existing click listeners to avoid duplicates
        binding.mainCategoryInput.setOnClickListener(null)

        // Restore selected category if exists
        selectedMainCategoryId?.let { id ->
            categories.find { it.id == id }?.let {
                binding.mainCategoryInput.setText(it.name)
                Log.d(TAG, "Restored selected category: ${it.name}")
            }
        }

        // Set click listener
        binding.mainCategoryInput.setOnClickListener {
            Log.d(TAG, "Main category input clicked, showing ${categories.size} options")

            OptionsPickerBottomSheet(
                title = "Select Category",
                options = categories.map { it.name }
            ) { selectedName, position ->
                val selectedCategory = categories[position]
                selectedMainCategoryId = selectedCategory.id
                binding.mainCategoryInput.setText(selectedName)
                Log.d(TAG, "✅ Selected category: ${selectedCategory.name} (ID: ${selectedCategory.id})")
                viewModel.onMainCategorySelected(selectedCategory.id)
            }.show(childFragmentManager, "category_picker")
        }

        // Force the input to be enabled and clickable
        binding.mainCategoryInput.isEnabled = true
        binding.mainCategoryInput.isClickable = true
        binding.mainCategoryInput.isFocusable = true

        // Set a hint if no text is set
        if (binding.mainCategoryInput.text.isNullOrEmpty()) {
            binding.mainCategoryInput.hint = "Select Category"
        }

        Log.d(TAG, "✅ Main category picker setup complete")
    }

    private fun setupSubCategoryPicker(subCategories: List<SubCategory>) {
        Log.d(TAG, "Setting up subcategory picker with ${subCategories.size} items")

        if (subCategories.isEmpty()) {
            Log.w(TAG, "Subcategories empty, disabling picker")
            binding.subCategoryInput.isEnabled = false
            return
        }

        binding.subCategoryInput.isEnabled = true

        // Restore selected subcategory if exists
        selectedSubCategoryId?.let { id ->
            subCategories.find { it.id == id }?.let {
                binding.subCategoryInput.setText(it.name)
                Log.d(TAG, "Restored subcategory: ${it.name}")
            }
        }

        binding.subCategoryInput.setOnClickListener {
            if (selectedMainCategoryId == null) {
                Log.w(TAG, "Subcategory clicked but no main category selected")
                Toast.makeText(requireContext(), "First select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            OptionsPickerBottomSheet(
                title = "Select Sub Category",
                options = subCategories.map { it.name }
            ) { selectedName, position ->
                val selected = subCategories[position]
                selectedSubCategoryId = selected.id
                binding.subCategoryInput.setText(selectedName)
                Log.d(TAG, "Selected subcategory: ${selected.name} (ID: ${selected.id})")
            }.show(childFragmentManager, "subcategory_picker")
        }
    }

    private fun setupConditionPicker(conditions: List<Condition>) {
        Log.d(TAG, "Setting up condition picker with ${conditions.size} items")

        // Restore selected condition if exists
        selectedConditionId?.let { id ->
            conditions.find { it.id == id }?.let {
                binding.conditionInput.setText(it.name)
                Log.d(TAG, "Restored condition: ${it.name}")
            }
        }

        binding.conditionInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Condition",
                options = conditions.map { it.name }
            ) { selectedName, position ->
                selectedConditionId = conditions[position].id
                binding.conditionInput.setText(selectedName)
                Log.d(TAG, "Selected condition: $selectedName (ID: $selectedConditionId)")
            }.show(childFragmentManager, "condition_picker")
        }
    }

    private fun setupSizePicker(sizes: List<Size>) {
        Log.d(TAG, "Setting up size picker with ${sizes.size} items")

        // Restore selected size if exists
        selectedSizeId?.let { id ->
            sizes.find { it.id == id }?.let {
                binding.sizeInput.setText(it.name)
                Log.d(TAG, "Restored size: ${it.name}")
            }
        }

        binding.sizeInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Size",
                options = sizes.map { it.name }
            ) { selectedName, position ->
                selectedSizeId = sizes[position].id
                binding.sizeInput.setText(selectedName)
                Log.d(TAG, "Selected size: $selectedName (ID: $selectedSizeId)")
            }.show(childFragmentManager, "size_picker")
        }
    }

    private fun setupBrandPicker(brands: List<Brand>) {
        Log.d(TAG, "Setting up brand picker with ${brands.size} items")

        // Restore selected brand if exists
        selectedBrandId?.let { id ->
            brands.find { it.id == id }?.let {
                binding.brandInput.setText(it.name)
                Log.d(TAG, "Restored brand: ${it.name}")
            }
        }

        binding.brandInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Brand",
                options = brands.map { it.name }
            ) { selectedName, position ->
                selectedBrandId = brands[position].id
                binding.brandInput.setText(selectedName)
                Log.d(TAG, "Selected brand: $selectedName (ID: $selectedBrandId)")
            }.show(childFragmentManager, "brand_picker")
        }
    }

    private fun setupColorPicker(colors: List<Color>) {
        Log.d(TAG, "Setting up color picker with ${colors.size} items")

        if (colors.isEmpty()) {
            Log.w(TAG, "Colors empty, disabling picker")
            binding.colorInput.isEnabled = false
            return
        }

        binding.colorInput.isEnabled = true

        // Restore selected color if exists
        selectedColorId?.let { id ->
            colors.find { it.id == id }?.let {
                binding.colorInput.setText(it.name)
                Log.d(TAG, "Restored color: ${it.name}")
            }
        }

        binding.colorInput.setOnClickListener {
            ColorPickerBottomSheet(colors) { selectedColor, position ->
                selectedColorId = selectedColor.id
                binding.colorInput.setText(selectedColor.name)
                Log.d(TAG, "Selected color: ${selectedColor.name} (ID: ${selectedColor.id})")
            }.show(childFragmentManager, "color_picker")
        }
    }

    private fun setupProvincePicker(provinces: List<Province>) {
        Log.d(TAG, "Setting up province picker with ${provinces.size} items")

        // Restore selected province if exists
        selectedProvinceId?.let { id ->
            provinces.find { it.id == id }?.let {
                binding.provinceInput.setText(it.name)
                Log.d(TAG, "Restored province: ${it.name}")
            }
        }

        binding.provinceInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Province",
                options = provinces.map { it.name }
            ) { selectedName, position ->
                val selectedProvince = provinces[position]
                selectedProvinceId = selectedProvince.id
                binding.provinceInput.setText(selectedName)
                Log.d(TAG, "Selected province: ${selectedProvince.name} (ID: ${selectedProvince.id})")
                viewModel.onProvinceSelected(selectedProvince.id)
            }.show(childFragmentManager, "province_picker")
        }
    }

    private fun setupTownPicker(towns: List<Town>) {
        Log.d(TAG, "Setting up town picker with ${towns.size} items")

        if (towns.isEmpty()) {
            Log.w(TAG, "Towns empty, hiding picker")
            binding.townInput.visibility = View.GONE
            binding.townLabel.visibility = View.GONE
            return
        }

        binding.townInput.visibility = View.VISIBLE
        binding.townLabel.visibility = View.VISIBLE

        // Restore selected town if exists
        selectedTownId?.let { id ->
            towns.find { it.id == id }?.let {
                binding.townInput.setText(it.name)
                Log.d(TAG, "Restored town: ${it.name}")
            }
        }

        binding.townInput.setOnClickListener {
            if (selectedProvinceId == null) {
                Log.w(TAG, "Town clicked but no province selected")
                Toast.makeText(requireContext(), "First select a province", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            OptionsPickerBottomSheet(
                title = "Select Town/City",
                options = towns.map { it.name }
            ) { selectedName, position ->
                selectedTownId = towns[position].id
                binding.townInput.setText(selectedName)
                Log.d(TAG, "Selected town: $selectedName (ID: $selectedTownId)")
            }.show(childFragmentManager, "town_picker")
        }
    }

    private fun setupGenderPicker(genders: List<Gender>) {
        Log.d(TAG, "Setting up gender picker with ${genders.size} items")

        // Restore selected gender if exists
        selectedGenderId?.let { id ->
            genders.find { it.id == id }?.let {
                binding.genderInput.setText(it.name)
                Log.d(TAG, "Restored gender: ${it.name}")
            }
        }

        binding.genderInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Gender",
                options = genders.map { it.name }
            ) { selectedName, position ->
                selectedGenderId = genders[position].id
                binding.genderInput.setText(selectedName)
                Log.d(TAG, "Selected gender: $selectedName (ID: $selectedGenderId)")
            }.show(childFragmentManager, "gender_picker")
        }
    }

    private fun setupSchoolPicker(schools: List<School>) {
        Log.d(TAG, "Setting up school picker with ${schools.size} items")

        // Restore selected school if exists
        selectedSchoolId?.let { id ->
            schools.find { it.id == id }?.let {
                binding.schoolInput.setText(it.name)
                Log.d(TAG, "Restored school: ${it.name}")
            }
        }

        binding.schoolInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select School",
                options = schools.map { it.name }
            ) { selectedName, position ->
                selectedSchoolId = schools[position].id
                binding.schoolInput.setText(selectedName)
                Log.d(TAG, "Selected school: $selectedName (ID: $selectedSchoolId)")
            }.show(childFragmentManager, "school_picker")
        }
    }

    private fun showSubcategoryLoading(show: Boolean) {
        if (show) {
            binding.subcategoryLoading.visibility = View.VISIBLE
            binding.subCategoryInput.visibility = View.GONE
        } else {
            binding.subcategoryLoading.visibility = View.GONE
            binding.subCategoryInput.visibility = View.VISIBLE
        }
    }

    private fun setupDefaultSelections() {
        Log.d(TAG, "Setting up default selections")

        // Quantity Spinner (keep as is)
        val quantities = listOf("1", "2", "3", "4", "5")
        val quantityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            quantities
        )
        quantityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.quantitySpinner.adapter = quantityAdapter

        // Restore selected quantity
        if (selectedQuantity > 0 && selectedQuantity <= 5) {
            binding.quantitySpinner.setSelection(selectedQuantity - 1)
        }

        binding.quantitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedQuantity = position + 1
                Log.d(TAG, "Quantity selected: $selectedQuantity")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun launchCameraSafely() {
        Log.d(TAG, "launchCameraSafely called, isCameraLaunched=$isCameraLaunched")

        if (!isCameraLaunched) {
            isCameraLaunched = true

            // Check if camera hardware exists
            val hasCamera = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            Log.d(TAG, "Device has camera: $hasCamera")

            if (!hasCamera) {
                isCameraLaunched = false
                Log.e(TAG, "No camera hardware on device")
                Toast.makeText(requireContext(), "Your device doesn't have a camera", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                Log.d(TAG, "Launching camera...")
                simpleCameraLauncher.launch(null)
                Log.d(TAG, "Camera launched successfully")
            } catch (e: Exception) {
                isCameraLaunched = false
                Log.e(TAG, "❌ Failed to launch camera", e)
                Toast.makeText(requireContext(), "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(TAG, "Camera already launching, skipping")
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")

        binding.coverPhoto.setOnClickListener {
            Log.d(TAG, "Cover photo clicked")
            pickImagesIfAllowed()
        }

        binding.differentAngle.setOnClickListener {
            Log.d(TAG, "Different angle clicked")
            pickImagesIfAllowed()
        }

        binding.labelPhoto.setOnClickListener {
            Log.d(TAG, "Label photo clicked")
            pickImagesIfAllowed()
        }

        binding.submitButton.setOnClickListener {
            Log.d(TAG, "Submit button clicked")
            if (validateForm()) {
                createItem()
            }
        }

        binding.deleteCover.setOnClickListener {
            Log.d(TAG, "Delete cover clicked")
            if (viewModel.images.value.isNotEmpty()) {
                viewModel.removeImageAt(0)
            }
        }

        binding.deleteAngle2.setOnClickListener {
            Log.d(TAG, "Delete angle 2 clicked")
            if (viewModel.images.value.size > 1) {
                viewModel.removeImageAt(1)
            }
        }

        binding.deleteAngle3.setOnClickListener {
            Log.d(TAG, "Delete angle 3 clicked")
            if (viewModel.images.value.size > 2) {
                viewModel.removeImageAt(2)
            }
        }
    }

    private fun pickImagesIfAllowed() {
        val currentCount = viewModel.images.value.size
        Log.d(TAG, "pickImagesIfAllowed called, current images: $currentCount/3")

        if (currentCount >= 3) {
            Log.w(TAG, "Maximum images reached")
            Toast.makeText(requireContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
            return
        }
        showImageSourceOptions()
    }

    private fun showImageSourceOptions() {
        Log.d(TAG, "Showing image source options dialog")

        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo")
            .setItems(options) { _, which ->
                Log.d(TAG, "Selected option: ${options[which]}")
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> launchGallery()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun checkCameraPermission() {
        Log.d(TAG, "Checking camera permission")

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Camera permission already granted")
                if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    launchCameraSafely()
                } else {
                    Log.e(TAG, "No camera hardware")
                    Toast.makeText(requireContext(), "Your device doesn't have a camera", Toast.LENGTH_SHORT).show()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.d(TAG, "Showing camera permission rationale")
                showCameraPermissionExplanation()
            }
            else -> {
                Log.d(TAG, "Requesting camera permission")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionExplanation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera Permission Needed")
            .setMessage("SkoolSwap needs camera permission to let you take photos of items you want to list.")
            .setPositiveButton("Allow") { _, _ ->
                Log.d(TAG, "User granted camera permission from dialog")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchGallery() {
        Log.d(TAG, "Launching gallery")
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch gallery", e)
            e.printStackTrace()
            handleGalleryFailure()
        }
    }

    private fun handleGalleryFailure() {
        AlertDialog.Builder(requireContext())
            .setTitle("Gallery Access Issue")
            .setMessage("There's a temporary issue accessing your gallery.\n\nYou can use 'Take Photo' to capture new images.")
            .setPositiveButton("Take Photo") { _, _ ->
                Log.d(TAG, "User chose to take photo instead")
                checkCameraPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBitmapToFile(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            Log.d(TAG, "saveBitmapToFile: Starting")
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "IMG_${timeStamp}.jpg"
            Log.d(TAG, "Filename: $filename")

            val cacheDir = requireContext().cacheDir
            Log.d(TAG, "Cache dir: ${cacheDir.absolutePath}")

            val file = File(cacheDir, filename)
            Log.d(TAG, "File path: ${file.absolutePath}")

            file.outputStream().use { out ->
                val success = bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                Log.d(TAG, "Bitmap compress success: $success")
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            Log.d(TAG, "✅ FileProvider URI: $uri")

            // Verify file exists
            if (file.exists()) {
                Log.d(TAG, "File size: ${file.length()} bytes")
            } else {
                Log.e(TAG, "File does not exist after writing!")
            }

            uri
        } catch (e: Exception) {
            Log.e(TAG, "❌ saveBitmapToFile failed", e)
            e.printStackTrace()
            null
        }
    }

    private fun createItem() {
        val name = binding.itemName.text.toString()
        val description = binding.description.text.toString()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        Log.d(TAG, "=== createItem START ===")
        Log.d(TAG, "Name: $name")
        Log.d(TAG, "Description: $description")
        Log.d(TAG, "Price: $price")
        Log.d(TAG, "Quantity: $selectedQuantity")
        Log.d(TAG, "MainCategoryId: $selectedMainCategoryId")
        Log.d(TAG, "SubCategoryId: $selectedSubCategoryId")
        Log.d(TAG, "BrandId: $selectedBrandId")
        Log.d(TAG, "SizeId: $selectedSizeId")
        Log.d(TAG, "SchoolId: $selectedSchoolId")
        Log.d(TAG, "ConditionId: $selectedConditionId")
        Log.d(TAG, "LocationId: $selectedTownId")
        Log.d(TAG, "ProvinceId: $selectedProvinceId")
        Log.d(TAG, "GenderId: $selectedGenderId")
        Log.d(TAG, "ColorId: $selectedColorId")

        if (selectedMainCategoryId == null || selectedSubCategoryId == null) {
            Log.e(TAG, "Category or subcategory not selected")
            Toast.makeText(requireContext(), "Please select category and subcategory", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val hasContactNumber = viewModel.hasContactNumber()

            if (hasContactNumber) {
                // User HAS contact number - create item directly
                Log.d(TAG, "User has contact number, creating item...")
                viewModel.createItemOfflineFirst(
                    context = requireContext(),
                    name = name,
                    description = description,
                    price = price,
                    quantity = selectedQuantity,
                    mainCategoryId = selectedMainCategoryId!!,
                    subCategoryId = selectedSubCategoryId!!,
                    brandId = selectedBrandId,
                    sizeId = selectedSizeId,
                    schoolId = selectedSchoolId,
                    conditionId = selectedConditionId,
                    locationId = selectedTownId,
                    provinceId = selectedProvinceId,
                    genderId = selectedGenderId,
                    colorId = selectedColorId,
                    tagIds = null
                )
                Log.d(TAG, "createItem called on ViewModel")
            } else {
                // User has NO contact number - show dialog to add contact number
                Log.d(TAG, "User does NOT have contact number, showing dialog")
                showMissingContactDialog()
            }
        }
    }

    private fun showMissingContactDialog() {
        DialogHelper.showConfirmationDialog(
            context = requireContext(),
            action = DialogAction.MissingContactNumber,
            onConfirm = {
                // User wants to add contact number - navigate to profile
                findNavController().navigate(R.id.action_createItemFragment_to_profileFragment)
            },
            onCancel = {
                // User chose not to add contact number
                Toast.makeText(requireContext(), "Please add a contact number to list items", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateImagePreview(uris: List<Uri>) {
        Log.d(TAG, "updateImagePreview: ${uris.size} images")
        binding.imagesSubheading.text = "${uris.size}/3 images selected"

        binding.deleteCover.visibility = View.GONE
        binding.deleteAngle2.visibility = View.GONE
        binding.deleteAngle3.visibility = View.GONE

        binding.coverPhoto.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.differentAngle.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.labelPhoto.setImageResource(R.drawable.ic_create_item_placeholder)

        lifecycleScope.launch {
            uris.forEachIndexed { index, uri ->
                when (index) {
                    0 -> {
                        Log.d(TAG, "Loading image 0: $uri")
                        loadImageWithGlide(uri, binding.coverPhoto)
                        binding.deleteCover.visibility = View.VISIBLE
                    }
                    1 -> {
                        Log.d(TAG, "Loading image 1: $uri")
                        loadImageWithGlide(uri, binding.differentAngle)
                        binding.deleteAngle2.visibility = View.VISIBLE
                    }
                    2 -> {
                        Log.d(TAG, "Loading image 2: $uri")
                        loadImageWithGlide(uri, binding.labelPhoto)
                        binding.deleteAngle3.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun loadImageWithGlide(uri: Uri, imageView: android.widget.ImageView) {
        try {
            Glide.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.ic_create_item_placeholder)
                .error(R.drawable.ic_create_item_placeholder)
                .centerCrop()
                .into(imageView)
            Log.d(TAG, "Glide loaded image: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Glide failed to load image", e)
            e.printStackTrace()
        }
    }

    private fun validateForm(): Boolean {
        Log.d(TAG, "Validating form")

        if (binding.itemName.text.isNullOrEmpty()) {
            Log.w(TAG, "Item name is empty")
            Toast.makeText(requireContext(), "Please enter item name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.description.text.isNullOrEmpty()) {
            Log.w(TAG, "Description is empty")
            Toast.makeText(requireContext(), "Please enter description", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.price.text.isNullOrEmpty()) {
            Log.w(TAG, "Price is empty")
            Toast.makeText(requireContext(), "Please enter price", Toast.LENGTH_SHORT).show()
            return false
        }
        val price = binding.price.text.toString().toDoubleOrNull()
        if (price == null || price <= 0) {
            Log.w(TAG, "Invalid price: ${binding.price.text}")
            Toast.makeText(requireContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show()
            return false
        }

        Log.d(TAG, "Form validation passed")
        return true
    }

    private fun showLoading() {
        Log.d(TAG, "Showing loading state")
        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Creating..."
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        Log.d(TAG, "Hiding loading state")
        binding.submitButton.isEnabled = true
        binding.submitButton.text = "Create Item"
        binding.progressBar.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        Log.d(TAG, "✅ Success: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        clearForm()
    }

    private fun showError(message: String) {
        Log.e(TAG, "❌ Error: $message")
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG).show()
    }

    private fun clearForm() {
        Log.d(TAG, "Clearing form")
        binding.itemName.text?.clear()
        binding.description.text?.clear()
        binding.price.text?.clear()
        viewModel.clearImages()
        binding.coverPhoto.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.differentAngle.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.labelPhoto.setImageResource(R.drawable.ic_create_item_placeholder)
        updateImagePreview(emptyList())

        selectedMainCategoryId = null
        selectedSubCategoryId = null
        selectedConditionId = null
        selectedSizeId = null
        selectedBrandId = null
        selectedColorId = null
        selectedProvinceId = null
        selectedTownId = null
        selectedSchoolId = null
        selectedGenderId = null
        selectedQuantity = 1

        // Clear input fields
        binding.mainCategoryInput.setText("")
        binding.subCategoryInput.setText("")
        binding.conditionInput.setText("")
        binding.sizeInput.setText("")
        binding.brandInput.setText("")
        binding.colorInput.setText("")
        binding.provinceInput.setText("")
        binding.townInput.setText("")
        binding.genderInput.setText("")
        binding.schoolInput.setText("")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")
        outState.putInt("selectedMainCategoryId", selectedMainCategoryId ?: -1)
        outState.putInt("selectedSubCategoryId", selectedSubCategoryId ?: -1)
        outState.putInt("selectedConditionId", selectedConditionId ?: -1)
        outState.putInt("selectedSizeId", selectedSizeId ?: -1)
        outState.putInt("selectedBrandId", selectedBrandId ?: -1)
        outState.putInt("selectedColorId", selectedColorId ?: -1)
        outState.putInt("selectedProvinceId", selectedProvinceId ?: -1)
        outState.putInt("selectedTownId", selectedTownId ?: -1)
        outState.putInt("selectedSchoolId", selectedSchoolId ?: -1)
        outState.putInt("selectedGenderId", selectedGenderId ?: -1)
        outState.putInt("selectedQuantity", selectedQuantity)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring state from savedInstanceState")
            selectedMainCategoryId = savedInstanceState.getInt("selectedMainCategoryId").takeIf { it != -1 }
            selectedSubCategoryId = savedInstanceState.getInt("selectedSubCategoryId").takeIf { it != -1 }
            selectedConditionId = savedInstanceState.getInt("selectedConditionId").takeIf { it != -1 }
            selectedSizeId = savedInstanceState.getInt("selectedSizeId").takeIf { it != -1 }
            selectedBrandId = savedInstanceState.getInt("selectedBrandId").takeIf { it != -1 }
            selectedColorId = savedInstanceState.getInt("selectedColorId").takeIf { it != -1 }
            selectedProvinceId = savedInstanceState.getInt("selectedProvinceId").takeIf { it != -1 }
            selectedTownId = savedInstanceState.getInt("selectedTownId").takeIf { it != -1 }
            selectedSchoolId = savedInstanceState.getInt("selectedSchoolId").takeIf { it != -1 }
            selectedGenderId = savedInstanceState.getInt("selectedGenderId").takeIf { it != -1 }
            selectedQuantity = savedInstanceState.getInt("selectedQuantity", 1)
            Log.d(TAG, "Restored quantity: $selectedQuantity")
        }
    }

    private fun navigateAfterSuccess() {
        Log.d(TAG, "Navigating after success")
        lifecycleScope.launch {
            delay(2000)
            try {
                findNavController().navigate(R.id.action_createItemFragment_to_mainFragment)
                Log.d(TAG, "Navigated to main fragment")
            } catch (e: Exception) {
                Log.e(TAG, "Navigation failed", e)
                try {
                    findNavController().navigate(R.id.nav_home)
                    Log.d(TAG, "Navigated to home")
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback navigation failed", e2)
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - resetting camera flag")
        isCameraLaunched = false
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }
}