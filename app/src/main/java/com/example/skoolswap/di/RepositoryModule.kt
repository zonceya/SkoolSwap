package com.example.skoolswap.di

import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
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
}