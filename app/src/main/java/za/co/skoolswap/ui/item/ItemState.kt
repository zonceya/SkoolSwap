package za.co.skoolswap.ui.item

import za.co.skoolswap.domain.model.Item

sealed class ItemState {
    data class Success(val item: Item) : ItemState()
    data class Error(val message: String) : ItemState()
}