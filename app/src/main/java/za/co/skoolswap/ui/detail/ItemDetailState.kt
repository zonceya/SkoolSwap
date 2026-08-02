package za.co.skoolswap.ui.detail

import za.co.skoolswap.domain.model.Item

sealed class ItemDetailState {
    object Loading : ItemDetailState()
    data class Success(val item: Item) : ItemDetailState()
    data class Error(val message: String) : ItemDetailState()
}