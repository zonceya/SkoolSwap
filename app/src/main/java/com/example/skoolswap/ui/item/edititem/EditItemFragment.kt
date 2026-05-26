package com.example.skoolswap.ui.item.edititem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
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
import com.example.skoolswap.databinding.FragmentEditItemBinding
import com.example.skoolswap.domain.model.EditImage
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.reference.*
import com.example.skoolswap.ui.component.ColorPickerBottomSheet
import com.example.skoolswap.ui.component.OptionsPickerBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditItemFragment : Fragment() {

    private var _binding: FragmentEditItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditItemViewModel by viewModels()
    private val itemId: String by lazy {
        arguments?.getString("itemId") ?: ""
    }

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

    // Track original values to detect changes
    private var originalItem: Item? = null
    private var originalName: String = ""
    private var originalDescription: String = ""
    private var originalPrice: Double = 0.0
    private var originalQuantity: Int = 1

    // Image handling
    private var pendingImagePosition: Int? = null
    private var pendingIsReplace: Boolean? = null
    private var isCameraLaunched = false

    // Camera launcher
    private val simpleCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        isCameraLaunched = false
        if (bitmap != null) {
            lifecycleScope.launch {
                val uri = saveBitmapToFile(bitmap)
                if (uri != null) {
                    val position = pendingImagePosition ?: return@launch
                    val isReplace = pendingIsReplace ?: false

                    if (isReplace) {
                        viewModel.replaceImage(uri, position)
                        Toast.makeText(requireContext(), "Image replaced", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addImage(uri, position)
                        Toast.makeText(requireContext(), "Image added", Toast.LENGTH_SHORT).show()
                    }

                    pendingImagePosition = null
                    pendingIsReplace = null
                }
            }
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val position = pendingImagePosition ?: return@registerForActivityResult
            val isReplace = pendingIsReplace ?: false

            lifecycleScope.launch {
                if (isReplace) {
                    viewModel.replaceImage(uri, position)
                    Toast.makeText(requireContext(), "Image replaced", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addImage(uri, position)
                    Toast.makeText(requireContext(), "Image added", Toast.LENGTH_SHORT).show()
                }
                pendingImagePosition = null
                pendingIsReplace = null
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
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
        setupImageGrid()
        setupQuantitySpinner()

        // Load the item
        if (itemId.isNotEmpty()) {
            viewModel.loadItem(itemId)
        } else {
            Toast.makeText(requireContext(), "Error: No item ID provided", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        // Observe item loading
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.item.collect { item ->
                    item?.let {
                        originalItem = it
                        populateItemData(it)
                    }
                }
            }
        }

        // Collect Main Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainCategories.collect { categories ->
                    Log.d("EditItemFragment", "📦 MAIN CATEGORIES received: ${categories.size}")
                    setupMainCategoryPicker(categories)
                }
            }
        }

        // Collect Sub Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    Log.d("EditItemFragment", "📦 SUBCATEGORIES received: ${subCategories.size}")
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

        // Observe UI State
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is EditItemUiState.Success -> {
                            hideLoading()
                            showSuccess(uiState.message)
                            navigateBack()
                        }
                        is EditItemUiState.Error -> {
                            hideLoading()
                            showError(uiState.message)
                        }
                        is EditItemUiState.Loading -> {
                            showLoading()
                        }
                        is EditItemUiState.Idle -> {
                            hideLoading()
                        }
                    }
                }
            }
        }

        // Observe delete confirmation
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showDeleteConfirmation.collect { show ->
                    if (show) {
                        showDeleteConfirmationDialog()
                        viewModel.deleteConfirmationShown()
                    }
                }
            }
        }
    }

    private fun setupImageGrid() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { images ->
                    renderImageGrid(images)
                    updateImageCountText()
                }
            }
        }
    }

    // ============ BOTTOM SHEET PICKER METHODS ============

    private fun setupMainCategoryPicker(categories: List<MainCategory>) {
        if (categories.isEmpty()) {
            binding.mainCategoryInput.visibility = View.GONE
            binding.mainCategoryLabel.visibility = View.GONE
            return
        }

        binding.mainCategoryInput.visibility = View.VISIBLE
        binding.mainCategoryLabel.visibility = View.VISIBLE

        // Restore using the ID to find the name
        selectedMainCategoryId?.let { id ->
            categories.find { it.id == id }?.let {
                binding.mainCategoryInput.setText(it.name)
            }
        }

        binding.mainCategoryInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = "Select Category",
                options = categories.map { it.name }
            ) { selectedName, position ->
                val selectedCategory = categories[position]
                selectedMainCategoryId = selectedCategory.id
                binding.mainCategoryInput.setText(selectedName)
                viewModel.onMainCategorySelected(selectedCategory.id)
            }.show(childFragmentManager, "category_picker")
        }
    }

    private fun setupSubCategoryPicker(subCategories: List<SubCategory>) {
        if (subCategories.isEmpty()) {
            binding.subCategoryInput.isEnabled = false
            return
        }

        binding.subCategoryInput.isEnabled = true

        // Restore using the ID to find the name
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
                Log.d("EditItemFragment", "Selected subcategory: ${selected.name} (ID: ${selected.id})")
            }.show(childFragmentManager, "subcategory_picker")
        }
    }

    private fun setupConditionPicker(conditions: List<Condition>) {
        // Restore using the ID to find the name
        selectedConditionId?.let { id ->
            conditions.find { it.id == id }?.let {
                binding.conditionInput.setText(it.name)
            }
        }
        // Fallback to the stored name if ID not found yet
        if (binding.conditionInput.text.isNullOrEmpty()) {
            originalItem?.conditionName?.let { binding.conditionInput.setText(it) }
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
        // Restore using the ID to find the name
        selectedSizeId?.let { id ->
            sizes.find { it.id == id }?.let {
                binding.sizeInput.setText(it.name)
            }
        }
        // Fallback to the stored name if ID not found yet
        if (binding.sizeInput.text.isNullOrEmpty()) {
            originalItem?.sizeName?.let { binding.sizeInput.setText(it) }
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
        // Restore using the ID to find the name
        selectedBrandId?.let { id ->
            brands.find { it.id == id }?.let {
                binding.brandInput.setText(it.name)
            }
        }
        // Fallback to the stored name if ID not found yet
        if (binding.brandInput.text.isNullOrEmpty()) {
            originalItem?.brandName?.let { binding.brandInput.setText(it) }
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
        // Restore using the ID to find the name
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

        // Restore using the ID to find the name
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
        // Restore using the ID to find the name
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
        // Restore using the ID to find the name
        selectedSchoolId?.let { id ->
            schools.find { it.id == id }?.let {
                binding.schoolInput.setText(it.name)
            }
        }
        // Fallback to the stored name if ID not found yet
        if (binding.schoolInput.text.isNullOrEmpty()) {
            originalItem?.schoolName?.let { binding.schoolInput.setText(it) }
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

    private fun renderImageGrid(images: List<EditImage>) {
        binding.imageGrid.removeAllViews()

        images.forEachIndexed { index, editImage ->
            val imageView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_editable_image, binding.imageGrid, false)

            val itemImage = imageView.findViewById<ImageView>(R.id.itemImage)
            val deleteButton = imageView.findViewById<ImageView>(R.id.deleteButton)
            val addButton = imageView.findViewById<View>(R.id.addButton)
            val loadingBar = imageView.findViewById<ProgressBar>(R.id.imageLoading)

            when (editImage) {
                is EditImage.Existing -> {
                    addButton.visibility = View.GONE
                    deleteButton.visibility = View.VISIBLE
                    loadingBar.visibility = View.GONE

                    if (editImage.isMarkedForDeletion) {
                        itemImage.alpha = 0.5f
                    } else {
                        itemImage.alpha = 1.0f
                    }

                    Glide.with(requireContext())
                        .load(editImage.url)
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .centerCrop()
                        .into(itemImage)

                    itemImage.setOnClickListener {
                        showImageSourceOptions(index, isReplace = true)
                    }

                    deleteButton.setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Remove Image")
                            .setMessage("Remove this image from your item?")
                            .setPositiveButton("Remove") { _, _ ->
                                viewModel.removeImage(index)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }

                is EditImage.New -> {
                    addButton.visibility = View.GONE
                    deleteButton.visibility = View.VISIBLE

                    if (editImage.isUploading) {
                        loadingBar.visibility = View.VISIBLE
                        itemImage.visibility = View.GONE
                    } else {
                        loadingBar.visibility = View.GONE
                        itemImage.visibility = View.VISIBLE

                        Glide.with(requireContext())
                            .load(editImage.uri)
                            .placeholder(R.drawable.ic_create_item_placeholder)
                            .error(R.drawable.ic_create_item_placeholder)
                            .centerCrop()
                            .into(itemImage)
                    }

                    itemImage.setOnClickListener {
                        showImageSourceOptions(index, isReplace = true)
                    }

                    deleteButton.setOnClickListener {
                        viewModel.removeImage(index)
                    }
                }

                EditImage.Empty -> {
                    addButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.GONE
                    itemImage.setImageResource(R.drawable.ic_create_item_placeholder)

                    addButton.setOnClickListener {
                        showImageSourceOptions(index, isReplace = false)
                    }
                }
            }

            binding.imageGrid.addView(imageView)
        }
    }

    private fun updateImageCountText() {
        val images = viewModel.images.value
        val imageCount = images.count { it !is EditImage.Empty }
        binding.imagesCount.text = "$imageCount/3 images - Tap to replace, ✕ to remove"
    }

    private fun showImageSourceOptions(position: Int, isReplace: Boolean) {
        pendingImagePosition = position
        pendingIsReplace = isReplace

        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle(if (isReplace) "Replace Image" else "Add Image")
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
                    Toast.makeText(requireContext(), "No camera available", Toast.LENGTH_SHORT).show()
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
            .setMessage("SkoolSwap needs camera permission to take photos of items")
            .setPositiveButton("Allow") { _, _ ->
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchCameraSafely() {
        if (!isCameraLaunched) {
            isCameraLaunched = true
            try {
                simpleCameraLauncher.launch(null)
            } catch (e: Exception) {
                isCameraLaunched = false
                Toast.makeText(requireContext(), "Failed to launch camera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to open gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "IMG_${timeStamp}.jpg"

            val file = File(requireContext().cacheDir, filename)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
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

    private fun populateItemData(item: Item) {
        originalName = item.name
        originalDescription = item.description
        originalPrice = item.price
        originalQuantity = item.quantity

        // Set text fields
        binding.itemName.setText(item.name)
        binding.description.setText(item.description)
        binding.price.setText(item.price.toString())

        // Set quantity
        selectedQuantity = item.quantity

        // Store selected IDs
        selectedMainCategoryId = item.mainCategoryId
        selectedSubCategoryId = item.subCategoryId
        selectedConditionId = item.itemConditionId
        selectedSizeId = item.sizeId
        selectedBrandId = item.brandId
        selectedColorId = item.colorId
        selectedProvinceId = item.provinceId
        selectedTownId = item.locationId
        selectedSchoolId = item.schoolId
        selectedGenderId = item.genderId

        // Note: The picker methods will restore the display names when reference data loads
        // No need to set text here as the picker methods will handle it

        val existingImages = item.images.map { image ->
            EditImage.Existing(image.id, image.url)
        }
        viewModel.setExistingImages(existingImages)
    }

    private fun setupQuantitySpinner() {
        val quantities = listOf("1", "2", "3", "4", "5")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            quantities
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.quantitySpinner.adapter = adapter

        if (selectedQuantity in 1..5) {
            binding.quantitySpinner.setSelection(selectedQuantity - 1)
        }

        binding.quantitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedQuantity = position + 1
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupClickListeners() {
        binding.updateButton.setOnClickListener {
            if (validateForm()) {
                updateItem()
            }
        }

        binding.deleteItemButton.setOnClickListener {
            viewModel.showDeleteConfirmation()
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

    private fun updateItem() {
        val name = binding.itemName.text.toString()
        val description = binding.description.text.toString()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        val hasChanges = name != originalName ||
                description != originalDescription ||
                price != originalPrice ||
                selectedQuantity != originalQuantity ||
                selectedMainCategoryId != originalItem?.mainCategoryId ||
                selectedSubCategoryId != originalItem?.subCategoryId ||
                selectedConditionId != originalItem?.itemConditionId ||
                selectedSizeId != originalItem?.sizeId ||
                selectedBrandId != originalItem?.brandId ||
                selectedColorId != originalItem?.colorId ||
                selectedProvinceId != originalItem?.provinceId ||
                selectedTownId != originalItem?.locationId ||
                selectedSchoolId != originalItem?.schoolId ||
                selectedGenderId != originalItem?.genderId ||
                viewModel.getDeletionIds().isNotEmpty() ||
                viewModel.getImagesForUpload().isNotEmpty()

        if (!hasChanges) {
            Toast.makeText(requireContext(), "No changes to save", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        viewModel.updateItem(
            context = requireContext(),
            itemId = itemId,
            name = if (name != originalName) name else null,
            description = if (description != originalDescription) description else null,
            price = if (price != originalPrice) price else null,
            quantity = if (selectedQuantity != originalQuantity) selectedQuantity else null,
            mainCategoryId = if (selectedMainCategoryId != originalItem?.mainCategoryId) selectedMainCategoryId else null,
            subCategoryId = if (selectedSubCategoryId != originalItem?.subCategoryId) selectedSubCategoryId else null,
            brandId = if (selectedBrandId != originalItem?.brandId) selectedBrandId else null,
            sizeId = if (selectedSizeId != originalItem?.sizeId) selectedSizeId else null,
            schoolId = if (selectedSchoolId != originalItem?.schoolId) selectedSchoolId else null,
            conditionId = if (selectedConditionId != originalItem?.itemConditionId) selectedConditionId else null,
            locationId = if (selectedTownId != originalItem?.locationId) selectedTownId else null,
            provinceId = if (selectedProvinceId != originalItem?.provinceId) selectedProvinceId else null,
            genderId = if (selectedGenderId != originalItem?.genderId) selectedGenderId else null,
            colorId = if (selectedColorId != originalItem?.colorId) selectedColorId else null,
            addImageUris = viewModel.getImagesForUpload(),
            removeImageIds = viewModel.getDeletionIds()
        )
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteItem(itemId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading() {
        binding.updateButton.isEnabled = false
        binding.deleteItemButton.isEnabled = false
        binding.updateButton.text = "Updating..."
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.updateButton.isEnabled = true
        binding.deleteItemButton.isEnabled = true
        binding.updateButton.text = "Update Item"
        binding.progressBar.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG).show()
    }

    private fun navigateBack() {
        lifecycleScope.launch {
            delay(1500)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "item_updated",
                true
            )
            findNavController().navigateUp()
        }
    }

    override fun onResume() {
        super.onResume()
        isCameraLaunched = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}