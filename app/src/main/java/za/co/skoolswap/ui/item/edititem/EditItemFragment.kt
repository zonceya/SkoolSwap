package za.co.skoolswap.ui.item.edititem

import android.Manifest
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
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.FragmentEditItemBinding
import za.co.skoolswap.domain.model.EditImage
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.reference.*
import za.co.skoolswap.ui.component.ColorPickerBottomSheet
import za.co.skoolswap.ui.component.OptionsPickerBottomSheet
import za.co.skoolswap.utils.DialogAction
import za.co.skoolswap.utils.DialogHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Private constants - internal to this file only
private const val MAX_IMAGES = 3
private const val QUANTITY_MIN = 1
private const val QUANTITY_MAX = 5
private const val IMAGE_COMPRESS_QUALITY = 90
private const val IMAGE_FILENAME_PREFIX = "IMG_"
private const val IMAGE_FILE_EXTENSION = ".jpg"
private const val IMAGE_DATE_FORMAT = "yyyyMMdd_HHmmss"

@AndroidEntryPoint
class EditItemFragment : Fragment() {

    private var _binding: FragmentEditItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditItemViewModel by viewModels()
    private var itemId: String = ""

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
        Timber.tag(LogTags.UI).d("📸 Camera callback")
        isCameraLaunched = false
        if (bitmap != null) {
            lifecycleScope.launch {
                val uri = saveBitmapToFile(bitmap)
                if (uri != null) {
                    val position = pendingImagePosition ?: return@launch
                    val isReplace = pendingIsReplace ?: false
                    if (isReplace) {
                        viewModel.replaceImage(uri, position)
                        Toast.makeText(requireContext(), getString(R.string.edit_item_image_replaced), Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addImage(uri, position)
                        Toast.makeText(requireContext(), getString(R.string.edit_item_image_added), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), getString(R.string.edit_item_image_replaced), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addImage(uri, position)
                    Toast.makeText(requireContext(), getString(R.string.edit_item_image_added), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), getString(R.string.edit_item_camera_permission_required), Toast.LENGTH_SHORT).show()
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

        setupStrings()
        itemId = arguments?.getString("itemId") ?: ""
        Timber.tag(LogTags.FRAGMENT).d("onViewCreated - Item ID: $itemId")
        hideFab()
        showAllShimmers()
        setupObservers()
        setupClickListeners()
        setupImageGrid()
        setupQuantitySpinner()

        if (itemId.isNotEmpty()) {
            viewModel.loadItem(itemId)
        } else {
            Toast.makeText(requireContext(), getString(R.string.edit_item_error_no_id), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupStrings() {
        binding.updateButton.text = getString(R.string.edit_item_update_button)
        binding.deleteItemButton.text = getString(R.string.edit_item_delete_button)
        binding.itemName.hint = getString(R.string.create_item_name_hint)
        binding.description.hint = getString(R.string.create_item_description_hint)
        binding.price.hint = getString(R.string.create_item_price_hint)
        binding.mainCategoryInput.hint = getString(R.string.create_item_select_category)
        binding.subCategoryInput.hint = getString(R.string.create_item_select_subcategory)
        binding.conditionInput.hint = getString(R.string.create_item_select_condition)
        binding.sizeInput.hint = getString(R.string.create_item_select_size)
        binding.brandInput.hint = getString(R.string.create_item_select_brand)
        binding.colorInput.hint = getString(R.string.create_item_select_color)
        binding.provinceInput.hint = getString(R.string.create_item_select_province)
        binding.townInput.hint = getString(R.string.create_item_select_town)
        binding.genderInput.hint = getString(R.string.create_item_select_gender)
        binding.schoolInput.hint = getString(R.string.create_item_select_school)
        binding.imagesCount.text = getString(R.string.edit_item_images_count, 0)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isReferenceDataLoaded.collect { loaded ->
                    Timber.tag(LogTags.UI).d("📌 Reference data loaded: $loaded")
                    if (loaded) {
                        checkAndHideShimmers()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.item.collect { item ->
                    item?.let {
                        Timber.tag(LogTags.UI).d("📦 Item received: ${item.name}")
                        originalItem = it
                        populateItemData(it)
                        restoreSelectionTexts()
                        checkAndHideShimmers()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainCategories.collect { categories ->
                    Timber.tag(LogTags.UI).d("📦 MAIN CATEGORIES received: ${categories.size}")
                    setupMainCategoryPicker(categories)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    Timber.tag(LogTags.UI).d("📦 SUBCATEGORIES received: ${subCategories.size}")
                    setupSubCategoryPicker(subCategories)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditions.collect { conditions ->
                    setupConditionPicker(conditions)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sizes.collect { sizes ->
                    setupSizePicker(sizes)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.brands.collect { brands ->
                    setupBrandPicker(brands)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colors.collect { colors ->
                    setupColorPicker(colors)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.provinces.collect { provinces ->
                    setupProvincePicker(provinces)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.towns.collect { towns ->
                    Timber.tag(LogTags.UI).d("📦 TOWNS received: ${towns.size}")
                    setupTownPicker(towns)

                    if (selectedTownId != null && binding.townInput.text.isNullOrEmpty()) {
                        val town = towns.find { it.id == selectedTownId }
                        if (town != null) {
                            binding.townInput.setText(town.name)
                            Timber.tag(LogTags.UI).d("✅ Restored town after towns loaded: ${town.name} (ID: $selectedTownId)")
                        } else {
                            Timber.tag(LogTags.UI).w("⚠️ Town not found for ID: $selectedTownId, available towns: ${towns.map { it.id }}")
                        }
                    }
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.schools.collect { schools ->
                    setupSchoolPicker(schools)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genders.collect { genders ->
                    setupGenderPicker(genders)
                    restoreSelectionTexts()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.tag(LogTags.UI).d("UI State: $uiState")
                    when (uiState) {
                        is EditItemUiState.Success -> {
                            Timber.tag(LogTags.UI).d("✅ Success: ${uiState.message}")
                            hideLoading()
                            showSuccess(uiState.message)
                            navigateBack()
                        }
                        is EditItemUiState.Error -> {
                            Timber.tag(LogTags.UI).e("❌ Error: ${uiState.message}")
                            hideLoading()
                            showError(uiState.message)
                        }
                        is EditItemUiState.Loading -> {
                            Timber.tag(LogTags.UI).d("Loading state")
                            showLoading()
                        }
                        is EditItemUiState.Idle -> {
                            Timber.tag(LogTags.UI).d("Idle state")
                            hideLoading()
                        }
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

    // ============ PICKER METHODS ============

    private fun setupMainCategoryPicker(categories: List<MainCategory>) {
        if (categories.isEmpty()) {
            binding.mainCategoryInput.isEnabled = false
            return
        }
        binding.mainCategoryInput.isEnabled = true

        binding.mainCategoryInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_category_title),
                options = categories.map { it.name }
            ) { selectedName, position ->
                val selectedCategory = categories[position]
                selectedMainCategoryId = selectedCategory.id
                binding.mainCategoryInput.setText(selectedName)
                viewModel.onMainCategorySelected(selectedCategory.id)
                Timber.tag(LogTags.UI).d("Selected main category: ${selectedCategory.name}")
            }.show(childFragmentManager, "category_picker")
        }
    }

    private fun setupSubCategoryPicker(subCategories: List<SubCategory>) {
        if (subCategories.isEmpty()) {
            binding.subCategoryInput.isEnabled = false
            return
        }
        binding.subCategoryInput.isEnabled = true

        binding.subCategoryInput.setOnClickListener {
            if (selectedMainCategoryId == null) {
                Toast.makeText(requireContext(), getString(R.string.create_item_select_category_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_subcategory_title),
                options = subCategories.map { it.name }
            ) { selectedName, position ->
                val selected = subCategories[position]
                selectedSubCategoryId = selected.id
                binding.subCategoryInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected subcategory: ${selected.name}")
            }.show(childFragmentManager, "subcategory_picker")
        }
    }

    private fun setupConditionPicker(conditions: List<Condition>) {
        if (conditions.isEmpty()) {
            binding.conditionInput.isEnabled = false
            return
        }
        binding.conditionInput.isEnabled = true

        binding.conditionInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_condition_title),
                options = conditions.map { it.name }
            ) { selectedName, position ->
                selectedConditionId = conditions[position].id
                binding.conditionInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected condition: $selectedName")
            }.show(childFragmentManager, "condition_picker")
        }
    }

    private fun setupSizePicker(sizes: List<Size>) {
        if (sizes.isEmpty()) {
            binding.sizeInput.isEnabled = false
            return
        }
        binding.sizeInput.isEnabled = true

        binding.sizeInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_size_title),
                options = sizes.map { it.name }
            ) { selectedName, position ->
                selectedSizeId = sizes[position].id
                binding.sizeInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected size: $selectedName")
            }.show(childFragmentManager, "size_picker")
        }
    }

    private fun setupBrandPicker(brands: List<Brand>) {
        if (brands.isEmpty()) {
            binding.brandInput.isEnabled = false
            return
        }
        binding.brandInput.isEnabled = true

        binding.brandInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_brand_title),
                options = brands.map { it.name }
            ) { selectedName, position ->
                selectedBrandId = brands[position].id
                binding.brandInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected brand: $selectedName")
            }.show(childFragmentManager, "brand_picker")
        }
    }

    private fun setupColorPicker(colors: List<Color>) {
        if (colors.isEmpty()) {
            binding.colorInput.isEnabled = false
            return
        }
        binding.colorInput.isEnabled = true

        binding.colorInput.setOnClickListener {
            ColorPickerBottomSheet(colors) { selectedColor, position ->
                selectedColorId = selectedColor.id
                binding.colorInput.setText(selectedColor.name)
                Timber.tag(LogTags.UI).d("Selected color: ${selectedColor.name}")
            }.show(childFragmentManager, "color_picker")
        }
    }

    private fun setupProvincePicker(provinces: List<Province>) {
        if (provinces.isEmpty()) {
            binding.provinceInput.isEnabled = false
            return
        }
        binding.provinceInput.isEnabled = true

        binding.provinceInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_province_title),
                options = provinces.map { it.name }
            ) { selectedName, position ->
                val selectedProvince = provinces[position]
                selectedProvinceId = selectedProvince.id
                binding.provinceInput.setText(selectedName)
                viewModel.onProvinceSelected(selectedProvince.id)
                Timber.tag(LogTags.UI).d("Selected province: ${selectedProvince.name}")
            }.show(childFragmentManager, "province_picker")
        }
    }

    private fun setupTownPicker(towns: List<Town>) {
        if (towns.isEmpty()) {
            binding.townInput.isEnabled = false
            return
        }
        binding.townInput.isEnabled = true

        binding.townInput.setOnClickListener {
            if (selectedProvinceId == null) {
                Toast.makeText(requireContext(), getString(R.string.create_item_select_province_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_town_title),
                options = towns.map { it.name }
            ) { selectedName, position ->
                selectedTownId = towns[position].id
                binding.townInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected town: $selectedName")
            }.show(childFragmentManager, "town_picker")
        }
    }

    private fun setupGenderPicker(genders: List<Gender>) {
        if (genders.isEmpty()) {
            binding.genderInput.isEnabled = false
            return
        }
        binding.genderInput.isEnabled = true

        binding.genderInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_gender_title),
                options = genders.map { it.name }
            ) { selectedName, position ->
                selectedGenderId = genders[position].id
                binding.genderInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected gender: $selectedName")
            }.show(childFragmentManager, "gender_picker")
        }
    }

    private fun setupSchoolPicker(schools: List<School>) {
        if (schools.isEmpty()) {
            binding.schoolInput.isEnabled = false
            return
        }
        binding.schoolInput.isEnabled = true

        binding.schoolInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.edit_item_select_school_title),
                options = schools.map { it.name }
            ) { selectedName, position ->
                selectedSchoolId = schools[position].id
                binding.schoolInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected school: $selectedName")
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
                    itemImage.alpha = if (editImage.isMarkedForDeletion) 0.5f else 1.0f

                    Glide.with(requireContext())
                        .load(editImage.url)
                        .placeholder(R.drawable.ic_create_item_placeholder)
                        .error(R.drawable.ic_create_item_placeholder)
                        .centerCrop()
                        .into(itemImage)

                    itemImage.setOnClickListener { showImageSourceOptions(index, isReplace = true) }
                    deleteButton.setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.edit_item_remove_image_title))
                            .setMessage(getString(R.string.edit_item_remove_image_message))
                            .setPositiveButton(getString(R.string.edit_item_remove)) { _, _ -> viewModel.removeImage(index) }
                            .setNegativeButton(getString(R.string.edit_item_cancel), null)
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
                    itemImage.setOnClickListener { showImageSourceOptions(index, isReplace = true) }
                    deleteButton.setOnClickListener { viewModel.removeImage(index) }
                }
                EditImage.Empty -> {
                    addButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.GONE
                    itemImage.setImageResource(R.drawable.ic_create_item_placeholder)
                    addButton.setOnClickListener { showImageSourceOptions(index, isReplace = false) }
                }
            }
            binding.imageGrid.addView(imageView)
        }
    }

    private fun updateImageCountText() {
        val images = viewModel.images.value
        val imageCount = images.count { it !is EditImage.Empty }
        binding.imagesCount.text = getString(R.string.edit_item_images_count, imageCount)
    }

    private fun showImageSourceOptions(position: Int, isReplace: Boolean) {
        pendingImagePosition = position
        pendingIsReplace = isReplace

        AlertDialog.Builder(requireContext())
            .setTitle(if (isReplace) getString(R.string.edit_item_replace_image) else getString(R.string.create_item_add_photo))
            .setItems(
                arrayOf(
                    getString(R.string.create_item_take_photo),
                    getString(R.string.create_item_choose_gallery),
                    getString(R.string.create_item_cancel)
                )
            ) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> launchGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    launchCameraSafely()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.create_item_no_camera), Toast.LENGTH_SHORT).show()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showCameraPermissionExplanation()
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showCameraPermissionExplanation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.create_item_camera_permission_title))
            .setMessage(getString(R.string.create_item_camera_permission_message))
            .setPositiveButton(getString(R.string.create_item_camera_permission_allow)) { _, _ ->
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(getString(R.string.create_item_cancel), null)
            .show()
    }

    private fun launchCameraSafely() {
        if (!isCameraLaunched) {
            isCameraLaunched = true
            try {
                simpleCameraLauncher.launch(null)
            } catch (e: Exception) {
                isCameraLaunched = false
                Toast.makeText(requireContext(), getString(R.string.create_item_camera_failed) + ": ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Failed to launch gallery")
            Toast.makeText(requireContext(), getString(R.string.create_item_gallery_access_issue), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        return try {
            val timeStamp = SimpleDateFormat(IMAGE_DATE_FORMAT, Locale.getDefault()).format(Date())
            val filename = "$IMAGE_FILENAME_PREFIX$timeStamp$IMAGE_FILE_EXTENSION"
            val file = File(requireContext().cacheDir, filename)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESS_QUALITY, out)
            }
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Failed to save bitmap to file")
            null
        }
    }

    private fun populateItemData(item: Item) {
        Timber.tag(LogTags.UI).d("=== POPULATING ITEM DATA ===")
        Timber.tag(LogTags.UI).d("Item: ${item.name}, Price: ${item.price}")

        originalName = item.name
        originalDescription = item.description
        originalPrice = item.price
        originalQuantity = item.quantity

        binding.itemName.setText(item.name)
        binding.description.setText(item.description)
        binding.price.setText(item.price.toString())

        selectedQuantity = item.quantity
        if (selectedQuantity in QUANTITY_MIN..QUANTITY_MAX) {
            binding.quantitySpinner.setSelection(selectedQuantity - 1)
        }

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

        Timber.tag(LogTags.UI).d("📍 Selected Town ID from locationId: $selectedTownId")
        Timber.tag(LogTags.UI).d("Selected IDs - MainCat: $selectedMainCategoryId, SubCat: $selectedSubCategoryId, Size: $selectedSizeId")

        selectedMainCategoryId?.let { viewModel.onMainCategorySelected(it) }
        selectedProvinceId?.let { viewModel.onProvinceSelected(it) }

        restoreSelectionTexts()

        val existingImages = item.images.map { image ->
            EditImage.Existing(image.id, image.url)
        }
        viewModel.setExistingImages(existingImages)
    }

    private fun restoreSelectionTexts() {
        Timber.tag(LogTags.UI).d("restoreSelectionTexts called - MainCat: $selectedMainCategoryId, SubCat: $selectedSubCategoryId")

        selectedMainCategoryId?.let { id ->
            if (binding.mainCategoryInput.text.isNullOrEmpty()) {
                viewModel.mainCategories.value.find { it.id == id }?.let {
                    binding.mainCategoryInput.setText(it.name)
                    Timber.tag(LogTags.UI).d("Restored main category: ${it.name}")
                }
            }
        }

        selectedSubCategoryId?.let { id ->
            if (binding.subCategoryInput.text.isNullOrEmpty()) {
                viewModel.subCategories.value.find { it.id == id }?.let {
                    binding.subCategoryInput.setText(it.name)
                    Timber.tag(LogTags.UI).d("Restored subcategory: ${it.name}")
                }
            }
        }

        selectedConditionId?.let { id ->
            if (binding.conditionInput.text.isNullOrEmpty()) {
                viewModel.conditions.value.find { it.id == id }?.let {
                    binding.conditionInput.setText(it.name)
                }
            }
        }

        selectedSizeId?.let { id ->
            if (binding.sizeInput.text.isNullOrEmpty()) {
                viewModel.sizes.value.find { it.id == id }?.let {
                    binding.sizeInput.setText(it.name)
                    Timber.tag(LogTags.UI).d("Restored size: ${it.name}")
                }
            }
        }

        selectedBrandId?.let { id ->
            if (binding.brandInput.text.isNullOrEmpty()) {
                viewModel.brands.value.find { it.id == id }?.let {
                    binding.brandInput.setText(it.name)
                }
            }
        }

        selectedColorId?.let { id ->
            if (binding.colorInput.text.isNullOrEmpty()) {
                viewModel.colors.value.find { it.id == id }?.let {
                    binding.colorInput.setText(it.name)
                }
            }
        }

        selectedProvinceId?.let { id ->
            if (binding.provinceInput.text.isNullOrEmpty()) {
                viewModel.provinces.value.find { it.id == id }?.let {
                    binding.provinceInput.setText(it.name)
                }
            }
        }

        selectedTownId?.let { id ->
            if (binding.townInput.text.isNullOrEmpty()) {
                val town = viewModel.towns.value.find { it.id == id }
                if (town != null) {
                    binding.townInput.setText(town.name)
                    Timber.tag(LogTags.UI).d("✅ Restored town: ${town.name} (ID: $id)")
                } else {
                    Timber.tag(LogTags.UI).w("⚠️ Town not found for ID: $id")
                }
            }
        }

        selectedSchoolId?.let { id ->
            if (binding.schoolInput.text.isNullOrEmpty()) {
                viewModel.schools.value.find { it.id == id }?.let {
                    binding.schoolInput.setText(it.name)
                }
            }
        }

        selectedGenderId?.let { id ->
            if (binding.genderInput.text.isNullOrEmpty()) {
                viewModel.genders.value.find { it.id == id }?.let {
                    binding.genderInput.setText(it.name)
                }
            }
        }
    }

    private fun setupQuantitySpinner() {
        val quantities = (QUANTITY_MIN..QUANTITY_MAX).map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, quantities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.quantitySpinner.adapter = adapter

        if (selectedQuantity in QUANTITY_MIN..QUANTITY_MAX) {
            binding.quantitySpinner.setSelection(selectedQuantity - 1)
        }

        binding.quantitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedQuantity = position + 1
                Timber.tag(LogTags.UI).d("Quantity selected: $selectedQuantity")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupClickListeners() {
        binding.updateButton.setOnClickListener {
            if (validateForm()) {
                showUpdateConfirmationDialog()
            }
        }

        binding.deleteItemButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showUpdateConfirmationDialog() {
        val name = binding.itemName.text.toString()
        val description = binding.description.text.toString()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        // Get the actual selected names for better display
        val newCategoryName = viewModel.mainCategories.value.find { it.id == selectedMainCategoryId }?.name ?: "Unknown"
        val originalCategoryName = originalItem?.mainCategoryId?.let { id ->
            viewModel.mainCategories.value.find { it.id == id }?.name
        } ?: "Unknown"

        val newSubCategoryName = viewModel.subCategories.value.find { it.id == selectedSubCategoryId }?.name ?: "Unknown"
        val originalSubCategoryName = originalItem?.subCategoryId?.let { id ->
            viewModel.subCategories.value.find { it.id == id }?.name
        } ?: "Unknown"

        val newBrandName = viewModel.brands.value.find { it.id == selectedBrandId }?.name ?: "None"
        val originalBrandName = originalItem?.brandId?.let { id ->
            viewModel.brands.value.find { it.id == id }?.name
        } ?: "None"

        val newSizeName = viewModel.sizes.value.find { it.id == selectedSizeId }?.name ?: "None"
        val originalSizeName = originalItem?.sizeId?.let { id ->
            viewModel.sizes.value.find { it.id == id }?.name
        } ?: "None"

        val newColorName = viewModel.colors.value.find { it.id == selectedColorId }?.name ?: "None"
        val originalColorName = originalItem?.colorId?.let { id ->
            viewModel.colors.value.find { it.id == id }?.name
        } ?: "None"

        val newConditionName = viewModel.conditions.value.find { it.id == selectedConditionId }?.name ?: "None"
        val originalConditionName = originalItem?.itemConditionId?.let { id ->
            viewModel.conditions.value.find { it.id == id }?.name
        } ?: "None"

        val newProvinceName = viewModel.provinces.value.find { it.id == selectedProvinceId }?.name ?: "None"
        val originalProvinceName = originalItem?.provinceId?.let { id ->
            viewModel.provinces.value.find { it.id == id }?.name
        } ?: "None"

        val newTownName = viewModel.towns.value.find { it.id == selectedTownId }?.name ?: "None"
        val originalTownName = originalItem?.locationId?.let { id ->
            viewModel.towns.value.find { it.id == id }?.name
        } ?: "None"

        val newSchoolName = viewModel.schools.value.find { it.id == selectedSchoolId }?.name ?: "None"
        val originalSchoolName = originalItem?.schoolId?.let { id ->
            viewModel.schools.value.find { it.id == id }?.name
        } ?: "None"

        val newGenderName = viewModel.genders.value.find { it.id == selectedGenderId }?.name ?: "None"
        val originalGenderName = originalItem?.genderId?.let { id ->
            viewModel.genders.value.find { it.id == id }?.name
        } ?: "None"

        // Build changes summary
        val changes = mutableListOf<String>()

        if (price != originalPrice) {
            changes.add("• Price: R${String.format("%.2f", originalPrice)} → R${String.format("%.2f", price)}")
        }

        if (name != originalName) changes.add("• Name: \"$originalName\" → \"$name\"")
        if (description != originalDescription) changes.add("• Description changed")
        if (selectedQuantity != originalQuantity) changes.add("• Quantity: $originalQuantity → $selectedQuantity")

        if (selectedMainCategoryId != originalItem?.mainCategoryId) {
            changes.add("• Category: $originalCategoryName → $newCategoryName")
        }

        if (selectedSubCategoryId != originalItem?.subCategoryId) {
            changes.add("• Subcategory: $originalSubCategoryName → $newSubCategoryName")
        }

        if (selectedBrandId != originalItem?.brandId) {
            changes.add("• Brand: $originalBrandName → $newBrandName")
        }

        if (selectedSizeId != originalItem?.sizeId) {
            changes.add("• Size: $originalSizeName → $newSizeName")
        }

        if (selectedColorId != originalItem?.colorId) {
            changes.add("• Color: $originalColorName → $newColorName")
        }

        if (selectedConditionId != originalItem?.itemConditionId) {
            changes.add("• Condition: $originalConditionName → $newConditionName")
        }

        if (selectedProvinceId != originalItem?.provinceId) {
            changes.add("• Province: $originalProvinceName → $newProvinceName")
        }

        if (selectedTownId != originalItem?.locationId) {
            changes.add("• Town: $originalTownName → $newTownName")
        }

        if (selectedSchoolId != originalItem?.schoolId) {
            changes.add("• School: $originalSchoolName → $newSchoolName")
        }

        if (selectedGenderId != originalItem?.genderId) {
            changes.add("• Gender: $originalGenderName → $newGenderName")
        }

        if (viewModel.getDeletionIds().isNotEmpty()) {
            changes.add("• ${viewModel.getDeletionIds().size} image(s) removed")
        }

        if (viewModel.getImagesForUpload().isNotEmpty()) {
            changes.add("• ${viewModel.getImagesForUpload().size} new image(s) added")
        }

        val changesSummary = if (changes.isEmpty()) {
            getString(R.string.edit_item_no_changes)
        } else {
            changes.joinToString("\n")
        }

        if (changes.isNotEmpty()) {
            DialogHelper.showConfirmationDialog(
                context = requireContext(),
                action = DialogAction.SaveChanges(changesSummary),
                onConfirm = {
                    Timber.tag(LogTags.UI).d("User confirmed update")
                    updateItem()
                }
            )
        } else {
            Toast.makeText(requireContext(), getString(R.string.edit_item_no_changes), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        DialogHelper.showConfirmationDialog(
            context = requireContext(),
            action = DialogAction.DeleteItem,
            onConfirm = {
                Timber.tag(LogTags.UI).d("User confirmed delete for item: $itemId")
                showLoading()
                viewModel.deleteItem(itemId)
            }
        )
    }

    private fun validateForm(): Boolean {
        if (binding.itemName.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.create_item_name_required), Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.description.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.create_item_description_required), Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.price.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.create_item_price_required), Toast.LENGTH_SHORT).show()
            return false
        }
        val price = binding.price.text.toString().toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(requireContext(), getString(R.string.create_item_price_required), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun updateItem() {
        val name = binding.itemName.text.toString()
        val description = binding.description.text.toString()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        viewModel.updateItemOfflineFirst(
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

    private fun showAllShimmers() {
        binding.mainCategoryShimmer.visibility = View.VISIBLE
        binding.mainCategoryInputLayout.visibility = View.GONE
        binding.mainCategoryShimmer.startShimmer()

        binding.subCategoryShimmer.visibility = View.VISIBLE
        binding.subCategoryInputLayout.visibility = View.GONE
        binding.subCategoryShimmer.startShimmer()

        binding.conditionShimmer.visibility = View.VISIBLE
        binding.conditionInputLayout.visibility = View.GONE
        binding.conditionShimmer.startShimmer()

        binding.sizeShimmer.visibility = View.VISIBLE
        binding.sizeInputLayout.visibility = View.GONE
        binding.sizeShimmer.startShimmer()

        binding.brandShimmer.visibility = View.VISIBLE
        binding.brandInputLayout.visibility = View.GONE
        binding.brandShimmer.startShimmer()

        binding.colorShimmer.visibility = View.VISIBLE
        binding.colorInputLayout.visibility = View.GONE
        binding.colorShimmer.startShimmer()

        binding.provinceShimmer.visibility = View.VISIBLE
        binding.provinceInputLayout.visibility = View.GONE
        binding.provinceShimmer.startShimmer()

        binding.townShimmer.visibility = View.VISIBLE
        binding.townInputLayout.visibility = View.GONE
        binding.townShimmer.startShimmer()

        binding.genderShimmer.visibility = View.VISIBLE
        binding.genderInputLayout.visibility = View.GONE
        binding.genderShimmer.startShimmer()

        binding.schoolShimmer.visibility = View.VISIBLE
        binding.schoolInputLayout.visibility = View.GONE
        binding.schoolShimmer.startShimmer()
    }

    private fun hideAllShimmers() {
        binding.mainCategoryShimmer.visibility = View.GONE
        binding.mainCategoryInputLayout.visibility = View.VISIBLE
        binding.mainCategoryShimmer.stopShimmer()

        binding.subCategoryShimmer.visibility = View.GONE
        binding.subCategoryInputLayout.visibility = View.VISIBLE
        binding.subCategoryShimmer.stopShimmer()

        binding.conditionShimmer.visibility = View.GONE
        binding.conditionInputLayout.visibility = View.VISIBLE
        binding.conditionShimmer.stopShimmer()

        binding.sizeShimmer.visibility = View.GONE
        binding.sizeInputLayout.visibility = View.VISIBLE
        binding.sizeShimmer.stopShimmer()

        binding.brandShimmer.visibility = View.GONE
        binding.brandInputLayout.visibility = View.VISIBLE
        binding.brandShimmer.stopShimmer()

        binding.colorShimmer.visibility = View.GONE
        binding.colorInputLayout.visibility = View.VISIBLE
        binding.colorShimmer.stopShimmer()

        binding.provinceShimmer.visibility = View.GONE
        binding.provinceInputLayout.visibility = View.VISIBLE
        binding.provinceShimmer.stopShimmer()

        binding.townShimmer.visibility = View.GONE
        binding.townInputLayout.visibility = View.VISIBLE
        binding.townShimmer.stopShimmer()

        binding.genderShimmer.visibility = View.GONE
        binding.genderInputLayout.visibility = View.VISIBLE
        binding.genderShimmer.stopShimmer()

        binding.schoolShimmer.visibility = View.GONE
        binding.schoolInputLayout.visibility = View.VISIBLE
        binding.schoolShimmer.stopShimmer()
    }

    private fun showLoading() {
        binding.updateButton.isEnabled = false
        binding.deleteItemButton.isEnabled = false
        binding.updateButton.text = getString(R.string.edit_item_updating)
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.updateButton.isEnabled = true
        binding.deleteItemButton.isEnabled = true
        binding.updateButton.text = getString(R.string.edit_item_update_button)
        binding.progressBar.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, getString(R.string.edit_item_error_prefix) + ": $message", Snackbar.LENGTH_LONG).show()
    }

    private fun navigateBack() {
        Timber.tag(LogTags.UI).d("Navigating back after success")
        findNavController().previousBackStackEntry?.savedStateHandle?.apply {
            set("item_updated", true)
            if (originalItem?.status != "sold") {
                set("updated_item_id", itemId)
            } else {
                set("deleted_item_id", itemId)
            }
        }
        findNavController().navigateUp()
    }

    private fun checkAndHideShimmers() {
        val referenceLoaded = viewModel.isReferenceDataLoaded.value
        val itemLoaded = viewModel.item.value != null

        Timber.tag(LogTags.UI).d("checkAndHideShimmers - referenceLoaded: $referenceLoaded, itemLoaded: $itemLoaded")

        if (referenceLoaded && itemLoaded) {
            hideAllShimmers()
            Timber.tag(LogTags.UI).d("✅ All data loaded, shimmer hidden")
        }
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
        Timber.tag(LogTags.UI).d("FAB hidden")
    }

    override fun onResume() {
        super.onResume()
        isCameraLaunched = false
        hideFab()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}