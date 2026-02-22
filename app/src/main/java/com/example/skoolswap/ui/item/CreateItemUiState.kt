package com.example.skoolswap.ui.item

sealed class CreateItemUiState {
    object Idle : CreateItemUiState()
    object Loading : CreateItemUiState()
    data class Success(val message: String) : CreateItemUiState()
    data class Error(val message: String) : CreateItemUiState()
}