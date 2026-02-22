package com.example.skoolswap.ui.item

import com.example.skoolswap.domain.model.Item

sealed class ItemState {
    data class Success(val item: Item) : ItemState()
    data class Error(val message: String) : ItemState()
}