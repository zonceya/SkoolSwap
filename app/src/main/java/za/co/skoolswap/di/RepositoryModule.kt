package za.co.skoolswap.di

import za.co.skoolswap.data.repository.AuthRepository
import za.co.skoolswap.data.repository.ProductsRepository
import za.co.skoolswap.data.repository.ReferenceDataRepository
import za.co.skoolswap.data.repository.UserSchoolRepository
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.ProductsRepositoryInterface
import za.co.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import za.co.skoolswap.domain.repository.UserSchoolRepositoryInterface
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