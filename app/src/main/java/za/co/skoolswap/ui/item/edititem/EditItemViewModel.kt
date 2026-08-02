package za.co.skoolswap.ui.item.edititem

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.domain.model.EditImage
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.reference.*
import za.co.skoolswap.domain.repository.ItemRepositoryInterface
import za.co.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import za.co.skoolswap.workers.EditItemWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Private constants - internal to this file only
private const val MAX_IMAGES = 3

@HiltViewModel
class EditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val referenceRepository: ReferenceDataRepositoryInterface,
) : ViewModel() {

    // ============ UI STATES ============
    private val _uiState = MutableStateFlow<EditItemUiState>(EditItemUiState.Idle)
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _item = MutableStateFlow<Item?>(null)
    val item: StateFlow<Item?> = _item.asStateFlow()

    // ============ UNIFIED IMAGE STATE ============
    private val _images = MutableStateFlow<List<EditImage>>(emptyList())
    val images: StateFlow<List<EditImage>> = _images.asStateFlow()

    private val _imagesToDelete = MutableStateFlow<Set<Long>>(emptySet())
    val imagesToDelete: StateFlow<Set<Long>> = _imagesToDelete.asStateFlow()

    // ============ DELETE CONFIRMATION ============
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

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

    private val _isReferenceDataLoaded = MutableStateFlow(false)
    val isReferenceDataLoaded: StateFlow<Boolean> = _isReferenceDataLoaded.asStateFlow()

    init {
        Timber.tag(LogTags.VIEW_MODEL).d("=== EditItemViewModel INIT ===")
        viewModelScope.launch {
            _isLoading.value = true
            Timber.tag(LogTags.VIEW_MODEL).d("Loading started")

            // Start collecting from DB FIRST
            launch {
                referenceRepository.getMainCategories().collect {
                    _mainCategories.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} main categories from DB")
                }
            }
            launch {
                referenceRepository.getColors().collect {
                    _colors.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} colors from DB")
                }
            }
            launch {
                referenceRepository.getSizes().collect {
                    _sizes.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} sizes from DB")
                }
            }
            launch {
                referenceRepository.getBrands().collect {
                    _brands.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} brands from DB")
                }
            }
            launch {
                referenceRepository.getConditions().collect {
                    _conditions.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} conditions from DB")
                }
            }
            launch {
                referenceRepository.getProvinces().collect {
                    _provinces.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} provinces from DB")
                }
            }
            launch {
                referenceRepository.getSchools().collect {
                    _schools.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} schools from DB")
                }
            }
            launch {
                referenceRepository.getGenders().collect {
                    _genders.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} genders from DB")
                }
            }
            launch {
                referenceRepository.getTags().collect {
                    _tags.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} tags from DB")
                }
            }
            launch {
                referenceRepository.getLocations().collect {
                    _locations.value = it
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${it.size} locations from DB")
                }
            }

            // Set up observers
            observeSubCategories()
            observeTowns()

            // Trigger refresh
            Timber.tag(LogTags.VIEW_MODEL).d("Refreshing reference data...")
            try {
                val result = referenceRepository.refreshAllReferenceDataBulk(forceRefresh = false)
                if (result.isSuccess) {
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Reference data refreshed successfully")
                } else {
                    Timber.tag(LogTags.VIEW_MODEL).w("⚠️ Refresh failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.VIEW_MODEL).e(e, "⚠️ Refresh error")
            } finally {
                _isLoading.value = false
                _isReferenceDataLoaded.value = true
                Timber.tag(LogTags.VIEW_MODEL).d("Reference data loading completed, isLoading=false")
            }
        }
    }

    // ============ OBSERVE DEPENDENT DATA ============
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSubCategories() {
        viewModelScope.launch {
            _selectedMainCategoryId
                .filterNotNull()
                .flatMapLatest { mainCategoryId ->
                    Timber.tag(LogTags.VIEW_MODEL).d("Fetching subcategories for mainCategoryId: $mainCategoryId")
                    referenceRepository.getSubCategories(mainCategoryId)
                }
                .collect { subCats ->
                    _subCategories.value = subCats
                    Timber.tag(LogTags.VIEW_MODEL).d("Received ${subCats.size} subcategories")
                }
        }
    }

    private fun observeTowns() {
        viewModelScope.launch {
            _selectedProvinceId
                .filterNotNull()
                .collect { provinceId ->
                    Timber.tag(LogTags.VIEW_MODEL).d("🔍 Province selected: $provinceId")
                    referenceRepository.refreshTowns(provinceId)
                    referenceRepository.getTowns(provinceId).collect { townList ->
                        _towns.value = townList
                        Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${townList.size} towns for province $provinceId")
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

    // ============ LOAD ITEM ============
    fun loadItem(itemId: String) {
        Timber.tag(LogTags.VIEW_MODEL).d("🔄 loadItem() called for: $itemId")
        viewModelScope.launch {
            _isLoading.value = true
            val result = itemRepository.getShopItemForEdit(itemId)

            result.fold(
                onSuccess = { item ->
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ SUCCESS - Item loaded:")
                    Timber.tag(LogTags.VIEW_MODEL).d("   ID: ${item.id}")
                    Timber.tag(LogTags.VIEW_MODEL).d("   Name: ${item.name}")
                    Timber.tag(LogTags.VIEW_MODEL).d("   mainCategoryId: ${item.mainCategoryId}")
                    Timber.tag(LogTags.VIEW_MODEL).d("   subCategoryId: ${item.subCategoryId}")
                    Timber.tag(LogTags.VIEW_MODEL).d("   Images: ${item.images.size}")

                    _item.value = item

                    item.mainCategoryId?.let {
                        _selectedMainCategoryId.value = it
                        Timber.tag(LogTags.VIEW_MODEL).d("🔑 Set selectedMainCategoryId: $it")
                    }
                    item.provinceId?.let {
                        _selectedProvinceId.value = it
                        Timber.tag(LogTags.VIEW_MODEL).d("🔑 Set selectedProvinceId: $it")
                    }

                    val existingImages = item.images.map { image ->
                        EditImage.Existing(image.id, image.url)
                    }
                    setExistingImages(existingImages)
                },
                onFailure = { error ->
                    Timber.tag(LogTags.VIEW_MODEL).e(error, "❌ FAILED - Could not load item")
                    _uiState.value = EditItemUiState.Error("Failed to load item: ${error.message}")
                }
            )
            _isLoading.value = false
        }
    }

    // ============ UNIFIED IMAGE METHODS ============
    fun setExistingImages(existingImages: List<EditImage>) {
        Timber.tag(LogTags.VIEW_MODEL).d("setExistingImages called with ${existingImages.size} images")
        existingImages.forEachIndexed { index, image ->
            if (image is EditImage.Existing) {
                Timber.tag(LogTags.VIEW_MODEL).d("Image $index: ID=${image.id}, URL=${image.url.take(100)}...")
            }
        }

        val imageList = existingImages.toMutableList()
        while (imageList.size < MAX_IMAGES) {
            imageList.add(EditImage.Empty)
        }

        _images.value = imageList
        Timber.tag(LogTags.VIEW_MODEL).d("Final image list size: ${imageList.size}")
    }

    fun addImage(uri: Uri, position: Int) {
        Timber.tag(LogTags.VIEW_MODEL).d("addImage - position: $position, uri: $uri")
        val currentList = _images.value.toMutableList()
        if (position in 0 until currentList.size) {
            currentList[position] = EditImage.New(uri, isUploading = false)
            _images.value = currentList
            Timber.tag(LogTags.VIEW_MODEL).d("Image added at position $position")
        }
    }

    fun replaceImage(uri: Uri, position: Int) {
        Timber.tag(LogTags.VIEW_MODEL).d("replaceImage - position: $position, uri: $uri")
        val currentList = _images.value.toMutableList()
        if (position in 0 until currentList.size) {
            val currentImage = currentList[position]
            if (currentImage is EditImage.Existing && !currentImage.isMarkedForDeletion) {
                _imagesToDelete.value += currentImage.id
                Timber.tag(LogTags.VIEW_MODEL).d("Marked existing image ${currentImage.id} for deletion")
            }
            currentList[position] = EditImage.New(uri, isUploading = false)
            _images.value = currentList
            Timber.tag(LogTags.VIEW_MODEL).d("Image replaced at position $position")
        }
    }

    fun removeImage(position: Int) {
        Timber.tag(LogTags.VIEW_MODEL).d("removeImage - position: $position")
        val currentList = _images.value.toMutableList()
        if (position in 0 until currentList.size) {
            val image = currentList[position]
            if (image is EditImage.Existing && !image.isMarkedForDeletion) {
                _imagesToDelete.value += image.id
                Timber.tag(LogTags.VIEW_MODEL).d("Marked image ${image.id} for deletion")
            }
            currentList[position] = EditImage.Empty
            _images.value = currentList
            Timber.tag(LogTags.VIEW_MODEL).d("Image removed at position $position")
        }
    }

    fun getImagesForUpload(): List<Uri> {
        val uris = _images.value.filterIsInstance<EditImage.New>().map { it.uri }
        Timber.tag(LogTags.VIEW_MODEL).d("getImagesForUpload: ${uris.size} images")
        return uris
    }

    fun getDeletionIds(): List<Long> {
        val ids = _imagesToDelete.value.toList()
        Timber.tag(LogTags.VIEW_MODEL).d("getDeletionIds: ${ids.size} ids")
        return ids
    }

    // ============ UPDATE ITEM ============
    fun updateItemOfflineFirst(
        context: Context,
        itemId: String,
        name: String?,
        description: String?,
        price: Double?,
        quantity: Int?,
        mainCategoryId: Int?,
        subCategoryId: Int?,
        brandId: Int?,
        sizeId: Int?,
        schoolId: Int?,
        conditionId: Int?,
        locationId: Int?,
        provinceId: Int?,
        genderId: Int?,
        colorId: Int?,
        addImageUris: List<Uri>,
        removeImageIds: List<Long>
    ) {
        Timber.tag(LogTags.VIEW_MODEL).d("=== updateItemOfflineFirst START ===")
        Timber.tag(LogTags.VIEW_MODEL).d("itemId: $itemId")
        Timber.tag(LogTags.VIEW_MODEL).d("mainCategoryId: $mainCategoryId")
        Timber.tag(LogTags.VIEW_MODEL).d("subCategoryId: $subCategoryId")
        Timber.tag(LogTags.VIEW_MODEL).d("addImageUris: ${addImageUris.size}")
        Timber.tag(LogTags.VIEW_MODEL).d("removeImageIds: ${removeImageIds.size}")

        _uiState.value = EditItemUiState.Loading

        viewModelScope.launch {
            try {
                val result = itemRepository.updateItemOfflineFirst(
                    context = context,
                    itemId = itemId,
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
                    tagIds = null,
                    addImageUris = addImageUris,
                    removeImageIds = removeImageIds
                )

                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Failed to save changes"
                    Timber.tag(LogTags.VIEW_MODEL).e("❌ Local save failed: $error")
                    _uiState.value = EditItemUiState.Error(error)
                    return@launch
                }

                val updatedItem = result.getOrNull()!!
                Timber.tag(LogTags.VIEW_MODEL).d("✅ Item saved locally, updating in background")

                _uiState.value = EditItemUiState.Success("Item updated, syncing in background")
                _imagesToDelete.value = emptySet()

                val worker = EditItemWorker.createOneTimeRequest(
                    itemId = itemId,
                    addImageUris = addImageUris,
                    removeImageIds = removeImageIds
                )
                WorkManager.getInstance(context).enqueue(worker)

                Timber.tag(LogTags.VIEW_MODEL).d("✅ Worker enqueued")
                Timber.tag(LogTags.VIEW_MODEL).d("🏁 updateItemOfflineFirst SUCCESS")

            } catch (e: Exception) {
                Timber.tag(LogTags.VIEW_MODEL).e(e, "❌ updateItemOfflineFirst FAILED")
                _uiState.value = EditItemUiState.Error(e.message ?: "Failed to update item")
            }
        }
    }

    fun showDeleteConfirmation() {
        Timber.tag(LogTags.VIEW_MODEL).d("showDeleteConfirmation called")
        _showDeleteConfirmation.value = true
    }

    fun deleteConfirmationShown() {
        Timber.tag(LogTags.VIEW_MODEL).d("deleteConfirmationShown called")
        _showDeleteConfirmation.value = false
    }

    fun deleteItem(itemId: String) {
        Timber.tag(LogTags.VIEW_MODEL).d("deleteItem called for: $itemId")
        viewModelScope.launch {
            _uiState.value = EditItemUiState.Loading
            val result = itemRepository.deleteItem(itemId)
            result.fold(
                onSuccess = {
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Item deleted successfully!")
                    _uiState.value = EditItemUiState.Success("Item deleted successfully!")
                },
                onFailure = { error ->
                    Timber.tag(LogTags.VIEW_MODEL).e(error, "❌ Delete failed")
                    val userMessage = when {
                        error.message?.contains("502") == true ->
                            "Server is temporarily unavailable. Please try again."
                        error.message?.contains("404") == true ->
                            "Item not found. It may have been already deleted."
                        else -> error.message ?: "Failed to delete item"
                    }
                    _uiState.value = EditItemUiState.Error(userMessage)
                }
            )
        }
    }
}