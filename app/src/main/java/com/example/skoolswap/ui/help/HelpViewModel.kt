// com/example/skoolswap/ui/help/HelpViewModel.kt
package com.example.skoolswap.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.User
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val authRepository: AuthRepositoryInterface
) : ViewModel() {

    // ✅ Use existing User from AuthRepository
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            // Get user from AuthRepository's cached user
            authRepository.getServerUser().collect { user ->
                _user.value = user
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}