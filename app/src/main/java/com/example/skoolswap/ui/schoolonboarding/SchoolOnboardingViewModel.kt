package com.example.skoolswap.ui.schoolonboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.SchoolRepository
import com.example.skoolswap.data.repository.UserSchoolRepository
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.domain.model.School
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchoolOnboardingViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val userSchoolRepository: UserSchoolRepository,
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingSchools = MutableStateFlow(false)
    val isLoadingSchools: StateFlow<Boolean> = _isLoadingSchools.asStateFlow()

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

    // School data - cached for local search
    private val _cachedSchools = MutableStateFlow<List<School>>(emptyList())
    val cachedSchools: StateFlow<List<School>> = _cachedSchools.asStateFlow()

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools.asStateFlow()

    private val _selectedSchool = MutableStateFlow<School?>(null)
    val selectedSchool: StateFlow<School?> = _selectedSchool.asStateFlow()

    init {
        loadProvinces()
    }

    fun loadProvinces() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Check if user is authenticated first
            val token = authRepository.getCurrentToken()
            if (token.isNullOrEmpty()) {
                _error.value = "Please log in to continue"
                _isLoading.value = false
                _profileComplete.value = true  // Trigger navigation to login
                return@launch
            }

            when (val result = schoolRepository.getProvinces()) {
                is Result.Success -> {
                    _provinces.value = result.data
                    Log.d("SchoolOnboardingVM", "✅ Loaded ${result.data.size} provinces")
                }
                is Result.Error -> {
                    val errorMsg = when {
                        result.exception.message?.contains("401") == true -> "Session expired. Please log in again."
                        else -> "Failed to load provinces: ${result.exception.message}"
                    }
                    _error.value = errorMsg
                    Log.e("SchoolOnboardingVM", "❌ Error loading provinces", result.exception)
                }
            }

            _isLoading.value = false
        }
    }

    fun selectProvince(province: Province, shouldClearSchool: Boolean = false) {
        Log.d("SchoolOnboardingVM", "📍 selectProvince: ${province.name}")

        _selectedProvince.value = province

        if (shouldClearSchool) {
            _selectedSchool.value = null
            _schools.value = emptyList()
            _cachedSchools.value = emptyList()
        }
    }

    fun loadAllSchoolsForProvince(province: Province) {
        viewModelScope.launch {
            _isLoadingSchools.value = true
            Log.d("SchoolOnboardingVM", "Loading schools for province: ${province.name}")

            // Use existing searchSchools with empty string to get all schools
            when (val result = schoolRepository.searchSchools(province.id, "")) {
                is Result.Success -> {
                    _cachedSchools.value = result.data
                    Log.d("SchoolOnboardingVM", "✅ Loaded ${_cachedSchools.value.size} schools for ${province.name}")
                }
                is Result.Error -> {
                    _error.value = "Failed to load schools: ${result.exception.message}"
                    Log.e("SchoolOnboardingVM", "❌ Error loading schools", result.exception)
                }
            }

            _isLoadingSchools.value = false
        }
    }

    fun searchSchoolsLocally(query: String) {
        if (query.length < 2) {
            _schools.value = emptyList()
            return
        }

        val results = _cachedSchools.value.filter { school ->
            school.name.contains(query, ignoreCase = true)
        }

        _schools.value = results
        Log.d("SchoolOnboardingVM", "🔍 Local search found ${results.size} results for '$query'")
    }

    fun selectSchool(school: School) {
        Log.d("SchoolOnboardingVM", "📝 School selected: ${school.name}")
        _selectedSchool.value = school
        _schools.value = emptyList() // Clear search results
    }

    fun submitSchoolSelection() {
        viewModelScope.launch {
            val school = _selectedSchool.value
            if (school == null) {
                _error.value = "Please select a school first"
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            Log.d("SchoolOnboardingVM", "Submitting school: ${school.name}")

            when (val result = userSchoolRepository.assignSchool(school.id)) {
                is Result.Success -> {
                    _updateSuccess.value = true
                    _profileComplete.value = true
                    Log.d("SchoolOnboardingVM", "✅ School assigned successfully: ${school.name}")
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Failed to assign school"
                    Log.e("SchoolOnboardingVM", "❌ Failed to assign school", result.exception)
                }
            }

            _isLoading.value = false
        }
    }

    fun clearSchoolSelection() {
        _selectedSchool.value = null
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