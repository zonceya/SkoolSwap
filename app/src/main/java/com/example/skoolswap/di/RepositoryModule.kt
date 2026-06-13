package com.example.skoolswap.di

import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.data.repository.ProductsRepository
import com.example.skoolswap.data.repository.ReferenceDataRepository
import com.example.skoolswap.data.repository.UserSchoolRepository
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
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
    fun bindReferenceDataRepository(impl: ReferenceDataRepository): ReferenceDataRepositoryInterface
    @Binds
    @Singleton
    abstract fun bindUserSchoolRepository(impl: UserSchoolRepository): UserSchoolRepositoryInterface
    @Binds
    @Singleton
    abstract fun bindProductsRepository(
        productsRepository: ProductsRepository
    ): ProductsRepositoryInterface
}