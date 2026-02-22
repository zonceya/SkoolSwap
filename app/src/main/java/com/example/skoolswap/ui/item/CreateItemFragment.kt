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
import com.example.skoolswap.databinding.FragmentCreateItemBinding
import com.example.skoolswap.domain.model.reference.*
import com.example.skoolswap.utils.ColorUtils
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

    // Simple camera launcher without FileProvider complexity
    private val simpleCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        isCameraLaunched = false
        if (bitmap != null) {
            lifecycleScope.launch {
                if (viewModel.images.value.size < 3) {
                    // Convert bitmap to URI and add
                    val uri = saveBitmapToFile(bitmap)
                    if (uri != null) {
                        viewModel.addImage(uri)
                        // Use requireContext() to ensure toast shows on current fragment
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

        // Restore any pending state
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
                    Log.d("CreateItemFragment", "📦 MAIN CATEGORIES received in fragment: ${categories.size}")
                    showSubcategoryLoading(false)
                    setupMainCategorySpinner(categories)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        showSubcategoryLoading(true)
                    }
                }
            }
        }

        // Collect Sub Categories
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    Log.d("CreateItemFragment", "📦 SUBCATEGORIES received in fragment: ${subCategories.size}")
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

    private fun setupMainCategorySpinner(categories: List<MainCategory>) {
        if (categories.isEmpty()) {
            binding.mainCategorySpinner.visibility = View.GONE
            binding.mainCategoryLabel.visibility = View.GONE
            return
        }

        binding.mainCategorySpinner.visibility = View.VISIBLE
        binding.mainCategoryLabel.visibility = View.VISIBLE

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mainCategorySpinner.adapter = adapter

        // Restore selected category if exists
        if (selectedMainCategoryId != null) {
            val position = categories.indexOfFirst { it.id == selectedMainCategoryId }
            if (position >= 0) {
                binding.mainCategorySpinner.setSelection(position)
            }
        }

        binding.mainCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                selectedMainCategoryId = selectedCategory.id
                viewModel.onMainCategorySelected(selectedCategory.id)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showSubcategoryLoading(show: Boolean) {
        if (show) {
            binding.subcategoryLoading.visibility = View.VISIBLE
            binding.subCategorySpinner.visibility = View.GONE
        } else {
            binding.subcategoryLoading.visibility = View.GONE
            binding.subCategorySpinner.visibility = View.VISIBLE
        }
    }

    private fun setupSubCategorySpinner(subCategories: List<SubCategory>) {
        Log.d("CreateItemFragment", "Setting up subcategory spinner with ${subCategories.size} items")

        if (subCategories.isEmpty()) {
            binding.subCategorySpinner.isEnabled = false
            binding.subCategoryLabel.visibility = View.VISIBLE
            return
        }

        binding.subCategorySpinner.isEnabled = true
        binding.subCategorySpinner.visibility = View.VISIBLE
        binding.subCategoryLabel.visibility = View.VISIBLE

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subCategories.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.subCategorySpinner.adapter = adapter

        // Restore selected subcategory if exists
        if (selectedSubCategoryId != null) {
            val position = subCategories.indexOfFirst { it.id == selectedSubCategoryId }
            if (position >= 0) {
                binding.subCategorySpinner.setSelection(position)
            }
        }

        binding.subCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = subCategories[position]
                selectedSubCategoryId = selected.id
                Log.d("CreateItemFragment", "Selected subcategory: ${selected.name} (ID: ${selected.id})")
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

        // Restore selected condition if exists
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

        // Restore selected size if exists
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

        // Restore selected brand if exists
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

    private fun setupColorsSpinner(colors: List<com.example.skoolswap.domain.model.reference.Color>) {
        Log.d("CreateItemFragment", "Setting up colors spinner with ${colors.size} colors")

        if (colors.isEmpty()) {
            binding.colorSpinner.isEnabled = false
            return
        }

        // Custom adapter with color preview using ColorUtils
        class ColorAdapter(context: Context, private val colorItems: List<com.example.skoolswap.domain.model.reference.Color>) :
            ArrayAdapter<com.example.skoolswap.domain.model.reference.Color>(context, 0, colorItems) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_color_spinner, parent, false)

                val colorItem = colorItems[position]

                // Set color name
                view.findViewById<TextView>(R.id.colorName).text = colorItem.name

                // Set color preview using hex from ColorUtils
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

        // Restore selected color if exists
        if (selectedColorId != null) {
            val position = colors.indexOfFirst { it.id == selectedColorId }
            if (position >= 0) {
                binding.colorSpinner.setSelection(position)
            }
        }

        // Set selection listener
        binding.colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedColorId = colors[position].id
                Log.d("CreateItemFragment", "Selected color: ${colors[position].name}")
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

        // Restore selected province if exists
        if (selectedProvinceId != null) {
            val position = provinces.indexOfFirst { it.id == selectedProvinceId }
            if (position >= 0) {
                binding.provinceSpinner.setSelection(position)
            }
        }

        binding.provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedProvince = provinces[position]
                selectedProvinceId = selectedProvince.id
                viewModel.onProvinceSelected(selectedProvince.id)
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

        // Restore selected town if exists
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

        // Restore selected school if exists
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

        // Restore selected gender if exists
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

    private fun setupDefaultSelections() {
        // Quantity Spinner
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
                selectedQuantity = (position + 1)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
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
            .setMessage(
                "There's a temporary issue accessing your gallery.\n\n" +
                        "You can use 'Take Photo' to capture new images."
            )
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

            // Save to app's cache directory (no permission needed)
            val file = File(requireContext().cacheDir, filename)
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Use FileProvider for Android 7+
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
            locationId = selectedTownId, // Using town_id as location_id
            provinceId = selectedProvinceId,
            genderId = selectedGenderId,
            colorId = selectedColorId,
            tagIds = null // Optional
        )
    }

    private fun updateImagePreview(uris: List<Uri>) {
        // Update image count
        binding.imagesSubheading.text = "${uris.size}/3 images selected"

        // Reset all delete buttons to GONE
        binding.deleteCover.visibility = View.GONE
        binding.deleteAngle2.visibility = View.GONE
        binding.deleteAngle3.visibility = View.GONE

        // Clear all images first
        binding.coverPhoto.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.differentAngle.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.labelPhoto.setImageResource(R.drawable.ic_create_item_placeholder)

        // Load images and show delete buttons
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
        // Clear image previews
        binding.coverPhoto.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.differentAngle.setImageResource(R.drawable.ic_create_item_placeholder)
        binding.labelPhoto.setImageResource(R.drawable.ic_create_item_placeholder)

        updateImagePreview(emptyList())

        // Reset selections
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
        isCameraLaunched = false // Reset camera flag
    }

    override fun onPause() {
        super.onPause()
        Log.d("CreateItemFragment", "onPause")
    }
}