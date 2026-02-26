package com.example.skoolswap.ui.item.edititem

sealed class EditItemUiState {
    object Idle : EditItemUiState()
    object Loading : EditItemUiState()
    data class Success(val message: String) : EditItemUiState()
    data class Error(val message: String) : EditItemUiState()
}