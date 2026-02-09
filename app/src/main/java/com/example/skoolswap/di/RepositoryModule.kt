// di/RepositoryModule.kt
package com.example.skoolswap.di

import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.data.repository.ItemTypeRepository
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemTypeRepositoryInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindAuthRepository(authRepository: AuthRepository): AuthRepositoryInterface

    @Binds
    @Singleton
    fun bindItemTypeRepository(impl: ItemTypeRepository): ItemTypeRepositoryInterface
}