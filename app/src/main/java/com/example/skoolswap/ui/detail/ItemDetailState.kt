package com.example.skoolswap.ui.detail

import com.example.skoolswap.domain.model.Item

sealed class ItemDetailState {
    object Loading : ItemDetailState()
    data class Success(val item: Item) : ItemDetailState()
    data class Error(val message: String) : ItemDetailState()
}