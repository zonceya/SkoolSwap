package com.example.skoolswap.ui.item.edititem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
import com.example.skoolswap.utils.ColorUtils
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
                    setupMainCategorySpinner(categories)
                }
            }
        }

        // Collect Sub Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    setupSubCategorySpinner(subCategories)
                }
            }
        }

        // Collect Conditions
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditions.collect { conditions ->
                    setupConditionsSpinner(conditions)
                }
            }
        }

        // Collect Sizes
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sizes.collect { sizes ->
                    setupSizesSpinner(sizes)
                }
            }
        }

        // Collect Brands
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.brands.collect { brands ->
                    setupBrandsSpinner(brands)
                }
            }
        }

        // Collect Colors
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colors.collect { colors ->
                    setupColorsSpinner(colors)
                }
            }
        }

        // Collect Provinces
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.provinces.collect { provinces ->
                    setupProvincesSpinner(provinces)
                }
            }
        }

        // Collect Towns
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.towns.collect { towns ->
                    setupTownsSpinner(towns)
                }
            }
        }

        // Collect Schools
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.schools.collect { schools ->
                    setupSchoolsSpinner(schools)
                }
            }
        }

        // Collect Genders
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genders.collect { genders ->
                    setupGendersSpinner(genders)
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
                    // Show existing image from server
                    addButton.visibility = View.GONE
                    deleteButton.visibility = View.VISIBLE
                    loadingBar.visibility = View.GONE

                    // Dim if marked for deletion
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

                    // Click to replace
                    itemImage.setOnClickListener {
                        showImageSourceOptions(index, isReplace = true)
                    }

                    // Delete button
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
                    // Show new image (local URI)
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

                    // Click to replace
                    itemImage.setOnClickListener {
                        showImageSourceOptions(index, isReplace = true)
                    }

                    // Delete button
                    deleteButton.setOnClickListener {
                        viewModel.removeImage(index)
                    }
                }

                EditImage.Empty -> {
                    // Show add button
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

    private fun setupMainCategorySpinner(categories: List<MainCategory>) {
        if (categories.isEmpty()) return

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mainCategorySpinner.adapter = adapter

        if (selectedMainCategoryId != null) {
            val position = categories.indexOfFirst { it.id == selectedMainCategoryId }
            if (position >= 0) {
                binding.mainCategorySpinner.setSelection(position)
            }
        }

        binding.mainCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                if (selectedMainCategoryId != selectedCategory.id) {
                    selectedMainCategoryId = selectedCategory.id
                    viewModel.onMainCategorySelected(selectedCategory.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSubCategorySpinner(subCategories: List<SubCategory>) {
        if (subCategories.isEmpty()) {
            binding.subCategorySpinner.isEnabled = false
            return
        }

        binding.subCategorySpinner.isEnabled = true

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subCategories.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.subCategorySpinner.adapter = adapter

        if (selectedSubCategoryId != null) {
            val position = subCategories.indexOfFirst { it.id == selectedSubCategoryId }
            if (position >= 0) {
                binding.subCategorySpinner.setSelection(position)
            }
        }

        binding.subCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSubCategoryId = subCategories[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupConditionsSpinner(conditions: List<Condition>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            conditions.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.conditionSpinner.adapter = adapter

        if (selectedConditionId != null) {
            val position = conditions.indexOfFirst { it.id == selectedConditionId }
            if (position >= 0) {
                binding.conditionSpinner.setSelection(position)
            }
        }

        binding.conditionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedConditionId = conditions[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSizesSpinner(sizes: List<Size>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sizes.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sizeSpinner.adapter = adapter

        if (selectedSizeId != null) {
            val position = sizes.indexOfFirst { it.id == selectedSizeId }
            if (position >= 0) {
                binding.sizeSpinner.setSelection(position)
            }
        }

        binding.sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSizeId = sizes[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupBrandsSpinner(brands: List<Brand>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            brands.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.brandSpinner.adapter = adapter

        if (selectedBrandId != null) {
            val position = brands.indexOfFirst { it.id == selectedBrandId }
            if (position >= 0) {
                binding.brandSpinner.setSelection(position)
            }
        }

        binding.brandSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBrandId = brands[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupColorsSpinner(colors: List<Color>) {
        if (colors.isEmpty()) {
            binding.colorSpinner.isEnabled = false
            return
        }

        class ColorAdapter(context: Context, private val colorItems: List<Color>) :
            ArrayAdapter<Color>(context, 0, colorItems) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_color_spinner, parent, false)

                val colorItem = colorItems[position]

                view.findViewById<TextView>(R.id.colorName).text = colorItem.name

                val colorHex = ColorUtils.getColorHex(colorItem.name)
                view.findViewById<View>(R.id.colorPreview)
                    .setBackgroundColor(android.graphics.Color.parseColor(colorHex))

                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return getView(position, convertView, parent)
            }
        }

        binding.colorSpinner.adapter = ColorAdapter(requireContext(), colors)
        binding.colorSpinner.isEnabled = true

        if (selectedColorId != null) {
            val position = colors.indexOfFirst { it.id == selectedColorId }
            if (position >= 0) {
                binding.colorSpinner.setSelection(position)
            }
        }

        binding.colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedColorId = colors[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupProvincesSpinner(provinces: List<Province>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            provinces.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.provinceSpinner.adapter = adapter

        if (selectedProvinceId != null) {
            val position = provinces.indexOfFirst { it.id == selectedProvinceId }
            if (position >= 0) {
                binding.provinceSpinner.setSelection(position)
            }
        }

        binding.provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedProvince = provinces[position]
                if (selectedProvinceId != selectedProvince.id) {
                    selectedProvinceId = selectedProvince.id
                    viewModel.onProvinceSelected(selectedProvince.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupTownsSpinner(towns: List<Town>) {
        if (towns.isEmpty()) {
            binding.townSpinner.visibility = View.GONE
            binding.townLabel.visibility = View.GONE
            return
        }

        binding.townSpinner.visibility = View.VISIBLE
        binding.townLabel.visibility = View.VISIBLE

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            towns.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.townSpinner.adapter = adapter

        if (selectedTownId != null) {
            val position = towns.indexOfFirst { it.id == selectedTownId }
            if (position >= 0) {
                binding.townSpinner.setSelection(position)
            }
        }

        binding.townSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTownId = towns[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSchoolsSpinner(schools: List<School>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            schools.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.schoolSpinner.adapter = adapter

        if (selectedSchoolId != null) {
            val position = schools.indexOfFirst { it.id == selectedSchoolId }
            if (position >= 0) {
                binding.schoolSpinner.setSelection(position)
            }
        }

        binding.schoolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSchoolId = schools[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupGendersSpinner(genders: List<Gender>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            genders.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinner.adapter = adapter

        if (selectedGenderId != null) {
            val position = genders.indexOfFirst { it.id == selectedGenderId }
            if (position >= 0) {
                binding.genderSpinner.setSelection(position)
            }
        }

        binding.genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGenderId = genders[position].id
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

        // Check if any changes were made
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
                viewModel.deleteItem(itemId)  // Changed from args.itemId to itemId
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