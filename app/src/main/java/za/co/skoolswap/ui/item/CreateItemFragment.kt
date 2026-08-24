package za.co.skoolswap.ui.item

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.FragmentCreateItemBinding
import za.co.skoolswap.domain.model.reference.*
import za.co.skoolswap.ui.component.ColorPickerBottomSheet
import za.co.skoolswap.ui.component.OptionsPickerBottomSheet
import za.co.skoolswap.ui.component.SearchableOptionsPickerBottomSheet
import za.co.skoolswap.ui.component.TownPickerBottomSheet
import za.co.skoolswap.ui.component.SchoolPickerBottomSheet
import za.co.skoolswap.ui.main.MainActivity
import za.co.skoolswap.ui.main.MainViewModel
import za.co.skoolswap.utils.DialogAction
import za.co.skoolswap.utils.DialogHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateItemFragment : Fragment() {

    companion object {
        private const val TAG = "CreateItemFragment"
        private const val MAX_IMAGES = 3
        private const val QUANTITY_MIN = 1
        private const val QUANTITY_MAX = 5
        private const val NAVIGATION_DELAY_MS = 2000L
        private const val IMAGE_COMPRESS_QUALITY = 90
        private const val IMAGE_FILENAME_PREFIX = "IMG_"
        private const val IMAGE_FILE_EXTENSION = ".jpg"
        private const val IMAGE_DATE_FORMAT = "yyyyMMdd_HHmmss"
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
        (activity as? MainActivity)?.let {
            val mainViewModel: MainViewModel by viewModels()
            mainViewModel.suppressNextResumeRefresh = false
        }
        Timber.tag(LogTags.UI).d("📸 Camera callback received, bitmap = ${if (bitmap != null) "not null" else "null"}")
        isCameraLaunched = false

        if (bitmap != null) {
            Timber.tag(LogTags.UI).d("Bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            lifecycleScope.launch {
                if (viewModel.images.value.size < MAX_IMAGES) {
                    Timber.tag(LogTags.UI).d("Attempting to save bitmap to file...")
                    val uri = saveBitmapToFile(bitmap)
                    if (uri != null) {
                        Timber.tag(LogTags.UI).d("✅ Image saved successfully: $uri")
                        viewModel.addImage(uri)
                        Toast.makeText(requireContext(), getString(R.string.create_item_photo_added), Toast.LENGTH_SHORT).show()
                    } else {
                        Timber.tag(LogTags.UI).e("❌ Failed to save bitmap to file")
                        Toast.makeText(requireContext(), getString(R.string.create_item_photo_failed), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Timber.tag(LogTags.UI).w("Max images reached (${viewModel.images.value.size}/$MAX_IMAGES)")
                    Toast.makeText(requireContext(), getString(R.string.create_item_max_images), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Timber.tag(LogTags.UI).e("❌ Bitmap is null - camera may have been cancelled or failed")
            Toast.makeText(requireContext(), getString(R.string.create_item_camera_failed), Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        Timber.tag(LogTags.UI).d("📷 Gallery callback, uris = ${uris?.size ?: 0} images")
        (activity as? MainActivity)?.let {
            val mainViewModel: MainViewModel by viewModels()
            mainViewModel.suppressNextResumeRefresh = false
        }
        if (uris.isNullOrEmpty()) {
            Timber.tag(LogTags.UI).w("No images selected from gallery")
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            var imagesAdded = 0
            uris.forEach { uri ->
                if (viewModel.images.value.size < MAX_IMAGES) {
                    Timber.tag(LogTags.UI).d("Adding image from gallery: $uri")
                    viewModel.addImage(uri)
                    imagesAdded++
                }
            }
            if (imagesAdded > 0) {
                Timber.tag(LogTags.UI).d("✅ Added $imagesAdded image(s) from gallery")
                Toast.makeText(requireContext(), "$imagesAdded image(s) added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.tag(LogTags.UI).d("Camera permission result: $isGranted")
        if (isGranted) {
            Timber.tag(LogTags.UI).d("Camera permission granted, launching camera")
            launchCameraSafely()
        } else {
            Timber.tag(LogTags.UI).e("Camera permission denied")
            Toast.makeText(requireContext(), getString(R.string.create_item_camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.tag(LogTags.FRAGMENT).d("onCreateView")
        _binding = FragmentCreateItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag(LogTags.FRAGMENT).d("onViewCreated")

        hideFab()
        setupObservers()
        setupClickListeners()
        setupDefaultSelections()
        setupStrings()
        restoreState(savedInstanceState)
        binding.deleteCover.visibility = View.GONE
        binding.deleteAngle2.visibility = View.GONE
        binding.deleteAngle3.visibility = View.GONE

        // ✅ Set initial lock states
        lockTown("Select province first")
        lockSchool("Select town first")
    }

    private fun setupStrings() {
        binding.submitButton.text = getString(R.string.create_item_submit)
        binding.imagesSubheading.text = getString(R.string.create_item_images_subheading, 0)
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
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
        Timber.tag(LogTags.UI).d("FAB hidden")
    }

    private fun setupObservers() {
        Timber.tag(LogTags.UI).d("Setting up observers")

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainCategories.collect { categories ->
                    Timber.tag(LogTags.UI).d("📦 MAIN CATEGORIES received: ${categories.size}")
                    showSubcategoryLoading(false)
                    setupMainCategoryPicker(categories)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    Timber.tag(LogTags.UI).d("Loading state: $isLoading")
                    showSubcategoryLoading(isLoading)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subCategories.collect { subCategories ->
                    Timber.tag(LogTags.UI).d("📦 SUBCATEGORIES received: ${subCategories.size}")
                    setupSubCategoryPicker(subCategories)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditions.collect { conditions ->
                    Timber.tag(LogTags.UI).d("Conditions received: ${conditions.size}")
                    setupConditionPicker(conditions)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sizes.collect { sizes ->
                    Timber.tag(LogTags.UI).d("Sizes received: ${sizes.size}")
                    setupSizePicker(sizes)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.brands.collect { brands ->
                    Timber.tag(LogTags.UI).d("Brands received: ${brands.size}")
                    setupBrandPicker(brands)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colors.collect { colors ->
                    Timber.tag(LogTags.UI).d("Colors received: ${colors.size}")
                    setupColorPicker(colors)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.provinces.collect { provinces ->
                    Timber.tag(LogTags.UI).d("Provinces received: ${provinces.size}")
                    setupProvincePicker(provinces)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.towns.collect { towns ->
                    Timber.tag(LogTags.UI).d("Towns received: ${towns.size}")
                    setupTownPicker(towns)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { uris ->
                    Timber.tag(LogTags.UI).d("📸 Images updated: ${uris.size} images")
                    updateImagePreview(uris)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.schools.collect { schools ->
                    Timber.tag(LogTags.UI).d("Schools received: ${schools.size}")
                    setupSchoolPicker(schools)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genders.collect { genders ->
                    Timber.tag(LogTags.UI).d("Genders received: ${genders.size}")
                    setupGenderPicker(genders)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.tag(LogTags.UI).d("UI State: $uiState")
                    when (uiState) {
                        is CreateItemUiState.Success -> {
                            Timber.tag(LogTags.UI).d("✅ Success state: ${uiState.message}")
                            hideLoading()
                            showSuccess(uiState.message)
                            navigateAfterSuccess()
                        }
                        is CreateItemUiState.Error -> {
                            Timber.tag(LogTags.UI).e("❌ Error state: ${uiState.message}")
                            hideLoading()
                            showError(uiState.message)
                        }
                        is CreateItemUiState.Loading -> {
                            Timber.tag(LogTags.UI).d("Loading state")
                            showLoading()
                        }
                        is CreateItemUiState.Idle -> {
                            Timber.tag(LogTags.UI).d("Idle state")
                            hideLoading()
                        }
                    }
                }
            }
        }
    }

    // ============ LOCK/UNLOCK HELPERS ============

    private fun lockTown(hint: String) {
        binding.townInput.isEnabled = false
        binding.townInput.alpha = 0.5f
        binding.townInput.hint = hint
        binding.townInput.setText("")
        Timber.tag(LogTags.UI).d("🔒 Town locked: $hint")
    }

    private fun unlockTown() {
        binding.townInput.isEnabled = true
        binding.townInput.alpha = 1.0f
        binding.townInput.hint = getString(R.string.create_item_select_town)
        Timber.tag(LogTags.UI).d("🔓 Town unlocked")
    }

    private fun lockSchool(hint: String) {
        binding.schoolInput.isEnabled = false
        binding.schoolInput.alpha = 0.5f
        binding.schoolInput.hint = hint
        binding.schoolInput.setText("")
        Timber.tag(LogTags.UI).d("🔒 School locked: $hint")
    }

    private fun unlockSchool() {
        binding.schoolInput.isEnabled = true
        binding.schoolInput.alpha = 1.0f
        binding.schoolInput.hint = getString(R.string.create_item_select_school)
        Timber.tag(LogTags.UI).d("🔓 School unlocked")
    }

    // ============ BOTTOM SHEET PICKER METHODS ============

    private fun setupMainCategoryPicker(categories: List<MainCategory>) {
        Timber.tag(LogTags.UI).d("🎯 setupMainCategoryPicker called with ${categories.size} categories")

        if (categories.isEmpty()) {
            Timber.tag(LogTags.UI).w("Categories list is empty, hiding picker")
            binding.mainCategoryInput.visibility = View.GONE
            binding.mainCategoryLabel.visibility = View.GONE
            return
        }

        binding.mainCategoryInput.visibility = View.VISIBLE
        binding.mainCategoryLabel.visibility = View.VISIBLE
        binding.mainCategoryInput.setOnClickListener(null)

        selectedMainCategoryId?.let { id ->
            categories.find { it.id == id }?.let {
                binding.mainCategoryInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored selected category: ${it.name}")
            }
        }

        binding.mainCategoryInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_category),
                options = categories.map { it.name }
            ) { selectedName, position ->
                val selectedCategory = categories[position]
                selectedMainCategoryId = selectedCategory.id
                binding.mainCategoryInput.setText(selectedName)
                viewModel.onMainCategorySelected(selectedCategory.id)
                Timber.tag(LogTags.UI).d("✅ MAIN CATEGORY SELECTED → ID: ${selectedCategory.id} | Name: ${selectedCategory.name}")
            }.show(childFragmentManager, "category_picker")
        }

        binding.mainCategoryInput.isEnabled = true
        binding.mainCategoryInput.isClickable = true
        binding.mainCategoryInput.isFocusable = true

        if (binding.mainCategoryInput.text.isNullOrEmpty()) {
            binding.mainCategoryInput.hint = getString(R.string.create_item_select_category)
        }

        Timber.tag(LogTags.UI).d("✅ Main category picker setup complete")
    }

    private fun setupSubCategoryPicker(subCategories: List<SubCategory>) {
        Timber.tag(LogTags.UI).d("Setting up subcategory picker with ${subCategories.size} items")

        if (subCategories.isEmpty()) {
            Timber.tag(LogTags.UI).w("Subcategories empty, disabling picker")
            binding.subCategoryInput.isEnabled = false
            return
        }

        binding.subCategoryInput.isEnabled = true

        selectedSubCategoryId?.let { id ->
            subCategories.find { it.id == id }?.let {
                binding.subCategoryInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored subcategory: ${it.name}")
            }
        }

        binding.subCategoryInput.setOnClickListener {
            if (selectedMainCategoryId == null) {
                Timber.tag(LogTags.UI).w("Subcategory clicked but no main category selected")
                Toast.makeText(requireContext(), getString(R.string.create_item_select_category_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (subCategories.isEmpty()) {
                Toast.makeText(requireContext(), "No subcategories available for this category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Use OptionsPickerBottomSheet (NO search for subcategories)
            OptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_subcategory),
                options = subCategories.map { it.name }
            ) { selectedName, position ->
                val selected = subCategories[position]
                selectedSubCategoryId = selected.id
                binding.subCategoryInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected subcategory: ${selected.name} (ID: ${selected.id})")
            }.show(childFragmentManager, "subcategory_picker")
        }
    }

    private fun setupConditionPicker(conditions: List<Condition>) {
        Timber.tag(LogTags.UI).d("Setting up condition picker with ${conditions.size} items")

        selectedConditionId?.let { id ->
            conditions.find { it.id == id }?.let {
                binding.conditionInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored condition: ${it.name}")
            }
        }

        binding.conditionInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_condition),
                options = conditions.map { it.name }
            ) { selectedName, position ->
                selectedConditionId = conditions[position].id
                binding.conditionInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected condition: $selectedName (ID: $selectedConditionId)")
            }.show(childFragmentManager, "condition_picker")
        }
    }
    private fun setupProvincePicker(provinces: List<Province>) {
        Timber.tag(LogTags.UI).d("Setting up province picker with ${provinces.size} items")

        selectedProvinceId?.let { id ->
            provinces.find { it.id == id }?.let {
                binding.provinceInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored province: ${it.name}")
                // ✅ If province is restored, unlock town
                unlockTown()
            }
        }

        // ✅ Set initial state - lock town if no province selected
        if (selectedProvinceId == null) {
            lockTown("Select province first")
        }

        binding.provinceInput.setOnClickListener {
            // ✅ Use OptionsPickerBottomSheet (NO search)
            OptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_province),
                options = provinces.map { it.name }
            ) { selectedName, position ->
                val selectedProvince = provinces[position]
                selectedProvinceId = selectedProvince.id
                binding.provinceInput.setText(selectedName)

                // ✅ Clear old selections and unlock town
                selectedTownId = null
                binding.townInput.setText("")
                selectedSchoolId = null
                binding.schoolInput.setText("")

                // ✅ Unlock town
                unlockTown()

                // ✅ Lock school until town is selected
                lockSchool("Select town first")

                Timber.tag(LogTags.UI).d("Selected province: ${selectedProvince.name} (ID: ${selectedProvince.id})")
                viewModel.onProvinceSelected(selectedProvince.id)
            }.show(childFragmentManager, "province_picker")
        }
    }
    private fun setupSizePicker(sizes: List<Size>) {
        Timber.tag(LogTags.UI).d("Setting up size picker with ${sizes.size} items")

        selectedSizeId?.let { id ->
            sizes.find { it.id == id }?.let {
                binding.sizeInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored size: ${it.name}")
            }
        }

        binding.sizeInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_size),
                options = sizes.map { it.name }
            ) { selectedName, position ->
                selectedSizeId = sizes[position].id
                binding.sizeInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected size: $selectedName (ID: $selectedSizeId)")
            }.show(childFragmentManager, "size_picker")
        }
    }

    private fun setupBrandPicker(brands: List<Brand>) {
        Timber.tag(LogTags.UI).d("Setting up brand picker with ${brands.size} items")

        selectedBrandId?.let { id ->
            brands.find { it.id == id }?.let {
                binding.brandInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored brand: ${it.name}")
            }
        }

        binding.brandInput.setOnClickListener {
            SearchableOptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_brand),
                options = brands.map { it.name }
            ) { selectedName, position ->
                selectedBrandId = brands[position].id
                binding.brandInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected brand: $selectedName (ID: $selectedBrandId)")
            }.show(childFragmentManager, "brand_picker")
        }
    }

    private fun setupColorPicker(colors: List<Color>) {
        Timber.tag(LogTags.UI).d("Setting up color picker with ${colors.size} items")

        if (colors.isEmpty()) {
            Timber.tag(LogTags.UI).w("Colors empty, disabling picker")
            binding.colorInput.isEnabled = false
            return
        }

        binding.colorInput.isEnabled = true

        selectedColorId?.let { id ->
            colors.find { it.id == id }?.let {
                binding.colorInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored color: ${it.name}")
            }
        }

        binding.colorInput.setOnClickListener {
            ColorPickerBottomSheet(colors) { selectedColor, position ->
                selectedColorId = selectedColor.id
                binding.colorInput.setText(selectedColor.name)
                Timber.tag(LogTags.UI).d("Selected color: ${selectedColor.name} (ID: ${selectedColor.id})")
            }.show(childFragmentManager, "color_picker")
        }
    }



    private fun setupTownPicker(towns: List<Town>) {
        Timber.tag(LogTags.UI).d("Setting up town picker with ${towns.size} towns")

        // ✅ Always show town input, never hide it
        binding.townInput.visibility = View.VISIBLE
        binding.townLabel.visibility = View.VISIBLE

        // ✅ Restore selected town if exists
        selectedTownId?.let { id ->
            towns.find { it.id == id }?.let {
                binding.townInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored town: ${it.name} (ID: ${it.id})")
                unlockSchool()
            }
        }

        // ✅ Set initial state based on province selection
        if (selectedProvinceId == null) {
            binding.townInput.isEnabled = false
            binding.townInput.alpha = 0.5f
            binding.townInput.hint = "Select province first"
            binding.townInput.setText("")
        } else if (towns.isEmpty()) {
            binding.townInput.isEnabled = false
            binding.townInput.alpha = 0.5f
            binding.townInput.hint = "Loading towns..."
            binding.townInput.setText("")
        } else {
            binding.townInput.isEnabled = true
            binding.townInput.alpha = 1.0f
            binding.townInput.hint = "Search towns..."
        }

        binding.townInput.setOnClickListener {
            if (selectedProvinceId == null) {
                Toast.makeText(requireContext(), "Please select a province first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (towns.isEmpty()) {
                Toast.makeText(requireContext(), "Loading towns, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            TownPickerBottomSheet(
                provinceId = selectedProvinceId!!
            ) { selectedTown ->
                selectedTownId = selectedTown.id
                binding.townInput.setText(selectedTown.name)
                Timber.tag(LogTags.UI).d("Selected town: ${selectedTown.name} (ID: ${selectedTown.id})")

                // ✅ Clear school and unlock it
                selectedSchoolId = null
                binding.schoolInput.setText("")
                unlockSchool()
            }.show(childFragmentManager, "town_picker")
        }
    }

    private fun setupGenderPicker(genders: List<Gender>) {
        Timber.tag(LogTags.UI).d("Setting up gender picker with ${genders.size} items")

        selectedGenderId?.let { id ->
            genders.find { it.id == id }?.let {
                binding.genderInput.setText(it.name)
                Timber.tag(LogTags.UI).d("Restored gender: ${it.name}")
            }
        }

        binding.genderInput.setOnClickListener {
            OptionsPickerBottomSheet(
                title = getString(R.string.create_item_select_gender),
                options = genders.map { it.name }
            ) { selectedName, position ->
                selectedGenderId = genders[position].id
                binding.genderInput.setText(selectedName)
                Timber.tag(LogTags.UI).d("Selected gender: $selectedName (ID: $selectedGenderId)")
            }.show(childFragmentManager, "gender_picker")
        }
    }

    // CreateItemFragment.kt - setupSchoolPicker
    private fun setupSchoolPicker(schools: List<School>) {
        // ...
        binding.schoolInput.setOnClickListener {
            if (selectedProvinceId == null) {
                Toast.makeText(requireContext(), "Please select a province first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTownId == null) {
                Toast.makeText(requireContext(), "Please select a town first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Get town name with better error handling
            val townName = viewModel.towns.value.find { it.id == selectedTownId }?.name
            if (townName == null) {
                Timber.tag(LogTags.UI).e("❌ Town not found in list for ID: $selectedTownId")
                Toast.makeText(requireContext(), "Error: Town not found. Please re-select town.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Timber.tag(LogTags.UI).d("🔍 Searching schools in town: $townName (ID: $selectedTownId)")

            SchoolPickerBottomSheet(
                provinceId = selectedProvinceId!!,
                townName = townName  // ✅ Now guaranteed non-null
            ) { selectedSchool ->
                selectedSchoolId = selectedSchool.id
                binding.schoolInput.setText(selectedSchool.name)
                Timber.tag(LogTags.UI).d("Selected school: ${selectedSchool.name} (ID: ${selectedSchool.id})")
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
        Timber.tag(LogTags.UI).d("Setting up default selections")

        val quantities = (QUANTITY_MIN..QUANTITY_MAX).map { it.toString() }
        val quantityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            quantities
        )
        quantityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.quantitySpinner.adapter = quantityAdapter

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

    private fun launchCameraSafely() {
        Timber.tag(LogTags.UI).d("launchCameraSafely called, isCameraLaunched=$isCameraLaunched")
        (activity as? MainActivity)?.let { mainActivity ->
            val mainViewModel: MainViewModel by viewModels()
            mainViewModel.suppressNextResumeRefresh = true
            Timber.tag(LogTags.UI).d("🔒 Suppressed next resume refresh")
        }

        if (!isCameraLaunched) {
            isCameraLaunched = true

            val hasCamera = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            Timber.tag(LogTags.UI).d("Device has camera: $hasCamera")

            if (!hasCamera) {
                isCameraLaunched = false
                Timber.tag(LogTags.UI).e("No camera hardware on device")
                Toast.makeText(requireContext(), getString(R.string.create_item_no_camera), Toast.LENGTH_SHORT).show()
                return
            }

            try {
                Timber.tag(LogTags.UI).d("Launching camera...")
                simpleCameraLauncher.launch(null)
                Timber.tag(LogTags.UI).d("Camera launched successfully")
            } catch (e: Exception) {
                isCameraLaunched = false
                Timber.tag(LogTags.UI).e(e, "❌ Failed to launch camera")
                Toast.makeText(requireContext(), getString(R.string.create_item_camera_failed), Toast.LENGTH_SHORT).show()
            }
        } else {
            Timber.tag(LogTags.UI).w("Camera already launching, skipping")
        }
    }

    private fun setupClickListeners() {
        Timber.tag(LogTags.UI).d("Setting up click listeners")

        binding.coverPhoto.setOnClickListener {
            Timber.tag(LogTags.UI).d("Cover photo clicked")
            pickImagesIfAllowed()
        }

        binding.differentAngle.setOnClickListener {
            Timber.tag(LogTags.UI).d("Different angle clicked")
            pickImagesIfAllowed()
        }

        binding.labelPhoto.setOnClickListener {
            Timber.tag(LogTags.UI).d("Label photo clicked")
            pickImagesIfAllowed()
        }

        binding.submitButton.setOnClickListener {
            Timber.tag(LogTags.UI).d("Submit button clicked")
            if (validateForm()) {
                createItem()
            }
        }

        binding.deleteCover.setOnClickListener {
            Timber.tag(LogTags.UI).d("Delete cover clicked")
            if (viewModel.images.value.isNotEmpty()) {
                viewModel.removeImageAt(0)
            }
        }

        binding.deleteAngle2.setOnClickListener {
            Timber.tag(LogTags.UI).d("Delete angle 2 clicked")
            if (viewModel.images.value.size > 1) {
                viewModel.removeImageAt(1)
            }
        }

        binding.deleteAngle3.setOnClickListener {
            Timber.tag(LogTags.UI).d("Delete angle 3 clicked")
            if (viewModel.images.value.size > 2) {
                viewModel.removeImageAt(2)
            }
        }
    }

    private fun pickImagesIfAllowed() {
        val currentCount = viewModel.images.value.size
        Timber.tag(LogTags.UI).d("pickImagesIfAllowed called, current images: $currentCount/$MAX_IMAGES")

        if (currentCount >= MAX_IMAGES) {
            Timber.tag(LogTags.UI).w("Maximum images reached")
            Toast.makeText(requireContext(), getString(R.string.create_item_max_images), Toast.LENGTH_SHORT).show()
            return
        }
        showImageSourceOptions()
    }

    private fun showImageSourceOptions() {
        Timber.tag(LogTags.UI).d("Showing image source options dialog")

        val options = arrayOf(
            getString(R.string.create_item_take_photo),
            getString(R.string.create_item_choose_gallery),
            getString(R.string.create_item_cancel)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.create_item_add_photo))
            .setItems(options) { _, which ->
                Timber.tag(LogTags.UI).d("Selected option: ${options[which]}")
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> launchGallery()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun checkCameraPermission() {
        Timber.tag(LogTags.UI).d("Checking camera permission")

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Timber.tag(LogTags.UI).d("Camera permission already granted")
                if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    launchCameraSafely()
                } else {
                    Timber.tag(LogTags.UI).e("No camera hardware")
                    Toast.makeText(requireContext(), getString(R.string.create_item_no_camera), Toast.LENGTH_SHORT).show()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Timber.tag(LogTags.UI).d("Showing camera permission rationale")
                showCameraPermissionExplanation()
            }
            else -> {
                Timber.tag(LogTags.UI).d("Requesting camera permission")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionExplanation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.create_item_camera_permission_title))
            .setMessage(getString(R.string.create_item_camera_permission_message))
            .setPositiveButton(getString(R.string.create_item_camera_permission_allow)) { _, _ ->
                Timber.tag(LogTags.UI).d("User granted camera permission from dialog")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(getString(R.string.create_item_cancel), null)
            .show()
    }

    private fun launchGallery() {
        Timber.tag(LogTags.UI).d("Launching gallery")
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Failed to launch gallery")
            handleGalleryFailure()
        }
    }

    private fun handleGalleryFailure() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.create_item_gallery_access_issue))
            .setMessage(getString(R.string.create_item_gallery_access_message))
            .setPositiveButton(getString(R.string.create_item_gallery_take_photo)) { _, _ ->
                Timber.tag(LogTags.UI).d("User chose to take photo instead")
                checkCameraPermission()
            }
            .setNegativeButton(getString(R.string.create_item_cancel), null)
            .show()
    }

    private fun saveBitmapToFile(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            Timber.tag(LogTags.UI).d("saveBitmapToFile: Starting")
            val timeStamp = SimpleDateFormat(IMAGE_DATE_FORMAT, Locale.getDefault()).format(Date())
            val filename = "$IMAGE_FILENAME_PREFIX$timeStamp$IMAGE_FILE_EXTENSION"
            Timber.tag(LogTags.UI).d("Filename: $filename")

            val cacheDir = requireContext().cacheDir
            Timber.tag(LogTags.UI).d("Cache dir: ${cacheDir.absolutePath}")

            val file = File(cacheDir, filename)
            Timber.tag(LogTags.UI).d("File path: ${file.absolutePath}")

            file.outputStream().use { out ->
                val success = bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, IMAGE_COMPRESS_QUALITY, out)
                Timber.tag(LogTags.UI).d("Bitmap compress success: $success")
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            Timber.tag(LogTags.UI).d("✅ FileProvider URI: $uri")

            if (file.exists()) {
                Timber.tag(LogTags.UI).d("File size: ${file.length()} bytes")
            } else {
                Timber.tag(LogTags.UI).e("File does not exist after writing!")
            }

            uri
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "❌ saveBitmapToFile failed")
            null
        }
    }

    private fun createItem() {
        val name = binding.itemName.text.toString().trim()
        val description = binding.description.text.toString().trim()
        val price = binding.price.text.toString().toDoubleOrNull() ?: 0.0

        val mainId = viewModel.selectedMainCategoryId.value
        val subId = selectedSubCategoryId

        Timber.tag(LogTags.UI).d("🚀 Submit attempt - MainCat from VM: $mainId | Local: $selectedMainCategoryId")
        Timber.tag(LogTags.UI).d("🚀 SubCat: $subId")

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.create_item_name_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.create_item_description_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (price <= 0) {
            Toast.makeText(requireContext(), getString(R.string.create_item_price_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (mainId == null || mainId == 0) {
            Toast.makeText(requireContext(), getString(R.string.create_item_category_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (subId == null || subId == 0) {
            Toast.makeText(requireContext(), getString(R.string.create_item_subcategory_required), Toast.LENGTH_SHORT).show()
            return
        }

        Timber.tag(LogTags.UI).d("✅ All checks passed - Using Main: $mainId, Sub: $subId")

        lifecycleScope.launch {
            val hasContact = viewModel.hasContactNumber()
            if (hasContact) {
                viewModel.createItemOfflineFirst(
                    context = requireContext(),
                    name = name,
                    description = description,
                    price = price,
                    quantity = selectedQuantity,
                    mainCategoryId = mainId,
                    subCategoryId = subId!!,
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
            } else {
                showMissingContactDialog()
            }
        }
    }

    private fun showMissingContactDialog() {
        DialogHelper.showConfirmationDialog(
            context = requireContext(),
            action = DialogAction.MissingContactNumber,
            onConfirm = {
                findNavController().navigate(R.id.action_createItemFragment_to_profileFragment)
            },
            onCancel = {
                Toast.makeText(requireContext(), getString(R.string.create_item_contact_required), Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateImagePreview(uris: List<Uri>) {
        Timber.tag(LogTags.UI).d("updateImagePreview: ${uris.size} images")
        binding.imagesSubheading.text = getString(R.string.create_item_images_subheading, uris.size)

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
                        Timber.tag(LogTags.UI).d("Loading image 0: $uri")
                        loadImageWithGlide(uri, binding.coverPhoto)
                        binding.deleteCover.visibility = View.VISIBLE
                    }
                    1 -> {
                        Timber.tag(LogTags.UI).d("Loading image 1: $uri")
                        loadImageWithGlide(uri, binding.differentAngle)
                        binding.deleteAngle2.visibility = View.VISIBLE
                    }
                    2 -> {
                        Timber.tag(LogTags.UI).d("Loading image 2: $uri")
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
            Timber.tag(LogTags.UI).d("Glide loaded image: $uri")
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Glide failed to load image")
        }
    }

    private fun validateForm(): Boolean {
        Timber.tag(LogTags.UI).d("Validating form - MainCat: $selectedMainCategoryId, SubCat: $selectedSubCategoryId")

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
        if (selectedMainCategoryId == null) {
            Toast.makeText(requireContext(), getString(R.string.create_item_category_required), Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedSubCategoryId == null) {
            Toast.makeText(requireContext(), getString(R.string.create_item_subcategory_required), Toast.LENGTH_SHORT).show()
            return false
        }

        Timber.tag(LogTags.UI).d("✅ Validation PASSED - Main: $selectedMainCategoryId | Sub: $selectedSubCategoryId")
        return true
    }

    private fun showLoading() {
        Timber.tag(LogTags.UI).d("Showing loading state")
        binding.submitButton.isEnabled = false
        binding.submitButton.text = getString(R.string.create_item_creating)
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        Timber.tag(LogTags.UI).d("Hiding loading state")
        binding.submitButton.isEnabled = true
        binding.submitButton.text = getString(R.string.create_item_submit)
        binding.progressBar.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        Timber.tag(LogTags.UI).d("✅ Success: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        clearForm()
    }

    private fun showError(message: String) {
        Timber.tag(LogTags.UI).e("❌ Error: $message")
        Snackbar.make(binding.root, getString(R.string.create_item_error_failed) + ": $message", Snackbar.LENGTH_LONG).show()
    }

    private fun clearForm() {
        Timber.tag(LogTags.UI).d("Clearing form")
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

        // ✅ Reset lock states
        lockTown("Select province first")
        lockSchool("Select town first")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.tag(LogTags.FRAGMENT).d("onSaveInstanceState")
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
            Timber.tag(LogTags.FRAGMENT).d("Restoring state from savedInstanceState")
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
            Timber.tag(LogTags.FRAGMENT).d("Restored quantity: $selectedQuantity")

            // ✅ Restore lock states
            if (selectedProvinceId != null) {
                unlockTown()
                if (selectedTownId != null) {
                    unlockSchool()
                } else {
                    lockSchool("Select town first")
                }
            } else {
                lockTown("Select province first")
                lockSchool("Select town first")
            }
        }
    }

    private fun navigateAfterSuccess() {
        Timber.tag(LogTags.UI).d("Navigating after success")
        lifecycleScope.launch {
            delay(NAVIGATION_DELAY_MS)
            try {
                findNavController().navigate(R.id.action_createItemFragment_to_mainFragment)
                Timber.tag(LogTags.UI).d("Navigated to main fragment")
            } catch (e: Exception) {
                Timber.tag(LogTags.UI).e(e, "Navigation failed")
                try {
                    findNavController().navigate(R.id.nav_home)
                    Timber.tag(LogTags.UI).d("Navigated to home")
                } catch (e2: Exception) {
                    Timber.tag(LogTags.UI).e(e2, "Fallback navigation failed")
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.tag(LogTags.FRAGMENT).d("onDestroyView")
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(LogTags.FRAGMENT).d("onResume - resetting camera flag")
        isCameraLaunched = false
        hideFab()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(LogTags.FRAGMENT).d("onPause")
    }
}