package com.example.skoolswap.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FavoriteRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepositoryInterface
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Item>>(emptyList())
    val favorites: StateFlow<List<Item>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFavorites()
    }

    fun refreshFavorites() {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                favoriteRepository.getAllFavorites().collect { items ->
                    _favorites.value = items
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load favorites"
                _isLoading.value = false
            }
        }
    }
}