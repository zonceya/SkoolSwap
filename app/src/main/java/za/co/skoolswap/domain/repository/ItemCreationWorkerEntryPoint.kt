package za.co.skoolswap.domain.repository

import za.co.skoolswap.data.local.database.dao.ItemDao
import za.co.skoolswap.data.local.database.dao.ItemImageDao
import za.co.skoolswap.data.repository.ImageUploadRepository
import za.co.skoolswap.data.repository.ItemRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// You need to update your EntryPoint
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ItemCreationWorkerEntryPoint {
    fun itemDao(): ItemDao
    fun itemImageDao(): ItemImageDao
    fun itemRepository(): ItemRepository
    fun imageUploadRepository(): ImageUploadRepository
}