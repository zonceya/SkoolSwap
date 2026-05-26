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
        isCameraLaunched = false
        if (bitmap != null) {
            lifecycleScope.launch {
                if (viewModel.images.value.size < 3) {
                    val uri = saveBitmapToFile(bitmap)
                    if (uri != null) {
                        viewModel.addImage(uri)
                        Toast.makeText(requireContext(), "Photo added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        lifecycleScope.launch {
            var imagesAdded = 0
            uris.forEach { uri ->
                if (viewModel.images.value.size < 3) {
                    viewModel.addImage(uri)
                    imagesAdded++
                }
            }
            if (imagesAdded > 0) {
                Toast.makeText(requireContext(), "$imagesAdded image(s) added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraSafely()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
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

        hideFab()
        setupObservers()
        setupClickListeners()
        setupDefaultSelections()
        restoreState(savedInstanceState)
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
    }

    private fun setupObservers() {
        // Collect Main Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainCategories.collect { categories ->
                    Log.d("CreateItemFragment", "📦 MAIN CATEGORIES received: ${categories.size}")
                    showSubcategoryLoading(false)
                    setupMainCategoryPicker(categories)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    showSubcategoryLoading(isLoading)
                }
            }
        }

        // Collect Sub Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    Log.d("CreateItemFragment", "📦 SUBCATEGORIES received: ${subCategories.size}")
                    setupSubCategoryPicker(subCategories)
                }
            }
        }

        // Collect Conditions
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditions.collect { conditions ->
                    setupConditionPicker(conditions)
                }
            }
        }

        // Collect Sizes
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sizes.collect { sizes ->
                    setupSizePicker(sizes)
                }
            }
        }

        // Collect Brands
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.brands.collect { brands ->
                    setupBrandPicker(brands)
                }
            }
        }

        // Collect Colors
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colors.collect { colors ->
                    setupColorPicker(colors)
                }
            }
        }

        // Collect Provinces
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.provinces.collect { provinces ->
                    setupProvincePicker(provinces)
                }
            }
        }

        // Collect Towns
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.towns.collect { towns ->
                    setupTownPicker(towns)
                }
            }
        }

        // Collect Images
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { uris ->
                    Log.d("CreateItemFragment", "📸 Images updated: ${uris.size} images")
                    updateImagePreview(uris)
                }
            }
        }

        // Collect Schools
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.schools.collect { schools ->
                    setupSchoolPicker(schools)
                }
            }
        }

        // Collect Genders
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genders.collect { genders ->
                    setupGenderPicker(genders)
                }
            }
        }

        // Observe UI State for navigation
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is CreateItemUiState.Success -> {
                            hideLoading()
                            showSuccess(uiState.message)
                            navigateAfterSuccess()
                        }
                        is CreateItemUiState.Error -> {
                            hideLoading()
                            showError(uiState.message)
                        }
                        is CreateItemUiState.Loading -> {
                            showLoading()
                        }
                        is CreateItemUiState.Idle -> {
                            hideLoading()
                        }
                    }
                }
            }
        }
    }

    // ============ BOTTOM SHEET PICKER METHODS ============

    private fun setupMainCategoryPicker(categories: List<MainCategory>) {
        Log.d("CreateItemFragment", "🎯 setupMainCategoryPicker called with ${categories.size} categories")

        // Log each category for debugging
        categories.forEachIndexed { index, category ->
            Log.d("CreateItemFragment", "  Category $index: ${category.name} (ID: ${category.id})")
        }

        if (categories.isEmpty()) {
            Log.w("CreateItemFragment", "Categories list is empty, hiding picker")
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
                Log.d("CreateItemFragment", "Restored selected category: ${it.name}")
            }
        }

        // Set click listener
        binding.mainCategoryInput.setOnClickListener {
            Log.d("CreateItemFragment", "Main category input clicked, showing ${categories.size} options")

            OptionsPickerBottomSheet(
                title = "Select Category",
                options = categories.map { it.name }
            ) { selectedName, position ->
                val selectedCategory = categories[position]
                selectedMainCategoryId = selectedCategory.id
                binding.mainCategoryInput.setText(selectedName)
                Log.d("CreateItemFragment", "✅ Selected category: ${selectedCategory.name} (ID: ${selectedCategory.id})")
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

        Log.d("CreateItemFragment", "✅ Main category picker setup complete")
    }

    private fun setupSubCategoryPicker(subCategories: List<SubCategory>) {
        if (subCategories.isEmpty()) {
            binding.subCategoryInput.isEnabled = false
            return
        }

        binding.subCategoryInput.isEnabled = true

        // Restore selected subcategory if exists
        selectedSubCategoryId?.let { id ->
            subCategories.find { it.id == id }?.let {
                binding.subCategoryInput.setText(it.name)
            }
        }

        binding.subCategoryInput.setOnClickListener {
            if (selectedMainCategoryId == null) {
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
                Log.d("CreateItemFragment", "Selected subcategory: ${selected.name} (ID: ${selected.id})")
            }.show(childFragmentManager, "subcategory_picker")
        }
    }

    private fun setupConditionPicker(conditions: List<Condition>) {
        // Restore selected condition if exists
        selectedConditionId?.let { id ->
            conditions.find { it.id == id }?.let {
                binding.conditionInput.setText(it.name)
            }
        }

        binding.conditionInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Condition",
                options = conditions.map { it.name }
            ) { selectedName, position ->
                selectedConditionId = conditions[position].id
                binding.conditionInput.setText(selectedName)
            }.show(childFragmentManager, "condition_picker")
        }
    }

    private fun setupSizePicker(sizes: List<Size>) {
        // Restore selected size if exists
        selectedSizeId?.let { id ->
            sizes.find { it.id == id }?.let {
                binding.sizeInput.setText(it.name)
            }
        }

        binding.sizeInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Size",
                options = sizes.map { it.name }
            ) { selectedName, position ->
                selectedSizeId = sizes[position].id
                binding.sizeInput.setText(selectedName)
            }.show(childFragmentManager, "size_picker")
        }
    }

    private fun setupBrandPicker(brands: List<Brand>) {
        // Restore selected brand if exists
        selectedBrandId?.let { id ->
            brands.find { it.id == id }?.let {
                binding.brandInput.setText(it.name)
            }
        }

        binding.brandInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Brand",
                options = brands.map { it.name }
            ) { selectedName, position ->
                selectedBrandId = brands[position].id
                binding.brandInput.setText(selectedName)
            }.show(childFragmentManager, "brand_picker")
        }
    }

    private fun setupColorPicker(colors: List<Color>) {
        if (colors.isEmpty()) {
            binding.colorInput.isEnabled = false
            return
        }

        binding.colorInput.isEnabled = true

        // Restore selected color if exists
        selectedColorId?.let { id ->
            colors.find { it.id == id }?.let {
                binding.colorInput.setText(it.name)
            }
        }

        binding.colorInput.setOnClickListener {
            ColorPickerBottomSheet(colors) { selectedColor, position ->
                selectedColorId = selectedColor.id
                binding.colorInput.setText(selectedColor.name)
            }.show(childFragmentManager, "color_picker")
        }
    }

    private fun setupProvincePicker(provinces: List<Province>) {
        // Restore selected province if exists
        selectedProvinceId?.let { id ->
            provinces.find { it.id == id }?.let {
                binding.provinceInput.setText(it.name)
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
                viewModel.onProvinceSelected(selectedProvince.id)
            }.show(childFragmentManager, "province_picker")
        }
    }

    private fun setupTownPicker(towns: List<Town>) {
        if (towns.isEmpty()) {
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
            }
        }

        binding.townInput.setOnClickListener {
            if (selectedProvinceId == null) {
                Toast.makeText(requireContext(), "First select a province", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            OptionsPickerBottomSheet(
                title = "Select Town/City",
                options = towns.map { it.name }
            ) { selectedName, position ->
                selectedTownId = towns[position].id
                binding.townInput.setText(selectedName)
            }.show(childFragmentManager, "town_picker")
        }
    }

    private fun setupGenderPicker(genders: List<Gender>) {
        // Restore selected gender if exists
        selectedGenderId?.let { id ->
            genders.find { it.id == id }?.let {
                binding.genderInput.setText(it.name)
            }
        }

        binding.genderInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Gender",
                options = genders.map { it.name }
            ) { selectedName, position ->
                selectedGenderId = genders[position].id
                binding.genderInput.setText(selectedName)
            }.show(childFragmentManager, "gender_picker")
        }
    }

    private fun setupSchoolPicker(schools: List<School>) {
        // Restore selected school if exists
        selectedSchoolId?.let { id ->
            schools.find { it.id == id }?.let {
                binding.schoolInput.setText(it.name)
            }
        }

        binding.schoolInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select School",
                options = schools.map { it.name }
            ) { selectedName, position ->
                selectedSchoolId = schools[position].id
                binding.schoolInput.setText(selectedName)
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
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun launchCameraSafely() {
        if (!isCameraLaunched) {
            isCameraLaunched = true
            try {
                simpleCameraLauncher.launch(null)
            } catch (e: Exception) {
                isCameraLaunched = false
                Toast.makeText(requireContext(), "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
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

        binding.deleteCover.setOnClickListener {
            if (viewModel.images.value.isNotEmpty()) {
                viewModel.removeImageAt(0)
            }
        }

        binding.deleteAngle2.setOnClickListener {
            if (viewModel.images.value.size > 1) {
                viewModel.removeImageAt(1)
            }
        }

        binding.deleteAngle3.setOnClickListener {
            if (viewModel.images.value.size > 2) {
                viewModel.removeImageAt(2)
            }
        }
    }

    private fun pickImagesIfAllowed() {
        if (viewModel.images.value.size >= 3) {
            Toast.makeText(requireContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
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
                    0 -> checkCameraPermission()
                    1 -> launchGallery()
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
                if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    launchCameraSafely()
                } else {
                    Toast.makeText(requireContext(), "Your device doesn't have a camera", Toast.LENGTH_SHORT).show()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionExplanation()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionExplanation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera Permission Needed")
            .setMessage("SkoolSwap needs camera permission to let you take photos of items you want to list.")
            .setPositiveButton("Allow") { _, _ ->
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchGallery() {
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
            .setMessage("There's a temporary issue accessing your gallery.\n\nYou can use 'Take Photo' to capture new images.")
            .setPositiveButton("Take Photo") { _, _ ->
                checkCameraPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBitmapToFile(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "IMG_${timeStamp}.jpg"
            val file = File(requireContext().cacheDir, filename)
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createItem() {
        val name = binding.itemName.text.toString()
        val description = binding.description.text.toString()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        if (selectedMainCategoryId == null || selectedSubCategoryId == null) {
            Toast.makeText(requireContext(), "Please select category and subcategory", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.createItem(
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
    }

    private fun updateImagePreview(uris: List<Uri>) {
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
                        loadImageWithGlide(uri, binding.coverPhoto)
                        binding.deleteCover.visibility = View.VISIBLE
                    }
                    1 -> {
                        loadImageWithGlide(uri, binding.differentAngle)
                        binding.deleteAngle2.visibility = View.VISIBLE
                    }
                    2 -> {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun validateForm(): Boolean {
        if (binding.itemName.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please enter item name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.description.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please enter description", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.price.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please enter price", Toast.LENGTH_SHORT).show()
            return false
        }
        val price = binding.price.text.toString().toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show()
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

    private fun clearForm() {
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
        }
    }

    private fun navigateAfterSuccess() {
        lifecycleScope.launch {
            delay(2000)
            try {
                findNavController().navigate(R.id.action_createItemFragment_to_mainFragment)
            } catch (e: Exception) {
                try {
                    findNavController().navigate(R.id.nav_home)
                } catch (e2: Exception) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        isCameraLaunched = false
    }

    override fun onPause() {
        super.onPause()
        Log.d("CreateItemFragment", "onPause")
    }
}