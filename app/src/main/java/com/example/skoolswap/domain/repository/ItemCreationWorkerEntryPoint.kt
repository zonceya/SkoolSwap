package com.example.skoolswap.domain.repository

import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.dao.ItemImageDao
import com.example.skoolswap.data.repository.ImageUploadRepository
import com.example.skoolswap.data.repository.ItemRepository
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