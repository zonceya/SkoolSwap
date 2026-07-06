package com.example.skoolswap.ui.item

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.mapper.toEntity
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.model.reference.*
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import com.example.skoolswap.workers.ItemCreationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val referenceRepository: ReferenceDataRepositoryInterface,
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    companion object {
        private const val TAG = "CreateItemViewModel"
    }

    // ============ UI STATES ============
    private val _uiState = MutableStateFlow<CreateItemUiState>(CreateItemUiState.Idle)
    val uiState: StateFlow<CreateItemUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _createdItem = MutableStateFlow<ItemState?>(null)
    val createdItem: StateFlow<ItemState?> = _createdItem.asStateFlow()

    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images.asStateFlow()

    // ============ REFERENCE DATA STATES ============
    private val _mainCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val mainCategories: StateFlow<List<MainCategory>> = _mainCategories.asStateFlow()

    private val _subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val subCategories: StateFlow<List<SubCategory>> = _subCategories.asStateFlow()

    private val _colors = MutableStateFlow<List<Color>>(emptyList())
    val colors: StateFlow<List<Color>> = _colors.asStateFlow()

    private val _sizes = MutableStateFlow<List<Size>>(emptyList())
    val sizes: StateFlow<List<Size>> = _sizes.asStateFlow()

    private val _brands = MutableStateFlow<List<Brand>>(emptyList())
    val brands: StateFlow<List<Brand>> = _brands.asStateFlow()

    private val _conditions = MutableStateFlow<List<Condition>>(emptyList())
    val conditions: StateFlow<List<Condition>> = _conditions.asStateFlow()

    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces: StateFlow<List<Province>> = _provinces.asStateFlow()

    private val _towns = MutableStateFlow<List<Town>>(emptyList())
    val towns: StateFlow<List<Town>> = _towns.asStateFlow()

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools.asStateFlow()

    private val _genders = MutableStateFlow<List<Gender>>(emptyList())
    val genders: StateFlow<List<Gender>> = _genders.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    // ============ SELECTED VALUES ============
    private val _selectedMainCategoryId = MutableStateFlow<Int?>(null)
    val selectedMainCategoryId: StateFlow<Int?> = _selectedMainCategoryId.asStateFlow()

    private val _selectedProvinceId = MutableStateFlow<Int?>(null)
    val selectedProvinceId: StateFlow<Int?> = _selectedProvinceId.asStateFlow()

    init {
        Timber.tag(LogTags.VIEW_MODEL).d("=== ViewModel INIT ===")
        viewModelScope.launch {
            _isLoading.value = true
            Timber.tag(LogTags.VIEW_MODEL).d("Loading started")

            // STEP 1: Start collecting from DB FIRST
            Timber.tag(LogTags.VIEW_MODEL).d("Setting up database collectors")
            launch {
                referenceRepository.getMainCategories().collect {
                    _mainCategories.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("📦 Loaded ${it.size} main categories from DB")
                    it.forEach { category ->
                        Timber.tag(LogTags.VIEW_MODEL).d("  - Category: ${category.name} (ID: ${category.id})")
                    }
                }
            }
            launch {
                referenceRepository.getColors().collect {
                    _colors.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("🎨 Loaded ${it.size} colors from DB")
                }
            }
            launch {
                referenceRepository.getSizes().collect {
                    _sizes.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("📏 Loaded ${it.size} sizes from DB")
                }
            }
            launch {
                referenceRepository.getBrands().collect {
                    _brands.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("🏷️ Loaded ${it.size} brands from DB")
                }
            }
            launch {
                referenceRepository.getConditions().collect {
                    _conditions.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Loaded ${it.size} conditions from DB")
                }
            }
            launch {
                referenceRepository.getProvinces().collect {
                    _provinces.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("🗺️ Loaded ${it.size} provinces from DB")
                }
            }
            launch {
                referenceRepository.getSchools().collect {
                    _schools.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("🏫 Loaded ${it.size} schools from DB")
                }
            }
            launch {
                referenceRepository.getGenders().collect {
                    _genders.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("👥 Loaded ${it.size} genders from DB")
                }
            }
            launch {
                referenceRepository.getTags().collect {
                    _tags.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("🏷️ Loaded ${it.size} tags from DB")
                }
            }
            launch {
                referenceRepository.getLocations().collect {
                    _locations.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("📍 Loaded ${it.size} locations from DB")
                }
            }

            // STEP 2: Set up observers
            Timber.tag(LogTags.VIEW_MODEL).d("Setting up observers for subcategories and towns")
            observeSubCategories()
            observeTowns()

            // STEP 3: Trigger refresh
            Timber.tag(LogTags.VIEW_MODEL).d("Triggering reference data refresh")
            try {
                val result = referenceRepository.refreshAllReferenceDataBulk(forceRefresh = false)
                if (result.isSuccess) {
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Reference data refreshed successfully")
                } else {
                    Timber.tag(LogTags.VIEW_MODEL).w("⚠️ Refresh failed, using cached: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.VIEW_MODEL).e(e, "⚠️ Refresh error: ${e.message}")
            }

            _isLoading.value = false
            Timber.tag(LogTags.VIEW_MODEL).d("Loading completed, isLoading=false")
            Timber.tag(LogTags.VIEW_MODEL).d("=== ViewModel INIT COMPLETE ===")
        }
    }

    // ============ OBSERVE DEPENDENT DATA ============
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSubCategories() {
        viewModelScope.launch {
            Timber.tag(LogTags.VIEW_MODEL).d("observeSubCategories: Starting observation")
            _selectedMainCategoryId
                .filterNotNull()
                .flatMapLatest { mainCategoryId ->
                    Timber.tag(LogTags.VIEW_MODEL).d("observeSubCategories: Main category ID changed to $mainCategoryId")
                    referenceRepository.getSubCategories(mainCategoryId)
                }
                .collect { subCats ->
                    Timber.tag(LogTags.VIEW_MODEL).d("📦 Received ${subCats.size} subcategories")
                    subCats.forEach { subCat ->
                        Timber.tag(LogTags.VIEW_MODEL).d("  - Subcategory: ${subCat.name} (ID: ${subCat.id})")
                    }
                    _subCategories.value = subCats
                }
        }
    }

    private fun observeTowns() {
        viewModelScope.launch {
            Timber.tag(LogTags.VIEW_MODEL).d("observeTowns: Starting observation")
            _selectedProvinceId
                .filterNotNull()
                .collect { provinceId ->
                    Timber.tag(LogTags.VIEW_MODEL).d("🔍 Province selected: $provinceId, refreshing towns")
                    referenceRepository.refreshTowns(provinceId)
                    referenceRepository.getTowns(provinceId).collect { townList ->
                        _towns.value = townList
                        Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${townList.size} towns for province $provinceId")
                        townList.forEach { town ->
                            Timber.tag(LogTags.VIEW_MODEL).d("  - Town: ${town.name} (ID: ${town.id})")
                        }
                    }
                }
        }
    }

    // ============ SELECTION METHODS ============
    fun onMainCategorySelected(mainCategoryId: Int?) {
        Timber.tag(LogTags.VIEW_MODEL).d("onMainCategorySelected: $mainCategoryId")
        _selectedMainCategoryId.value = mainCategoryId
    }

    fun onProvinceSelected(provinceId: Int?) {
        Timber.tag(LogTags.VIEW_MODEL).d("onProvinceSelected: $provinceId")
        _selectedProvinceId.value = provinceId
    }

    // ============ IMAGE METHODS ============
    fun addImage(uri: Uri) {
        Timber.tag(LogTags.VIEW_MODEL).d("addImage: $uri")
        val currentImages = _images.value.toMutableList()
        Timber.tag(LogTags.VIEW_MODEL).d("Current images count: ${currentImages.size}")

        if (currentImages.size < 3) {
            currentImages.add(uri)
            _images.value = currentImages
            Timber.tag(LogTags.VIEW_MODEL).d("✅ Image added. New count: ${currentImages.size}")
        } else {
            Timber.tag(LogTags.VIEW_MODEL).w("Cannot add image, max limit reached (3/3)")
        }
    }

    fun removeImage(uri: Uri) {
        Timber.tag(LogTags.VIEW_MODEL).d("removeImage: $uri")
        val currentImages = _images.value.toMutableList()
        val removed = currentImages.remove(uri)
        _images.value = currentImages
        Timber.tag(LogTags.VIEW_MODEL).d("Image removed: $removed. Remaining count: ${currentImages.size}")
    }

    fun clearImages() {
        Timber.tag(LogTags.VIEW_MODEL).d("clearImages: Clearing all images")
        _images.value = emptyList()
    }

    suspend fun hasContactNumber(): Boolean {
        return try {
            val userProfile = authRepository.getServerUser().firstOrNull()
            val hasContactNumber = !userProfile?.mobile.isNullOrEmpty()
            Timber.tag(LogTags.VIEW_MODEL).d("Has contact number: $hasContactNumber")
            hasContactNumber
        } catch (e: Exception) {
            Timber.tag(LogTags.VIEW_MODEL).e(e, "Error checking contact number")
            false
        }
    }

    fun removeImageAt(index: Int) {
        Timber.tag(LogTags.VIEW_MODEL).d("removeImageAt: index=$index")
        val currentImages = _images.value.toMutableList()
        if (index < currentImages.size) {
            val removed = currentImages.removeAt(index)
            _images.value = currentImages
            Timber.tag(LogTags.VIEW_MODEL).d("✅ Removed image at index $index: $removed. Remaining: ${currentImages.size}")
        } else {
            Timber.tag(LogTags.VIEW_MODEL).w("Invalid index $index, current size: ${currentImages.size}")
        }
    }

    // ============ CREATE ITEM ============
    fun createItemOfflineFirst(
        context: Context,
        name: String,
        description: String,
        price: Double,
        quantity: Int,
        mainCategoryId: Int,
        subCategoryId: Int,
        brandId: Int?,
        sizeId: Int?,
        schoolId: Int?,
        conditionId: Int?,
        locationId: Int?,
        provinceId: Int?,
        genderId: Int?,
        colorId: Int?,
        tagIds: List<Int>? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = CreateItemUiState.Loading

                val currentImageUris = _images.value

                val result = itemRepository.createItemOfflineFirst(
                    context = context,
                    name = name,
                    description = description,
                    mainCategoryId = mainCategoryId,
                    subCategoryId = subCategoryId,
                    brandId = brandId,
                    price = price,
                    quantity = quantity,
                    itemConditionId = conditionId,
                    provinceId = provinceId,
                    locationId = locationId,
                    genderId = genderId,
                    schoolId = schoolId,
                    sizeId = sizeId,
                    colorId = colorId,
                    tagIds = tagIds,
                    imageUris = currentImageUris
                )

                if (result.isFailure) {
                    _uiState.value = CreateItemUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to save item"
                    )
                    return@launch
                }

                _uiState.value = CreateItemUiState.Success("Item saved, uploading in background")
                clearImages()

                val localItem = result.getOrNull()!!
                val worker = ItemCreationWorker.createOneTimeRequest(localItem.id)
                WorkManager.getInstance(context).enqueue(worker)

                Timber.tag(LogTags.VIEW_MODEL).d("✅ Item saved locally, worker enqueued")

            } catch (e: Exception) {
                Timber.tag(LogTags.VIEW_MODEL).e(e, "❌ Failed to create item locally")
                _uiState.value = CreateItemUiState.Error(e.message ?: "Failed to create item")
            }
        }
    }

    fun resetState() {
        Timber.tag(LogTags.VIEW_MODEL).d("resetState: Resetting to Idle")
        _uiState.value = CreateItemUiState.Idle
        _createdItem.value = null
    }
}