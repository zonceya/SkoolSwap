package za.co.skoolswap.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.FavoriteRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepositoryInterface,
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Item>>(emptyList())
    val favorites: StateFlow<List<Item>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadFavorites()
    }

    fun refreshFavorites() {
        loadFavorites()
    }

    private fun loadFavorites() {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userId = authRepository.getCurrentUserId()

                if (userId == null) {
                    Timber.tag(LogTags.VIEW_MODEL).e("No user logged in")
                    _favorites.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                Timber.tag(LogTags.VIEW_MODEL).d("Loading favorites for user: $userId")

                favoriteRepository.getAllFavorites(userId).collect { items ->
                    _favorites.value = items
                    _isLoading.value = false
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${items.size} favorites")
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.VIEW_MODEL).e(e, "Failed to load favorites")
                _error.value = e.message ?: "Failed to load favorites"
                _isLoading.value = false
            }
        }
    }
}