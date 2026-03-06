package com.example.skoolswap.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.SchoolRepository
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
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface,
    private val schoolRepository: SchoolRepository
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

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    init {
        loadProvinces()
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

    fun selectProvince(province: Province) {
        _selectedProvince.value = province
        _selectedSchool.value = null
        _schools.value = emptyList()
        _isSearchActive.value = true
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

    fun selectSchool(school: School) {
        _selectedSchool.value = school
        _isSearchActive.value = false
        _schools.value = emptyList()
        checkProfileComplete()
    }

    fun clearSearch() {
        _isSearchActive.value = false
        _schools.value = emptyList()
    }

    fun updateMobile(mobile: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false

            val result = authRepository.updateMobile(mobile)

            result.onSuccess {
                _updateSuccess.value = true
                refreshUserProfile()
                checkProfileComplete()
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to update mobile"
            }

            _isLoading.value = false
        }
    }

    fun completeProfile() {
        Log.e("ProfileViewModel", "📞 completeProfile() called")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val school = _selectedSchool.value
            if (school == null) {
                _error.value = "Please select a school"
                _isLoading.value = false
                return@launch
            }

            val result = authRepository.assignSchool(school.id)

            result.onSuccess {
                _profileComplete.value = true
                Log.e("ProfileViewModel", "✅ profileComplete set to true")
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to complete profile"
            }

            _isLoading.value = false
        }
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
        // Profile is complete when school is selected
        _profileComplete.value = _selectedSchool.value != null
    }

    fun clearError() {
        _error.value = null
    }
    init {
        Log.e("ProfileViewModel", "🏁 INIT - profileComplete: ${profileComplete.value}")
    }

    fun clearSuccess() {
        _updateSuccess.value = false
    }

    fun resetNavigation() {
        _profileComplete.value = false
    }
}