package com.example.skoolswap.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.remote.api.RetrofitClient
import com.example.skoolswap.data.remote.api.UserApiService
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ❌ REMOVE THIS - Hilt provides Context automatically
    // @Provides
    // @Singleton
    // fun provideContext(@ApplicationContext context: Context): Context {
    //     return context.applicationContext
    // }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkoolSwapDatabase {
        return SkoolSwapDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserApiService(): UserApiService {
        return RetrofitClient.instance.create(UserApiService::class.java)
    }

    // ✅ ADD THIS: Provide CredentialManager properly
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    // ✅ ADD THIS: Provide FirebaseAuth instance
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}