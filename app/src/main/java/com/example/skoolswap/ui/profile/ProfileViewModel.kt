package com.example.skoolswap.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.SchoolRepository
import com.example.skoolswap.data.repository.UserSchoolRepository
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.domain.model.School
import com.example.skoolswap.domain.model.SchoolMapping
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val _originalMobile = MutableStateFlow<String?>(null)
    private val _originalSchool = MutableStateFlow<School?>(null)

    // Track pending changes (not yet saved)
    private val _pendingMobile = MutableStateFlow<String?>(null)
    private val _pendingSchool = MutableStateFlow<School?>(null)

    // UI state for confirmation dialog
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
                }
                is Result.Error -> {
                    _error.value = "Failed to load provinces: ${result.exception.message}"
                }
            }

            _isLoading.value = false
        }
    }
    fun clearSchoolSelection() {
        Log.d("ProfileViewModel", "⚠️⚠️⚠️ clearSchoolSelection() called! Previous school: ${_selectedSchool.value?.name}")
        _selectedSchool.value = null
    }

    fun initializeProfile(mobile: String?, school: School?) {
        _originalMobile.value = mobile
        _originalSchool.value = school
        _pendingMobile.value = mobile
        _pendingSchool.value = school
        _selectedSchool.value = school
    }
    fun previewSchool(school: School) {
        _pendingSchool.value = school
        _selectedSchool.value = school
    }
    fun previewMobile(mobile: String) {
        _pendingMobile.value = mobile
    }
    fun checkForChanges(): Boolean {
        val mobileChanged = _pendingMobile.value != _originalMobile.value
        val schoolChanged = _pendingSchool.value?.id != _originalSchool.value?.id

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

            // Save mobile if changed
            _pendingMobile.value?.let { mobile ->
                if (mobile != _originalMobile.value && mobile.isNotBlank()) {
                    authRepository.updateMobile(mobile)
                }
            }

            // Save school if changed
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
                }
            }

            // Update originals after save
            _originalMobile.value = _pendingMobile.value
            _originalSchool.value = _pendingSchool.value

            _isLoading.value = false
            _showConfirmationDialog.value = false
            _updateSuccess.value = true
            _profileComplete.value = true
        }
    }
    fun cancelConfirmation() {
        // Revert to original values
        _pendingMobile.value = _originalMobile.value
        _pendingSchool.value = _originalSchool.value
        _selectedSchool.value = _originalSchool.value
        _showConfirmationDialog.value = false

        // Update UI
        _error.value = "Update cancelled"
    }
    fun checkExistingSchoolMapping() {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "🔍 Checking existing school mapping")

            when (val result = userSchoolRepository.getCurrentSchoolMapping()) {
                is Result.Success -> {
                    if (result.data != null) {
                        _currentSchoolMapping.value = result.data
                        _hasExistingSchool.value = true

                        Log.d("ProfileViewModel", "✅ Found existing school: ${result.data.schoolName}")

                        // Create school object
                        val school = School(
                            id = result.data.schoolId,
                            name = result.data.schoolName,
                            provinceId = result.data.provinceId,
                            provinceName = null,
                            locationId = null,
                            schoolType = result.data.schoolType
                        )

                        // Set selected school
                        _selectedSchool.value = school

                        // Find and set the province
                        result.data.provinceId?.let { provinceId ->
                            val province = _provinces.value.find { it.id == provinceId }
                            if (province != null) {
                                _selectedProvince.value = province
                                Log.d("ProfileViewModel", "✅ Set province: ${province.name}")
                            }
                        }
                    } else {
                        _hasExistingSchool.value = false
                        Log.d("ProfileViewModel", "ℹ️ No existing school found")
                    }
                }
                is Result.Error -> {
                    _error.value = "Failed to check school status"
                    _hasExistingSchool.value = false
                    Log.e("ProfileViewModel", "❌ Error checking school: ${result.exception.message}")
                }
            }
        }
    }

    // In ProfileViewModel.kt - modify selectProvince()
    fun selectProvince(province: Province, shouldClearSchool: Boolean = false) {
        Log.d("ProfileViewModel", "📍 selectProvince: ${province.name}, clearSchool: $shouldClearSchool")

        // ✅ Don't clear if we already have a school and it matches this province
        val currentSchool = _selectedSchool.value
        if (currentSchool != null && currentSchool.provinceId == province.id) {
            Log.d("ProfileViewModel", "✅ Keeping school because it matches province: ${currentSchool.name}")
            _selectedProvince.value = province
            return
        }

        _selectedProvince.value = province

        if (shouldClearSchool) {
            Log.d("ProfileViewModel", "🗑️ Clearing school selection")
            _selectedSchool.value = null
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
                }
                is Result.Error -> {
                    _error.value = "Search failed: ${result.exception.message}"
                }
            }

            _isSearching.value = false
        }
    }

    // In ProfileViewModel.kt
    fun selectSchool(school: School) {
        Log.d("ProfileViewModel", "📝 selectSchool called with: ${school.name} (ID: ${school.id})")

        // Set the school FIRST
        _selectedSchool.value = school

        // Verify it was set
        Log.d("ProfileViewModel", "✅ After setting school: ${_selectedSchool.value?.name}")

        _isSearchActive.value = false
        _schools.value = emptyList()

        Log.d("ProfileViewModel", "✅ Final check - school is: ${_selectedSchool.value?.name}")
        Log.d("ProfileViewModel", "✅ School selected: ${school.name} (waiting for submit)")
    }

    fun submitSchoolSelection() {
        viewModelScope.launch {
            val school = _selectedSchool.value ?: return@launch

            _isLoading.value = true
            _error.value = null

            if (_hasExistingSchool.value && _currentSchoolMapping.value != null) {
                // Update existing mapping
                val mapping = _currentSchoolMapping.value!!
                when (val result = userSchoolRepository.updateSchoolMapping(mapping.mappingId, school.id)) {
                    is Result.Success -> {
                        _currentSchoolMapping.value = result.data
                        _updateSuccess.value = true
                        _profileComplete.value = true
                        Log.d("ProfileViewModel", "✅ School updated to: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to update school"
                    }
                }
            } else {
                // First time assignment
                when (val result = userSchoolRepository.assignSchool(school.id)) {
                    is Result.Success -> {
                        refreshCurrentMapping()
                        _updateSuccess.value = true
                        _profileComplete.value = true
                        Log.d("ProfileViewModel", "✅ New school assigned: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to assign school"
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
                // Update existing mapping
                val mapping = _currentSchoolMapping.value!!
                when (val result = userSchoolRepository.updateSchoolMapping(mapping.mappingId, school.id)) {
                    is Result.Success -> {
                        _currentSchoolMapping.value = result.data
                        _updateSuccess.value = true
                        Log.d("ProfileViewModel", "✅ School updated to: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to update school"
                        Log.e("ProfileViewModel", "❌ Update failed: ${result.exception.message}")
                    }
                }
            } else {
                // First time assignment
                when (val result = userSchoolRepository.assignSchool(school.id)) {
                    is Result.Success -> {
                        // Refresh to get the mapping
                        refreshCurrentMapping()
                        _updateSuccess.value = true
                        Log.d("ProfileViewModel", "✅ New school assigned: ${school.name}")
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message ?: "Failed to assign school"
                        Log.e("ProfileViewModel", "❌ Assign failed: ${result.exception.message}")
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
                    Log.e("ProfileViewModel", "Failed to refresh mapping")
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
            Log.d("ProfileViewModel", "⚠️ Mobile is empty - skipping update")
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
                Log.d("ProfileViewModel", "✅ Mobile updated: $mobile")
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to update mobile"
            }

            _isLoading.value = false
        }
    }

    fun submitProfile() {
        Log.e("ProfileViewModel", "📞 submitProfile() called")

        // Just show success message - school is already saved when selected
        _profileComplete.value = true
    }

    fun refreshUserProfile() {
        viewModelScope.launch {
            val result = authRepository.refreshUserProfile()
            result.onFailure { throwable ->
                _error.value = "Failed to refresh profile: ${throwable.message}"
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