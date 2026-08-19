package za.co.skoolswap.ui.schoolonboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.repository.SchoolRepository
import za.co.skoolswap.data.repository.UserSchoolRepository
import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.domain.model.School
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
class SchoolOnboardingViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val userSchoolRepository: UserSchoolRepository,
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    companion object {
        private const val MIN_SEARCH_LENGTH = 2
    }

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

            val token = authRepository.getCurrentToken()
            if (token.isNullOrEmpty()) {
                _error.value = "Please log in to continue"
                _isLoading.value = false
                _profileComplete.value = true
                return@launch
            }

            when (val result = schoolRepository.getProvinces()) {
                is Result.Success -> {
                    _provinces.value = result.data
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Loaded ${result.data.size} provinces")
                }
                is Result.Error -> {
                    val errorMsg = when {
                        result.exception.message?.contains("401") == true -> "Session expired. Please log in again."
                        else -> "Failed to load provinces: ${result.exception.message}"
                    }
                    _error.value = errorMsg
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Error loading provinces")
                }
            }

            _isLoading.value = false
        }
    }

    fun selectProvince(province: Province, shouldClearSchool: Boolean = false) {
        Timber.tag(LogTags.VIEW_MODEL).d("📍 selectProvince: ${province.name}")

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
            Timber.tag(LogTags.VIEW_MODEL).d("Loading schools for province: ${province.name}")

            when (val result = schoolRepository.searchSchools(province.id, "")) {
                is Result.Success -> {
                    _cachedSchools.value = result.data
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Loaded ${_cachedSchools.value.size} schools for ${province.name}")
                }
                is Result.Error -> {
                    _error.value = "Failed to load schools: ${result.exception.message}"
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Error loading schools")
                }
            }

            _isLoadingSchools.value = false
        }
    }

    fun searchSchools(query: String) {
        if (query.length < MIN_SEARCH_LENGTH) {
            _schools.value = emptyList()
            return
        }

        viewModelScope.launch {
            val provinceId = _selectedProvince.value?.id ?: return@launch
            when (val result = schoolRepository.searchSchools(provinceId, query)) {
                is Result.Success -> {
                    _schools.value = result.data
                    Timber.tag(LogTags.VIEW_MODEL).d("🔍 API search found ${result.data.size} results for '$query'")
                }
                is Result.Error -> {
                    _error.value = "Search failed: ${result.exception.message}"
                }
            }
        }
    }

    fun selectSchool(school: School) {
        Timber.tag(LogTags.VIEW_MODEL).d("📝 School selected: ${school.name}")
        _selectedSchool.value = school
        _schools.value = emptyList()
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

            Timber.tag(LogTags.VIEW_MODEL).d("Submitting school: ${school.name}")

            when (val result = userSchoolRepository.assignSchool(school.id)) {
                is Result.Success -> {
                    // ✅ Sync the local cache (Room + AppPreferences) so IntroFragment's
                    // fast-path check sees schoolMapped = true on next launch
                    val refreshResult = authRepository.refreshUserProfile()
                    if (refreshResult.isFailure) {
                        Timber.tag(LogTags.VIEW_MODEL)
                            .w("⚠️ School assigned on server but local cache refresh failed: ${refreshResult.exceptionOrNull()?.message}")
                        // Don't block success on this — server is source of truth,
                        // and IntroFragment's fallback (restoreSession) will still catch it next launch.
                    }

                    _updateSuccess.value = true
                    _profileComplete.value = true
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ School assigned successfully: ${school.name}")
                }
                is Result.Error -> {
                    val errorMsg = result.exception.message ?: "Failed to assign school"
                    _error.value = errorMsg
                    Timber.tag(LogTags.VIEW_MODEL).e(result.exception, "❌ Failed to assign school")
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