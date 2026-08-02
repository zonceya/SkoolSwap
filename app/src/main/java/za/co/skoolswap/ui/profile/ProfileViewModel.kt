package za.co.skoolswap.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.repository.SchoolRepository
import za.co.skoolswap.data.repository.UserSchoolRepository
import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.domain.model.School
import za.co.skoolswap.domain.model.SchoolMapping
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val schoolRepository: SchoolRepository,
    private val userSchoolRepository: UserSchoolRepository
) : ViewModel() {

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Error states
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Success states
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _profileComplete = MutableStateFlow(false)
    val profileComplete: StateFlow<Boolean> = _profileComplete.asStateFlow()

    // Province data
    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces: StateFlow<List<Province>> = _provinces.asStateFlow()

    private val _selectedProvince = MutableStateFlow<Province?>(null)
    val selectedProvince: StateFlow<Province?> = _selectedProvince.asStateFlow()

    // School data
    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools.asStateFlow()

    private val _selectedSchool = MutableStateFlow<School?>(null)
    val selectedSchool: StateFlow<School?> = _selectedSchool.asStateFlow()

    private val _currentSchoolMapping = MutableStateFlow<SchoolMapping?>(null)
    val currentSchoolMapping: StateFlow<SchoolMapping?> = _currentSchoolMapping.asStateFlow()

    private val _hasExistingSchool = MutableStateFlow(false)
    val hasExistingSchool: StateFlow<Boolean> = _hasExistingSchool.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val _originalMobile = MutableStateFlow<String?>(null)
    private val _originalSchool = MutableStateFlow<School?>(null)

    private val _pendingMobile = MutableStateFlow<String?>(null)
    private val _pendingSchool = MutableStateFlow<School?>(null)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _showConfirmationDialog = MutableStateFlow(false)
    val showConfirmationDialog: StateFlow<Boolean> = _showConfirmationDialog.asStateFlow()

    val pendingSchool: StateFlow<School?> = _pendingSchool.asStateFlow()

    private val _changesSummary = MutableStateFlow<String?>(null)
    val changesSummary: StateFlow<String?> = _changesSummary.asStateFlow()

    private val _isNewSectionLoading = MutableStateFlow(false)
    val isNewSectionLoading: StateFlow<Boolean> = _isNewSectionLoading.asStateFlow()

    init {
        loadProvinces()
        checkExistingSchoolMapping()
    }

    fun loadProvinces() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = schoolRepository.getProvinces()) {
                is Result.Success -> {
                    _provinces.value = result.data
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Loaded ${result.data.size} provinces")
                }
                is Result.Error -> {
                    _error.value = "Failed to load provinces: ${result.exception.message}"
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Failed to load provinces")
                }
            }

            _isLoading.value = false
        }
    }

    fun clearSchoolSelection() {
        Timber.tag(LogTags.VIEW_MODEL).d("⚠️ clearSchoolSelection() called! Previous school: ${_selectedSchool.value?.name}")
        _selectedSchool.value = null
        _pendingSchool.value = null
    }

    fun initializeProfile(mobile: String?, school: School?) {
        val normalizedMobile = mobile?.takeIf { it.isNotBlank() } ?: ""

        if (_originalMobile.value == null) {
            _originalMobile.value = normalizedMobile
            _pendingMobile.value = normalizedMobile
        }

        Timber.tag(LogTags.VIEW_MODEL).d("initializeProfile - mobile: $normalizedMobile")
    }

    fun previewSchool(school: School) {
        _pendingSchool.value = school
        _selectedSchool.value = school
    }

    fun previewMobile(mobile: String) {
        _pendingMobile.value = mobile
    }

    fun checkForChanges(): Boolean {
        val originalMobileNormalized = _originalMobile.value ?: ""
        val pendingMobileNormalized = _pendingMobile.value ?: ""

        val mobileChanged = pendingMobileNormalized != originalMobileNormalized

        val originalSchoolId = _originalSchool.value?.id ?: -1
        val pendingSchoolId = _pendingSchool.value?.id ?: -1
        val schoolChanged = pendingSchoolId != originalSchoolId

        Timber.tag(LogTags.VIEW_MODEL).d("checkForChanges - mobileChanged: $mobileChanged, schoolChanged: $schoolChanged")

        return mobileChanged || schoolChanged
    }

    fun prepareConfirmationDialog() {
        val changes = mutableListOf<String>()

        if (_pendingMobile.value != _originalMobile.value) {
            changes.add("• Mobile: ${_originalMobile.value} → ${_pendingMobile.value}")
        }

        if (_pendingSchool.value?.id != _originalSchool.value?.id) {
            changes.add("• School: ${_originalSchool.value?.name} → ${_pendingSchool.value?.name}")
        }

        _changesSummary.value = changes.joinToString("\n")
        _showConfirmationDialog.value = true
    }

    fun confirmAndSave() {
        viewModelScope.launch {
            _isLoading.value = true

            _pendingMobile.value?.let { mobile ->
                if (mobile != _originalMobile.value && mobile.isNotBlank()) {
                    authRepository.updateMobile(mobile)
                    _originalMobile.value = mobile
                }
            }

            _pendingSchool.value?.let { school ->
                if (school.id != _originalSchool.value?.id) {
                    if (_hasExistingSchool.value && _currentSchoolMapping.value != null) {
                        userSchoolRepository.updateSchoolMapping(
                            _currentSchoolMapping.value!!.mappingId,
                            school.id
                        )
                    } else {
                        userSchoolRepository.assignSchool(school.id)
                    }
                    _originalSchool.value = school
                }
            }

            _isLoading.value = false
            _showConfirmationDialog.value = false
            _updateSuccess.value = true
            _profileComplete.value = true
        }
    }

    fun cancelConfirmation() {
        _pendingMobile.value = _originalMobile.value
        _pendingSchool.value = _originalSchool.value
        _selectedSchool.value = _originalSchool.value
        _showConfirmationDialog.value = false
        _error.value = "Update cancelled"
    }

    fun checkExistingSchoolMapping() {
        viewModelScope.launch {
            Timber.tag(LogTags.VIEW_MODEL).d("🔍 Checking existing school mapping")

            when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                is Result.Success -> {
                    if (result.data != null) {
                        _currentSchoolMapping.value = result.data
                        _hasExistingSchool.value = true

                        Timber.tag(LogTags.VIEW_MODEL).d("✅ Found existing school: ${result.data.schoolName}")

                        val school = School(
                            id = result.data.schoolId,
                            name = result.data.schoolName,
                            provinceId = result.data.provinceId,
                            provinceName = null,
                            locationId = null,
                            schoolType = result.data.schoolType
                        )

                        _selectedSchool.value = school
                        _originalSchool.value = school
                        _pendingSchool.value = school

                        result.data.provinceId?.let { provinceId ->
                            val province = _provinces.value.find { it.id == provinceId }
                            if (province != null) {
                                _selectedProvince.value = province
                                Timber.tag(LogTags.VIEW_MODEL).d("✅ Set province: ${province.name}")
                            }
                        }
                    } else {
                        _hasExistingSchool.value = false
                        Timber.tag(LogTags.VIEW_MODEL).d("ℹ️ No existing school found")
                    }
                    _isInitialized.value = true
                }
                is Result.Error -> {
                    _error.value = "Failed to check school status"
                    _hasExistingSchool.value = false
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Error checking school")
                    _isInitialized.value = true
                }
            }
        }
    }

    fun selectProvince(province: Province, shouldClearSchool: Boolean = false) {
        Timber.tag(LogTags.VIEW_MODEL).d("📍 selectProvince: ${province.name}, clearSchool: $shouldClearSchool")

        val currentSchool = _selectedSchool.value
        if (currentSchool != null && currentSchool.provinceId == province.id) {
            Timber.tag(LogTags.VIEW_MODEL).d("✅ Keeping school because it matches province: ${currentSchool.name}")
            _selectedProvince.value = province
            return
        }

        _selectedProvince.value = province

        if (shouldClearSchool) {
            Timber.tag(LogTags.VIEW_MODEL).d("🗑️ Clearing school selection")
            _selectedSchool.value = null
            _pendingSchool.value = null
            _schools.value = emptyList()
        }
    }

    fun searchSchools(query: String) {
        val province = _selectedProvince.value ?: return

        if (query.length < 2) {
            _schools.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null

            when (val result = schoolRepository.searchSchools(province.id, query)) {
                is Result.Success -> {
                    _schools.value = result.data
                    Timber.tag(LogTags.VIEW_MODEL).d("🔍 Found ${result.data.size} schools for '$query'")
                }
                is Result.Error -> {
                    _error.value = "Search failed: ${result.exception.message}"
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Search failed")
                }
            }

            _isSearching.value = false
        }
    }

    fun selectSchool(school: School) {
        Timber.tag(LogTags.VIEW_MODEL).d("📝 selectSchool called with: ${school.name} (ID: ${school.id})")

        _selectedSchool.value = school

        Timber.tag(LogTags.VIEW_MODEL).d("✅ After setting school: ${_selectedSchool.value?.name}")

        _isSearchActive.value = false
        _schools.value = emptyList()

        Timber.tag(LogTags.VIEW_MODEL).d("✅ Final check - school is: ${_selectedSchool.value?.name}")
    }

    fun submitSchoolSelection() {
        viewModelScope.launch {
            val school = _selectedSchool.value ?: return@launch

            _isLoading.value = true
            _error.value = null

            if (_hasExistingSchool.value && _currentSchoolMapping.value != null) {
                val mapping = _currentSchoolMapping.value!!
                when (val result = userSchoolRepository.updateSchoolMapping(mapping.mappingId, school.id)) {
                    is Result.Success -> {
                        _currentSchoolMapping.value = result.data
                        _updateSuccess.value = true
                        _profileComplete.value = true
                        Timber.tag(LogTags.VIEW_MODEL).d("✅ School updated to: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to update school"
                        Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Update failed")
                    }
                }
            } else {
                when (val result = userSchoolRepository.assignSchool(school.id)) {
                    is Result.Success -> {
                        refreshCurrentMapping()
                        _updateSuccess.value = true
                        _profileComplete.value = true
                        Timber.tag(LogTags.VIEW_MODEL).d("✅ New school assigned: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to assign school"
                        Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Assign failed")
                    }
                }
            }

            _isLoading.value = false
        }
    }

    private fun saveSchoolSelection() {
        viewModelScope.launch {
            val school = _selectedSchool.value ?: return@launch

            _isLoading.value = true
            _error.value = null

            if (_hasExistingSchool.value && _currentSchoolMapping.value != null) {
                val mapping = _currentSchoolMapping.value!!
                when (val result = userSchoolRepository.updateSchoolMapping(mapping.mappingId, school.id)) {
                    is Result.Success -> {
                        _currentSchoolMapping.value = result.data
                        _updateSuccess.value = true
                        Timber.tag(LogTags.VIEW_MODEL).d("✅ School updated to: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to update school"
                        Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Update failed")
                    }
                }
            } else {
                when (val result = userSchoolRepository.assignSchool(school.id)) {
                    is Result.Success -> {
                        refreshCurrentMapping()
                        _updateSuccess.value = true
                        Timber.tag(LogTags.VIEW_MODEL).d("✅ New school assigned: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to assign school"
                        Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Assign failed")
                    }
                }
            }

            _isLoading.value = false
        }
    }

    private fun refreshCurrentMapping() {
        viewModelScope.launch {
            when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                is Result.Success -> {
                    if (result.data != null) {
                        _currentSchoolMapping.value = result.data
                        _hasExistingSchool.value = true
                    }
                }
                is Result.Error -> {
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "Failed to refresh mapping")
                }
            }
        }
    }

    fun clearSearch() {
        _isSearchActive.value = false
        _schools.value = emptyList()
    }

    fun updateMobile(mobile: String) {
        if (mobile.isBlank()) {
            Timber.tag(LogTags.VIEW_MODEL).d("⚠️ Mobile is empty - skipping update")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false

            val result = authRepository.updateMobile(mobile)

            result.onSuccess {
                _updateSuccess.value = true
                refreshUserProfile()
                Timber.tag(LogTags.VIEW_MODEL).d("✅ Mobile updated: $mobile")
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to update mobile"
                Timber.tag(LogTags.VIEW_MODEL).e(throwable, "❌ Failed to update mobile")
            }

            _isLoading.value = false
        }
    }

    fun submitProfile() {
        Timber.tag(LogTags.VIEW_MODEL).d("📞 submitProfile() called")
        _profileComplete.value = true
    }

    fun refreshUserProfile() {
        viewModelScope.launch {
            val result = authRepository.refreshUserProfile()
            result.onFailure { throwable ->
                _error.value = "Failed to refresh profile: ${throwable.message}"
                Timber.tag(LogTags.VIEW_MODEL).e(throwable, "❌ Failed to refresh profile")
            }
        }
    }

    private fun checkProfileComplete() {
        _profileComplete.value = _selectedSchool.value != null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _updateSuccess.value = false
    }

    fun resetNavigation() {
        _profileComplete.value = false
    }
}